package com.rainbow;

import com.rainbow.assistant.Assistant;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {
    @Autowired
    private OpenAiChatModel openAiChatModel ;

    @Test
    public void testChat(){
        Assistant assistant = AiServices.create(Assistant.class, openAiChatModel) ;
        String chat = assistant.chat(1L ,"你是谁");
        System.out.println(chat);
    }

    @Autowired
    private Assistant assistant ;
    @Test
    public void testChat2(){
        //调用service的接口
        String answer1 = assistant.chat(1L,"我是Rainbow");
        System.out.println(answer1);
        String answer2 = assistant.chat(2L,"我是谁");
        System.out.println(answer2);
    }


}
