package com.estn.ai_agent_projet_academique.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "TEXT")
  private String question;

  @Column(columnDefinition = "TEXT")
  private String answer;

  private String model;
  private String sessionId;
  private LocalDateTime createdAt;
}