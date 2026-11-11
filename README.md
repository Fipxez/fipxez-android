# FIPXEZ AI — Android Personal Assistant

FIPXEZ AI is an Android chat workspace that connects to a model provider using an OpenAI-compatible Chat Completions API. The app is a client interface; the intelligence and availability depend on the model provider and model ID configured by the user.

## Features in this build
- Dark mobile chat interface with a configurable model ID and API base URL.
- Supports providers that implement the OpenAI-compatible `/chat/completions` endpoint.
- API key stored using Android secure storage; endpoint, model, system instructions, and recent chat history stored locally.
- Conversation context sends the most recent 24 messages; up to 60 messages are retained locally.
- Clear-chat action and editable system instructions.

## First-time setup
1. Install and open the Android APK.
2. Tap the settings/tune icon.
3. Enter the API base URL supplied by your AI provider (default: `https://api.openai.com/v1`).
4. Enter a model ID that is enabled for your provider account.
5. Paste your own provider API key and save.
6. Return to chat and send a message.

FIPXEZ does not include a bundled AI model or a free API account. The user must supply a provider endpoint, a supported model ID, and their own API key. Model usage may incur charges according to the provider's pricing. Never share your API key or commit it to the repository.

## Privacy and limitations
- API keys are stored with Android secure storage and are not embedded in the APK or source code.
- Chat history and configuration are stored locally on the device. Clearing chat removes the saved local conversation.
- Messages are sent to the configured model provider when the user sends a prompt. Review that provider's privacy and retention terms.
- The app currently supports text chat through the compatible chat-completions API. It does not yet include web search, voice, image input, file uploads, autonomous agents, or a fully offline on-device model.
- The app does not bypass provider safeguards. Capabilities and response behavior depend on the chosen model and provider.

## Build APK with GitHub Actions
The workflow `.github/workflows/build-android.yml` runs automatically after relevant source changes:
1. Open the repository's **Actions** tab.
2. Select **Build FIPXEZ Android APK**.
3. Open a completed run and download the **FIPXEZ-Android-APK** artifact.

The APK is an unsigned release build for testing/sideloading. A production release requires a signing keystore.
