<template>
  <t-card bordered class="skeleton-list-card">
    <template #header>
      <div class="skeleton-card-header">
        <div class="skeleton-line skeleton-line-md skeleton-shimmer" />
      </div>
    </template>
    <div class="skeleton-items">
      <div v-for="i in items" :key="i" class="skeleton-item">
        <div class="skeleton-line skeleton-line-label skeleton-shimmer" />
        <div class="skeleton-bar skeleton-shimmer" />
        <div class="skeleton-line skeleton-line-value skeleton-shimmer" />
      </div>
    </div>
  </t-card>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ items?: number }>(), { items: 4 })
</script>

<style scoped>
.skeleton-list-card {
  border-radius: var(--radius-2xl);
}

.skeleton-card-header {
  display: flex;
  align-items: center;
}

.skeleton-items {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  padding: var(--space-sm) 0;
}

.skeleton-item {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.skeleton-line {
  border-radius: var(--radius-sm);
  background: #e8e8e8;
  height: 14px;
}

.skeleton-line-md { width: 40%; }
.skeleton-line-label { width: 80px; flex-shrink: 0; }

.skeleton-bar {
  flex: 1;
  height: 10px;
  border-radius: 3px;
  background: #e8e8e8;
}

.skeleton-line-value { width: 40px; flex-shrink: 0; }

.skeleton-shimmer {
  background: linear-gradient(90deg, #e8e8e8 25%, #f5f5f5 50%, #e8e8e8 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
