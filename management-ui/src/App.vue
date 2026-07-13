<template>
  <el-config-provider :locale="zhCn">
    <div class="app-compliance-shell">
      <router-view class="app-compliance-page" />
      <ComplianceFooter />
      <GlobalRequestOverlay />
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import GlobalRequestOverlay from '@/components/GlobalRequestOverlay.vue'

defineOptions({ name: 'App' })
</script>

<style>
.app-compliance-shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow-x: hidden;
}

.app-compliance-page {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
}
</style>
