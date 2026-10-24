use std::future::Future;
use std::pin::Pin;
use std::sync::atomic::{AtomicI64, Ordering};
use std::task::{Context, Poll};

use jni::JavaVM;
use jni::objects::{JClass, JObject, JObjectArray, JString, JValue};
use jni::refs::Global;
use jni::{jni_sig, jni_str};
use solidcraft_ui_egui::{FilePickerFuture, Services};
use std::sync::Mutex;

static NEXT_REQUEST_ID: AtomicI64 = AtomicI64::new(1);
static ACTIVITY_CLASS: Mutex<Option<Global<JClass<'static>>>> = Mutex::new(None);

struct Picker {
    id: i64,
    save: bool,
    name: String,
    extensions: Vec<String>,
    requested: bool,
}

impl Future for Picker {
    type Output = Option<String>;

    fn poll(mut self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Self::Output> {
        if !self.requested {
            if let Err(error) = request_document(self.id, self.save, &self.name, &self.extensions) {
                log::error!("Could not start Android document picker: {error}");
                return Poll::Ready(None);
            }
            self.requested = true;
        }
        match take_document_result(self.id) {
            Ok(Some(path)) if path.is_empty() => Poll::Ready(None),
            Ok(Some(path)) => Poll::Ready(Some(path)),
            Ok(None) => Poll::Pending,
            Err(error) => {
                log::error!("Could not read Android document picker result: {error}");
                Poll::Ready(None)
            }
        }
    }
}

fn picker(save: bool, name: &str, extensions: &[&str]) -> FilePickerFuture {
    Box::pin(Picker {
        id: NEXT_REQUEST_ID.fetch_add(1, Ordering::Relaxed),
        save,
        name: name.to_owned(),
        extensions: extensions.iter().map(|extension| (*extension).to_owned()).collect(),
        requested: false,
    })
}

pub(super) fn services() -> Services {
    Services {
        pick_open: Some(Box::new(|extensions| picker(false, "", extensions))),
        pick_save: Some(Box::new(|name, extensions| picker(true, name, extensions))),
        persist_document_save: Some(std::sync::Arc::new(persist_document_save)),
        ..Default::default()
    }
}

fn request_document(id: i64, save: bool, name: &str, extensions: &[String]) -> Result<(), String> {
    with_jni(|env| {
        let class = activity_class(env)?;
        let name = env.new_string(name)?;
        let name_object: JObject<'_> = name.into();
        let empty = env.new_string("")?;
        let extension_array = JObjectArray::<JString>::new(env, extensions.len(), &empty)?;
        for (index, extension) in extensions.iter().enumerate() {
            let extension = env.new_string(extension)?;
            extension_array.set_element(env, index, &extension)?;
        }
        let extension_array_object: &JObject<'_> = extension_array.as_ref();
        env.call_static_method(
            class,
            jni_str!("requestDocument"),
            jni_sig!("(JZLjava/lang/String;[Ljava/lang/String;)V"),
            &[JValue::Long(id), JValue::Bool(save), JValue::Object(&name_object), JValue::Object(extension_array_object)],
        )?;
        Ok(())
    })
}

fn take_document_result(id: i64) -> Result<Option<String>, String> {
    with_jni(|env| {
        let class = activity_class(env)?;
        let result = env.call_static_method(class, jni_str!("takeDocumentResult"), jni_sig!("(J)Ljava/lang/String;"), &[JValue::Long(id)])?.l()?;
        if result.is_null() {
            return Ok(None);
        }
        let value = JString::cast_local(env, result)?;
        Ok(Some(value.try_to_string(env)?))
    })
}

fn persist_document_save(path: &str) -> Result<(), String> {
    let message = with_jni(|env| {
        let class = activity_class(env)?;
        let path = env.new_string(path)?;
        let path_object: JObject<'_> = path.into();
        let result = env
            .call_static_method(
                class,
                jni_str!("writeBackDocument"),
                jni_sig!("(Ljava/lang/String;)Ljava/lang/String;"),
                &[JValue::Object(&path_object)],
            )?
            .l()?;
        if result.is_null() {
            return Ok("Android did not return a document save result".to_string());
        }
        let value = JString::cast_local(env, result)?;
        value.try_to_string(env)
    })?;
    if message.is_empty() { Ok(()) } else { Err(message) }
}

fn activity_class<'local>(env: &mut jni::Env<'local>) -> jni::errors::Result<JClass<'local>> {
    let mut cached = ACTIVITY_CLASS.lock().unwrap_or_else(std::sync::PoisonError::into_inner);
    if cached.is_none() {
        let class = env.load_class(jni_str!("org.solidcraft.mobile.MainActivity"))?;
        *cached = Some(env.new_global_ref(class)?);
    }
    let Some(class) = cached.as_ref() else {
        return Err(jni::errors::Error::NullPtr("Android activity class was not cached"));
    };
    env.new_cast_local_ref::<JClass<'local>>(class.as_obj())
}

fn with_jni<T>(call: impl FnOnce(&mut jni::Env<'_>) -> jni::errors::Result<T>) -> Result<T, String> {
    JavaVM::singleton().map_err(jni_error)?.attach_current_thread(call).map_err(jni_error)
}

fn jni_error(error: impl std::fmt::Display) -> String {
    error.to_string()
}
