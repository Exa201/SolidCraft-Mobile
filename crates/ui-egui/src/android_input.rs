//! Adapts Android system input events to the keyboard actions understood by SolidCraft's UI.

/// Route Android's system Back key through the existing Escape handling for dialogs and tools.
///
/// winit exposes `KEYCODE_BACK` as `egui::Key::BrowserBack`; the shared CAD UI uses Escape to
/// cancel a current tool, close a menu, or dismiss a dialog. This is called only by the Android
/// host, so physical browser-back keys on desktop retain their normal meaning.
pub fn map_system_back_to_escape(input: &mut egui::RawInput) -> usize {
    let mut mapped = 0;
    for event in &mut input.events {
        if let egui::Event::Key { key, .. } = event
            && *key == egui::Key::BrowserBack
        {
            *key = egui::Key::Escape;
            mapped += 1;
        }
    }
    mapped
}

#[cfg(test)]
mod tests {
    use super::*;
    use egui::{Event, Key, Modifiers, RawInput};

    fn key(key: Key, pressed: bool) -> Event {
        Event::Key { key, physical_key: None, pressed, repeat: false, modifiers: Modifiers::NONE }
    }

    #[test]
    fn android_system_back_press_and_release_map_to_escape() {
        let mut input = RawInput { events: vec![key(Key::BrowserBack, true), key(Key::BrowserBack, false)], ..Default::default() };

        assert_eq!(map_system_back_to_escape(&mut input), 2);
        assert_eq!(input.events, vec![key(Key::Escape, true), key(Key::Escape, false)]);
    }

    #[test]
    fn other_keys_and_non_key_events_are_preserved() {
        let events = vec![key(Key::A, true), Event::Text("solid".into()), key(Key::Escape, true)];
        let mut input = RawInput { events: events.clone(), ..Default::default() };

        assert_eq!(map_system_back_to_escape(&mut input), 0);
        assert_eq!(input.events, events);
    }
}
