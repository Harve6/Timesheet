# Releasing TradeHours on Google Play

Everything in the code is ready. These are the steps only you can do.

## 0. Decide the price first
A **free** app can never be changed to **paid** later. Paid can go to free, but not back. If you want $0.99, publish it as paid from day one.

## 1. Accounts (one time)
1. Create a Google Play Developer account at play.google.com/console (one-time fee, about $25, needs ID verification).
2. Set up a **merchant profile** in the Console. Required to sell paid apps; it is where payouts and tax info go.

## 2. Testing rule for new accounts
New personal developer accounts must run a **closed test with at least 12 testers for 14 days in a row** before they can apply for production access. Start this early: invite 12+ friends/coworkers by email in the Console and have them install. Check the Console for the current requirement, since Google changes it.

## 3. Create your signing key (one time - guard it!)
1. Android Studio: **Build > Generate Signed App Bundle / APK > Android App Bundle > Create new...**
2. Save the `.jks` file **outside** the project folder, pick strong passwords, and **back it up** (cloud + USB) along with the passwords. If you lose it you cannot update the app the normal way.
3. Create `keystore.properties` in the project root (it is git-ignored):
   ```
   storeFile=C:/path/to/your-key.jks
   storePassword=...
   keyAlias=...
   keyPassword=...
   ```
Never commit or share these. Google Play App Signing (on by default) then holds the final signing key.

## 4. Build the release
```
./gradlew bundleRelease
```
Upload `app/build/outputs/bundle/release/app-release.aab`. Each upload needs a higher `versionCode` in `app/build.gradle.kts`.

Test the release build first: in Android Studio pick the **release** build variant and run it on the emulator. Enter hours, copy, restart the app, and check that the data is still there (release builds are shrunk with R8).

## 5. Play Console listing
- Create app > Paid > price $0.99
- Text: copy from `listing.md`
- Icon: `icon-512.png`; feature graphic: `feature-graphic-1024x500.png`
- Screenshots: take from the emulator
- **Privacy policy URL:** required. Fill in the blanks in `privacy-policy.md` and host it somewhere public (for example GitHub Pages on the repo), then paste the link.
- **Data safety form:** no data collected, no data shared. (The app declares the notifications and run-at-startup permissions for reminders; neither collects data.)
- **Content rating** questionnaire: no violence, etc. Everyone.
- **Target audience:** 18+ (not for children).
- **Ads:** no.

## 6. Before each update
Bump `versionCode` (+1) and `versionName`, run `./gradlew testDebugUnitTest bundleRelease`, upload.
