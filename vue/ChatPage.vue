<!-- src/pages/ChatPage.vue -->
<template>
  <q-page padding>
    <q-drawer v-model="drawerOpen" side="left" bordered>
      <q-list>
        <q-item label="Create New Chat" clickable @click="createNewChat">
          <q-item-section>
            <q-icon name="add" />
            <span class="q-ml-sm">Create New Chat</span>
          </q-item-section>
        </q-item>
        <q-separator />
        <q-item
          v-for="chat in chats"
          :key="chat._id"
          clickable
          @click="selectChat(chat._id)"
        >
          <q-item-section>
            <div v-if="editingChatId !== chat._id">
              {{ chat.name }}
            </div>
            <div v-else>
              <q-input
                v-model="chat.name"
                dense
                autofocus
                @blur="updateChatName(chat)"
                @keyup.enter="updateChatName(chat)"
              />
            </div>
          </q-item-section>
          <q-item-section side>
            <q-btn
              flat
              icon="edit"
              size="sm"
              @click.stop="editChatName(chat._id)"
            />
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-btn icon="menu" @click="toggleDrawer" />

    <div class="column full-height">
      <!-- Chat Messages -->
      <div class="col grow scroll">
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="q-mb-md message-container"
        >
          <div v-if="message.sender === 'user'">
            <q-chat-message
              :text="[message.text]"
              :sent="true"
              :name="'You'"
              :avatar="userAvatar"
              bg-color="blue"
              text-color="white"
              class="user-message"
            />
          </div>
          <div v-else>
            <q-chat-message
              :text="[message.text]"
              :sent="false"
              :name="'VGRS'"
              :avatar="botAvatar"
              bg-color="white"
              text-color="dark-grey"
              class="bot-message"
            />
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="row items-center q-pt-md input-area">
        <q-input
          filled
          v-model="inputText"
          placeholder="Type a message..."
          @keyup.enter="sendMessage"
          class="col message-input"
        />
        <q-btn
          color="primary"
          icon="send"
          @click="sendMessage"
          round
          dense
          class="q-ml-sm send-button"
        />
      </div>
    </div>
  </q-page>

  <!-- Accept Game Recommendation Dialog -->
  <q-dialog v-model="acceptGameDialog">
    <q-card>
      <q-card-section>
        Do you want to accept this game recommendation?
      </q-card-section>
      <q-card-actions align="right">
        <q-btn flat label="Cancel" v-close-popup />
        <q-btn flat label="Accept" @click="acceptGame(recommendedGameId)" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useChatStore } from 'src/stores/chatStore';
import { ChatMessage } from 'src/types/chat';
import { api } from 'src/boot/axios';

const drawerOpen = ref(false);
const inputText = ref<string>('');
const userAvatar = 'src/assets/user_icon.png';
const botAvatar = 'src/assets/NPC_Fairy.jpg';

const chatStore = useChatStore();
const chats = computed(() => chatStore.chats);
const messages = computed<ChatMessage[]>(() => chatStore.messages);

const editingChatId = ref<string | null>(null);

onMounted(async () => {
  await chatStore.fetchChats();
  const lastChatId = localStorage.getItem('currentChatId');
  if (lastChatId) {
    await chatStore.loadChat(lastChatId);
  }
  console.log(chatStore.chats);
});

function toggleDrawer() {
  drawerOpen.value = !drawerOpen.value;
}

async function selectChat(chatId: string) {
  await chatStore.loadChat(chatId);
  drawerOpen.value = false;
}

async function createNewChat() {
  await chatStore.createNewChat();
  drawerOpen.value = false;
}

function editChatName(chatId: string) {
  editingChatId.value = chatId;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function updateChatName(chat: any) {
  try {
    await api.patch(`/chat/chats/${chat._id}`, { name: chat.name });
    editingChatId.value = null;
    await chatStore.fetchChats();
  } catch (error) {
    console.error('Error updating chat name:', error);
  }
}

async function sendMessage() {
  const trimmedInput = inputText.value.trim();
  if (trimmedInput === '') return;

  const userMessage: ChatMessage = {
    text: trimmedInput,
    sender: 'user',
  };
  chatStore.addMessage(userMessage);
  inputText.value = '';
  await sendToBackend(userMessage.text);
}

async function sendToBackend(text: string) {
  try {
    const response = await api.post('/chatgpt/ask', {
      question: text,
      chatId: chatStore.currentChatId,
    });
    const data = response.data;

    const botMessage: ChatMessage = {
      text: data.answer || 'Sorry, I could not process your request.',
      sender: 'bot',
    };
    chatStore.addMessage(botMessage);

    if (!chatStore.currentChatId) {
      chatStore.currentChatId = data.chatId;
      await chatStore.fetchChats();
    }

    if (data.recommendedGames && data.recommendedGames.length > 0) {
      showAcceptGamePopup(data.recommendedGames[0]);
    }
  } catch (error) {
    console.error('Error communicating with backend:', error);
    const errorMessage: ChatMessage = {
      text: 'An error occurred while communicating with the server.',
      sender: 'bot',
    };
    chatStore.addMessage(errorMessage);
  }
}

const acceptGameDialog = ref(false);
const recommendedGameId = ref('');

function showAcceptGamePopup(gameId: string) {
  recommendedGameId.value = gameId;
  acceptGameDialog.value = true;
}

async function acceptGame(gameId: string) {
  await api.post(`/chat/chats/${chatStore.currentChatId}/accept-game`, { gameId });
  acceptGameDialog.value = false;
}
</script>

<style lang="scss" scoped>
$primary-black: #000000;
$primary-orange: #FF6F00;
$accent-grey: #4F4F4F;
$accent-white: #FFFFFF;
$accent-dark-blue: #1E3A8A;
$accent-neon-green: hsl(111, 32%, 64%);
$accent-light-grey: #cacaca;
$bot-bg: #2E2E2E;
$user-bg: #1E3A8A;

.column {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.q-page {
  background: linear-gradient(135deg, #2b2b2b, #1a1a1a);
}

.scroll {
  overflow-y: auto;
}

.message-container {
  margin-bottom: 12px;
}

.user-message {
  background-color: $user-bg;
  border-radius: 16px;
  color: $accent-white;
  padding: 8px 16px;
}

.bot-message {
  background-color: $bot-bg;
  border-radius: 16px;
  color: $accent-light-grey;
  padding: 8px 16px;
}

.input-area {
  background-color: $accent-grey;
  border-top: 1px solid $accent-light-grey;
  padding: 12px 16px;
}


.message-input {
  background-color: #333;
  color: #FFFFFF;
  border-radius: 8px;

  ::v-deep .q-field__native {
    color: #FFFFFF;
  }
}
.q-field

.send-button {
  background-color: $primary-orange;
  color: $accent-white;
  border-radius: 50%;
  &:hover {
    background-color: $primary-orange;
  }
}
</style>
