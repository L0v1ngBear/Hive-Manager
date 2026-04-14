<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 font-body">
    <div class="max-w-6xl mx-auto">
      <section class="bg-surface-container-lowest rounded-2xl border border-outline-variant/20 shadow-sm p-6 md:p-8">
        <div class="flex items-start justify-between gap-6 flex-col md:flex-row">
          <div>
            <p class="text-sm font-bold tracking-[0.25em] text-primary/70 uppercase">平台管理</p>
            <h1 class="text-3xl md:text-4xl font-black tracking-tight text-primary mt-3">租户管理</h1>
            <p class="text-sm md:text-base text-on-surface-variant mt-4 max-w-2xl">
              这里已经预留为多租户管理入口，后端分页与详情接口也已经接好。等你把页面设计完成后，我们再把筛选、列表、详情抽屉补上。
            </p>
          </div>

          <div class="rounded-2xl bg-primary/8 border border-primary/15 px-5 py-4 min-w-[220px]">
            <div class="text-xs font-bold tracking-[0.2em] text-primary/70 uppercase">当前状态</div>
            <div class="text-lg font-black text-primary mt-2">已预留路由与权限</div>
            <div class="text-sm text-on-surface-variant mt-2">仅拥有租户查看权限的账号可见</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'PlatformTenantPage' })
</script>
