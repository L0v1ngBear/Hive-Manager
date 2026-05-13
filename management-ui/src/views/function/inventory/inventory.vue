<template>
  <div class="function-page-shell h-full min-h-0 font-sans">
    <div class="function-page-container space-y-6 p-2 md:p-4">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined text-[16px]">inventory_2</span>
            仓库调度中心
          </div>
          <h1 class="function-page-title">库存实时管理</h1>
          <p class="function-page-desc">
            管理布匹入库、出库、库存预警和库存流水，支持按型号聚合查看总米数，并可展开查看每匹布明细。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <button
            v-permission="'inventory:cloth:in'"
            @click="handleTemplateDownload"
            class="inventory-secondary-btn"
          >
            <span class="material-symbols-outlined text-[20px]">description</span>
            字段说明
          </button>
          <button
            v-permission="'inventory:cloth:in'"
            @click="triggerImport"
            class="inventory-secondary-btn"
          >
            <span class="material-symbols-outlined text-[20px]">file_upload</span>
            导入外部库存
          </button>
          <button v-permission="'inventory:cloth:in'" @click="openInDrawer" class="function-action-primary">
            <span class="material-symbols-outlined text-[20px]">add_circle</span>
            新增入库
          </button>
          <button v-permission="'inventory:cloth:out'" @click="openOutDrawer()" class="function-action-dark">
            <span class="material-symbols-outlined text-[20px]">outbox</span>
            扫码出库
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
        <div class="inventory-stat-card">
          <span class="material-symbols-outlined inventory-stat-bg text-blue-50">all_inbox</span>
          <p class="inventory-stat-label">可用总库存</p>
          <div class="inventory-stat-value-wrap">
            <h3 class="inventory-stat-value text-blue-600" :title="meter(summary.totalMeters)">
              {{ formatBigNumber(summary.totalMeters) }}
            </h3>
            <span class="inventory-stat-unit">{{ getUnit(summary.totalMeters, '米') }}</span>
          </div>
        </div>

        <div class="inventory-stat-card">
          <span class="material-symbols-outlined inventory-stat-bg text-slate-50">layers</span>
          <p class="inventory-stat-label">在库布匹</p>
          <div class="inventory-stat-value-wrap">
            <h3 class="inventory-stat-value text-slate-800" :title="summary.clothCount">
              {{ formatBigNumber(summary.clothCount) }}
            </h3>
            <span class="inventory-stat-unit">{{ getUnit(summary.clothCount, '匹') }}</span>
          </div>
        </div>

        <div class="inventory-stat-card border-amber-100">
          <span class="material-symbols-outlined inventory-stat-bg text-amber-50">warning</span>
          <p class="inventory-stat-label text-amber-600/80">低库存预警</p>
          <div class="inventory-stat-value-wrap">
            <h3 class="inventory-stat-value text-amber-600" :title="summary.warningCount">
              {{ summary.warningCount }}
            </h3>
            <span class="text-xs font-medium text-amber-500/70">低于 100 米</span>
          </div>
        </div>

        <div class="inventory-stat-card">
          <span class="material-symbols-outlined inventory-stat-bg text-emerald-50">move_to_inbox</span>
          <p class="inventory-stat-label">今日入库</p>
          <div class="inventory-stat-value-wrap">
            <h3 class="inventory-stat-value text-emerald-500" :title="meter(summary.todayInMeters)">
              {{ formatBigNumber(summary.todayInMeters) }}
            </h3>
            <span class="inventory-stat-unit">{{ getUnit(summary.todayInMeters, '米') }}</span>
          </div>
        </div>

        <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 p-6 text-white shadow-lg shadow-slate-900/20 transition-all hover:-translate-y-0.5">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-white/5 transition-transform">outbox</span>
          <p class="relative z-10 text-xs font-bold uppercase tracking-widest text-slate-300">今日出库</p>
          <div class="relative z-10 mt-3 flex min-w-0 items-baseline gap-1">
            <h3 class="truncate text-3xl font-black xl:text-4xl" :title="meter(summary.todayOutMeters)">
              {{ formatBigNumber(summary.todayOutMeters) }}
            </h3>
            <span class="whitespace-nowrap text-xs font-medium text-slate-400">{{ getUnit(summary.todayOutMeters, '米') }}</span>
          </div>
        </div>
      </section>

      <section class="grid grid-cols-1 gap-6 xl:grid-cols-[1fr_340px]">
        <div class="flex min-h-[500px] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div class="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 bg-slate-50/50 px-6 py-5">
            <div class="flex flex-wrap items-center gap-3">
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[20px] text-slate-400">search</span>
                <input
                  v-model.trim="query.keyword"
                  @keyup.enter="handleFilter"
                  class="w-64 max-w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-4 text-sm outline-none transition-all focus:border-blue-500 focus:ring-4 focus:ring-blue-600/10"
                  placeholder="搜索条码、型号或规格"
                />
              </div>
              <div class="relative">
                <select v-model="query.status" class="min-w-[120px] cursor-pointer appearance-none rounded-xl border border-slate-200 bg-white py-2.5 pl-4 pr-10 text-sm outline-none transition-all focus:border-blue-500 focus:ring-4 focus:ring-blue-600/10">
                  <option value="">全部状态</option>
                  <option value="0">在库</option>
                  <option value="2">部分出库</option>
                  <option value="1">已出库</option>
                </select>
                <span class="material-symbols-outlined pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">expand_more</span>
              </div>
              <button @click="handleFilter" class="rounded-xl bg-blue-50 px-5 py-2.5 text-sm font-bold text-blue-600 transition-colors hover:bg-blue-100">查询</button>
              <button @click="resetFilter" class="rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-50">重置</button>
            </div>
            <span class="rounded-lg border border-slate-100 bg-white px-3 py-1.5 text-xs font-medium text-slate-500 shadow-sm">
              共 <b class="text-slate-800">{{ pagination.total }}</b> 条记录
            </span>
          </div>

          <div class="relative flex-1 overflow-x-auto">
            <div v-if="loading" class="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 bg-white/70 backdrop-blur-[2px]">
              <span class="material-symbols-outlined animate-spin text-4xl text-blue-600">progress_activity</span>
              <span class="text-sm font-medium text-blue-600">正在加载库存数据...</span>
            </div>
            <table class="w-full min-w-[1040px] border-collapse text-left">
              <thead class="sticky top-0 z-0 bg-slate-50/80">
                <tr>
                  <th class="inventory-th">{{ fieldLabel('modelCode', '型号') }}</th>
                  <th class="inventory-th text-right">{{ fieldLabel('spec', '规格') }}</th>
                  <th class="inventory-th text-right">{{ fieldLabel('totalMeters', '总米数') }}</th>
                  <th class="inventory-th text-right">{{ fieldLabel('remainingMeters', '剩余米数') }}</th>
                  <th class="inventory-th">{{ fieldLabel('status', '库存状态') }}</th>
                  <th class="inventory-th">{{ fieldLabel('updateTime', '更新时间') }}</th>
                  <th class="inventory-th text-right">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr v-for="item in rows" :key="`${item.modelCode}-${item.spec}`" class="group cursor-pointer transition-colors hover:bg-blue-50/40" @click="openDetail(item)">
                  <td class="px-6 py-4 text-sm font-bold text-slate-700">{{ item.modelCode }}</td>
                  <td class="px-6 py-4 text-right text-sm text-slate-600">{{ meter(item.spec) }}</td>
                  <td class="px-6 py-4 text-right text-sm text-slate-600">{{ meter(item.totalMeters) }}</td>
                  <td class="px-6 py-4 text-right text-sm font-black text-blue-600">{{ meter(item.remainingMeters) }}</td>
                  <td class="px-6 py-4">
                    <span :class="statusClass(item.status)" class="inline-flex rounded-md px-2.5 py-1 text-[11px] font-bold tracking-wider">
                      {{ item.statusName || statusLabel(item.status) }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-xs text-slate-500">{{ formatDateTime(item.latestTime || item.updateTime) }}</td>
                  <td class="space-x-2 whitespace-nowrap px-6 py-4 text-right">
                    <button @click.stop="openDetail(item)" class="rounded-lg px-3 py-1.5 text-xs font-bold text-blue-600 transition-colors hover:bg-blue-100/50">详情</button>
                  </td>
                </tr>
                <tr v-if="!loading && rows.length === 0">
                  <td colspan="7" class="px-6 py-16 text-center">
                    <div class="flex flex-col items-center justify-center text-slate-400">
                      <span class="material-symbols-outlined mb-2 text-5xl opacity-50">search_off</span>
                      <p class="text-sm">暂无符合条件的库存记录</p>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="flex items-center justify-between border-t border-slate-100 bg-slate-50 p-4 text-sm text-slate-500">
            <span>第 <b class="text-slate-800">{{ query.pageNum }}</b> / {{ totalPages }} 页</span>
            <div class="flex gap-2">
              <button @click="changePage(query.pageNum - 1)" :disabled="query.pageNum <= 1" class="inventory-page-btn">上一页</button>
              <button @click="changePage(query.pageNum + 1)" :disabled="query.pageNum >= totalPages" class="inventory-page-btn">下一页</button>
            </div>
          </div>
        </div>

        <aside class="space-y-6">
          <section class="inventory-side-card">
            <div class="mb-5 flex items-center justify-between">
              <h2 class="flex items-center gap-2 text-base font-black text-slate-800">
                <span class="material-symbols-outlined text-[20px] text-blue-600">monitoring</span>
                7 天出入库趋势
              </h2>
            </div>
            <div class="space-y-4">
              <div v-for="item in trendRows" :key="item.statDate" class="group">
                <div class="mb-1.5 flex justify-between text-xs font-medium text-slate-500">
                  <span>{{ item.statDate }}</span>
                  <span class="text-slate-400">
                    <span class="font-bold text-emerald-600">入 {{ meter(item.inMeters) }}</span> /
                    <span class="font-bold text-slate-800">出 {{ meter(item.outMeters) }}</span>
                  </span>
                </div>
                <div class="flex h-2.5 overflow-hidden rounded-full bg-slate-100 ring-1 ring-inset ring-slate-200/50">
                  <div class="bg-emerald-400 transition-all duration-500" :style="{ width: trendWidth(item.inMeters) }"></div>
                  <div class="bg-slate-800 transition-all duration-500" :style="{ width: trendWidth(item.outMeters) }"></div>
                </div>
              </div>
              <div v-if="trendRows.length === 0" class="flex flex-col items-center justify-center py-4 text-sm text-slate-400">
                <span class="material-symbols-outlined mb-1 opacity-50">bar_chart</span>
                暂无趋势数据
              </div>
            </div>
          </section>

          <section class="inventory-side-card">
            <h2 class="mb-5 flex items-center gap-2 text-base font-black text-slate-800">
              <span class="material-symbols-outlined text-[20px] text-amber-500">error</span>
              低库存预警
            </h2>
            <div class="space-y-3">
              <div v-for="item in warningRows" :key="`${item.modelCode}-${item.spec}`" class="rounded-xl border border-amber-100/80 bg-amber-50/50 p-4 transition-colors hover:bg-amber-50">
                <div class="flex items-center justify-between">
                  <p class="text-sm font-black text-amber-900">{{ item.modelCode }}</p>
                  <span class="rounded bg-amber-100 px-2 py-0.5 text-[10px] font-bold text-amber-700">需关注</span>
                </div>
                <p class="mt-2 flex items-center gap-1 text-xs text-amber-700/80">
                  剩余 <b class="text-sm text-amber-600">{{ meter(item.totalMeters ?? item.remainingMeters) }}</b> 米
                </p>
              </div>
              <div v-if="warningRows.length === 0" class="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50 py-6 text-sm text-slate-400">
                <span class="material-symbols-outlined mb-1 text-emerald-500 opacity-50">check_circle</span>
                库位充足，暂无预警
              </div>
            </div>
          </section>

          <section class="inventory-side-card">
            <h2 class="mb-5 flex items-center gap-2 text-base font-black text-slate-800">
              <span class="material-symbols-outlined text-[20px] text-slate-400">history</span>
              最近操作记录
            </h2>
            <div class="custom-scrollbar max-h-[320px] space-y-4 overflow-y-auto pr-2">
              <div v-for="item in recordRows" :key="item.id" class="group flex gap-4">
                <div class="relative mt-1 flex flex-col items-center">
                  <div :class="Number(item.operateType) === 1 ? 'bg-slate-800 ring-slate-200' : 'bg-emerald-500 ring-emerald-100'" class="z-10 h-2.5 w-2.5 rounded-full ring-4"></div>
                  <div class="absolute top-3 h-full w-px bg-slate-100 group-last:hidden"></div>
                </div>
                <div class="flex-1 pb-1">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-bold text-slate-800">
                      {{ item.operateTypeName || operateTypeLabel(item.operateType) }}
                      <span :class="Number(item.operateType) === 1 ? 'text-slate-600' : 'text-emerald-600'">
                        {{ meter(item.operateMeters) }}
                      </span>
                      米
                    </p>
                  </div>
                  <p class="mt-1 text-xs font-medium text-slate-500">型号：{{ item.modelCode || '--' }}</p>
                  <p class="mt-1 flex items-center gap-1 text-[11px] text-slate-400">
                    <span class="material-symbols-outlined text-[12px]">person</span>
                    {{ item.operatorName || '系统' }}
                    <span class="mx-1 opacity-50">·</span>
                    {{ formatDateTime(item.createTime) }}
                  </p>
                </div>
              </div>
              <div v-if="recordRows.length === 0" class="flex flex-col items-center justify-center py-4 text-sm text-slate-400">
                暂无库存操作记录
              </div>
            </div>
          </section>
        </aside>
      </section>
    </div>

    <transition name="fade">
      <div v-if="detailVisible || inVisible || outVisible" class="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm transition-opacity" @click="closePanels"></div>
    </transition>

    <input ref="importInputRef" type="file" accept=".xlsx,.xls,.csv" class="hidden" @change="handleImportChange" />

    <aside class="inventory-drawer" :class="detailVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 w-full bg-blue-600"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="text-xl font-black text-slate-900">库存详情</h3>
          <p class="mt-1.5 inline-block rounded bg-slate-200/50 px-2 py-0.5 font-mono text-xs font-medium text-slate-500">
            {{ detailRecord?.modelCode || '--' }} / {{ meter(detailRecord?.spec) }}
          </p>
        </div>
        <button @click="detailVisible = false" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div v-if="detailRecord" class="flex-1 space-y-6 overflow-y-auto p-6">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-2xl border border-blue-100 bg-blue-50/50 p-5">
            <span class="text-[11px] font-bold uppercase tracking-wider text-blue-600/80">剩余可用米数</span>
            <p class="mt-1 text-3xl font-black text-blue-600">{{ meter(detailRecord.remainingMeters) }}</p>
          </div>
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-5">
            <span class="text-[11px] font-bold uppercase tracking-wider text-slate-500">布匹数</span>
            <p class="mt-2 text-3xl font-black text-slate-800">{{ detailRecord.rollCount || detailRows.length || 0 }}</p>
          </div>
        </div>

        <div class="divide-y divide-slate-100 rounded-2xl border border-slate-100">
          <div class="inventory-detail-row">
            <span>{{ fieldLabel('modelCode', '型号') }}</span>
            <b>{{ detailRecord.modelCode }}</b>
          </div>
          <div class="inventory-detail-row">
            <span>{{ fieldLabel('spec', '规格') }}</span>
            <b>{{ meter(detailRecord.spec) }}</b>
          </div>
          <div class="inventory-detail-row">
            <span>{{ fieldLabel('totalMeters', '总米数') }}</span>
            <b>{{ meter(detailRecord.totalMeters) }} 米</b>
          </div>
          <div class="inventory-detail-row">
            <span>首次入库时间</span>
            <b>{{ formatDateTime(detailRecord.inTime) }}</b>
          </div>
          <div class="inventory-detail-row">
            <span>最近出库时间</span>
            <b>{{ formatDateTime(detailRecord.outTime) || '--' }}</b>
          </div>
        </div>

        <div class="overflow-hidden rounded-2xl border border-slate-100">
          <div class="flex items-center justify-between bg-slate-50 px-4 py-3">
            <span class="text-sm font-black text-slate-800">单匹布明细</span>
            <span v-if="detailLoading" class="text-xs text-blue-600">加载中...</span>
          </div>
          <div class="max-h-[360px] divide-y divide-slate-100 overflow-y-auto">
            <div v-for="cloth in detailRows" :key="cloth.id" class="space-y-2 p-4 text-sm">
              <div class="flex items-center justify-between gap-3">
                <span class="break-all font-mono font-bold text-slate-800">{{ cloth.barcode }}</span>
                <span :class="statusClass(cloth.status)" class="shrink-0 rounded px-2 py-0.5 text-[11px] font-bold">
                  {{ cloth.statusName || statusLabel(cloth.status) }}
                </span>
              </div>
              <div class="grid grid-cols-2 gap-2 text-xs text-slate-500">
                <span>{{ fieldLabel('totalMeters', '总米数') }}：<b class="text-slate-700">{{ meter(cloth.totalMeters) }}</b></span>
                <span>{{ fieldLabel('remainingMeters', '剩余米数') }}：<b class="text-blue-600">{{ meter(cloth.remainingMeters) }}</b></span>
                <span>入库：{{ formatDateTime(cloth.inTime) }}</span>
                <span>更新：{{ formatDateTime(cloth.updateTime) }}</span>
              </div>
              <div v-if="customInventoryFields.length" class="grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-3 text-xs text-slate-500">
                <span v-for="field in customInventoryFields" :key="`${cloth.id}-${field.key}`">
                  {{ field.label }}：
                  <b class="text-slate-700">{{ customFieldValue(cloth, field) }}</b>
                </span>
              </div>
              <button
                v-permission="'inventory:cloth:out'"
                @click="openOutDrawer(cloth)"
                :disabled="Number(cloth.remainingMeters || 0) <= 0"
                class="mt-2 w-full rounded-lg bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-700 transition-colors hover:bg-emerald-100 disabled:opacity-40"
              >
                扫码出库
              </button>
            </div>
            <div v-if="!detailLoading && detailRows.length === 0" class="p-8 text-center text-sm text-slate-400">
              暂无单匹布明细
            </div>
          </div>
        </div>
      </div>
    </aside>

    <aside class="inventory-drawer" :class="inVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 w-full bg-emerald-500"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="flex items-center gap-2 text-xl font-black text-slate-900">
            <span class="material-symbols-outlined text-emerald-500">add_circle</span>
            新增入库
          </h3>
          <p class="mt-1.5 text-xs text-slate-500">条码不填时系统会自动生成唯一标识号，打印标签请在小程序端完成。</p>
        </div>
        <button @click="inVisible = false" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 space-y-6 overflow-y-auto p-6">
        <label class="block">
          <span class="inventory-field-label">
            <span class="material-symbols-outlined text-[16px] text-slate-400">qr_code</span>
            {{ fieldLabel('barCode', '条码') }}
          </span>
          <input v-model.trim="inForm.barcode" class="inventory-input" placeholder="留空则自动生成" />
        </label>

        <label class="block">
          <span class="inventory-field-label">
            <span class="material-symbols-outlined text-[16px] text-slate-400">category</span>
            {{ fieldLabel('modelCode', '型号') }}
            <span v-if="fieldRequired('modelCode')" class="text-rose-500">*</span>
          </span>
          <input v-model.trim="inForm.modelCode" @input="loadModelOptions" class="inventory-input" placeholder="搜索或输入型号" />
          <div v-if="modelOptions.length" class="mt-3 flex flex-wrap gap-2 rounded-xl border border-slate-100 bg-slate-50 p-3">
            <button v-for="item in modelOptions" :key="`${item.modelCode}-${item.spec}`" @click="pickModel(item)" class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-600 shadow-sm transition-colors hover:border-emerald-400 hover:text-emerald-700">
              {{ item.modelCode }} <span class="mx-1 text-slate-300">|</span> {{ meter(item.spec) }}
            </button>
          </div>
        </label>

        <div class="grid grid-cols-2 gap-4">
          <label class="block">
            <span class="inventory-field-label">
              <span class="material-symbols-outlined text-[16px] text-slate-400">straighten</span>
              {{ fieldLabel('spec', '规格') }}
              <span v-if="fieldRequired('spec')" class="text-rose-500">*</span>
            </span>
            <input v-model.trim="inForm.spec" type="number" min="0" step="0.01" class="inventory-input" placeholder="0.00" />
          </label>
          <label class="block">
            <span class="inventory-field-label">
              <span class="material-symbols-outlined text-[16px] text-slate-400">input</span>
              {{ fieldLabel('totalMeters', '入库米数') }}
              <span v-if="fieldRequired('totalMeters')" class="text-rose-500">*</span>
            </span>
            <input v-model.trim="inForm.meters" type="number" min="0" step="0.01" class="inventory-input" placeholder="0.00" />
          </label>
        </div>

        <div v-if="customInventoryFields.length" class="space-y-4 rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
          <div>
            <h4 class="text-sm font-black text-slate-800">租户自定义字段</h4>
            <p class="mt-1 text-xs text-slate-500">这些字段只属于当前租户，用于适配客户原有库存台账。</p>
          </div>
          <label v-for="field in customInventoryFields" :key="field.key" class="block">
            <span class="inventory-field-label">
              {{ field.label }}
              <span v-if="field.required" class="text-rose-500">*</span>
            </span>
            <input
              v-model.trim="inForm.customFields[field.key]"
              :type="customFieldInputType(field)"
              class="inventory-input bg-white"
              :placeholder="`请输入${field.label}`"
            />
          </label>
        </div>
      </div>
      <div class="flex gap-3 border-t border-slate-100 bg-slate-50 p-6">
        <button @click="inVisible = false" class="inventory-cancel-btn">取消</button>
        <button @click="submitIn" class="inventory-confirm-btn bg-emerald-500 text-white shadow-emerald-500/20 hover:bg-emerald-600">确认入库</button>
      </div>
    </aside>

    <aside class="inventory-drawer" :class="outVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 w-full bg-slate-800"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="flex items-center gap-2 text-xl font-black text-slate-900">
            <span class="material-symbols-outlined text-slate-700">outbox</span>
            扫码出库
          </h3>
          <p class="mt-1.5 text-xs text-slate-500">请扫描或输入布匹条码，系统会校验剩余可出库米数。</p>
        </div>
        <button @click="outVisible = false" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="flex-1 space-y-6 overflow-y-auto p-6">
        <label class="block">
          <span class="inventory-field-label">
            <span class="material-symbols-outlined text-[16px] text-slate-400">barcode_scanner</span>
            布匹条码 <span class="text-rose-500">*</span>
          </span>
          <div class="relative">
            <input v-model.trim="outForm.barcode" @change="lookupBarcode" class="inventory-input pr-12 font-mono" placeholder="请将光标放在此处扫码" autofocus />
            <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-300">qr_code_scanner</span>
          </div>
        </label>

        <div v-if="outPreview" class="relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4">
          <div class="absolute right-0 top-0 h-full w-1 bg-blue-500"></div>
          <div class="flex flex-col gap-2">
            <p class="flex justify-between text-sm">
              <span class="text-slate-500">{{ fieldLabel('modelCode', '型号') }}</span>
              <span class="font-bold text-slate-800">{{ outPreview.modelCode }}</span>
            </p>
            <p class="flex justify-between text-sm">
              <span class="text-slate-500">{{ fieldLabel('remainingMeters', '当前可出') }}</span>
              <span class="font-bold text-blue-600">{{ meter(outPreview.remainingMeters) }} 米</span>
            </p>
          </div>
        </div>

        <label class="block">
          <span class="inventory-field-label">
            <span class="material-symbols-outlined text-[16px] text-slate-400">output</span>
            {{ fieldLabel('remainingMeters', '出库米数') }}
            <span class="text-rose-500">*</span>
          </span>
          <input v-model.trim="outForm.meters" type="number" min="0" step="0.01" class="inventory-input" placeholder="请输入本次出库米数" />
        </label>
      </div>
      <div class="flex gap-3 border-t border-slate-100 bg-slate-50 p-6">
        <button @click="outVisible = false" class="inventory-cancel-btn">取消</button>
        <button @click="submitOut" class="inventory-confirm-btn bg-slate-800 text-white shadow-slate-800/20 hover:bg-slate-900">确认出库</button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { getCurrentTenantFieldConfig } from '@/api/tenantFieldConfig'
import { customTenantFields, defaultTenantFieldConfig, mergeTenantFieldConfig } from '@/utils/tenantFieldConfig'
import {
  downloadInventoryImportTemplate,
  getInventoryModelDetail,
  getInventoryModelPage,
  getInventorySummary,
  getInventoryTrend,
  getInventoryWarnings,
  getRecentInventoryRecords,
  importInventory,
  inCloth,
  outCloth,
  searchInventoryBarcode,
  searchInventoryModels
} from './api/inventory.js'

const route = useRoute()
const rows = ref([])
const warningRows = ref([])
const recordRows = ref([])
const trendRows = ref([])
const modelOptions = ref([])
const loading = ref(false)
const summary = reactive({ totalMeters: 0, clothCount: 0, warningCount: 0, todayInMeters: 0, todayOutMeters: 0 })
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const detailVisible = ref(false)
const inVisible = ref(false)
const outVisible = ref(false)
const detailRecord = ref(null)
const detailRows = ref([])
const detailLoading = ref(false)
const outPreview = ref(null)
const importInputRef = ref(null)
const inForm = reactive({ barcode: '', modelCode: '', spec: '', meters: '', customFields: {} })
const outForm = reactive({ barcode: '', meters: '' })
const inventoryFieldConfig = ref(defaultInventoryFieldConfig())

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const maxTrendValue = computed(() => Math.max(1, ...trendRows.value.flatMap((item) => [Number(item.inMeters || 0), Number(item.outMeters || 0)])))
const customInventoryFields = computed(() => customTenantFields(inventoryFieldConfig.value))

applyRouteKeyword()
refreshAll()

watch(
  () => [route.query.keyword, route.query.q],
  async () => {
    applyRouteKeyword()
    await fetchData()
  }
)

function applyRouteKeyword() {
  const routeKeyword = String(route.query.keyword || route.query.q || '').trim()
  if (routeKeyword !== query.keyword) {
    query.keyword = routeKeyword
    query.pageNum = 1
  }
}

function defaultInventoryFieldConfig() {
  return defaultTenantFieldConfig('inventory')
}

function mergeInventoryFieldConfig(rows) {
  return mergeTenantFieldConfig('inventory', rows)
}

function fieldLabel(key, fallback) {
  return inventoryFieldConfig.value[key]?.label || fallback || key
}

function fieldRequired(key) {
  return inventoryFieldConfig.value[key]?.required === true
}

function customFieldInputType(field) {
  if (field?.fieldType === 'number') return 'number'
  if (field?.fieldType === 'date') return 'date'
  if (field?.fieldType === 'datetime') return 'datetime-local'
  return 'text'
}

function resetCustomFields() {
  inForm.customFields = {}
  customInventoryFields.value.forEach((field) => {
    if (field?.key) {
      inForm.customFields[field.key] = ''
    }
  })
}

function customFieldValue(row, field) {
  const value = row?.customFields?.[field?.key]
  return value == null || value === '' ? '--' : value
}

async function refreshAll() {
  await Promise.all([fetchFieldConfig(), fetchData(), fetchSummary(), fetchWarnings(), fetchRecords(), fetchTrend()])
}

async function fetchFieldConfig() {
  try {
    const configRows = await getCurrentTenantFieldConfig('inventory')
    inventoryFieldConfig.value = mergeInventoryFieldConfig(Array.isArray(configRows) ? configRows : [])
  } catch (error) {
    inventoryFieldConfig.value = defaultInventoryFieldConfig()
  }
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getInventoryModelPage({
      ...query,
      status: query.status === '' ? undefined : Number(query.status),
      keyword: query.keyword || undefined
    })
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  Object.assign(summary, await getInventorySummary())
}

async function fetchWarnings() {
  warningRows.value = await getInventoryWarnings()
}

async function fetchRecords() {
  recordRows.value = await getRecentInventoryRecords()
}

async function fetchTrend() {
  trendRows.value = await getInventoryTrend()
}

function handleFilter() {
  query.pageNum = 1
  fetchData()
}

function resetFilter() {
  query.keyword = ''
  query.status = ''
  query.pageNum = 1
  fetchData()
}

function changePage(pageNum) {
  if (pageNum < 1 || pageNum > totalPages.value) {
    return
  }
  query.pageNum = pageNum
  fetchData()
}

async function openDetail(record) {
  if (!record) return
  detailRecord.value = record
  detailVisible.value = true
  detailRows.value = []
  detailLoading.value = true
  try {
    detailRows.value = await getInventoryModelDetail({
      modelCode: record.modelCode,
      spec: record.spec,
      status: query.status === '' ? undefined : Number(query.status)
    })
  } finally {
    detailLoading.value = false
  }
}

function openInDrawer() {
  Object.assign(inForm, { barcode: '', modelCode: '', spec: '', meters: '' })
  resetCustomFields()
  modelOptions.value = []
  inVisible.value = true
}

function openOutDrawer(record) {
  Object.assign(outForm, { barcode: record?.barcode || '', meters: record?.remainingMeters ? String(record.remainingMeters) : '' })
  outPreview.value = record || null
  outVisible.value = true
}

function closePanels() {
  detailVisible.value = false
  inVisible.value = false
  outVisible.value = false
}

async function loadModelOptions() {
  if (!inForm.modelCode) {
    modelOptions.value = []
    return
  }
  modelOptions.value = await searchInventoryModels({ keyword: inForm.modelCode })
}

function pickModel(item) {
  inForm.modelCode = item.modelCode
  inForm.spec = item.spec == null ? '' : String(item.spec)
  modelOptions.value = []
}

async function lookupBarcode() {
  if (!outForm.barcode) {
    outPreview.value = null
    return
  }
  outPreview.value = await searchInventoryBarcode({ barCode: outForm.barcode })
}

async function submitIn() {
  if (!inForm.modelCode || Number(inForm.spec) <= 0 || Number(inForm.meters) <= 0) {
    ElMessage.warning('请填写型号、规格和有效入库米数')
    return
  }
  for (const field of customInventoryFields.value) {
    const value = String(inForm.customFields?.[field.key] || '').trim()
    if (field.required && !value) {
      ElMessage.warning(`请填写自定义字段：${field.label}`)
      return
    }
    if (field.fieldType === 'number' && value && !Number.isFinite(Number(value))) {
      ElMessage.warning(`自定义字段必须是数字：${field.label}`)
      return
    }
  }
  const customFields = customInventoryFields.value.reduce((values, field) => {
    const value = String(inForm.customFields?.[field.key] || '').trim()
    if (value) values[field.key] = value
    return values
  }, {})
  const result = await inCloth({
    barcode: inForm.barcode || undefined,
    modelCode: inForm.modelCode,
    spec: Number(inForm.spec),
    meters: Number(inForm.meters),
    inType: 'manual',
    customFields
  })
  const taskNo = result?.labelTask?.printTaskNo
  ElMessage.success(taskNo ? `入库成功，已生成小程序标签打印任务：${taskNo}` : '入库成功')
  inVisible.value = false
  await refreshAll()
}

async function submitOut() {
  if (!outForm.barcode || Number(outForm.meters) <= 0) {
    ElMessage.warning('请填写条码和有效出库米数')
    return
  }
  await outCloth({ barcode: outForm.barcode, meters: Number(outForm.meters) })
  ElMessage.success('出库成功')
  outVisible.value = false
  await refreshAll()
}

async function handleTemplateDownload() {
  const blob = await downloadInventoryImportTemplate()
  await downloadBlob(blob, '外部库存导入字段说明.xlsx')
}

function triggerImport() {
  importInputRef.value?.click()
}

async function handleImportChange(event) {
  const [file] = event.target.files || []
  if (!file) return
  try {
    const result = await importInventory(file)
    const failText = (result.failMessages || []).slice(0, 8).join('\n')
    await ElMessageBox.alert(
      `导入完成：成功 ${result.successCount || 0} 条，失败 ${result.failCount || 0} 条，生成标签任务 ${result.printTaskCount || 0} 条。${failText ? `\n\n失败明细：\n${failText}` : ''}`,
      '库存导入结果'
    )
    await refreshAll()
  } finally {
    event.target.value = ''
  }
}

async function downloadBlob(blob, fileName) {
  if (!blob || blob.size === 0) {
    ElMessage.error('下载失败：文件为空')
    return
  }
  const contentType = String(blob.type || '').toLowerCase()
  if (contentType.includes('application/json') || contentType.includes('text/plain')) {
    const text = await blob.text()
    let message = text || '下载失败'
    try {
      const parsed = JSON.parse(text)
      message = parsed.msg || parsed.message || parsed.error || message
    } catch (ignored) {
      // Non-JSON text response; keep the original message for troubleshooting.
    }
    ElMessage.error(message)
    return
  }
  if (fileName.endsWith('.xlsx')) {
    const header = new Uint8Array(await blob.slice(0, 4).arrayBuffer())
    const isZipBasedExcel = header.length >= 2 && header[0] === 0x50 && header[1] === 0x4b
    if (!isZipBasedExcel) {
      ElMessage.error('下载失败：服务端返回的不是有效 Excel 文件')
      return
    }
  }
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 0)
}

function trendWidth(value) {
  return `${Math.min(100, Math.round((Number(value || 0) / maxTrendValue.value) * 100))}%`
}

function statusLabel(value) {
  if (Number(value) === 1) return '已出库'
  if (Number(value) === 2) return '部分出库'
  return '在库'
}

function operateTypeLabel(value) {
  return Number(value) === 1 ? '出库' : '入库'
}

function statusClass(value) {
  if (Number(value) === 1) return 'bg-slate-100 text-slate-600 ring-1 ring-inset ring-slate-200/60'
  if (Number(value) === 2) return 'bg-amber-50 text-amber-600 ring-1 ring-inset ring-amber-200/60'
  return 'bg-emerald-50 text-emerald-600 ring-1 ring-inset ring-emerald-200/60'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '--'
}

function meter(value) {
  return Number(value || 0).toFixed(2)
}

function formatBigNumber(value) {
  const num = Number(value || 0)
  if (num >= 10000) {
    return (num / 10000).toFixed(2)
  }
  return Number.isInteger(num) ? String(num) : num.toFixed(2)
}

function getUnit(value, defaultUnit = '米') {
  const num = Number(value || 0)
  return num >= 10000 ? `万${defaultUnit}` : defaultUnit
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.inventory-secondary-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 0.75rem;
  border: 1px solid rgb(226 232 240);
  background: #fff;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: rgb(51 65 85);
  transition: background-color 0.2s ease;
}

.inventory-secondary-btn:hover {
  background: rgb(248 250 252);
}

.inventory-stat-card {
  position: relative;
  overflow: hidden;
  border-radius: 1rem;
  border: 1px solid rgb(241 245 249);
  background: #fff;
  padding: 1.5rem;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.04);
  transition: box-shadow 0.2s ease;
}

.inventory-stat-card:hover {
  box-shadow: 0 8px 20px rgb(15 23 42 / 0.08);
}

.inventory-stat-bg {
  position: absolute;
  right: -1rem;
  bottom: -1rem;
  font-size: 100px;
  opacity: 0.5;
  transition: transform 0.2s ease;
}

.inventory-stat-card:hover .inventory-stat-bg {
  transform: scale(1.1);
}

.inventory-stat-label {
  position: relative;
  z-index: 1;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgb(100 116 139);
}

.inventory-stat-value-wrap {
  position: relative;
  z-index: 1;
  margin-top: 0.75rem;
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: 0.25rem;
}

.inventory-stat-value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 1.875rem;
  font-weight: 900;
}

@media (min-width: 1280px) {
  .inventory-stat-value {
    font-size: 2.25rem;
  }
}

.inventory-stat-unit {
  white-space: nowrap;
  font-size: 0.75rem;
  font-weight: 500;
  color: rgb(148 163 184);
}

.inventory-th {
  padding: 1rem 1.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: rgb(100 116 139);
}

.inventory-page-btn {
  border-radius: 0.75rem;
  border: 1px solid rgb(226 232 240);
  background: #fff;
  padding: 0.5rem 1rem;
  font-weight: 500;
  color: rgb(51 65 85);
  transition: background-color 0.2s ease;
}

.inventory-page-btn:hover:not(:disabled) {
  background: rgb(248 250 252);
}

.inventory-page-btn:disabled {
  opacity: 0.5;
}

.inventory-side-card {
  border-radius: 1rem;
  border: 1px solid rgb(241 245 249);
  background: #fff;
  padding: 1.5rem;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.04);
}

.inventory-drawer {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 50;
  display: flex;
  height: 100%;
  width: 100%;
  flex-direction: column;
  background: #fff;
  box-shadow: 0 25px 50px -12px rgb(15 23 42 / 0.25);
  transition: transform 0.3s ease-in-out;
}

@media (min-width: 640px) {
  .inventory-drawer {
    width: 420px;
  }
}

.inventory-close-btn {
  border-radius: 9999px;
  padding: 0.375rem;
  color: rgb(148 163 184);
  transition: background-color 0.2s ease, color 0.2s ease;
}

.inventory-close-btn:hover {
  background: rgb(241 245 249);
  color: rgb(71 85 105);
}

.inventory-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  font-size: 0.875rem;
}

.inventory-detail-row span {
  color: rgb(100 116 139);
}

.inventory-detail-row b {
  font-weight: 700;
  color: rgb(30 41 59);
}

.inventory-field-label {
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: rgb(51 65 85);
}

.inventory-input {
  width: 100%;
  border-radius: 0.75rem;
  border: 1px solid rgb(226 232 240);
  background: rgb(248 250 252 / 0.5);
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.inventory-input::placeholder {
  color: rgb(203 213 225);
}

.inventory-input:focus {
  border-color: rgb(16 185 129);
  background: #fff;
  box-shadow: 0 0 0 4px rgb(16 185 129 / 0.1);
}

.inventory-cancel-btn,
.inventory-confirm-btn {
  flex: 1;
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 700;
  transition: transform 0.15s ease, background-color 0.2s ease;
}

.inventory-cancel-btn {
  border: 1px solid rgb(226 232 240);
  background: #fff;
  color: rgb(51 65 85);
}

.inventory-cancel-btn:hover {
  background: rgb(241 245 249);
}

.inventory-confirm-btn {
  box-shadow: 0 10px 18px rgb(15 23 42 / 0.12);
}

.inventory-confirm-btn:active {
  transform: scale(0.95);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 10px;
}
</style>
