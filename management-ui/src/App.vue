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
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import GlobalRequestOverlay from '@/components/GlobalRequestOverlay.vue'
import { getCurrentSession } from '@/api/auth'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'App' })

const router = useRouter()
const userStore = useUserStore()

onMounted(async () => {
  if (!userStore.token) {
    return
  }
  try {
    const session = await getCurrentSession()
    userStore.refreshCurrentSession(session)
    await router.replace(router.currentRoute.value.fullPath)
  } catch {
    // The shared request handler clears an invalid session and redirects to login.
  }
})
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
