<template>
  <div class="max-w-7xl mx-auto space-y-8 pb-12">

    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h3 class="text-2xl font-black tracking-tight text-on-surface mb-1">早上好，陈总。</h3>
        <p class="text-on-surface-variant text-sm">当前待发货订单 15 笔，有 2 条次品登记需要您的确认。</p>
      </div>
      <div class="flex items-center gap-3">
        <button class="bg-surface-container-high text-on-surface px-4 py-2.5 rounded-xl text-sm font-bold hover:bg-surface-container-highest transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">receipt_long</span>
          新建销售单
        </button>
        <button class="bg-primary text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-sm hover:bg-primary/90 transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">precision_manufacturing</span>
          下达生产单
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">

      <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm ring-1 ring-outline-variant/20 relative overflow-hidden group hover:shadow-md transition-shadow">
        <div class="flex justify-between items-start mb-6">
          <div class="w-10 h-10 rounded-lg bg-primary-container flex items-center justify-center text-on-primary-container">
            <span class="material-symbols-outlined">shopping_cart</span>
          </div>
          <span class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-green-100 text-green-800 text-xs font-bold">
            <span class="material-symbols-outlined text-[14px]">trending_up</span> +12%
          </span>
        </div>
        <div>
          <p class="text-on-surface-variant text-sm font-medium mb-1">本月新增订单 (笔)</p>
          <h4 class="text-3xl font-black tracking-tight text-on-surface">328</h4>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm ring-1 ring-outline-variant/20 relative overflow-hidden group hover:shadow-md transition-shadow">
        <div class="flex justify-between items-start mb-6">
          <div class="w-10 h-10 rounded-lg bg-surface-container-high flex items-center justify-center text-on-surface">
            <span class="material-symbols-outlined">inventory_2</span>
          </div>
          <span class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-orange-100 text-orange-800 text-xs font-bold">
            2款面料临界预警
          </span>
        </div>
        <div>
          <p class="text-on-surface-variant text-sm font-medium mb-1">当前布匹总库存 (米)</p>
          <h4 class="text-3xl font-black tracking-tight text-on-surface">184,920</h4>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-sm ring-1 ring-outline-variant/20 relative overflow-hidden group hover:shadow-md transition-shadow">
        <div class="flex justify-between items-start mb-6">
          <div class="w-10 h-10 rounded-lg bg-error-container flex items-center justify-center text-on-error-container">
            <span class="material-symbols-outlined">fact_check</span>
          </div>
        </div>
        <div>
          <p class="text-on-surface-variant text-sm font-medium mb-1">待处理审批 / 请假</p>
          <h4 class="text-3xl font-black tracking-tight text-error">5</h4>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

      <section class="lg:col-span-2 bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 p-6">
        <div class="flex justify-between items-center mb-6">
          <h4 class="text-base font-bold text-on-surface">近7日出入库流量趋势</h4>
          <button class="text-on-surface-variant hover:text-primary"><span class="material-symbols-outlined">more_horiz</span></button>
        </div>
        <div class="h-64 bg-surface-container-low rounded-xl flex items-center justify-center text-on-surface-variant text-sm border border-dashed border-outline-variant/50">
          [ Echarts：出入库趋势折线图渲染区 ]
        </div>
      </section>

      <div class="space-y-6">

        <section class="bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 p-6">
          <h4 class="text-base font-bold text-on-surface mb-6">业务异常动态</h4>
          <div class="space-y-4">

            <div class="p-4 bg-error-container/50 rounded-xl border-l-4 border-error">
              <div class="flex justify-between items-start mb-1">
                <p class="text-xs font-bold text-error">新增次品登记</p>
                <span class="text-[10px] text-error/70">10分钟前</span>
              </div>
              <p class="text-[12px] text-on-error-container leading-relaxed">
                仓库员(张三) 扫码上报 <strong>T800-210</strong> 次品 <strong>15.5米</strong>，原因：染色不均。
              </p>
            </div>

            <div class="p-4 bg-orange-50 rounded-xl border-l-4 border-orange-400">
              <div class="flex justify-between items-start mb-1">
                <p class="text-xs font-bold text-orange-600">库存余量告警</p>
                <span class="text-[10px] text-orange-600/70">1小时前</span>
              </div>
              <p class="text-[12px] text-orange-800 leading-relaxed">
                面料型号 <strong>Q200-磨毛</strong> 当前可用库存仅剩 <strong>80米</strong>，已低于安全阈值。
              </p>
            </div>
          </div>
        </section>

        <section class="bg-surface-container-highest rounded-2xl p-6 relative overflow-hidden">
          <div class="relative z-10">
            <h4 class="text-sm font-bold text-primary mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px]">co_present</span>
              今日考勤异常
            </h4>

            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <img alt="头像" class="w-8 h-8 rounded-full object-cover" src="https://ui-avatars.com/api/?name=Li+Hua&background=ffdad6&color=93000a">
                  <div>
                    <p class="text-sm font-bold text-on-surface leading-tight">李华</p>
                    <p class="text-[11px] text-on-surface-variant mt-0.5">织造一车间</p>
                  </div>
                </div>
                <span class="bg-error-container text-on-error-container text-[11px] font-bold px-2 py-0.5 rounded">迟到 45分</span>
              </div>

              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <img alt="头像" class="w-8 h-8 rounded-full object-cover" src="https://ui-avatars.com/api/?name=Wang+Wei&background=ffdad6&color=93000a">
                  <div>
                    <p class="text-sm font-bold text-on-surface leading-tight">王伟</p>
                    <p class="text-[11px] text-on-surface-variant mt-0.5">销售部</p>
                  </div>
                </div>
                <span class="bg-orange-200 text-orange-800 text-[11px] font-bold px-2 py-0.5 rounded">外勤未打卡</span>
              </div>
            </div>

          </div>
        </section>

      </div>

    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'DashboardOverview' });
</script>
