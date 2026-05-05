<template>
  <t-card bordered class="skeleton-table-card">
    <div class="skeleton-header">
      <div class="skeleton-line skeleton-line-md skeleton-shimmer" />
      <div class="skeleton-line skeleton-line-sm skeleton-shimmer" />
    </div>
    <div class="skeleton-rows">
      <div v-for="i in rows" :key="i" class="skeleton-row">
        <div v-for="j in cols" :key="j" class="skeleton-cell skeleton-shimmer" />
      </div>
    </div>
  </t-card>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ rows?: number; cols?: number }>(), { rows: 5, cols: 6 })
</script>

<style scoped>
.skeleton-table-card {
  border-radius: var(--radius-2xl);
}

.skeleton-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}

.skeleton-rows {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.skeleton-row {
  display: flex;
  gap: var(--space-xl);
  padding: var(--space-sm) 0;
}

.skeleton-cell {
  flex: 1;
  height: 18px;
  border-radius: var(--radius-sm);
  background: #e8e8e8;
}

.skeleton-line {
  border-radius: var(--radius-sm);
  background: #e8e8e8;
  height: 16px;
}

.skeleton-line-md { width: 30%; }
.skeleton-line-sm { width: 15%; height: 13px; }

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
