package com.rainbow;

import com.rainbow.assistant.Assistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {
    @Test
    public void testLLMDemo(){
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();
        String answer = model.chat("你好") ;
        System.out.println(answer);
    }


    @Autowired
    private OpenAiChatModel openAiChatModel ;
    @Test
    public void testLLMSpringBoot(){
        String chat = openAiChatModel.chat("你是谁");
        System.out.println(chat);
    }

    @Test
    public void testChatMemory3() {
        //创建chatMemory
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        //创建AIService
        Assistant assistant = AiServices
                .builder(Assistant.class)
                .chatLanguageModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();
        //调用service的接口
        String answer1 = assistant.chat(1L,"我是Rainbow");
        System.out.println(answer1);
        String answer2 = assistant.chat(1L,"我是谁");
        System.out.println(answer2);
    }
}
