package com.estn.ai_agent_projet_academique.rag;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngester {

    private final SimpleVectorStore simpleVectorStore;
    private final File vectorStoreFile;

    public String ingest(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("Ingestion de : {}", fileName);

        byte[] bytes = file.getBytes();
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override public String getFilename() { return fileName; }
        };

        List<Document> documents;
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder().build())
                    .withPagesPerDocument(1)
                    .build();
            documents = new PagePdfDocumentReader(resource, config).get();
            log.info("PDF lu : {} pages", documents.size());
        } else {
            documents = new TikaDocumentReader(resource).get();
            log.info("Tika lu : {}", fileName);
        }

        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build()
                .apply(documents);

        simpleVectorStore.add(chunks);
        log.info("{} chunks ajoutes en memoire", chunks.size());

        simpleVectorStore.save(vectorStoreFile);
        log.info("VectorStore sauvegarde dans : {}", vectorStoreFile.getAbsolutePath());

        return fileName + " ingere avec succes (" + chunks.size() + " chunks)";
    }
}