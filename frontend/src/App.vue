<template>
  <t-config-provider :global-config="tdesignLocale">
    <router-view />
  </t-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import enConfig from 'tdesign-vue-next/es/locale/en_US'
import zhConfig from 'tdesign-vue-next/es/locale/zh_CN'

const { locale } = useI18n()

const tdesignLocale = computed(() => (locale.value === 'zh' ? zhConfig : enConfig))
</script>

<style>
body, html {
  margin: 0;
  padding: 0;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  height: 100%;
}
#app {
  height: 100vh;
}
</style>
