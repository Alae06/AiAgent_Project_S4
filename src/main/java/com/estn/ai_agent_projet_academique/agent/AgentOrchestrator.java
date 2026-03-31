package com.estn.ai_agent_projet_academique.agent;


import com.estn.ai_agent_projet_academique.entities.ChatMessage;
import com.estn.ai_agent_projet_academique.rag.DocumentIngester;
import com.estn.ai_agent_projet_academique.rag.RagService;
import com.estn.ai_agent_projet_academique.repositories.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

    @Slf4j
    @RequiredArgsConstructor
    @Service
    public class AgentOrchestrator {
        private final DocumentIngester documentIngester;
        private final RagService ragService;
        private final ChatMessageRepository chatMessageRepository;

        public String uploadDocument(MultipartFile file) throws IOException {
            return documentIngester.ingest(file);
        }

        public String askQuestion(String question, String sessionId) {
            String answer = ragService.ask(question, sessionId);

            ChatMessage msg = ChatMessage.builder()
                    .question(question)
                    .answer(answer)
                    .model("mistral")
                    .sessionId(sessionId)
                    .createdAt(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(msg);

            return answer;
        }

        public List<ChatMessage> getHistory(String sessionId) {
            return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        }

        public void clearHistory(String sessionId) {
            chatMessageRepository.deleteBySessionId(sessionId);
            ragService.clearSession(sessionId);
        }
    }