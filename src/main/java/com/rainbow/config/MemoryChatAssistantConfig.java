package com.rainbow.config;

import com.rainbow.store.MongoChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryChatAssistantConfig {
    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore ;
    @Bean
    public ChatMemoryProvider chatMemory(){
        return memoryId -> MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(20)
//                .chatMemoryStore(new InMemoryChatMemoryStore())
                .chatMemoryStore(mongoChatMemoryStore)
                .build();
    }
}
