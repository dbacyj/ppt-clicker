# Generating the APK signing key & configuring GitHub Secrets

An APK must be signed to be installable. The GitHub Actions workflow signs it automatically — you only need to do this once: generate a keystore → encode it → store it in GitHub Secrets.

> The keystore is secret — **never** commit it (it is excluded by `.gitignore`).

## Step 1: Generate a keystore

Run this on any machine with a JDK:

```bash
keytool -genkey -v \
  -keystore ppt-clicker.keystore \
  -alias pptclicker \
  -keyalg RSA -keysize 2048 -validity 10000
```

Follow the prompts for passwords and identity. This produces `ppt-clicker.keystore`.

> No JDK installed? Any Mac has one built in, or use GitHub Codespaces.

## Step 2: Encode the keystore as base64

```bash
base64 -i ppt-clicker.keystore -o keystore.b64
cat keystore.b64   # a long base64 string
```

## Step 3: Add 4 secrets to the GitHub repo

Repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | The full base64 content from step 2 |
| `KEYSTORE_PASSWORD` | The keystore password from step 1 |
| `KEY_ALIAS` | `pptclicker` (the `-alias` value from step 1) |
| `KEY_PASSWORD` | The key password from step 1 (usually the same as the keystore password) |

## Step 4: Release

```bash
git tag v0.1.0
git push origin v0.1.0
```

On push, GitHub Actions will:

1. Restore the keystore from `KEYSTORE_BASE64`
2. Build a signed APK
3. Publish it to GitHub Releases (with an auto-generated changelog)

Track progress under the **Actions** tab; once complete, the APK appears on the **Releases** page.

---

## Security notes

- **Back up the keystore locally** (password manager, encrypted USB). If you lose it, new versions cannot be signed with the same key, so users can't upgrade.
- GitHub Secrets are encrypted at rest and only decrypted at action runtime; they never appear in logs.
- The keystore is excluded by `.gitignore` and will not be committed.
