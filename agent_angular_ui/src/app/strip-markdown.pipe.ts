import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'stripMarkdown', standalone: true })
export class StripMarkdownPipe implements PipeTransform {
  transform(text: string): string {
    if (!text) return '';
    return text
      .replace(/#{1,6}\s+/g, '')           // ## headings
      .replace(/\*\*(.+?)\*\*/g, '$1')     // **bold**
      .replace(/\*(.+?)\*/g, '$1')         // *italic*
      .replace(/__(.+?)__/g, '$1')         // __bold__
      .replace(/_(.+?)_/g, '$1')           // _italic_
      .replace(/~~(.+?)~~/g, '$1')         // ~~strikethrough~~
      .replace(/`{3}[\s\S]*?`{3}/g, '')    // ```code blocks```
      .replace(/`(.+?)`/g, '$1')           // `inline code`
      .replace(/\[(.+?)\]\(.+?\)/g, '$1') // [links](url)
      .replace(/^[-*+]\s+/gm, '• ')        // - bullet points
      .replace(/^\d+\.\s+/gm, '')          // 1. numbered lists
      .replace(/^>{1,}\s+/gm, '')          // > blockquotes
      .replace(/[-*_]{3,}/g, '')           // --- horizontal rules
      .replace(/\n{3,}/g, '\n\n')          // collapse excess newlines
      .trim();
  }
}
