package com.adria.bank.ai.web.rest;

import com.adria.bank.ai.service.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService service;

    public AssistantController(AssistantService service) { this.service = service; }

    @GetMapping("/tools")
    public List<AssistantService.ToolDescription> tools() { return service.tools(); }

    @PostMapping("/chat")
    public Answer chat(@Valid @RequestBody Question question) { return new Answer(service.ask(question.message())); }

    public record Question(@NotBlank @Size(max = 4000) String message) {}
    public record Answer(String answer) {}
}
