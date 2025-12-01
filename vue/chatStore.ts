// src/stores/chatStore.ts
import { defineStore } from 'pinia';
import { ChatMessage } from 'src/types/chat';
import { api } from 'src/boot/axios';

interface ChatState {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  chats: any[];
  currentChatId: string | null;
  messages: ChatMessage[];
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    chats: [],
    currentChatId: null,
    messages: [],
  }),

  actions: {
    async fetchChats() {
      const response = await api.get('/chat/chats');
      this.chats = response.data;
    },
    async loadChat(chatId: string) {
      this.currentChatId = chatId;
      localStorage.setItem('currentChatId', chatId);
      const response = await api.get(`/chat/chats/${chatId}`);
      this.messages = response.data.messages;
    },

    async createNewChat() {
      const response = await api.post('/chat/chats');
      this.currentChatId = response.data._id;
      localStorage.setItem('currentChatId', this.currentChatId!);
      this.messages = [];
      await this.fetchChats();
    },
    addMessage(message: ChatMessage) {
      this.messages.push(message);
    },

    

    // Clears all messages from the store
    clearMessages() {
      this.messages = [];
    },
  },
});
