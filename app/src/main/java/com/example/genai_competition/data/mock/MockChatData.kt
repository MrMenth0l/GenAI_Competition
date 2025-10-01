package com.example.genai_competition.data.mock

import android.net.Uri
import com.example.genai_competition.data.model.AttachmentType
import com.example.genai_competition.data.model.ChatAttachment
import com.example.genai_competition.data.model.ChatMessage
import com.example.genai_competition.data.model.ChatRole
import java.time.Instant
import java.util.UUID

object MockChatData {

    private data class MockScript(
        val intro: List<ChatMessage>,
        val replies: List<String>
    )

    private val scripts: Map<String, MockScript> = mapOf(
        "cs-algorithms" to MockScript(
            intro = listOf(
                assistant(
                    "Welcome back, Alex! Ready to tackle divide and conquer today? I've outlined the steps in the review guide below.",
                    attachments = listOf(
                        pdfAttachment(
                            name = "DivideAndConquer_Review.pdf",
                            uri = "https://example.com/mock/divide-and-conquer.pdf"
                        )
                    )
                ),
                user("Thanks! I'm stuck on choosing the pivot for quicksort and how it affects performance."),
            ),
            replies = listOf(
                "Great question. Think about how picking a bad pivot affects the partition balance. What strategies do you know for pivot selection?",
                "Nice. Try coding a median-of-three variant tonight and record the comparisons count—it's a quick experiment.",
                "Want a rapid quiz on divide and conquer recurrences next?"
            )
        ),
        "cs-systems" to MockScript(
            intro = listOf(
                assistant(
                    "Hi Alex, this week's systems lab focuses on virtual memory. Here's the visual cheat sheet we reviewed in class.",
                    attachments = listOf(
                        imageAttachment(
                            name = "VirtualMemoryLayers.png",
                            uri = "https://picsum.photos/seed/vmm/800/480"
                        )
                    )
                ),
                user("Amazing. I still mix up when a TLB miss triggers a page table walk versus a page fault."),
            ),
            replies = listOf(
                "Start by walking through the access timeline. After a TLB miss, what's the next hardware structure consulted?",
                "Exactly. Build a 4-step flashcard sequence for the memory access path. It'll cement when faults actually trap to the OS.",
                "Remember to note how write-back caches complicate things—we'll cover scenarios tomorrow."
            )
        ),
        "cs-theory" to MockScript(
            intro = listOf(
                assistant("Welcome, Alex! Ready for more practice on context-free grammars?"),
                user("Yes! I need help converting grammar rules into Chomsky normal form."),
            ),
            replies = listOf(
                "Start by eliminating epsilon productions. Which non-terminals currently derive ε?",
                "Good. After removing unit productions, try rewriting rule S -> ABa into CNF by introducing a helper non-terminal.",
                "Lock it in by deriving the word 'abba' with the new grammar—you'll see the structure clearly."
            )
        )
    )

    fun initialMessagesFor(courseId: String): List<ChatMessage> {
        val script = scripts[courseId] ?: return emptyList()
        return script.intro
    }

    fun nextAssistantMessage(courseId: String, turnIndex: Int): ChatMessage {
        val script = scripts[courseId]
        val introAssistantCount = script?.intro?.count { it.role == ChatRole.ASSISTANT } ?: 0
        val replyIndex = (turnIndex - introAssistantCount).coerceAtLeast(0)
        val reply = script?.replies?.getOrNull(replyIndex) ?: defaultReply(courseId, replyIndex)
        return assistant(reply)
    }

    private fun assistant(content: String, attachments: List<ChatAttachment> = emptyList()): ChatMessage =
        ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.ASSISTANT,
            content = content,
            timestamp = Instant.now(),
            attachments = attachments
        )

    private fun user(content: String): ChatMessage =
        ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            content = content,
            timestamp = Instant.now()
        )

    private fun pdfAttachment(name: String, uri: String): ChatAttachment =
        ChatAttachment(
            uri = Uri.parse(uri),
            name = name,
            type = AttachmentType.PDF
        )

    private fun imageAttachment(name: String, uri: String): ChatAttachment =
        ChatAttachment(
            uri = Uri.parse(uri),
            name = name,
            type = AttachmentType.IMAGE
        )

    private fun defaultReply(courseId: String, turnIndex: Int): String =
        "Let's keep building your progress for $courseId. Turn $turnIndex is a great time to summarise what clicked."
}
