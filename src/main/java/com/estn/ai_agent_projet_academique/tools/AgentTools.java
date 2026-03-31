package com.estn.ai_agent_projet_academique.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AgentTools {

    @Tool(description = "Returns the current date and time. Use when the user asks about the current date or time.")
    public String getCurrentDateTime() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy, HH:mm"));
    }

    @Tool(description = "Count the number of words in a given text.")
    public int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}