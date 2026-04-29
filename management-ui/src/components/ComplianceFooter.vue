<template>
  <footer class="compliance-footer">
    <span>{{ siteConfig.copyright }}</span>
    <nav class="compliance-footer__links">
      <a :href="siteConfig.icpUrl" target="_blank" rel="noopener noreferrer">
        {{ siteConfig.icpNumber }}
      </a>
      <router-link v-if="!userStore.isDeveloper" to="/manual">使用手册</router-link>
      <router-link to="/privacy">隐私政策</router-link>
      <router-link to="/terms">服务条款</router-link>
      <a :href="`mailto:${siteConfig.supportEmail}`">联系技术支持</a>
    </nav>
  </footer>
</template>

<script setup>
import { siteConfig } from '@/config/site'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'ComplianceFooter' })

const userStore = useUserStore()
</script>

<style scoped>
.compliance-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem 1.5rem;
  padding: 0.875rem 2rem;
  color: rgba(95, 90, 78, 0.82);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  background: rgba(255, 253, 248, 0.9);
  border-top: 1px solid rgba(233, 197, 109, 0.42);
  backdrop-filter: blur(16px);
}

.compliance-footer__links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.75rem 1.5rem;
}

.compliance-footer a {
  color: inherit;
  text-decoration: none;
  transition: color 0.18s ease;
}

.compliance-footer a:hover {
  color: #f5a400;
}

@media (max-width: 768px) {
  .compliance-footer {
    justify-content: center;
    padding: 1rem;
    text-align: center;
  }
}
</style>
