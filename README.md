# WASSaver - WhatsApp Status Saver

A clean, modern Android app to view, save, and repost WhatsApp & WhatsApp Business statuses.

## Features

- **WhatsApp & WA Business support** — Switch between WA and WAB status folders
- **Media filters** — Filter by All, Photos, or Videos
- **Grid view** — Beautiful thumbnail grid with type badges
- **Full-screen viewer** — Swipe through statuses with image zoom and video playback
- **Save to gallery** — One-tap save to Pictures/WASSaver
- **Repost** — Direct repost to WhatsApp
- **Share** — Share to any app
- **Saved tab** — View all previously saved statuses
- **Media tab** — Browse WhatsApp's private media folders (images & videos)
- **Auto-save watcher** — Automatically saves new private media while app is open
- **Direct Message** — Send WhatsApp messages to any number without saving as contact
- **Dark/Light theme** — WhatsApp-inspired color palette with system theme support

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Coil for image loading & video thumbnails
- Media3/ExoPlayer for video playback
- Storage Access Framework (SAF) for Android 11+ compatibility
- MVVM architecture

## Requirements

- Android 8.0 (API 26) or higher
- WhatsApp / WhatsApp Business installed

## CI/CD — GitHub Actions

The project includes a GitHub Actions workflow (`.github/workflows/build.yml`) that automatically builds a signed release APK.

### Setup GitHub Secrets

Go to your repo → **Settings → Secrets and variables → Actions** and add these 4 secrets:

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Your keystore file encoded as base64 |
| `KEYSTORE_PASSWORD` | Keystore store password |
| `KEY_ALIAS` | Key alias name |
| `KEY_PASSWORD` | Key password |

### How to encode your keystore as base64

```bash
# On Linux/Mac:
base64 -i your-keystore.jks | tr -d '\n'

# On Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks"))
```

Copy the output and paste it as the `KEYSTORE_BASE64` secret.

### How it works

- **On every push to `main`** — Builds signed APK, uploads as artifact
- **On tag push (`v*`)** — Builds signed APK + creates a GitHub Release with the APK attached
- **Manual trigger** — Use "Run workflow" button in Actions tab

### Creating a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

This will trigger the workflow, build a signed `WASSaver-v1.0.0.apk`, and create a GitHub Release automatically.

---

Built with ❤️ by Parveen Bhadoo

Credits: [Claude Opus 4.6 1M](https://anthropic.com) by [Anthropic](https://anthropic.com)
