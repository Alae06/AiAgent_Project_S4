import { Component, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef, Pipe, PipeTransform } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService } from './services/service';

@Pipe({ name: 'stripMarkdown', standalone: true })
export class StripMarkdownPipe implements PipeTransform {
  transform(text: string): string {
    if (!text) return '';
    return text
      .replace(/#{1,6}\s+/g, '')
      .replace(/\*\*(.+?)\*\*/gs, '$1')
      .replace(/\*(.+?)\*/gs, '$1')
      .replace(/__(.+?)__/gs, '$1')
      .replace(/_(.+?)_/gs, '$1')
      .replace(/~~(.+?)~~/gs, '$1')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/`(.+?)`/g, '$1')
      .replace(/\[(.+?)\]\(.+?\)/g, '$1')
      .replace(/^[-*+]\s+/gm, '- ')
      .replace(/^\d+\.\s+/gm, '')
      .replace(/^>\s+/gm, '')
      .replace(/[-*_]{3,}/g, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }
}

interface Message {
  from: 'user' | 'ai';
  text: string;
  time: string;
  isVoice?: boolean;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, StripMarkdownPipe],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent implements AfterViewChecked {

  @ViewChild('msgBox') msgBox!: ElementRef;

  messages: Message[] = [];
  question = '';
  isLoading = false;
  sessionId = 'session-' + Date.now();

  fileName = '';
  isUploading = false;
  documentReady = false;
  uploadError = '';

  isListening = false;
  private recognition: any = null;

  voiceOutputEnabled = false;
  isSpeaking = false;

  constructor(private aiService: AiService, private cdr: ChangeDetectorRef) {
    this.initSpeechRecognition();
  }

  ngAfterViewChecked() {
    try {
      this.msgBox.nativeElement.scrollTop = this.msgBox.nativeElement.scrollHeight;
    } catch {}
  }

  private initSpeechRecognition() {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) return;
    this.recognition = new SR();
    this.recognition.continuous = false;
    this.recognition.interimResults = false;
    this.recognition.lang = 'fr-FR';

    this.recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript.trim();
      this.isListening = false;
      if (transcript) {
        this.addMessage('user', transcript, true);
        this.isLoading = true;
        this.cdr.detectChanges();
        this.askAndSpeak(transcript);
      } else {
        this.cdr.detectChanges();
      }
    };

    this.recognition.onend = () => {
      this.isListening = false;
      this.cdr.detectChanges();
    };

    this.recognition.onerror = (event: any) => {
      this.isListening = false;
      const err = event.error;
      if (err === 'no-speech') {
        this.addMessage('ai', 'Aucune voix detectee. Reessayez en parlant clairement.');
      } else if (err === 'audio-capture') {
        this.addMessage('ai', 'Microphone introuvable. Verifiez votre microphone.');
      } else if (err === 'not-allowed') {
        this.addMessage('ai', 'Acces micro refuse. Autorisez le micro dans votre navigateur.');
      } else {
        this.addMessage('ai', 'Erreur vocale: ' + err + '. Tapez votre question.');
      }
      this.cdr.detectChanges();
    };
  }

  toggleListening() {
    if (!this.recognition) {
      this.addMessage('ai', 'La reconnaissance vocale necessite Chrome ou Edge.');
      this.cdr.detectChanges();
      return;
    }
    if (this.isListening) {
      this.recognition.stop();
      this.isListening = false;
      this.cdr.detectChanges();
    } else {
      try {
        this.recognition.start();
        this.isListening = true;
        this.cdr.detectChanges();
      } catch {
        this.recognition.stop();
        setTimeout(() => {
          this.recognition.start();
          this.isListening = true;
          this.cdr.detectChanges();
        }, 300);
      }
    }
  }

  toggleVoiceOutput() {
    this.voiceOutputEnabled = !this.voiceOutputEnabled;
    if (!this.voiceOutputEnabled) {
      window.speechSynthesis.cancel();
      this.isSpeaking = false;
    }
    this.cdr.detectChanges();
  }

  private speak(text: string) {
    if (!this.voiceOutputEnabled) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(this.stripMarkdown(text));
    utterance.lang = 'fr-FR';
    utterance.rate = 1.0;
    const voices = window.speechSynthesis.getVoices();
    const frVoice = voices.find(v => v.lang.startsWith('fr'));
    if (frVoice) utterance.voice = frVoice;
    utterance.onstart = () => { this.isSpeaking = true; this.cdr.detectChanges(); };
    utterance.onend   = () => { this.isSpeaking = false; this.cdr.detectChanges(); };
    utterance.onerror = () => { this.isSpeaking = false; this.cdr.detectChanges(); };
    window.speechSynthesis.speak(utterance);
  }

  stopSpeaking() {
    window.speechSynthesis.cancel();
    this.isSpeaking = false;
    this.cdr.detectChanges();
  }

  private stripMarkdown(text: string): string {
    if (!text) return '';
    return text
      .replace(/#{1,6}\s+/g, '')
      .replace(/\*\*(.+?)\*\*/gs, '$1')
      .replace(/\*(.+?)\*/gs, '$1')
      .replace(/__(.+?)__/gs, '$1')
      .replace(/_(.+?)_/gs, '$1')
      .replace(/~~(.+?)~~/gs, '$1')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/`(.+?)`/g, '$1')
      .replace(/\[(.+?)\]\(.+?\)/g, '$1')
      .replace(/^[-*+]\s+/gm, '')
      .replace(/^\d+\.\s+/gm, '')
      .replace(/^>\s+/gm, '')
      .replace(/[-*_]{3,}/g, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }

  sendQuestion() {
    const q = this.question.trim();
    if (!q || !this.documentReady || this.isLoading) return;
    this.addMessage('user', q);
    this.question = '';
    this.isLoading = true;
    this.cdr.detectChanges();
    this.askAndSpeak(q);
  }

  private askAndSpeak(question: string) {
    this.aiService.askQuestion(question, this.sessionId).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.addMessage('ai', res.answer);
        this.cdr.detectChanges();
        this.speak(res.answer);
      },
      error: (err) => {
        this.isLoading = false;
        this.addMessage('ai', 'Erreur de reponse. Verifiez que le backend tourne sur le port 8080.');
        this.cdr.detectChanges();
        console.error(err);
      }
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.fileName = file.name;
    this.isUploading = true;
    this.uploadError = '';
    this.documentReady = false;
    this.cdr.detectChanges();
    this.aiService.uploadDocument(file).subscribe({
      next: (res) => {
        this.isUploading = false;
        this.documentReady = true;
        this.addMessage('ai', 'Document pret. ' + res.message + '\n\nVous pouvez maintenant poser vos questions.');
        this.cdr.detectChanges();
      },
      error: () => {
        this.isUploading = false;
        this.uploadError = 'Echec du chargement. Le backend tourne sur le port 8080 ?';
        this.cdr.detectChanges();
      }
    });
  }

  clearChat() {
    window.speechSynthesis.cancel();
    this.messages = [];
    this.aiService.clearHistory(this.sessionId).subscribe();
    this.sessionId = 'session-' + Date.now();
    this.cdr.detectChanges();
  }

  private addMessage(from: 'user' | 'ai', text: string, isVoice = false) {
    const d = new Date();
    const time = d.getHours().toString().padStart(2, '0') + ':'
      + d.getMinutes().toString().padStart(2, '0');
    this.messages.push({ from, text, time, isVoice });
  }
}
