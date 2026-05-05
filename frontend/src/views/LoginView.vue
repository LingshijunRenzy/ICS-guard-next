<template>
  <t-layout class="login-container">
    <t-card class="login-card" :bordered="false">
      <template #header>
        <h2 class="login-title">{{ $t('login.title') }}</h2>
      </template>
      <t-form ref="formRef" :data="form" :rules="rules" label-align="top">
        <t-form-item :label="$t('login.username')" name="username">
          <t-input v-model="form.username" :placeholder="$t('login.usernamePlaceholder')" @enter="handleLogin" />
        </t-form-item>
        <t-form-item :label="$t('login.password')" name="password">
          <t-input v-model="form.password" type="password" :placeholder="$t('login.passwordPlaceholder')" @enter="handleLogin" />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" :loading="auth.loading" block @click="handleLogin">
            {{ $t('login.loginBtn') }}
          </t-button>
        </t-form-item>
        <p v-if="error" class="error-msg">{{ error }}</p>
      </t-form>
    </t-card>
  </t-layout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import type { FormInstanceFunctions, FormRules } from 'tdesign-vue-next'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstanceFunctions>()
const error = ref('')

const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
}

async function handleLogin() {
  const result = await formRef.value?.validate()
  if (result !== true) return
  error.value = ''
  const res = await auth.login({ username: form.username, password: form.password })
  if (res.code === 200) {
    router.push('/')
  } else {
    error.value = res.msg || t('login.loginFailed')
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-login);
}

.login-card {
  width: 400px;
}

.login-title {
  text-align: center;
  margin: 0;
  letter-spacing: 2px;
}

.error-msg {
  color: var(--color-danger);
  text-align: center;
  margin: 0;
}
</style>
