package com.estn.ai_agent_projet_academique.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RagService {

    private final ChatClient mistralClient;
    private final VectorStore vectorStore;
    private final Map<String, List<Message>> sessionHistories = new ConcurrentHashMap<>();

    public RagService(
            @Qualifier("mistralClient") ChatClient mistralClient,
            VectorStore vectorStore) {
        this.mistralClient = mistralClient;
        this.vectorStore   = vectorStore;
    }

    public String ask(String question, String sessionId) {
        log.info("Question [session={}]: {}", sessionId, question);

        List<Message> history = sessionHistories
                .computeIfAbsent(sessionId, k -> new ArrayList<>());

        // Cherche les chunks pertinents — fonctionne meme avec les docs
        // charges depuis vectors.json au demarrage
        List<org.springframework.ai.document.Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(5).build()
        );

        log.info("{} chunks trouves pour la question", docs != null ? docs.size() : 0);

        StringBuilder context = new StringBuilder();
        if (docs != null && !docs.isEmpty()) {
            context.append("Contexte extrait du document:\n");
            docs.forEach(doc -> context.append(doc.getText()).append("\n---\n"));
        }

        String fullUserMessage = context.isEmpty()
                ? question
                : context + "\nQuestion: " + question;

        history.add(new UserMessage(fullUserMessage));

        String answer = mistralClient
                .prompt(new Prompt(history))
                .call()
                .content();

        history.add(new AssistantMessage(answer));

        log.info("Reponse [session={}]: {} chars", sessionId, answer.length());
        return answer;
    }

    public void clearSession(String sessionId) {
        sessionHistories.remove(sessionId);
        log.info("Memoire effacee pour session: {}", sessionId);
    }
}