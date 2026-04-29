# ☢️ SoutBoy

> One keystroke. One log. No more typing `System.out.println` by hand.

SoutBoy is an IntelliJ IDEA plugin inspired by the legendary **Pip-Boy** from Fallout. Place your cursor on any variable, press `Ctrl+Alt+;`, and it generates a formatted debug log — dropped right after the current statement, no matter how many lines it spans.

---

## ✨ Features

- **Smart variable detection** — works with selected text, cursor position, or walks back to find the nearest identifier
- **Multiline statement support** — inserts after the full statement, even chained calls like `.withClaim().sign(algorithm);`
- **Block-aware insertion** — logs inside `if`, `for`, `while` blocks instead of after them
- **Condition logging** — when the cursor is on an `if` or `while`, logs the condition expression
- **Class name & line number** — auto-detected, no setup needed
- **Indentation preserved** — output always aligns with your code

---

## 🚀 Generated output

```java
// Cursor on: token
System.out.println("☢️ 42 ~ AuthService ~ token: " + token);

// Cursor on: if (!isValid)
System.out.println("☢️ 17 ~ UserService ~ !isValid: " + !isValid);

// Cursor on a multiline chain:
String token = JWT.create()
        .withClaim("info", data)
        .sign(algorithm);
// → log inserted after the semicolon ↑
```

---

## ⚠️ Known Issues

- **Kotlin files** — variable detection is limited in `.kt` files. The plugin was built for Java and may not work as expected in Kotlin projects.

## ⌨️ Shortcut

| Action | Shortcut         |
|--------|------------------|
| Generate log | `Ctrl + Alt + ;` |

---

## 📦 Installation

### Via JetBrains Marketplace
1. Open IntelliJ IDEA
2. Go to **Settings → Plugins → Marketplace**
3. Search for **SoutBoy**
4. Click **Install**

### Manual installation
1. Download the latest `.zip` from [Releases](https://github.com/Pedro-Rbeiro/sout-boy/releases)
2. Go to **Settings → Plugins → ⚙️ → Install Plugin from Disk**
3. Select the downloaded `.zip`

---

## 🛠️ Building from source

```bash
git clone https://github.com/Pedro-Rbeiro/sout-boy.git
cd sout-boy
./gradlew buildPlugin
```

The plugin zip will be in `build/distributions/`.

To run in a sandbox IDE:

```bash
./gradlew runIde
```

---

## 🤝 Contributing

PRs are welcome! Feel free to open an issue if you find a bug or have a feature idea.

---

## 👤 Author

**Pedro Ribeiro**
[github.com/Pedro-Rbeiro](https://github.com/Pedro-Rbeiro)

---

## 📄 License

MIT
