import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UploadResponse { message: string; }
export interface AskResponse    { answer: string;  }
export interface ChatMessage {
  id: number;
  question: string;
  answer: string;
  model: string;
  sessionId: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AiService {

  private readonly API = 'http://localhost:8080/api';
  constructor(private http: HttpClient) {}

  uploadDocument(file: File): Observable<UploadResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<UploadResponse>(`${this.API}/docs/upload`, form);
  }

  askQuestion(question: string, sessionId: string): Observable<AskResponse> {
    return this.http.post<AskResponse>(`${this.API}/chat/ask`, { question, sessionId });
  }

  getHistory(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API}/chat/history/${sessionId}`);
  }

  clearHistory(sessionId: string): Observable<any> {
    return this.http.delete(`${this.API}/chat/history/${sessionId}`);
  }
}
