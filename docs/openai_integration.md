# OpenAI Integration Guide

## Overview
The tutor experience relies on OpenAI's Chat Completions API to generate contextual study support. Each chat session combines course metadata, student profile information, and the ongoing message history before contacting the API. Attachments such as PDFs and images are summarized on-device so that only concise context is sent to the model, keeping prompts efficient and reducing token usage.

## API Key Configuration
OpenAI credentials are read from the Gradle `local.properties` file during build time and exposed via `BuildConfig.OPENAI_API_KEY`:

```
openai.api.key=sk-your-key-here
```

- The key **must never** be committed to version control.
- An empty or missing key automatically switches the app to the mock data flow supplied by `MockChatData`, allowing UI testing without network calls.

## Request Pipeline
1. **View layer** (`ChatScreen.kt`)
   - Collects the message text and any attachments selected by the student.
   - Runs local attachment analysis (see below).
   - Sends the enriched message to `TutorViewModel`.

2. **View model** (`TutorViewModel.kt`)
   - Maintains per-course chat state and queues the outgoing message.
   - Calls `TutorRepository.requestTutorResponse` once local enrichment is finished.

3. **Repository** (`TutorRepository.kt`)
   - Builds a system prompt that includes:
     - Student profile (name, program, academic year from `CourseCatalog.sampleStudent`).
     - Course metadata (name, term, professor, pensum topics, current date).
     - Behavioral instructions (ask clarifying questions, ground answers in the pensum, reference attachments, etc.).
   - Converts prior messages plus the latest user message into `OpenAIMessage` objects.
   - For each message it serializes:
     - Text content, defaulting to "(No text message provided)" when empty.
     - Attachment descriptors in the form:
       ```
       - Pdf: lecture-notes.pdf
         Summary: Key concept bullets …
       ```
       Summaries are included only when available.

4. **Network layer** (`OpenAIService.kt`)
   - Uses OkHttp to POST to `https://api.openai.com/v1/chat/completions`.
   - Sends `{ model: "gpt-4o-mini", temperature: 0.2, messages: [...] }`.
   - Parses the first assistant message from the response and maps it to a `ChatMessage`.
   - Propagates IO or API errors back up the stack for UI display.

## Local Attachment Processing
To avoid uploading large documents verbatim, the app summarizes attachments before they reach OpenAI.

### Analyzer (`AttachmentAnalyzer.kt`)
- Initialized with the application `Context` to access the `ContentResolver`.
- **PDFs**
  - Uses `pdfbox-android` to read up to the first three pages.
  - Extracts text with `PDFTextStripper` and condenses it to at most 800 characters.
- **Images**
  - Decodes the bitmap locally and runs Google ML Kit's on-device text recognition (`TextRecognition.getClient`).
  - Collapses recognized text to 800 characters and recycles the bitmap to free memory.
- Returns a new `ChatAttachment` list where each item may include a `summary` string. If extraction fails or yields blank text, the original attachment is retained without a summary.

### UI Integration
- `ChatScreen` holds pending attachments and, on send, calls `AttachmentAnalyzer.enrichAttachments` inside a coroutine. A progress guard prevents duplicate sends while processing.
- Errors from the analyzer (e.g., unreadable file) surface via `SnackbarHostState` and allow the user to retry without crashing the session.
- Attachment previews render summaries beneath the thumbnail (images) or file name (PDFs) so the user can confirm what will be transmitted.

### Prompt Impact
- When `TutorRepository.renderMessageContent` serializes the conversation, each attachment contributes both its name and summary. This keeps prompts succinct while preserving the important content from supporting files.

## Mock vs. Live Behavior
- **Mock Mode**: Active when `BuildConfig.OPENAI_API_KEY` is blank. Predefined scripts and attachments demonstrate the UI, including summarized attachment examples, without contacting OpenAI.
- **Live Mode**: With a valid key, the app switches to real API calls. All other logic—including attachment summarization and prompt assembly—remains identical.

## Dependencies
Relevant Gradle artifacts introduced for this workflow:

- `com.squareup.okhttp3:okhttp` & `logging-interceptor` – HTTP client.
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` – coroutine support in Android.
- `com.tom-roush:pdfbox-android` – PDF parsing.
- `com.google.mlkit:text-recognition` + `kotlinx-coroutines-play-services` – on-device OCR.
- `io.coil-kt:coil-compose` – image rendering for attachment previews.

## Error Handling & UX Notes
- Failed network calls bubble up as snackbar messages via the view model's `errorMessage` flag.
- Attachment processing failures do not block sending; the user is notified and can try again or send without summaries.
- Summaries are optional: if local extraction returns nothing, attachments are still listed by file name so the assistant knows they exist.

By combining rich local context with targeted summaries, the tutor delivers high-quality assistance while keeping OpenAI prompt sizes—and associated costs—under control.
