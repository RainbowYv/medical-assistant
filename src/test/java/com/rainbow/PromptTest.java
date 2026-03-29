package com.rainbow;

import com.rainbow.assistant.Assistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PromptTest {

    @Autowired
    private Assistant assistant;
    @Test
    public void testSystemMessage() {
        String answer = assistant.chat(3L,"今天几号");
        System.out.println(answer);
    }
}
