 # 🤖 Kali AI - Offline First Smart Assistant

एक एडवांस AI असिस्टेंट जो ऑफलाइन काम करता है और GitHub पर ऑटोमैटिक बैकअप लेता है।

## ✨ Features

- 📱 **Offline First** - बिना इंटरनेट के काम करता है
- 🔄 **Silent GitHub Sync** - बैकग्राउंड में ऑटोमैटिक बैकअप
- 🎤 **Voice Control** - "Kali" बोलकर एक्टिवेट करें
- 📞 **Phone Control** - कॉल, मैसेज, ऐप कंट्रोल
- 🧠 **AI Powered** - DeepSeek + Gemini इंटीग्रेशन
- 💾 **Local Storage** - सभी बातें फोन में सेव

## 🚀 Setup Instructions

### 1. GitHub Repository Setup

```bash
# Repository बनाएं
1. GitHub पर जाएं
2. New Repository बनाएं (Private recommended)
3. नाम: Kali-AI-Core
4. Personal Access Token बनाएं (Settings → Developer Settings)
```

### 2. API Keys Setup

`local.properties` फाइल बनाएं (यह git में नहीं जाएगी):

```properties
DEEPSEEK_API_KEY=sk-your-deepseek-key
GEMINI_API_KEY=your-gemini-key
GITHUB_TOKEN=ghp_your-github-token
GITHUB_USERNAME=your-github-username
GITHUB_REPO=Kali-AI-Core
```

### 3. Android Studio Setup

```bash
1. Project को Clone करें
2. Android Studio में Open करें
3. Sync Gradle Files
4. Run on Device
5. Accessibility Service चालू करें
6. सभी Permissions दें
```

## 📊 How It Works
