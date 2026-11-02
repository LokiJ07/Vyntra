# Vyntra v0.1.2 – Private Signing Setup (Windows / GitHub)

This release permanently fixes the "different signing certificate" problem for
**future Vyntra updates** by using one private signing key on every GitHub build.
Do NOT commit the signing key/password to GitHub. Treat the `.jks` as a private
credential; keep a second backup in a safe place.

## 1. Download your private signing key

Use the SAME `vyntra-release.jks` that signed your v0.1.1 APK. Do NOT generate a new key. Keep it
somewhere safe on your own PC. Never put it in the source project or repository.
The private signing key is NOT included in the public source ZIP.

## 2. Copy the keystore as Base64 to clipboard (PowerShell)

Move the downloaded `.jks` to your Windows Downloads directory, then run:

```powershell
$path = Join-Path $env:USERPROFILE 'Downloads\vyntra-release.jks'
[Convert]::ToBase64String([IO.File]::ReadAllBytes($path)) | Set-Clipboard
```

This copies Base64 text to clipboard. Do NOT paste it into the chat or your repo.

## 3. Create two GitHub Actions repository secrets

Go to your GitHub `Vyntra` repository:
Settings > Secrets and variables > Actions > New repository secret

- Name: `VYNTRA_KEYSTORE_BASE64` — paste the Base64 text from clipboard.
- Name: `VYNTRA_KEYSTORE_PASSWORD` — paste the private password supplied with
  the signing key. The key alias is already set to `vyntra` in Gradle.

If you lost the password, do not recreate a new key to update existing apps.
Restore your previously saved key/password instead.

## 4. Push the changed source; find your signed APK

Commit the updated files to your existing Vyntra repository. Then visit:
Actions > Vyntra Android CI > latest run > Artifacts > Vyntra-v0.1.2-SIGNED-...

Download/extract the artifact and install the APK on your Android device.
All future Vyntra builds use this exact same signing certificate, provided you
keep the secrets unchanged. The workflow automatically increases versionCode
using the GitHub Actions run number, so a later successful build can update an
earlier CI-generated APK without uninstalling it.

## 5. Important caveat about older APKs

If an older Vyntra APK was signed with a different key (e.g. random CI debug
key), Android cannot install the new APK as an in-place update. One initial
uninstall may be unavoidable. After installing the new signed APK, do not switch
back to differently-signed APKs. LokiTunes is a separate app and is unaffected.

Never upload or publish the JKS file, the Base64 secret, or its password in
GitHub source, Issues, Actions logs, or APK release assets.
