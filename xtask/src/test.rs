//! `cargo xtask test`: build the workspace's tests once, then run test binaries with bounded
//! parallelism. Running every binary at once can exhaust memory and GPU resources; doc tests
//! and any other `side` jobs run alongside on their own thread.

use std::process::{Command, Stdio};
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::mpsc;
use std::time::Instant;

use crate::{cargo, root};

/// A test executable from `cargo test --no-run`.
struct TestBin {
    label: String,
    exe: String,
    dir: String,
}

/// Build the tests with `cargo_args` (e.g. `--locked`), run each binary with `test_args`, and run
/// the `side` jobs one after another meanwhile. Each prints a line when it ends; a failure prints
/// its whole output.
pub fn run(cargo_args: &[&str], test_args: &[&str], side: Vec<(String, Command)>) -> Result<(), String> {
    let bins = build(cargo_args)?;
    let start = Instant::now();
    let (tx, rx) = mpsc::channel::<(String, Result<String, String>, f64)>();
    let mut failed = Vec::new();
    let next = AtomicUsize::new(0);
    std::thread::scope(|scope| {
        // The workspace contains GPU, geometry, and UI test binaries that each use significant
        // memory. Keep a small fixed number of processes active instead of spawning all 79 at
        // once. Each Rust test binary also gets a bounded internal worker count.
        let workers = env_limit("SOLIDCRAFT_TEST_WORKERS", 2).min(bins.len());
        let test_threads = env_limit("SOLIDCRAFT_TEST_THREADS", 2).to_string();
        for _ in 0..workers {
            let tx = tx.clone();
            let next = &next;
            let bins = &bins;
            let test_threads = test_threads.clone();
            scope.spawn(move || {
                loop {
                    let index = next.fetch_add(1, Ordering::Relaxed);
                    let Some(b) = bins.get(index) else { break };
                    let t0 = Instant::now();
                    let mut c = Command::new(&b.exe);
                    c.args(test_args).current_dir(&b.dir).env("CARGO_MANIFEST_DIR", &b.dir).env("RUST_TEST_THREADS", &test_threads);
                    let _ = tx.send((b.label.clone(), finish(c, &b.label), t0.elapsed().as_secs_f64()));
                }
            });
        }
        let tx_side = tx.clone();
        scope.spawn(move || {
            for (label, c) in side {
                let t0 = Instant::now();
                let _ = tx_side.send((label.clone(), finish(c, &label), t0.elapsed().as_secs_f64()));
            }
        });
        drop(tx);
        for (label, result, secs) in rx {
            match result {
                Ok(summary) => println!("ok    {label:<44} {secs:6.1}s  {summary}"),
                Err(output) => {
                    println!("FAIL  {label:<44} {secs:6.1}s\n{output}");
                    failed.push(label);
                }
            }
        }
    });
    println!("test: {} binaries in {:.1}s", bins.len(), start.elapsed().as_secs_f64());
    if failed.is_empty() { Ok(()) } else { Err(format!("failed: {}", failed.join(", "))) }
}

/// Read an optional positive limit for test scheduling. Invalid and zero values use the default.
fn env_limit(name: &str, default: usize) -> usize {
    env_limit_from(std::env::var(name).ok().as_deref(), default)
}

fn env_limit_from(value: Option<&str>, default: usize) -> usize {
    value.and_then(|value| value.parse::<usize>().ok()).filter(|&value| value > 0).unwrap_or(default)
}

/// Run to the end: the last `test result:` line on success, all output on failure.
fn finish(mut c: Command, label: &str) -> Result<String, String> {
    if std::env::var_os("SOLIDCRAFT_TEST_NOCAPTURE").is_some() {
        c.stdin(Stdio::null()).stdout(Stdio::piped()).stderr(Stdio::piped());
        let mut child = c.spawn().map_err(|e| format!("failed to spawn: {e}"))?;
        let stdout = child.stdout.take().expect("piped stdout");
        let stderr = child.stderr.take().expect("piped stderr");
        let label_out = label.to_string();
        let out_thread = std::thread::spawn(move || {
            use std::io::{BufRead, BufReader};
            for line in BufReader::new(stdout).lines().map_while(Result::ok) {
                println!("[{label_out}] {line}");
            }
        });
        let label_err = label.to_string();
        let err_thread = std::thread::spawn(move || {
            use std::io::{BufRead, BufReader};
            for line in BufReader::new(stderr).lines().map_while(Result::ok) {
                eprintln!("[{label_err}] {line}");
            }
        });
        let status = child.wait().map_err(|e| format!("failed to wait: {e}"))?;
        let _ = out_thread.join();
        let _ = err_thread.join();
        if status.success() { Ok("output streamed; see test lines above".into()) } else { Err(format!("exited with {status}")) }
    } else {
        let out = c.stdin(Stdio::null()).output().map_err(|e| format!("failed to spawn: {e}"))?;
        let stdout = String::from_utf8_lossy(&out.stdout);
        if out.status.success() {
            Ok(stdout.lines().rev().find(|l| l.starts_with("test result:")).unwrap_or("").trim_start_matches("test result: ").to_string())
        } else {
            Err(format!("{stdout}{}\n({})", String::from_utf8_lossy(&out.stderr), out.status))
        }
    }
}

/// `cargo test --workspace --profile ci --no-run`, and the executables it built.
fn build(cargo_args: &[&str]) -> Result<Vec<TestBin>, String> {
    let mut c = cargo();
    c.args(["test", "--profile", "ci", "--no-run", "--message-format=json-render-diagnostics"]);
    if !has_package_selector(cargo_args) {
        c.arg("--workspace");
    }
    c.args(cargo_args);
    let scope = if has_package_selector(cargo_args) { format!("package-filtered {cargo_args:?}") } else { "workspace".into() };
    eprintln!("$ cargo test ({scope}) --profile ci --no-run");
    let out = c.stderr(Stdio::inherit()).output().map_err(|e| format!("cargo test --no-run: failed to spawn: {e}"))?;
    if !out.status.success() {
        return Err(format!("cargo test --no-run: exited with {}", out.status));
    }
    let mut bins: Vec<TestBin> = Vec::new();
    for line in String::from_utf8_lossy(&out.stdout).lines() {
        let Ok(m) = serde_json::from_str::<serde_json::Value>(line) else { continue };
        let Some(exe) = m["executable"].as_str().filter(|_| m["reason"] == "compiler-artifact" && m["profile"]["test"] == true) else {
            continue;
        };
        if bins.iter().any(|b| b.exe == exe) {
            continue;
        }
        let manifest = std::path::Path::new(m["manifest_path"].as_str().unwrap_or_default()).to_path_buf();
        let dir = manifest.parent().map(|d| d.to_path_buf()).unwrap_or_else(root);
        let pkg = dir.file_name().map(|n| n.to_string_lossy().into_owned()).unwrap_or_default();
        let kind = m["target"]["kind"][0].as_str().unwrap_or("");
        let label = format!("{pkg} {kind} {}", m["target"]["name"].as_str().unwrap_or(""));
        bins.push(TestBin { label, exe: exe.to_string(), dir: dir.to_string_lossy().into_owned() });
    }
    if bins.is_empty() {
        return Err("cargo test --no-run built no test executables".into());
    }
    Ok(bins)
}

/// The doc tests (`cargo test --doc`), to run as a side job.
pub fn doc_tests(cargo_args: &[&str], test_args: &[&str]) -> (String, Command) {
    let mut c = cargo();
    c.args(["test", "--workspace", "--profile", "ci", "--doc"]).args(cargo_args).arg("--").args(test_args);
    ("doc tests".into(), c)
}

/// Run doc tests only when no package/target selectors would conflict with `--doc`.
pub fn doc_tests_if_unfiltered(cargo_args: &[&str], test_args: &[&str]) -> Vec<(String, Command)> {
    if has_package_selector(cargo_args) || has_target_selector(cargo_args) { Vec::new() } else { vec![doc_tests(cargo_args, test_args)] }
}

fn has_package_selector(cargo_args: &[&str]) -> bool {
    let selectors = ["-p", "--package"];
    cargo_args.iter().any(|arg| selectors.contains(arg) || arg.starts_with("--package="))
}

fn has_target_selector(cargo_args: &[&str]) -> bool {
    let selectors = ["--workspace", "--all", "--lib", "--bin", "--test", "--example", "--bench", "--all-targets"];
    cargo_args.iter().any(|arg| selectors.contains(arg))
}

#[cfg(test)]
mod tests {
    use super::{doc_tests_if_unfiltered, env_limit_from, has_package_selector, has_target_selector};

    #[test]
    fn test_limit_uses_positive_environment_values_and_defaults_otherwise() {
        assert_eq!(env_limit_from(None, 2), 2);
        assert_eq!(env_limit_from(Some("0"), 2), 2);
        assert_eq!(env_limit_from(Some("invalid"), 2), 2);
        assert_eq!(env_limit_from(Some("1"), 2), 1);
    }

    #[test]
    fn package_filtered_tests_do_not_schedule_conflicting_workspace_doc_tests() {
        assert!(doc_tests_if_unfiltered(&["-p", "solidcraft-kernel"], &[]).is_empty());
        assert_eq!(doc_tests_if_unfiltered(&[], &[]).len(), 1);
    }

    #[test]
    fn package_and_target_selectors_are_detected_separately() {
        assert!(has_package_selector(&["-p", "solidcraft-kernel"]));
        assert!(has_package_selector(&["--package=solidcraft-kernel"]));
        assert!(has_target_selector(&["--lib"]));
        assert!(!has_package_selector(&["--features", "default"]));
    }
}
