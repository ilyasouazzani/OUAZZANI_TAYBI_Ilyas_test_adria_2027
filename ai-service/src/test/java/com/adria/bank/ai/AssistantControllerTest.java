package com.adria.bank.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AssistantControllerTest.TestTools.class)
class AssistantControllerTest {
    @Autowired MockMvc mvc;
    private static final AtomicReference<ToolCallback[]> CALLBACKS = new AtomicReference<>(new ToolCallback[0]);

    @TestConfiguration
    static class TestTools {
        @Bean
        ToolCallbackProvider testToolProvider() { return CALLBACKS::get; }
    }

    @BeforeEach
    void prepare() { CALLBACKS.set(new ToolCallback[0]); }

    @Test
    void reportsDisabledLlmInsteadOfInventingAnAnswer() throws Exception {
        mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Quel est mon solde ?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("LLM disabled")));
    }

    @Test
    void rejectsEmptyQuestion() throws Exception {
        mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingOrMalformedQuestion() throws Exception {
        mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesDefinitionsFromTheToolProvider() throws Exception {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(definition.name()).thenReturn("remote_account_balance");
        when(definition.description()).thenReturn("Account balance from MCP");
        when(definition.inputSchema()).thenReturn("{}");
        CALLBACKS.set(new ToolCallback[]{callback});
        mvc.perform(get("/api/assistant/tools")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("remote_account_balance"));
    }
}
