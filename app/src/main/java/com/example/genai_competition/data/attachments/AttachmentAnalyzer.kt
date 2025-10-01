package com.example.genai_competition.data.attachments

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.example.genai_competition.data.model.AttachmentType
import com.example.genai_competition.data.model.ChatAttachment
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AttachmentAnalyzer(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun enrichAttachments(attachments: List<ChatAttachment>): List<ChatAttachment> = withContext(Dispatchers.IO) {
        attachments.map { attachment ->
            val summary = when (attachment.type) {
                AttachmentType.PDF -> extractPdfSummary(attachment.uri)
                AttachmentType.IMAGE -> extractImageSummary(attachment.uri)
            }
            if (summary.isNullOrBlank()) attachment else attachment.copy(summary = summary)
        }
    }

    private fun extractPdfSummary(uri: Uri): String? {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                pdfToSummary(stream)
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun pdfToSummary(stream: InputStream): String {
        PDDocument.load(stream).use { document ->
            val stripper = PDFTextStripper().apply {
                startPage = 1
                endPage = minOf(document.numberOfPages, MAX_PDF_PAGES)
            }
            val raw = stripper.getText(document)
            return condense(raw)
        }
    }

    private suspend fun extractImageSummary(uri: Uri): String? = withContext(Dispatchers.Default) {
        val bitmap = loadBitmap(uri) ?: return@withContext null
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = runCatching {
                Tasks.await(textRecognizer.process(inputImage))
            }.getOrNull()
            val text = visionText?.text?.takeIf { it.isNotBlank() }
            text?.let { condense(it) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    companion object {
        private const val MAX_PDF_PAGES = 3
        private const val SUMMARY_LIMIT = 800

        @VisibleForTesting
        internal fun condense(text: String?): String {
            if (text.isNullOrBlank()) return ""
            val collapsed = text
                .replace("\\s+".toRegex(), " ")
                .trim()
            return if (collapsed.length <= SUMMARY_LIMIT) {
                collapsed
            } else {
                collapsed.take(SUMMARY_LIMIT).trimEnd() + "…"
            }
        }
    }
}
