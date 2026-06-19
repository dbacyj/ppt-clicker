# 落地页（GitHub Pages）

本目录是项目的官方下载/介绍页面，通过 GitHub Pages 免费托管。

## 开启方式

1. 仓库创建后，进入 **Settings → Pages**
2. **Source** 选择 `Deploy from a branch`
3. **Branch** 选 `main`，文件夹选 `/ (root)`
4. 点击 Save

> GitHub Pages 默认从仓库根目录找 `index.html`。本目录的 `index.html` 会在根路径直接生效，无需额外配置。

## 自定义域名（可选）

如需绑定自己的域名（如 `pptclicker.com`）：
1. 在域名 DNS 添加 CNAME 记录指向 `<你的用户名>.github.io`
2. 在本目录创建 `CNAME` 文件，写入你的域名
3. 在 Pages Settings 填入自定义域名

## 替换占位符

`index.html` 中有几处 `TODO` 需要替换：
- `YOUR_GITHUB_USERNAME` → 你的 GitHub 用户名（3 处）
- 赞助链接 → 你的 GitHub Sponsors 和爱发电实际地址

替换后直接 push 到 main 分支即可自动更新。
