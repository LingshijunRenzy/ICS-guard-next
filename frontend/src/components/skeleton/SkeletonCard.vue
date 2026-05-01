<template>
  <el-card shadow="hover" class="skeleton-card">
    <div class="skeleton-content">
      <div class="skeleton-icon skeleton-shimmer" />
      <div class="skeleton-lines">
        <div class="skeleton-line skeleton-line-sm skeleton-shimmer" />
        <div class="skeleton-line skeleton-line-lg skeleton-shimmer" />
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.skeleton-card {
  border-radius: var(--radius-2xl);
  border: none;
}

.skeleton-content {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-sm) var(--space-xs);
}

.skeleton-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-icon);
  flex-shrink: 0;
}

.skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  border-radius: var(--radius-sm);
  height: 16px;
  background: #e8e8e8;
}

.skeleton-line-sm {
  width: 60%;
}

.skeleton-line-lg {
  width: 40%;
  height: 24px;
}

.skeleton-shimmer {
  background: linear-gradient(90deg, #e8e8e8 25%, #f5f5f5 50%, #e8e8e8 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
