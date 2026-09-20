# TradeHours

A dead-simple hours tracker for people who work with their hands. Put your hours and jobsite in for each day, tap **COPY**, and paste the week into a text to your boss, foreman, or whoever needs it. No accounts, no internet, no payroll clutter.

---

### How it works

1. Open the app. You're looking at this week, Sunday through Saturday.
2. Type the **hours** and the **jobsite** on each day. It saves as you type.
3. Tap **COPY**, open your texts, and paste.

That's it. What gets pasted looks like this:

```
Sun  
Mon  8  Main St Hospital
Tue  9.5  Main St Hospital +Travel +Park $10
Wed  8  Airport Job
Thu  8  Airport Job
Fri  6  Shop
Sat
```

### Features

* **One-screen week:** every day in front of you, with today highlighted and a running **total hours** at the top.
* **Autosave:** nothing to press, nothing to lose.
* **Travel and parking:** tap a day's label to mark travel paid or add a parking amount. They show up in the copied text as `+Travel` and `+Park $10`.
* **Jobsite suggestions:** sites you've typed before pop up as you type.
* **Browse any week:** use the arrows at the top to go back and fix a past week, or jump back to this week.
* **History:** week, month, and year totals, plus a copy button on every past week. Tap a week to open it.
* **Clear week:** wipes the week you're looking at (it asks first).
* **100% offline:** everything is stored on your phone. No internet permission at all.

### Coming later

* Settings, including choosing which day your week starts on (it's Sunday for now).

---

### Tech stack

* Kotlin, Jetpack Compose (Material 3)
* MVVM with Kotlin Coroutines and StateFlow
* Room database for local storage
* `minSdk` 26 (Android 8.0)

### Building

Open the project in Android Studio and press Run, or from a terminal:

```
./gradlew assembleDebug
```
