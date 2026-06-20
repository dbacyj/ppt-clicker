# 落地页 / Landing Page (GitHub Pages)

本目录是项目的官网下载页，通过 GitHub Pages 免费托管。
This directory is the project's website, hosted free via GitHub Pages.

地址 / URL: https://dbacyj.github.io/ppt-clicker/

## 配置 / Setup

GitHub Pages 已配置为从 `main` 分支的 `/docs` 目录发布：
GitHub Pages is configured to serve from the `/docs` folder of the `main` branch:

1. 仓库 → **Settings → Pages**
2. **Source**: `Deploy from a branch`
3. **Branch**: `main` / `/docs`

## 自动下载链接 / Auto download links

`index.html` 包含一段 JS，启动时从 GitHub Releases API 拉取最新 APK 的下载地址和大小，自动填充到下载按钮。无需手动维护版本号。
The page fetches the latest APK URL and size from the GitHub Releases API on load, so download links stay in sync with releases automatically.

## 自定义域名 / Custom domain (optional)

如需绑定自己的域名，在 `docs/` 添加 `CNAME` 文件并配置 DNS。/ Add a `CNAME` file here to use a custom domain.
