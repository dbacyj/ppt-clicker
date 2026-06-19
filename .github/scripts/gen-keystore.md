# 生成 APK 签名密钥 & 配置 GitHub Secrets

APK 必须签名才能在手机上安装。本项目的 GitHub Actions 工作流会自动签名，
你只需做一次：生成 keystore → 编码 → 存到 GitHub Secrets。

> keystore 是私密的，**绝对不要**提交到仓库（已在 .gitignore 中排除）。

## 第 1 步：生成 keystore（本地任一装了 JDK 的机器，或用在线工具）

```bash
keytool -genkey -v \
  -keystore ppt-clicker.keystore \
  -alias pptclicker \
  -keyalg RSA -keysize 2048 -validity 10000
```

按提示填写密码和身份信息（密码记好，后面要用）。完成后得到 `ppt-clicker.keystore` 文件。

> 没装 JDK？可以用任意一台 Mac（自带）或用 GitHub Codespaces。
> 或者临时用一个在线 keytool 服务（注意安全性，仅个人项目用）。

## 第 2 步：把 keystore 编码成 base64

```bash
base64 -i ppt-clicker.keystore -o keystore.b64
# 输出一长串 base64 文本
cat keystore.b64
```

## 第 3 步：在 GitHub 仓库添加 4 个 Secrets

进入仓库 → **Settings → Secrets and variables → Actions → New repository secret**，添加：

| Secret 名 | 值 |
|-----------|----|
| `KEYSTORE_BASE64` | 第 2 步 `keystore.b64` 的全部内容（一长串 base64） |
| `KEYSTORE_PASSWORD` | 第 1 步设置的 keystore 密码 |
| `KEY_ALIAS` | `pptclicker`（第 1 步 `-alias` 的值） |
| `KEY_PASSWORD` | 第 1 步设置的 key 密码（通常与 keystore 密码相同） |

## 第 4 步：发布

```bash
git tag v0.1.0
git push origin v0.1.0
```

推送 tag 后，GitHub Actions 自动：
1. 从 `KEYSTORE_BASE64` 还原 keystore
2. 编译签名 APK
3. 发布到 GitHub Release（带自动生成的 changelog）

在仓库的 **Actions** 标签页能看到编译进度，完成后 **Releases** 页面就有可下载的 APK。

---

## 安全说明
- keystore 文件请**本地备份**（如存到密码管理器、加密 U 盘）。丢失后无法给新版本用同一签名升级。
- GitHub Secrets 是加密存储的，Actions 运行时才解密注入，不会出现在日志里。
- keystore 已被 `.gitignore` 排除，不会被提交。
