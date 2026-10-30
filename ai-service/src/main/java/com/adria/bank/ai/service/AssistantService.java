package com.adria.bank.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private final ObjectProvider<ChatClient> clients;
    private final ToolCallbackProvider tools;

    public AssistantService(ObjectProvider<ChatClient> clients, ToolCallbackProvider tools) {
        this.clients = clients;
        this.tools = tools;
    }

    public String ask(String message) {
        ChatClient client = clients.getIfAvailable();
        if (client == null) {
            throw new AssistantUnavailableException("LLM disabled: configure GEMINI_API_KEY locally and AI_CHAT_MODEL=google-genai, then restart ai-service");
        }
        try {
            String answer = client.prompt().user(message).call().content();
            if (answer == null || answer.isBlank()) throw new IllegalStateException("Empty model response");
            return answer;
        } catch (RuntimeException exception) {
            log.warn("Assistant request failed: {}", exception.getClass().getSimpleName());
            throw new AssistantUnavailableException("Assistant unavailable: LLM or MCP request failed; no operation is confirmed");
        }
    }

    public List<ToolDescription> tools() {
        return Arrays.stream(tools.getToolCallbacks()).map(callback -> callback.getToolDefinition())
                .map(definition -> new ToolDescription(definition.name(), definition.description(), definition.inputSchema()))
                .toList();
    }

    public record ToolDescription(String name, String description, String inputSchema) {}
}
