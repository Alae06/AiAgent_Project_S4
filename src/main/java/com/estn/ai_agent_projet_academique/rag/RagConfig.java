package com.estn.ai_agent_projet_academique.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class RagConfig implements WebMvcConfigurer {

    private static final String STORE_PATH = "src/main/resources/store/vectors.json";

    @Bean
    public SimpleVectorStore simpleVectorStore(
            @Qualifier("mistralAiEmbeddingModel") EmbeddingModel embeddingModel) throws IOException {

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        File vectorFile = new File(STORE_PATH);
        vectorFile.getParentFile().mkdirs();

        if (vectorFile.exists() && vectorFile.length() > 10) {
            log.info("Chargement du VectorStore depuis : {}", vectorFile.getAbsolutePath());
            store.load(vectorFile);
            log.info("VectorStore charge avec succes.");
        } else {
            log.info("Aucun VectorStore existant — demarrage vide.");
        }

        return store;
    }

    @Bean
    @Primary
    public VectorStore vectorStore(SimpleVectorStore simpleVectorStore) {
        return simpleVectorStore;
    }

    @Bean
    public File vectorStoreFile() {
        File file = new File(STORE_PATH);
        file.getParentFile().mkdirs();
        return file;
    }

    @Bean
    @Qualifier("mistralClient")
    public ChatClient mistralChatClient(MistralAiChatModel mistralModel) {
        return ChatClient.builder(mistralModel)
                .defaultSystem("""
                        Tu es un assistant intelligent et precis.
                        Tu reponds UNIQUEMENT en te basant sur le document fourni ET sur l'historique de la conversation.
                        Si la reponse n'est pas dans le document, dis-le clairement.
                        Tu te souviens de tout ce qui a ete dit dans la conversation.
                        Reponds dans la meme langue que la question.
                        """)
                .build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}