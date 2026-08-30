# TradeHours ⏱️🏗️

A fast, no-nonsense, offline-first Android time tracker built specifically for construction tradespeople. 

Most time-tracking apps are bloated, require paid subscriptions, and rely on constant cloud connectivity. **TradeHours** cuts out the payroll clutter: workers just log where they were, how many hours they worked, and what they did. On Friday, it formats a clean, organized text summary ready to send straight to the boss, foreman, or shop steward.

---

### Key Features

* **⚡ Frictionless Daily Logging:** Big, high-contrast, glove-friendly controls (+/- 0.5h steps) to log site hours in seconds.
* **📍 Address & Site Autocomplete:** Remembers previous jobs, addresses, and numbers to minimize repetitive typing.
* **📴 100% Offline-First:** Fully functional on remote jobs, in basements, and inside mechanical rooms with zero internet dependency (powered by Room DB).
* **📱 1-Tap Text Export:** Instantly generates a clean, readable weekly breakdown to share directly via SMS or email.
* **🛠️ Built for the Trades:** Keeps worker tracking simple while leaving complex overtime/double-time splits to payroll.

---

### Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Architecture:** MVVM + Kotlin Coroutines & StateFlow
* **Local Persistence:** Android Jetpack Room Database
* **Adaptive Layouts:** Supports phones, tablets, and foldables.
