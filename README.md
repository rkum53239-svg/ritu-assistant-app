# Ritu Voice Assistant

Ritu Voice Assistant ek Android app hai jo background me microphone listen karta hai, wake word "ritu" ya "hey ritu" detect karta hai, aur Gemini se answer leta hai. App me settings bhi hain: API key, wake words, model, language, offline mode, TTS voice, system prompt, start/stop, permissions, aur live logs.

## Features
- Background foreground service
- Wake words: ritu, hey ritu
- Manual trigger: Jarvis
- SpeechRecognizer with hi-IN/en-IN
- Offline preference support
- Fallback when offline speech pack missing
- Gemini REST API with maxOutputTokens 300
- Android TTS fallback
- Accessibility automation helper
- Live logs

## Build steps
1. Android Studio install karo.
2. Repo ko clone karo.
3. Android Studio me open project karo.
4. Gradle Sync karo.
5. Phone ko USB se connect karo.
6. Run > Run app.
7. Debug APK generate karne ke liye GitHub Actions se artifact download karo.

## Run steps
1. App launch karo.
2. Gemini API key daalo.
3. Mic permission, accessibility, overlay, battery optimization setup karo.
4. Start assistant button press karo.
5. "Ritu" ya "Hey Ritu" bolke wake karo.
6. "Jarvis, <question>" bolke direct command do.
7. Follow-up 50 seconds ke andar bina wake-word ke bhi chalega.

## Important notes
- Default model: gemini-3.5-flash-lite
- Offline preference default ON
- Silence timeout 800 ms
- AI response Hinglish me 1-3 chhote sentences me dega

## GitHub Actions
`.github/workflows/debug-apk.yml` file debug APK build karta hai.
