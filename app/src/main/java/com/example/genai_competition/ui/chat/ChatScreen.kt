package com.example.genai_competition.ui.chat

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.genai_competition.data.attachments.AttachmentAnalyzer
import com.example.genai_competition.data.model.AttachmentType
import com.example.genai_competition.data.model.ChatAttachment
import com.example.genai_competition.data.model.ChatMessage
import com.example.genai_competition.data.model.ChatRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: CourseChatState,
    onBack: () -> Unit,
    onSendMessage: (String, List<ChatAttachment>) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val messageListState = rememberLazyListState()
    val attachmentAnalyzer = remember(context) { AttachmentAnalyzer(context) }

    var messageText by rememberSaveable(state.course.id) { mutableStateOf("") }
    val pendingAttachments = remember(state.course.id) { mutableStateListOf<ChatAttachment>() }
    var isProcessing by rememberSaveable(state.course.id) { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            addAttachmentFromUri(
                context = context,
                uri = uri,
                type = AttachmentType.IMAGE,
                onSuccess = { pendingAttachments.add(it) },
                onFailure = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Unable to attach image.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Ignore if permission cannot be persisted, proceed anyway.
            }
            addAttachmentFromUri(
                context = context,
                uri = uri,
                type = AttachmentType.PDF,
                onSuccess = { pendingAttachments.add(it) },
                onFailure = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Unable to attach document.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            messageListState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    LaunchedEffect(state.errorMessage) {
        val error = state.errorMessage
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            onDismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Professor ${state.course.professor} · ${state.course.term}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MessagesList(
                messages = state.messages,
                listState = messageListState,
                modifier = Modifier.weight(1f),
                onAttachmentClick = { attachment ->
                    handleAttachmentOpen(
                        context = context,
                        attachment = attachment,
                        snackbarHostState = snackbarHostState,
                        scope = coroutineScope
                    )
                }
            )

            ChatInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (isProcessing) return@ChatInputBar
                    coroutineScope.launch {
                        try {
                            isProcessing = true
                            val enriched = attachmentAnalyzer.enrichAttachments(pendingAttachments.toList())
                            onSendMessage(messageText, enriched)
                            messageText = ""
                            pendingAttachments.clear()
                        } catch (throwable: Throwable) {
                            snackbarHostState.showSnackbar(
                                message = throwable.message ?: "Failed to process attachments.",
                                duration = SnackbarDuration.Short
                            )
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                pickImage = {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                pickPdf = {
                    pickPdfLauncher.launch(arrayOf("application/pdf"))
                },
                isSending = state.isSending || isProcessing,
                attachments = pendingAttachments,
                onRemoveAttachment = { attachment ->
                    pendingAttachments.removeAll { it.id == attachment.id }
                }
            )
        }
    }
}

@Composable
private fun MessagesList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onAttachmentClick: (ChatAttachment) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                onAttachmentClick = onAttachmentClick
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onAttachmentClick: (ChatAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .background(color = bubbleColor, shape = MaterialTheme.shapes.large)
                .padding(16.dp)
        ) {
            Text(
                text = message.content.ifBlank { "(No message content)" },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )

            if (message.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    message.attachments.forEach { attachment ->
                        AttachmentPreview(
                            attachment = attachment,
                            onClick = { onAttachmentClick(attachment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    attachment: ChatAttachment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (attachment.type) {
        AttachmentType.IMAGE -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
            ) {
                Column {
                    AsyncImage(
                        model = attachment.uri,
                        contentDescription = attachment.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    if (!attachment.summary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = attachment.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
        AttachmentType.PDF -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = attachment.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!attachment.summary.isNullOrBlank()) {
                        Text(
                            text = attachment.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    pickImage: () -> Unit,
    pickPdf: () -> Unit,
    isSending: Boolean,
    attachments: List<ChatAttachment>,
    onRemoveAttachment: (ChatAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (attachments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attachments, key = { it.id }) { attachment ->
                    AttachmentChip(
                        attachment = attachment,
                        onRemove = { onRemoveAttachment(attachment) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageChange,
            placeholder = { Text("Ask a question or describe your study goal...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                errorContainerColor = MaterialTheme.colorScheme.errorContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (!isSending && (messageText.isNotBlank() || attachments.isNotEmpty())) {
                    onSend()
                }
            })
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = pickImage) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "Attach image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = pickPdf) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = "Attach PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            val canSend = (messageText.isNotBlank() || attachments.isNotEmpty()) && !isSending
            Button(
                onClick = onSend,
                enabled = canSend,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: ChatAttachment,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val icon = when (attachment.type) {
                AttachmentType.IMAGE -> Icons.Outlined.Image
                AttachmentType.PDF -> Icons.Outlined.AttachFile
            }
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = onRemove) {
                Text("Remove")
            }
        }
    }
}

private fun addAttachmentFromUri(
    context: android.content.Context,
    uri: Uri,
    type: AttachmentType,
    onSuccess: (ChatAttachment) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    runCatching {
        val name = resolveDisplayName(context, uri)
        ChatAttachment(
            uri = uri,
            name = name,
            type = type
        )
    }.onSuccess(onSuccess).onFailure(onFailure)
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index != -1 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }
    return uri.lastPathSegment ?: "Attachment"
}

private fun handleAttachmentOpen(
    context: android.content.Context,
    attachment: ChatAttachment,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val mime = when (attachment.type) {
        AttachmentType.IMAGE -> "image/*"
        AttachmentType.PDF -> "application/pdf"
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(attachment.uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "No app available to open ${attachment.type.name.lowercase()} files.",
                duration = SnackbarDuration.Short
            )
        }
    }
}
