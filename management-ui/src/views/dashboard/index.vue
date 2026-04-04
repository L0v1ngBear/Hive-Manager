<template>
  <div class="max-w-7xl mx-auto space-y-8 pb-12">

    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h3 class="text-2xl font-black tracking-tight text-on-surface mb-1">早上好，陈总。</h3>
        <p class="text-on-surface-variant text-sm">当前生产效率高达 94%。有 2 条异常预警需要您的关注。</p>
      </div>
      <button class="bg-primary text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm hover:bg-primary/90 transition-colors flex items-center gap-2">
        <span class="material-symbols-outlined text-lg">add</span>
        新建生产单
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">

      <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm ring-1 ring-outline-variant/20 relative overflow-hidden group hover:shadow-md transition-shadow">
        <div class="flex justify-between items-start mb-6">
          <div class="w-10 h-10 rounded-lg bg-primary-container flex items-center justify-center text-on-primary-container">
            <span class="material-symbols-outlined">analytics</span>
          </div>
          <span class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-green-100 text-green-800 text-xs font-bold">
            <span class="material-symbols-outlined text-[14px]">trending_up</span> +4.2%
          </span>
        </div>
        <div>
          <p class="text-on-surface-variant text-sm font-medium mb-1">本月总产量 (米)</p>
          <h4 class="text-3xl font-black tracking-tight text-on-surface">124,500</h4>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm ring-1 ring-outline-variant/20 relative overflow-hidden group hover:shadow-md transition-shadow">
        <div class="flex justify-between items-start mb-6">
          <div class="w-10 h-10 rounded-lg bg-surface-container-high flex items-center justify-center text-on-surface">
            <span class="material-symbols-outlined">inventory_2</span>
          </div>
          <span class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-orange-100 text-orange-800 text-xs font-bold">
            2款面料临界
          </span>
        </div>
        <div>
          <p class="text-on-surface-variant text-sm font-medium mb-1">布匹库存总量 (卷)</p>
          <h4 class="text-3xl font-black tracking-tight text-on-surface">8,920</h4>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm ring-1 ring-outline-variant/20 relative overflow-hidden group hover:shadow-md transition-shadow">
        <div class="flex justify-between items-start mb-6">
          <div class="w-10 h-10 rounded-lg bg-error-container flex items-center justify-center text-on-error-container">
            <span class="material-symbols-outlined">warning</span>
          </div>
        </div>
        <div>
          <p class="text-on-surface-variant text-sm font-medium mb-1">待审批单据</p>
          <h4 class="text-3xl font-black tracking-tight text-error">12</h4>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

      <section class="lg:col-span-2 bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 p-6">
        <div class="flex justify-between items-center mb-6">
          <h4 class="text-base font-bold text-on-surface">本周织机综合运转效率</h4>
          <button class="text-on-surface-variant hover:text-primary"><span class="material-symbols-outlined">more_horiz</span></button>
        </div>
        <div class="h-64 bg-surface-container-low rounded-xl flex items-center justify-center text-on-surface-variant text-sm">
          [ Echarts 图表渲染区 ]
        </div>
      </section>

      <div class="space-y-6">
        <section class="bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 p-6">
          <h4 class="text-base font-bold text-on-surface mb-6">严重预警动态</h4>
          <div class="space-y-4">
            <div class="p-4 bg-error-container/50 rounded-xl border-l-4 border-error">
              <p class="text-xs font-bold text-error mb-1">织机设备停机</p>
              <p class="text-[12px] text-on-error-container leading-relaxed">B车间的 #04 与 #05 织机报告了主轴电机技术故障，需立即派单维修。</p>
            </div>
            <div class="p-4 bg-orange-50 rounded-xl border-l-4 border-orange-400">
              <p class="text-xs font-bold text-orange-600 mb-1">库存余量告警</p>
              <p class="text-[12px] text-orange-800 leading-relaxed">面料型号 T800-210 库存低于安全阈值 (仅剩 200 米)。</p>
            </div>
          </div>
        </section>

        <section class="bg-surface-container-highest rounded-2xl p-6 relative overflow-hidden">
          <div class="relative z-10">
            <h4 class="text-sm font-bold text-primary mb-4">当前值班车间主管</h4>
            <div class="flex items-center gap-4">
              <img alt="头像" class="w-12 h-12 rounded-full object-cover grayscale" src="https://ui-avatars.com/api/?name=Li+Hua&background=random">
              <div>
                <p class="text-sm font-bold text-on-surface">李华</p>
                <p class="text-xs text-on-surface-variant flex items-center gap-1 mt-0.5">
                  <span class="material-symbols-outlined text-[14px]">call</span> 138-xxxx-8899
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
// 严格的组合式 API 语法
defineOptions({ name: 'DashboardOverview' });

// 此处可引入 API 进行图表数据获取
</script>
