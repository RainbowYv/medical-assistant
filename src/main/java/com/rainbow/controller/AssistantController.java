package com.rainbow.controller;

import com.rainbow.assistant.Assistant;
import com.rainbow.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "硅谷小智")
@RestController
@RequestMapping("/medical/assistant")
public class AssistantController {
    @Autowired
    private Assistant assistant ;

    @Operation(summary = "对话")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatForm chatForm){
        return assistant.chat(chatForm.getMemoryId(),chatForm.getMessage());
    }

}
