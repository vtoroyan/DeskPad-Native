# DeskPad Android build package

This archive contains the original DeskPad 0.2-a11y source project plus a GitHub Actions workflow.

## Important
This workflow builds the **original 0.2-a11y app** for a baseline test. It does NOT remove AccessibilityService or implement the planned Native 0.3 input architecture.

## Upload to GitHub
1. Extract this ZIP.
2. In `vtoroyan/DeskPad-Native`, upload the contents of the `deskpad-app-0.2-a11y` folder to the repository root, preserving `.github/workflows/android.yml`.
3. Commit the upload to `main`.
4. Open the repository's Actions tab. `Build Android APK` should start after the push.
5. Open the completed workflow run and download the `deskpad-debug-apk` artifact.
6. Extract the artifact ZIP and install `app-debug.apk` on the Pixel 8.

## What this tests
It confirms the original source project can be built by GitHub Actions and installed. It does not solve the banking-app conflict. Native 0.3 requires further code changes and device testing.
