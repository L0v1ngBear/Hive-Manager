<template>
  <main class="flex min-h-[calc(100vh-5rem)] items-center justify-center px-6 py-12">
    <section class="w-full max-w-xl rounded-lg border border-outline-variant/30 bg-white px-8 py-10 text-center shadow-sm">
      <span class="material-symbols-outlined text-5xl text-on-surface-variant/45">lock</span>
      <h1 class="mt-5 text-2xl font-black text-on-surface">暂无权限</h1>
      <p class="mt-3 text-sm leading-6 text-on-surface-variant">
        当前账号未分配该入口权限，页面内容已隐藏。请联系企业负责人在角色管理或员工单独权限中开通。
      </p>
      <button
        class="mt-7 rounded-lg bg-primary px-6 py-3 text-sm font-bold text-white shadow-sm shadow-primary/20"
        @click="goBack"
      >
        返回上一页
      </button>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineOptions({ name: 'NoPermission' })

const router = useRouter()

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.replace('/dashboard')
}
</script>
