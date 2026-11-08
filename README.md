# Jarvis Assistant (Android)

A voice assistant for Android. Say the wake word (default: "jarvis") followed by a command.

## Build and run
1. Install Android Studio and open the `JarvisAssistant` folder.
2. Let Gradle sync (Android Studio will create the Gradle wrapper and may offer version upgrades; accept them).
3. Connect your phone (USB debugging on) and press Run.
4. Open the app, optionally change the wake word, tap **Start listening**.
5. Allow every permission, and when asked, turn on **Display over other apps** for Jarvis, then tap Start again.

The phone needs the Google app / Google speech services for voice recognition, and Spotify installed for music.

## Make it the default assistant
Tap **Set as default assistant** in the app, or go to Settings > Apps > Default apps > Digital assistant app, choose **Jarvis Assistant** and confirm. After that, the power-button or home-button long press (or your phone's assistant gesture) wakes Jarvis straight away and it says "Yes?" so you can speak a command without the wake word. Menu names vary a little between phone makers.

## Example commands
Say "jarvis" first, e.g. "jarvis play Believer on Spotify".

| Area | Say |
|---|---|
| Calls | "answer the call", "cut the call", "call Mom" |
| Chaining | "answer the call and then play Believer on Spotify" |
| Music | "play <song or artist>", "pause", "resume", "next", "previous", "volume up/down" |
| Apps | "open WhatsApp", "open Maps" |
| Messaging | "send message to Raj saying I will be late", "whatsapp Raj saying hello" |
| Time | "what time is it", "what's the date", "set a timer for 5 minutes", "set an alarm for 7:30 am" |
| Tools | "turn on the flashlight", "weather" |
| Questions | "who is Elon Musk", "what is photosynthesis" |

While a call is ringing or active, "answer" and "cut the call" work without the wake word.

## Good to know
- Replies are spoken with audio ducking, so Spotify gets quieter while Jarvis talks and then returns to normal; it is not stopped.
- Continuous listening can cause a brief dip or beep on some phones when the recognizer restarts. This is a limit of Android's built-in speech recognizer.
- Wake word detection uses the speech recognizer, so pick a name it hears reliably (it may mishear unusual names).
- Ending calls needs Android 9 or newer. Answering needs Android 8 or newer.
- WhatsApp opens the chat with your message filled in; you tap send. Change `defaultCountryCode` in `ContactsHelper.kt` (default 91) for your country.
- Some phone makers (Xiaomi, Oppo, Vivo, Samsung) kill background apps. Set battery usage for Jarvis to "Unrestricted".
- Unknown commands fall back to a web search.

## Build the APK on GitHub (no Android Studio needed)
1. Create a repository and upload this whole folder (keep the folder structure; the hidden `.github` folder must be included).
2. Open the **Actions** tab, run **Build APK**, and download `jarvis-debug-apk` from the run's Artifacts.
3. Unzip, copy `app-debug.apk` to your phone and install it.
