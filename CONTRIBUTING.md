# Contributing to PPT Clicker

Thanks for your interest in contributing! This project welcomes contributions of all kinds — bug reports, feature ideas, translations, documentation, and code.

## Ways to contribute

- **Report bugs** — open an [issue](https://github.com/dbacyj/ppt-clicker/issues/new?labels=bug) with steps to reproduce, your device/OS, and what you expected vs. what happened.
- **Suggest features** — open an issue with the `enhancement` label.
- **Improve docs** — typos, clarifications, and translations are all welcome.
- **Write code** — see [good first issues](https://github.com/dbacyj/ppt-clicker/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) for a starting point.

## Development workflow

1. Fork the repo and create a branch:
   ```bash
   git checkout -b fix/my-bugfix
   ```
2. Make your changes. Keep commits focused.
3. Verify it builds:
   ```bash
   cd android-app && ./gradlew assembleDebug
   ```
4. Open a pull request against `main`. Describe **what** changed and **why**.

## Commit messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` a new feature
- `fix:` a bug fix
- `docs:` documentation only
- `refactor:` code change that neither fixes a bug nor adds a feature
- `chore:` build, CI, tooling

Example: `fix: handle null device in HID sendKey`

## Code style

- Kotlin: follow the [official style guide](https://developer.android.com/kotlin/style-guide)
- Keep public APIs documented
- Match the style of surrounding code

## Reporting security issues

Please **do not** open a public issue for security vulnerabilities. See [SECURITY.md](SECURITY.md) if present, or email the maintainer privately.

## License

By contributing, you agree that your contributions will be licensed under the [MIT license](LICENSE).
