<!-- src/pages/LoginPage.vue -->
<template>
    <q-page class="login-page flex flex-center">
      <div class="login-container q-pa-md q-gutter-md">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-h6">Login</div>
          </q-card-section>
  
          <q-card-section>
            <q-input v-model="username" label="Username" dense outlined />
            <q-input v-model="password" label="Password" type="password" dense outlined />
          </q-card-section>
  
          <q-card-actions align="center">
            <q-btn label="Login" color="primary" @click="login" />
          </q-card-actions>
  
          <q-card-section class="q-pt-none">
            <div>
              Don't have an account?
              <router-link to="/register">Register</router-link>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </q-page>
  </template>
  
  <script setup lang="ts">
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { api } from 'src/boot/axios';
  import { Notify } from 'quasar';
  
  const router = useRouter();
  const username = ref('');
  const password = ref('');
  
  async function login() {
    try {
      const response = await api.post('/auth/login', {
        username: username.value,
        password: password.value,
      });
      localStorage.setItem('token', response.data.token);
      Notify.create({
        type: 'positive',
        message: 'Login successful',
      });
      router.push('/chat');
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (error: any) {
      console.error('Login failed:', error);
      Notify.create({
        type: 'negative',
        message: error.response?.data?.message || 'Login failed',
      });
    }
  }
  </script>
  
  <style scoped>
  .login-page {
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .login-container {
    width: 300px;
  }
  </style>
  