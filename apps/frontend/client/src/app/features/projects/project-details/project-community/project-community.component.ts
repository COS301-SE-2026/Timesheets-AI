/*
Project Community: the # channel for a project.
- messages (polls every 8s, no websockets)
- 5 reactions (like, dislike, done, issue, important)
- @mention autocomplete (mentions notify the teammate)

Author: Zamokuhle Zwane
Date: 28/09/2026
*/

import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { EMPTY, Subscription, catchError, exhaustMap, filter, interval, map } from 'rxjs';
import {
  ProjectCommunityService,
  ProjectMessage,
  ReactionSummary,
  ReactionType,
} from '../../../../core/services/project-community.service';

// only what the community needs from a project member
export interface CommunityMember {
  workspaceMemberId: string;
  firstName: string;
  lastName: string;
}

interface Segment {
  text: string;
  mention: boolean;
}

type FeedItem =
  | { kind: 'day'; key: string; label: string }
  | {
      kind: 'message';
      key: string;
      message: ProjectMessage;
      grouped: boolean;
      time: string;
      segments: Segment[];
      initials: string;
      colour: string;
    };

const REACTION_ORDER: ReactionType[] = ['LIKE', 'DISLIKE', 'CHECK', 'CROSS', 'IMPORTANT'];

const REACTION_META: Record<ReactionType, { icon: string; label: string; tone: string }> = {
  LIKE: { icon: 'fa-solid fa-thumbs-up', label: 'Like', tone: 'like' },
  DISLIKE: { icon: 'fa-solid fa-thumbs-down', label: 'Dislike', tone: 'dislike' },
  CHECK: { icon: 'fa-solid fa-circle-check', label: 'Done', tone: 'check' },
  CROSS: { icon: 'fa-solid fa-xmark', label: 'Issue', tone: 'cross' },
  IMPORTANT: { icon: 'fa-solid fa-circle-exclamation', label: 'Important', tone: 'important' },
};

const QUICK_EMOJIS = ['🙂', '👍', '🎉', '🚀', '👀', '🙏'];
const POLL_MS = 8000;
const GROUP_WINDOW_MS = 5 * 60 * 1000;

@Component({
  selector: 'app-project-community',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-community.component.html',
  styleUrl: './project-community.component.scss',
})
export class ProjectCommunityComponent implements OnInit, OnDestroy {
  private readonly community = inject(ProjectCommunityService);

  readonly projectId = input.required<string>();
  readonly projectName = input<string>('');
  readonly members = input<CommunityMember[]>([]);
  // set when arriving from a notification: scroll to and flash this message
  readonly highlightMessageId = input<string | null>(null);

  @ViewChild('scroller') private scroller?: ElementRef<HTMLElement>;
  @ViewChild('composer') private composer?: ElementRef<HTMLInputElement>;

  protected readonly meta = REACTION_META;
  protected readonly order = REACTION_ORDER;
  protected readonly emojis = QUICK_EMOJIS;

  protected readonly messages = signal<ProjectMessage[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly sendError = signal<string | null>(null);
  protected readonly sending = signal(false);
  protected readonly draft = signal('');
  protected readonly emojiOpen = signal(false);
  protected readonly mentionQuery = signal<string | null>(null);
  protected readonly mentionIndex = signal(0);
  protected readonly flashId = signal<string | null>(null);

  private pollSub?: Subscription;
  private initialised = false;
  private stickToBottom = true;
  // bumped on every local change so a slow poll can't overwrite it with stale data
  private mutationVersion = 0;
  private readonly reacting = new Set<string>();

  // "Project Alpha" -> "project-alpha"
  protected readonly channelName = computed(() => {
    const slug = this.projectName()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
    return slug || 'project';
  });

  protected readonly suggestions = computed<CommunityMember[]>(() => {
    const query = this.mentionQuery();
    if (query === null) {
      return [];
    }
    const needle = query.toLowerCase();
    return this.members()
      .filter((member) =>
        `${member.firstName} ${member.lastName}`
          .toLowerCase()
          .split(/\s+/)
          .some((part) => part.startsWith(needle)),
      )
      .slice(0, 5);
  });

  private readonly mentionRegex = computed<RegExp | null>(() => {
    const names = this.members()
      .map((member) => `${member.firstName} ${member.lastName}`.trim())
      .filter(Boolean)
      .sort((a, b) => b.length - a.length)
      .map((name) => name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    return names.length ? new RegExp(`@(${names.join('|')})(?!\\w)`, 'gi') : null;
  });

  // messages interleaved with day dividers; follow-up messages from the same person collapse
  protected readonly feed = computed<FeedItem[]>(() => {
    const items: FeedItem[] = [];
    const regex = this.mentionRegex();
    let lastDay = '';
    let previous: ProjectMessage | null = null;

    for (const message of this.messages()) {
      const created = new Date(message.createdAt);
      const day = created.toDateString();

      if (day !== lastDay) {
        items.push({ kind: 'day', key: `day-${day}`, label: this.dayLabel(created) });
        lastDay = day;
        previous = null;
      }

      const grouped =
        previous !== null &&
        previous.authorWorkspaceMemberId === message.authorWorkspaceMemberId &&
        created.getTime() - new Date(previous.createdAt).getTime() < GROUP_WINDOW_MS;

      items.push({
        kind: 'message',
        key: message.id,
        message,
        grouped,
        time: created.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
        segments: this.toSegments(message.content, regex),
        initials: this.initials(message.authorName),
        colour: message.mine ? 'avatar-me' : this.colourFor(message.authorName),
      });

      previous = message;
    }

    return items;
  });

  ngOnInit(): void {
    this.fetchInitial();

    this.pollSub = interval(POLL_MS)
      .pipe(
        filter(() => this.initialised && !document.hidden && !this.sending()),
        exhaustMap(() => {
          const version = this.mutationVersion;
          return this.community.getMessages(this.projectId()).pipe(
            map((list) => ({ list, version })),
            catchError(() => EMPTY),
          );
        }),
      )
      .subscribe(({ list, version }) => {
        if (version === this.mutationVersion) {
          this.applyMessages(list);
        }
      });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  protected retry(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.fetchInitial();
  }

  private fetchInitial(): void {
    this.community.getMessages(this.projectId()).subscribe({
      next: (list) => {
        this.loading.set(false);
        this.loadError.set(false);
        this.applyMessages(list);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  private applyMessages(list: ProjectMessage[]): void {
    const grew = list.length > this.messages().length;
    this.messages.set(list);

    if (!this.initialised) {
      this.initialised = true;
      this.scrollInitial();
    } else if (grew && this.stickToBottom) {
      this.scrollToBottom();
    }
  }

  // sending

  protected send(): void {
    const content = this.draft().trim();
    if (!content || this.sending()) {
      return;
    }

    this.sending.set(true);
    this.sendError.set(null);
    this.mutationVersion++;

    this.community.sendMessage(this.projectId(), content).subscribe({
      next: (message) => {
        this.messages.update((list) => [...list, message]);
        this.draft.set('');
        this.sending.set(false);
        this.closeMention();
        this.emojiOpen.set(false);
        this.stickToBottom = true;
        this.scrollToBottom();
        this.composer?.nativeElement.focus();
      },
      // the draft stays in the box so nothing is lost
      error: () => {
        this.sending.set(false);
        this.sendError.set("Couldn't send your message. Please try again.");
      },
    });
  }

  protected remove(message: ProjectMessage): void {
    if (!window.confirm('Delete this message?')) {
      return;
    }
    this.mutationVersion++;
    this.community.deleteMessage(this.projectId(), message.id).subscribe({
      next: () => this.messages.update((list) => list.filter((m) => m.id !== message.id)),
      error: () => this.sendError.set("Couldn't delete that message."),
    });
  }

  // reactions

  protected react(message: ProjectMessage, type: ReactionType): void {
    const key = `${message.id}:${type}`;
    if (this.reacting.has(key)) {
      return;
    }
    this.reacting.add(key);
    this.mutationVersion++;

    // update instantly, then replace with what the server says (or roll back)
    this.replace(this.withToggledReaction(message, type));

    this.community.toggleReaction(this.projectId(), message.id, type).subscribe({
      next: (updated) => {
        this.reacting.delete(key);
        this.replace(updated);
      },
      error: () => {
        this.reacting.delete(key);
        this.replace(message);
      },
    });
  }

  private withToggledReaction(message: ProjectMessage, type: ReactionType): ProjectMessage {
    const existing = message.reactions.find((r) => r.type === type);
    let reactions: ReactionSummary[];

    if (!existing) {
      reactions = [...message.reactions, { type, count: 1, reactedByMe: true }];
    } else if (existing.reactedByMe) {
      reactions = message.reactions
        .map((r) => (r.type === type ? { ...r, count: r.count - 1, reactedByMe: false } : r))
        .filter((r) => r.count > 0);
    } else {
      reactions = message.reactions.map((r) =>
        r.type === type ? { ...r, count: r.count + 1, reactedByMe: true } : r,
      );
    }

    reactions.sort((a, b) => REACTION_ORDER.indexOf(a.type) - REACTION_ORDER.indexOf(b.type));
    return { ...message, reactions };
  }

  private replace(updated: ProjectMessage): void {
    this.messages.update((list) => list.map((m) => (m.id === updated.id ? updated : m)));
  }

  // composer: typing, @mentions, emoji

  protected onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.draft.set(input.value);
    this.updateMention(input);
  }

  protected onCaretMove(): void {
    const input = this.composer?.nativeElement;
    if (input) {
      this.updateMention(input);
    }
  }

  protected onKeydown(event: KeyboardEvent): void {
    const options = this.suggestions();

    if (options.length) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        this.mentionIndex.update((i) => (i + 1) % options.length);
        return;
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault();
        this.mentionIndex.update((i) => (i - 1 + options.length) % options.length);
        return;
      }
      if (event.key === 'Enter' || event.key === 'Tab') {
        event.preventDefault();
        this.pickMention(options[this.mentionIndex()] ?? options[0]);
        return;
      }
      if (event.key === 'Escape') {
        this.closeMention();
        return;
      }
    }

    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      this.send();
    }
  }

  private updateMention(input: HTMLInputElement): void {
    const caret = input.selectionStart ?? input.value.length;
    const match = /(?:^|\s)@([\w.-]*)$/.exec(input.value.slice(0, caret));
    this.mentionQuery.set(match ? match[1] : null);
    this.mentionIndex.set(0);
  }

  protected closeMention(): void {
    this.mentionQuery.set(null);
    this.mentionIndex.set(0);
  }

  protected pickMention(member: CommunityMember): void {
    const input = this.composer?.nativeElement;
    if (!input) {
      return;
    }
    const caret = input.selectionStart ?? input.value.length;
    const before = input.value
      .slice(0, caret)
      .replace(/@[\w.-]*$/, `@${member.firstName} ${member.lastName} `);
    const value = before + input.value.slice(caret);

    input.value = value;
    this.draft.set(value);
    this.closeMention();
    input.focus();
    input.setSelectionRange(before.length, before.length);
  }

  protected insertEmoji(emoji: string): void {
    const input = this.composer?.nativeElement;
    if (!input) {
      return;
    }
    const start = input.selectionStart ?? input.value.length;
    const end = input.selectionEnd ?? start;
    const value = input.value.slice(0, start) + emoji + input.value.slice(end);

    input.value = value;
    this.draft.set(value);
    this.emojiOpen.set(false);
    input.focus();
    input.setSelectionRange(start + emoji.length, start + emoji.length);
  }

  // scrolling

  protected onScroll(): void {
    const el = this.scroller?.nativeElement;
    if (el) {
      this.stickToBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.scroller?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }

  private scrollInitial(): void {
    const target = this.highlightMessageId();
    if (!target) {
      this.scrollToBottom();
      return;
    }

    setTimeout(() => {
      const el = document.getElementById(`msg-${target}`);
      if (!el) {
        this.scrollToBottom();
        return;
      }
      el.scrollIntoView({ block: 'center' });
      this.stickToBottom = false;
      this.flashId.set(target);
      setTimeout(() => this.flashId.set(null), 3000);
    });
  }

  // display helpers

  protected initials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (!parts.length) {
      return '?';
    }
    return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
  }

  private colourFor(name: string): string {
    let sum = 0;
    for (const char of name) {
      sum += char.charCodeAt(0);
    }
    return `avatar-${sum % 4}`;
  }

  private dayLabel(date: Date): string {
    const formatted = date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return `Today, ${formatted}`;
    }
    if (date.toDateString() === yesterday.toDateString()) {
      return `Yesterday, ${formatted}`;
    }
    return formatted;
  }

  // splits a message into plain text and @Name pieces so mentions can be highlighted
  private toSegments(content: string, regex: RegExp | null): Segment[] {
    if (!regex) {
      return [{ text: content, mention: false }];
    }

    const segments: Segment[] = [];
    let last = 0;

    for (const match of content.matchAll(regex)) {
      const start = match.index ?? 0;
      if (start > last) {
        segments.push({ text: content.slice(last, start), mention: false });
      }
      segments.push({ text: match[0], mention: true });
      last = start + match[0].length;
    }

    if (last < content.length) {
      segments.push({ text: content.slice(last), mention: false });
    }

    return segments.length ? segments : [{ text: content, mention: false }];
  }
}