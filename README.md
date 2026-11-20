# Phone-to-car trip handoff

Transfer a trip from a phone to a simulated car display and keep its order and destination intact across retries.

## First acceptance test

Send a trip with two stops, drop the acknowledgement, and resend the same trip revision. The car display should show exactly one trip with both stops in order and the phone should eventually mark it delivered.

The Android screen presents both endpoints and offers a button to drop an
acknowledgement after delivery. Send / Retry confirms that same revision.
Save a new destination to create a higher revision. The receiver keeps one
trip, rejects older revisions, and rejects reused revisions with changed
content. This demo models the transport in-process and restores both endpoints
through Android's saved instance state.

Use JDK 17 or 21 and Android SDK Platform 35 with Build Tools 34.0.0. Run
`gradlew.bat :app:testDebugUnitTest :app:assembleDebug` (Windows) or
`./gradlew :app:testDebugUnitTest :app:assembleDebug` (macOS/Linux).
The APK supports API 31 and newer. JVM tests cover lost acknowledgements,
retry, newer and stale revisions, conflicting payloads, and invalid trips.
