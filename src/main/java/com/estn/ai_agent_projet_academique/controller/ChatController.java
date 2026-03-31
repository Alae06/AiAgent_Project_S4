package com.estn.ai_agent_projet_academique.controller;


import com.estn.ai_agent_projet_academique.agent.AgentOrchestrator;
import com.estn.ai_agent_projet_academique.entities.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AgentOrchestrator orchestrator;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(
            @RequestBody Map<String, String> body) {

        String question  = body.getOrDefault("question", "").trim();
        String sessionId = body.getOrDefault("sessionId", "default");

        if (question.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La question ne peut pas etre vide"));
        }

        String answer = orchestrator.askQuestion(question, sessionId);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> history(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(orchestrator.getHistory(sessionId));
    }

    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, String>> clear(
            @PathVariable String sessionId) {
        orchestrator.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("message", "Historique et memoire effaces"));
    }
}