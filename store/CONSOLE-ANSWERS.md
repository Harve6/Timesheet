# Play Console answers for TradeHours

Ready-to-use answers for the forms in Play Console (Policy and programs > App content, and Store presence).
Everything here is true for the app as built. Change a line only if the app changes.

## Create app
- App name: **TradeHours**
- Default language: English (United States)
- App or game: **App**
- Free or paid: **Paid** (cannot be changed to free-then-paid later; see RELEASE.md)
- Declarations: accept the Developer Program Policies and US export laws boxes

## Store listing (Grow > Store presence > Main store listing)
- Short description, full description: `listing.md`
- App icon: `icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots: everything in `screenshots/`, in the order listed in `listing.md`
- Category: **Productivity**
- Contact email (public): choose one you are happy to show
- Privacy policy URL: the GitHub Pages link once it exists

## App content
| Form | Answer |
|---|---|
| Privacy policy | The public URL of `privacy-policy.md` |
| App access | All functionality is available without login or special access |
| Ads | **No**, the app does not contain ads |
| Content rating | Category **Utility / Productivity**. Answer **No** to everything (violence, sexual content, language, controlled substances, gambling, user-generated content, sharing location or personal info). Expected rating: Everyone |
| Target audience | **18 and over** only. Not designed for children |
| News app | No |
| COVID-19 contact tracing / status | No |
| Data safety | **No data collected, no data shared.** All data stays on the device. (The app has no internet permission.) |
| Government app | No |
| Financial features | None |
| Health apps | No |
| Advertising ID | **No**, the app does not use the advertising ID |

## Permissions the app declares
- `POST_NOTIFICATIONS` - so optional reminders can appear. No special declaration needed.
- `RECEIVE_BOOT_COMPLETED` - so reminders survive a phone restart. Normal permission, no declaration needed.
- No internet, no location, no contacts, no camera, no exact-alarm permission.

## Pricing and distribution
- Price: **$0.99**. Google converts to local currencies.
- Countries: pick the ones you want (start with all available, or just the US and Canada).
- Contains ads: No.

## Release
- Upload `app/build/outputs/bundle/release/app-release.aab` (built with your upload key; see RELEASE.md)
- Accept **Play App Signing** when asked
- First track: **Closed testing** (12+ testers for 14 days), then apply for production access
- Release notes: "First release of TradeHours: log your hours by day and jobsite, copy the week and paste it into a text."

## Message to send your 12 testers
> I made an Android app for tracking work hours (you put in hours and the jobsite each day, tap Copy, and paste it into a text to your boss). Google makes me test it with 12 people for 2 weeks before I can release it. Can you help? Send me the Gmail address you use on your phone and I will add you. You will get a link to install it. Just keep it installed for the 2 weeks and open it now and then. Thanks!
