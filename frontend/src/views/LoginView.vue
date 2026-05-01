<template>
  <el-container class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 class="login-title">{{ $t('login.title') }}</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item :label="$t('login.username')" prop="username">
          <el-input v-model="form.username" :placeholder="$t('login.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('login.password')" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="$t('login.passwordPlaceholder')" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="auth.loading" class="login-btn">
            {{ $t('login.loginBtn') }}
          </el-button>
        </el-form-item>
        <p v-if="error" class="error-msg">{{ error }}</p>
      </el-form>
    </el-card>
  </el-container>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const error = ref('')

const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
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

.login-btn {
  width: 100%;
}

.error-msg {
  color: var(--color-danger);
  text-align: center;
  margin: 0;
}
</style>
