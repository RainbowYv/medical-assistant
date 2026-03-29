package com.rainbow.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemory",
        tools = "appointmentTools"
)
public interface Assistant {
    @SystemMessage(fromResource = "system-prompt-template.txt")
    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);
}
