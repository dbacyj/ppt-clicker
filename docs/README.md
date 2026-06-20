# Website (GitHub Pages)

This directory holds the project's website, hosted free via GitHub Pages.

URL: https://dbacyj.github.io/ppt-clicker/

## Setup

GitHub Pages is configured to serve from the `/docs` folder of the `main` branch:

1. Repo → **Settings → Pages**
2. **Source**: `Deploy from a branch`
3. **Branch**: `main` / `/docs`

## Auto download links

`index.html` includes a script that fetches the latest APK's download URL and size from the GitHub Releases API on page load, so the download button stays in sync with releases automatically — no manual version bumps.

## Custom domain (optional)

To use a custom domain, add a `CNAME` file in this directory and configure your DNS accordingly.
