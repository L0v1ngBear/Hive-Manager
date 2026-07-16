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
          <el-tooltip :disabled="canConfigureWarning" content="暂无 inventory:warning:setting 权限"><span><el-button
            :disabled="!canConfigureWarning"
            @click="openWarningSetting"
            class="inventory-secondary-btn"
          >
            <span class="material-symbols-outlined text-[20px]">tune</span>
            预警设置
          </el-button></span></el-tooltip>
          <el-tooltip :disabled="canImportInventory" content="暂无 inventory:import 权限"><span><el-button
            :disabled="!canImportInventory"
            @click="handleTemplateDownload"
            class="inventory-secondary-btn"
          >
            <span class="material-symbols-outlined text-[20px]">description</span>
            导入说明
          </el-button></span></el-tooltip>
          <el-tooltip :disabled="canImportInventory" content="暂无 inventory:import 权限"><span><el-button
            :disabled="!canImportInventory"
            @click="triggerImport"
            class="inventory-secondary-btn"
          >
            <span class="material-symbols-outlined text-[20px]">file_upload</span>
            导入外部库存
          </el-button></span></el-tooltip>
          <el-tooltip :disabled="canInInventory" content="暂无 inventory:cloth:in 权限"><span><el-button
            :disabled="!canInInventory"
            @click="triggerImageRecognition"
            class="inventory-secondary-btn"
          >
            <span class="material-symbols-outlined text-[20px]">photo_camera</span>
            图片识别入库
          </el-button></span></el-tooltip>
          <el-tooltip :disabled="canInInventory" content="暂无 inventory:cloth:in 权限"><span><el-button :disabled="!canInInventory" @click="openInDrawer" class="function-action-primary">
            <span class="material-symbols-outlined text-[20px]">add_circle</span>
            新增入库
          </el-button></span></el-tooltip>
          <el-tooltip :disabled="canOutInventory" content="暂无 inventory:cloth:out 权限"><span><el-button :disabled="!canOutInventory" @click="openOutDrawer()" class="function-action-dark">
            <span class="material-symbols-outlined text-[20px]">outbox</span>
            扫码出库
          </el-button></span></el-tooltip>
        </div>
      </header>

      <section v-if="canListInventory" class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
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
            <span class="text-xs font-medium text-amber-500/70">低于 {{ meter(summary.warningThresholdMeters) }} 米</span>
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
                <el-input
                  v-model.trim="query.keyword"
                  @keyup.enter="handleFilter"
                  class="w-64 max-w-full"
                  placeholder="搜索条码、型号或规格"
                />
              </div>
              <div class="relative">
                <el-select v-model="query.status" class="min-w-[120px]">
                  <el-option label="全部状态" value="" />
                  <el-option label="在库" value="0" />
                  <el-option label="部分出库" value="2" />
                  <el-option label="已出库" value="1" />
                </el-select>
                <span class="material-symbols-outlined pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">expand_more</span>
              </div>
              <el-select v-model="query.timeOrder" @change="handleFilter" class="min-w-[132px]"><el-option label="先进先出" value="fifo" /><el-option label="先进后出" value="lifo" /></el-select>
              <el-input-number v-model="query.specMin" :min="0" :step="0.01" :precision="2" placeholder="规格下限" class="w-28" />
              <el-input-number v-model="query.specMax" :min="0" :step="0.01" :precision="2" placeholder="规格上限" class="w-28" />
              <el-input-number v-model="query.remainingMin" :min="0" :step="0.01" :precision="2" placeholder="剩余米数下限" class="w-32" />
              <el-input-number v-model="query.remainingMax" :min="0" :step="0.01" :precision="2" placeholder="剩余米数上限" class="w-32" />
              <DateFilterInput v-model="query.updatedStart" placeholder="更新开始" class="rounded-xl border border-slate-200 bg-white py-2.5 px-3 text-sm outline-none transition-all focus:border-blue-500 focus:ring-4 focus:ring-blue-600/10" />
              <DateFilterInput v-model="query.updatedEnd" placeholder="更新结束" class="rounded-xl border border-slate-200 bg-white py-2.5 px-3 text-sm outline-none transition-all focus:border-blue-500 focus:ring-4 focus:ring-blue-600/10" />
              <el-button type="primary" @click="handleFilter">查询</el-button>
              <el-button @click="resetFilter">重置</el-button>
              <TableColumnSettings
                :columns="inventoryTableColumns"
                export-module="inventory"
                @move="moveInventoryTableColumn"
                @reset="resetInventoryTableColumns"
              />
            </div>
            <span class="rounded-lg border border-slate-100 bg-white px-3 py-1.5 text-xs font-medium text-slate-500 shadow-sm">
              共 <b class="text-slate-800">{{ pagination.total }}</b> 条记录
            </span>
          </div>

          <div v-if="!canListInventory" class="flex min-h-[360px] flex-1 items-center justify-center p-8"><el-empty description="暂无 inventory:list 权限，无法查看库存内容" /></div>
          <div v-else-if="listLoadError" class="flex min-h-[360px] flex-1 items-center justify-center p-8"><el-empty :description="listLoadError.message"><el-button type="primary" @click="fetchData">重试</el-button></el-empty></div>
          <div v-else-if="loading" v-loading="loading" class="min-h-[360px] flex-1" element-loading-text="正在加载库存数据" />
          <div v-else class="responsive-table-wrap relative flex-1">
            <table v-if="rows.length" class="responsive-data-table w-full border-collapse text-left">
              <thead class="sticky top-0 z-0 bg-slate-50/80">
                <tr>
                  <th
                    v-for="column in inventoryTableColumns"
                    :key="column.key"
                    class="inventory-th"
                    :class="column.align === 'right' ? 'text-right' : ''"
                  >
                    {{ column.label }}
                  </th>
                  <th class="inventory-th text-right">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr v-for="item in rows" :key="`${item.modelCode}-${item.spec}`" class="group cursor-pointer transition-colors hover:bg-blue-50/40" @click="openDetail(item)">
                  <td
                    v-for="column in inventoryTableColumns"
                    :key="column.key"
                    :data-label="column.label"
                    class="px-6 py-4"
                    :class="inventoryTableCellClass(column.key)"
                  >
                    <template v-if="column.key === 'modelCode'">{{ item.modelCode }}</template>
                    <template v-else-if="column.key === 'spec'">{{ meter(item.spec) }}</template>
                    <template v-else-if="column.key === 'totalMeters'">{{ meter(item.totalMeters) }}</template>
                    <template v-else-if="column.key === 'remainingMeters'">{{ meter(item.remainingMeters) }}</template>
                    <template v-else-if="column.key === 'status'">
                      <el-tag :type="statusTagType(item.status)" size="small">
                        {{ item.statusName || statusLabel(item.status) }}
                      </el-tag>
                    </template>
                    <template v-else-if="column.key === 'updateTime'">{{ formatDateTime(item.latestTime || item.updateTime) }}</template>
                  </td>
                  <td class="space-x-2 whitespace-nowrap px-6 py-4 text-right" data-label="操作">
                    <el-button link type="primary" @click.stop="openDetail(item)">详情</el-button>
                  </td>
                </tr>
              </tbody>
            </table>
            <el-empty v-else-if="listLoaded" description="暂无符合条件的库存记录" />
          </div>

          <div class="flex items-center justify-between border-t border-slate-100 bg-slate-50 p-4 text-sm text-slate-500">
            <span>第 <b class="text-slate-800">{{ query.pageNum }}</b> / {{ totalPages }} 页</span>
            <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="pagination.total" layout="prev, pager, next" @current-change="changePage" />
          </div>
        </div>

        <aside class="space-y-6">
          <section v-if="canReadTrend" class="inventory-side-card">
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

          <section v-if="canReadWarnings" class="inventory-side-card">
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

          <section v-if="canReadRecords" class="inventory-side-card">
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
      <div v-if="detailVisible || inVisible || outVisible || imageRecognitionVisible" class="fixed inset-0 z-[90] bg-slate-900/40 backdrop-blur-sm transition-opacity" @click="closePanels"></div>
    </transition>

    <el-drawer v-model="warningSettingVisible" title="库存预警设置" size="min(92vw, 420px)" destroy-on-close>
      <el-form :model="warningSettingForm" label-position="top">
        <el-form-item label="低库存预警阈值（米）" required>
          <el-input-number v-model="warningSettingForm.threshold" :min="0" :max="999999999.99" :step="0.01" :precision="2" class="w-full" />
        </el-form-item>
        <p class="text-sm text-slate-500">库存型号总米数小于等于该值时触发预警。</p>
        <div class="mt-6 flex justify-end gap-3">
          <el-button @click="warningSettingVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!canConfigureWarning" @click="saveWarningSetting">保存</el-button>
        </div>
      </el-form>
    </el-drawer>

    <input ref="importInputRef" type="file" accept=".xlsx,.xls,.csv" class="hidden" @change="handleImportChange" />

    <el-drawer v-model="detailVisible" title="库存型号详情" size="min(92vw, 520px)" destroy-on-close>
      <div class="h-1.5 w-full bg-blue-600"></div>
      <div class="flex items-start justify-between gap-3 border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="text-xl font-black text-slate-900">库存详情</h3>
          <p class="mt-1.5 inline-block rounded bg-slate-200/50 px-2 py-0.5 font-mono text-xs font-medium text-slate-500">
            {{ detailRecord?.modelCode || '--' }} / {{ meter(detailRecord?.spec) }}
          </p>
        </div>
        <el-button circle @click="detailVisible = false" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </el-button>
      </div>
      <div v-if="detailRecord" class="flex-1 space-y-6 overflow-y-auto p-6">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-2xl border border-blue-100 bg-blue-50/50 p-5">
            <span class="text-[11px] font-bold uppercase tracking-wider text-blue-600/80">剩余可用米数</span>
            <p class="mt-1 text-3xl font-black text-blue-600">{{ meter(detailRecord.remainingMeters) }}</p>
          </div>
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-5">
            <span class="text-[11px] font-bold uppercase tracking-wider text-slate-500">布匹数</span>
            <p class="mt-2 text-3xl font-black text-slate-800">{{ detailRecord.rollCount || 0 }}</p>
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

        <div class="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
          <div class="flex items-start gap-3">
            <span class="material-symbols-outlined rounded-xl bg-white p-2 text-blue-600 shadow-sm">format_list_bulleted</span>
            <div class="min-w-0 flex-1">
              <h4 class="text-sm font-black text-slate-900">单匹布明细已拆分为独立页面</h4>
              <p class="mt-1 text-xs leading-5 text-slate-500">
                当前型号共 {{ detailRecord.rollCount || 0 }} 匹布。为避免明细过长影响抽屉体验，请进入独立页面查看条码、米数、自定义信息和出库操作。
              </p>
            </div>
          </div>
          <el-button
            :disabled="!canReadDetail"
            :title="canReadDetail ? '查看单匹布明细' : '暂无 inventory:detail 权限'"
            @click="openModelClothPage"
            class="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-3 text-sm font-black text-white shadow-lg shadow-blue-600/20 transition-colors hover:bg-blue-700"
          >
            查看单匹布明细
            <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
          </el-button>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="inVisible" title="新增入库" size="min(92vw, 520px)" destroy-on-close @closed="closeInDrawer">
      <el-form :model="inForm" label-position="top">
      <div class="h-1.5 w-full bg-emerald-500"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="flex items-center gap-2 text-xl font-black text-slate-900">
            <span class="material-symbols-outlined text-emerald-500">add_circle</span>
            新增入库
          </h3>
          <p class="mt-1.5 text-xs text-slate-500">条码不填时系统会自动生成唯一标识号，标签打印请到现场打印页完成。</p>
        </div>
        <div class="inventory-drawer-actions">
          <el-button circle @click="closeInDrawer" class="inventory-close-btn">
            <span class="material-symbols-outlined">close</span>
          </el-button>
        </div>
      </div>
      <div class="flex-1 space-y-6 overflow-y-auto p-6">
        <BusinessTimeCorrectionPanel
          v-model="inForm.inTime"
          :active="inTimeCorrectionMode"
          data-field="inventory.inTime"
          title="业务时间修正"
          label="业务时间"
          description="用于修正当前布匹的业务入库时间。"
        />
        <el-form-item :label="fieldLabel('barCode', '条码')">
          <span class="inventory-field-label">
            <span class="material-symbols-outlined text-[16px] text-slate-400">qr_code</span>
            {{ fieldLabel('barCode', '条码') }}
          </span>
          <el-input v-model.trim="inForm.barcode" placeholder="留空则自动生成" />
        </el-form-item>

        <label class="block">
          <span class="inventory-field-label">
            <span class="material-symbols-outlined text-[16px] text-slate-400">category</span>
            {{ fieldLabel('modelCode', '型号') }}
            <span v-if="fieldRequired('modelCode')" class="text-rose-500">*</span>
          </span>
          <el-input v-model.trim="inForm.modelCode" data-field="inventory.modelCode" @input="loadModelOptions" placeholder="搜索或输入型号" />
          <div v-if="modelOptions.length" class="mt-3 flex flex-wrap gap-2 rounded-xl border border-slate-100 bg-slate-50 p-3">
            <el-button v-for="item in modelOptions" :key="`${item.modelCode}-${item.spec}`" @click="pickModel(item)" class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-600 shadow-sm transition-colors hover:border-emerald-400 hover:text-emerald-700">
              {{ item.modelCode }} <span class="mx-1 text-slate-300">|</span> {{ meter(item.spec) }}
            </el-button>
          </div>
        </label>

        <div class="grid grid-cols-2 gap-4">
          <label class="block">
            <span class="inventory-field-label">
              <span class="material-symbols-outlined text-[16px] text-slate-400">straighten</span>
              {{ fieldLabel('spec', '规格') }}
              <span v-if="fieldRequired('spec')" class="text-rose-500">*</span>
            </span>
            <el-input-number v-model="inForm.spec" data-field="inventory.spec" :min="0" :step="0.01" :precision="2" controls-position="right" class="w-full" placeholder="0.00" />
          </label>
          <label class="block">
            <span class="inventory-field-label">
              <span class="material-symbols-outlined text-[16px] text-slate-400">input</span>
              {{ fieldLabel('totalMeters', '入库米数') }}
              <span v-if="fieldRequired('totalMeters')" class="text-rose-500">*</span>
            </span>
            <el-input-number v-model="inForm.meters" data-field="inventory.meters" :min="0" :step="0.01" :precision="2" controls-position="right" class="w-full" placeholder="0.00" />
          </label>
        </div>

        <div v-if="customInventoryFields.length" class="space-y-4 rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
          <div>
            <h4 class="text-sm font-black text-slate-800">自定义信息项</h4>
            <p class="mt-1 text-xs text-slate-500">这些信息项只属于当前组织，用于适配原有库存台账。</p>
          </div>
          <label v-for="field in customInventoryFields" :key="field.key" class="block">
            <span class="inventory-field-label">
              {{ field.label }}
              <span v-if="field.required" class="text-rose-500">*</span>
            </span>
            <el-input
              v-model.trim="inForm.customFields[field.key]"
              :data-field="`inventory.custom.${field.key}`"
              :type="customFieldInputType(field)"
              :placeholder="`请输入${field.label}`"
            />
          </label>
        </div>
      </div>
      <div class="flex gap-3 border-t border-slate-100 bg-slate-50 p-6">
        <el-button @click="closeInDrawer">取消</el-button>
        <el-tooltip :disabled="canInInventory" content="暂无 inventory:cloth:in 权限"><span><el-button type="primary" :disabled="!canInInventory" @click="submitIn">确认入库</el-button></span></el-tooltip>
      </div>
      </el-form>
    </el-drawer>

    <el-drawer v-model="imageRecognitionVisible" title="图片识别入库" size="min(96vw, 620px)" destroy-on-close>
      <el-form label-position="top">
      <div class="h-1.5 w-full bg-blue-500"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="flex items-center gap-2 text-xl font-black text-slate-900">
            <span class="material-symbols-outlined text-blue-500">photo_camera</span>
            图片识别入库
          </h3>
          <p class="mt-1.5 text-xs text-slate-500">先上传图片，系统带出候选信息；确认前请人工核对，避免错入库。</p>
        </div>
        <el-button circle @click="imageRecognitionVisible = false" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </el-button>
      </div>
      <div class="flex-1 space-y-5 overflow-y-auto p-6">
        <DragAttachmentUpload
          title="点击或拖拽上传码单、库存卡或布匹标签照片"
          helper-text="支持 PNG、JPG、JPEG、WEBP，单张不超过 5MB；识别后需要人工核对"
          accept="image/png,image/jpeg,image/webp"
          icon="photo_camera"
          :uploading="imageRecognitionUploading"
          :file-name="imageRecognitionResult.fileName"
          :file-url="imageRecognitionResult.fileUrl"
          :file-size="imageRecognitionResult.fileSize"
          :downloadable="false"
          @select="handleImageRecognitionFile"
          @remove="clearImageRecognitionResult"
        />
        <div class="rounded-2xl border border-blue-100 bg-blue-50/70 p-4">
          <div class="flex items-start gap-3">
            <span class="material-symbols-outlined rounded-xl bg-white p-2 text-blue-600 shadow-sm">info</span>
            <div class="text-xs leading-5 text-blue-900/80">
              <p class="font-bold">{{ imageRecognitionResult.message || '请上传清晰的码单、库存卡或布匹标签照片。' }}</p>
              <p class="mt-1">当前识别结果会作为草稿，不会自动写入库存；点击“确认入库”后才会生成库存记录并准备标签打印。</p>
            </div>
          </div>
        </div>

        <div v-if="imageRecognitionResult.fileUrl" class="overflow-hidden rounded-2xl border border-slate-100 bg-slate-50">
          <img :src="imageRecognitionResult.fileUrl" alt="入库识别图片" class="h-44 w-full object-cover" />
          <div class="flex items-center justify-between px-4 py-3 text-xs text-slate-500">
            <span class="truncate">{{ imageRecognitionResult.fileName }}</span>
            <span>置信度 {{ recognitionConfidenceText(imageRecognitionResult.confidence) }}</span>
          </div>
        </div>

        <div v-for="(candidate, index) in imageRecognitionCandidates" :key="candidate.localId" class="space-y-4 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm">
          <div class="flex items-center justify-between">
            <h4 class="text-sm font-black text-slate-900">候选布匹 {{ index + 1 }}</h4>
            <el-button
              v-if="imageRecognitionCandidates.length > 1"
              @click="removeRecognitionCandidate(index)"
              class="rounded-lg px-2 py-1 text-xs font-bold text-rose-500 hover:bg-rose-50"
            >
              删除
            </el-button>
          </div>

          <label class="block">
            <span class="inventory-field-label">{{ fieldLabel('barCode', '条码') }}</span>
            <el-input v-model.trim="candidate.barcode" placeholder="留空则自动生成" />
          </label>

          <label class="block">
            <span class="inventory-field-label">
              {{ fieldLabel('modelCode', '型号') }}
              <span class="text-rose-500">*</span>
            </span>
            <el-input v-model.trim="candidate.modelCode" placeholder="请输入或核对型号" />
          </label>

          <div class="grid grid-cols-2 gap-4">
            <label class="block">
              <span class="inventory-field-label">
                {{ fieldLabel('spec', '规格') }}
                <span class="text-rose-500">*</span>
              </span>
              <el-input-number v-model="candidate.spec" :min="0" :step="0.01" :precision="2" controls-position="right" class="w-full" placeholder="0.00" />
            </label>
            <label class="block">
              <span class="inventory-field-label">
                {{ fieldLabel('totalMeters', '入库米数') }}
                <span class="text-rose-500">*</span>
              </span>
              <el-input-number v-model="candidate.meters" :min="0" :step="0.01" :precision="2" controls-position="right" class="w-full" placeholder="0.00" />
            </label>
          </div>

          <div v-if="customInventoryFields.length" class="space-y-4 rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
            <h5 class="text-xs font-black text-slate-700">自定义信息项</h5>
            <label v-for="field in customInventoryFields" :key="field.key" class="block">
              <span class="inventory-field-label">
                {{ field.label }}
                <span v-if="field.required" class="text-rose-500">*</span>
              </span>
              <el-input
                v-model.trim="candidate.customFields[field.key]"
                :type="customFieldInputType(field)"
                :placeholder="`请输入${field.label}`"
              />
            </label>
          </div>

          <label class="flex cursor-pointer items-start gap-3 rounded-2xl border border-blue-100 bg-blue-50/80 p-3 text-xs text-blue-900">
            <el-checkbox v-model="candidate.manualVerified" class="mt-0" />
            <span>
              <b class="block text-sm text-blue-950">已人工核对该候选布匹</b>
              <span class="text-blue-900/70">确认型号、规格、米数和条码无误后再入库。</span>
            </span>
          </label>
        </div>

        <el-button @click="addRecognitionCandidate" class="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-blue-200 bg-blue-50/60 px-4 py-3 text-sm font-black text-blue-600 hover:bg-blue-50">
          <span class="material-symbols-outlined text-[18px]">add</span>
          继续添加一匹布
        </el-button>
      </div>
      <div class="flex gap-3 border-t border-slate-100 bg-slate-50 p-6">
        <el-button @click="imageRecognitionVisible = false">取消</el-button>
        <el-tooltip :disabled="canInInventory" content="暂无 inventory:cloth:in 权限"><span><el-button type="primary" :disabled="!canInInventory" @click="submitRecognizedInventory">确认入库</el-button></span></el-tooltip>
      </div>
      </el-form>
    </el-drawer>

    <el-drawer v-model="outVisible" title="扫码出库" size="min(92vw, 480px)" destroy-on-close>
      <el-form :model="outForm" label-position="top">
      <div class="h-1.5 w-full bg-slate-800"></div>
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50/50 p-6">
        <div>
          <h3 class="flex items-center gap-2 text-xl font-black text-slate-900">
            <span class="material-symbols-outlined text-slate-700">outbox</span>
            扫码出库
          </h3>
          <p class="mt-1.5 text-xs text-slate-500">请扫描或输入布匹条码，系统会校验剩余可出库米数。</p>
        </div>
        <el-button circle @click="closeOutDrawer" class="inventory-close-btn">
          <span class="material-symbols-outlined">close</span>
        </el-button>
      </div>
      <div class="flex-1 space-y-6 overflow-y-auto p-6">
        <label class="block">
          <span class="inventory-field-label">
            <span class="material-symbols-outlined text-[16px] text-slate-400">barcode_scanner</span>
            布匹条码 <span class="text-rose-500">*</span>
          </span>
          <div class="relative">
            <el-input v-model.trim="outForm.barcode" data-field="inventory.outBarcode" @change="lookupBarcode" class="font-mono" placeholder="请将光标放在此处扫码" autofocus />
            <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-300">qr_code_scanner</span>
          </div>
        </label>

        <div v-if="outPreviewMatchesBarcode" class="relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4">
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
          <el-input-number v-model="outForm.meters" data-field="inventory.outMeters" :min="0" :step="0.01" :precision="2" class="w-full" />
        </label>
      </div>
      <div class="flex gap-3 border-t border-slate-100 bg-slate-50 p-6">
        <el-button @click="closeOutDrawer">取消</el-button>
        <el-tooltip :disabled="canOutInventory" content="暂无 inventory:cloth:out 权限"><span><el-button type="primary" :disabled="!canOutInventory || !outPreviewMatchesBarcode" @click="submitOut">确认出库</el-button></span></el-tooltip>
      </div>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElButton, ElCheckbox, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox, ElOption, ElPagination, ElSelect, ElTag, ElTooltip } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { warnAndFocusField } from '@/utils/formFocus'
import { getCurrentTenantFieldConfig } from '@/api/tenantFieldConfig'
import { customTenantFields, defaultTenantFieldConfig, mergeTenantFieldConfig } from '@/utils/tenantFieldConfig'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import DateFilterInput from '@/components/DateFilterInput.vue'
import BusinessTimeCorrectionPanel from '@/components/BusinessTimeCorrectionPanel.vue'
import DragAttachmentUpload from '@/components/DragAttachmentUpload.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import { useTimeCorrectionMode } from '@/composables/useTimeCorrectionMode'
import {
  downloadInventoryImportTemplate,
  getInventoryModelPage,
  getInventorySummary,
  getInventoryTrend,
  getInventoryWarningSetting,
  getInventoryWarnings,
  getRecentInventoryRecords,
  importInventory,
  inCloth,
  outCloth,
  recognizeInventoryImage,
  searchInventoryBarcode,
  searchInventoryModels,
  updateInventoryWarningSetting
} from './api/inventory.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const rows = ref([])
const warningRows = ref([])
const recordRows = ref([])
const trendRows = ref([])
const modelOptions = ref([])
const loading = ref(false)
const listLoadError = ref(null)
const listLoaded = ref(false)
let listRequestId = 0
const summary = reactive({ totalMeters: 0, clothCount: 0, warningCount: 0, warningThresholdMeters: 100, todayInMeters: 0, todayOutMeters: 0 })
const pagination = reactive({ total: 0, pages: 0 })
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  specMin: '',
  specMax: '',
  remainingMin: '',
  remainingMax: '',
  updatedStart: '',
  updatedEnd: '',
  timeOrder: 'fifo'
})
const detailVisible = ref(false)
const inVisible = ref(false)
const outVisible = ref(false)
const imageRecognitionVisible = ref(false)
const warningSettingVisible = ref(false)
const warningSettingForm = reactive({ threshold: 100 })
const detailRecord = ref(null)
const outPreview = ref(null)
const outPreviewBarcode = ref('')
let outPreviewRequestId = 0
const importInputRef = ref(null)
const imageRecognitionUploading = ref(false)
const inForm = reactive({ barcode: '', modelCode: '', spec: null, meters: null, inTime: '', customFields: {} })
const outForm = reactive({ barcode: '', meters: '' })
const {
  timeCorrectionMode: inTimeCorrectionMode,
  closeTimeCorrectionMode: closeInTimeCorrectionMode
} = useTimeCorrectionMode({
  isAvailable: () => inVisible.value
})
const imageRecognitionResult = reactive({ fileName: '', fileUrl: '', fileSize: 0, confidence: 0, message: '' })
const imageRecognitionCandidates = ref([])
const inventoryFieldConfig = ref(defaultInventoryFieldConfig())
const canListInventory = computed(() => userStore.hasPermission('inventory:list'))
const canReadDetail = computed(() => userStore.hasPermission('inventory:detail'))
const canReadWarnings = computed(() => userStore.hasPermission('inventory:warning:list'))
const canReadRecords = computed(() => userStore.hasPermission('inventory:record:list'))
const canReadTrend = computed(() => userStore.hasPermission('inventory:trend'))
const canConfigureWarning = computed(() => userStore.hasPermission('inventory:warning:setting'))
const canInInventory = computed(() => userStore.hasPermission('inventory:cloth:in'))
const canOutInventory = computed(() => userStore.hasPermission('inventory:cloth:out'))
const canImportInventory = computed(() => userStore.hasPermission('inventory:import'))
const outPreviewMatchesBarcode = computed(() => Boolean(outPreview.value && outPreviewBarcode.value && outPreviewBarcode.value === outForm.barcode))

watch(() => outForm.barcode, (barcode) => {
  if (barcode !== outPreviewBarcode.value) outPreview.value = null
})

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const maxTrendValue = computed(() => Math.max(1, ...trendRows.value.flatMap((item) => [Number(item.inMeters || 0), Number(item.outMeters || 0)])))
const customInventoryFields = computed(() => customTenantFields(inventoryFieldConfig.value))
const defaultInventoryTableColumns = computed(() => [
  { key: 'modelCode', label: fieldLabel('modelCode', '型号') },
  { key: 'spec', label: fieldLabel('spec', '规格'), align: 'right' },
  { key: 'totalMeters', label: fieldLabel('totalMeters', '总米数'), align: 'right' },
  { key: 'remainingMeters', label: fieldLabel('remainingMeters', '剩余米数'), align: 'right' },
  { key: 'status', label: fieldLabel('status', '库存状态') },
  { key: 'updateTime', label: fieldLabel('updateTime', '更新时间') }
])
const {
  orderedColumns: inventoryTableColumns,
  moveColumn: moveInventoryTableColumn,
  resetColumns: resetInventoryTableColumns
} = useLocalTableColumns('inventory.model.list', defaultInventoryTableColumns)
const inventoryTableColumnCount = computed(() => inventoryTableColumns.value.length + 1)

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

function inventoryTableCellClass(key) {
  if (key === 'modelCode') return 'text-sm font-bold text-slate-700'
  if (key === 'remainingMeters') return 'text-right text-sm font-black text-blue-600'
  if (key === 'spec' || key === 'totalMeters') return 'text-right text-sm text-slate-600'
  if (key === 'updateTime') return 'text-xs text-slate-500'
  return ''
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

async function refreshAll() {
  const requests = [fetchFieldConfig()]
  if (canListInventory.value) requests.push(fetchData(), fetchSummary())
  if (canReadWarnings.value) requests.push(fetchWarnings())
  if (canReadRecords.value) requests.push(fetchRecords())
  if (canReadTrend.value) requests.push(fetchTrend())
  await Promise.all(requests)
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
  if (!canListInventory.value) {
    rows.value = []
    listLoaded.value = false
    return
  }
  const requestId = ++listRequestId
  rows.value = []
  listLoadError.value = null
  listLoaded.value = false
  loading.value = true
  try {
    const data = await getInventoryModelPage({
      ...query,
      status: query.status === '' ? undefined : Number(query.status),
      keyword: query.keyword || undefined,
      specMin: query.specMin || undefined,
      specMax: query.specMax || undefined,
      remainingMin: query.remainingMin || undefined,
      remainingMax: query.remainingMax || undefined,
      updatedStart: query.updatedStart || undefined,
      updatedEnd: query.updatedEnd || undefined,
      timeOrder: query.timeOrder || 'fifo'
    })
    if (requestId !== listRequestId) return
    rows.value = data.data || []
    pagination.total = Number(data.total || 0)
    pagination.pages = Number(data.pages || 0)
    listLoaded.value = true
  } catch (error) {
    if (requestId !== listRequestId) return
    listLoadError.value = resolveLoadFailure(error, '库存列表')
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

async function fetchSummary() {
  const data = await getInventorySummary()
  Object.assign(summary, data)
  if (data?.warningThresholdMeters == null && canReadWarnings.value) {
    try {
      const setting = await getInventoryWarningSetting()
      summary.warningThresholdMeters = setting?.warningThresholdMeters ?? summary.warningThresholdMeters
    } catch (error) {
      summary.warningThresholdMeters = summary.warningThresholdMeters || 100
    }
  }
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
  query.specMin = ''
  query.specMax = ''
  query.remainingMin = ''
  query.remainingMax = ''
  query.updatedStart = ''
  query.updatedEnd = ''
  query.timeOrder = 'fifo'
  query.pageNum = 1
  fetchData()
}

async function openWarningSetting() {
  if (!requireUiPermission('inventory:warning:setting')) return
  warningSettingForm.threshold = Number(summary.warningThresholdMeters ?? 100)
  warningSettingVisible.value = true
}

async function saveWarningSetting() {
  if (!requireUiPermission('inventory:warning:setting')) return
  const threshold = Number(warningSettingForm.threshold)
  if (!Number.isFinite(threshold) || threshold < 0 || threshold > 999999999.99) {
    ElMessage.warning('库存预警阈值不合法')
    return
  }
  const result = await updateInventoryWarningSetting({
    warningThresholdMeters: Number(threshold.toFixed(2))
  })
  summary.warningThresholdMeters = result?.warningThresholdMeters ?? threshold
  warningSettingVisible.value = false
  const requests = []
  if (canListInventory.value) requests.push(fetchSummary())
  if (canReadWarnings.value) requests.push(fetchWarnings())
  await Promise.all(requests)
  ElMessage.success('库存预警阈值已更新')
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
}

function openModelClothPage() {
  if (!requireUiPermission('inventory:detail')) return
  if (!detailRecord.value?.modelCode) {
    ElMessage.warning('请先选择库存型号')
    return
  }
  router.push({
    name: 'InventoryModelDetail',
    query: {
      modelCode: detailRecord.value.modelCode,
      spec: detailRecord.value.spec,
      status: query.status === '' ? undefined : query.status,
      timeOrder: query.timeOrder || 'fifo'
    }
  })
}

function openInDrawer() {
  if (!requireUiPermission('inventory:cloth:in')) return
  Object.assign(inForm, { barcode: '', modelCode: '', spec: null, meters: null, inTime: '' })
  resetCustomFields()
  modelOptions.value = []
  inVisible.value = true
}

function closeInDrawer() {
  inVisible.value = false
  closeInTimeCorrectionMode()
}

function openOutDrawer(record) {
  if (!requireUiPermission('inventory:cloth:out')) return
  Object.assign(outForm, { barcode: record?.barcode || '', meters: record?.remainingMeters ? String(record.remainingMeters) : '' })
  outPreview.value = record || null
  outPreviewBarcode.value = record?.barcode || ''
  outVisible.value = true
}

function closeOutDrawer() {
  outPreviewRequestId += 1
  outVisible.value = false
  outPreview.value = null
  outPreviewBarcode.value = ''
}

function closePanels() {
  detailVisible.value = false
  closeInDrawer()
  closeOutDrawer()
  imageRecognitionVisible.value = false
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
  inForm.spec = item.spec == null ? null : Number(item.spec)
  modelOptions.value = []
}

async function lookupBarcode() {
  const barcode = outForm.barcode
  const requestId = ++outPreviewRequestId
  outPreview.value = null
  outPreviewBarcode.value = ''
  if (!barcode) {
    return
  }
  try {
    const preview = await searchInventoryBarcode({ barCode: barcode })
    if (requestId !== outPreviewRequestId) return
    outPreview.value = preview || null
    outPreviewBarcode.value = barcode
  } catch (error) {
    if (requestId !== outPreviewRequestId) return
    outPreview.value = null
    outPreviewBarcode.value = ''
    throw error
  }
}

function triggerImageRecognition() {
  if (!requireUiPermission('inventory:cloth:in')) return
  imageRecognitionVisible.value = true
}

async function handleImageRecognitionFile(file) {
  if (!file) return
  try {
    validateRecognitionFile(file)
    imageRecognitionUploading.value = true
    const result = await recognizeInventoryImage(file)
    Object.assign(imageRecognitionResult, {
      fileName: result?.fileName || file.name,
      fileUrl: result?.fileUrl || '',
      fileSize: result?.fileSize || file.size || 0,
      confidence: Number(result?.confidence || 0),
      message: result?.message || '图片已上传，请核对识别结果后确认入库。'
    })
    const candidates = Array.isArray(result?.candidates) && result.candidates.length
      ? result.candidates
      : [{}]
    imageRecognitionCandidates.value = candidates.map(normalizeRecognitionCandidate)
    imageRecognitionVisible.value = true
  } catch (error) {
    if (error?.message && !['图片过大', '图片格式不支持', '图片内容为空'].includes(error.message)) {
      ElMessage.error(error?.msg || error?.message || '图片识别失败')
    }
  } finally {
    imageRecognitionUploading.value = false
  }
}

function clearImageRecognitionResult() {
  Object.assign(imageRecognitionResult, { fileName: '', fileUrl: '', fileSize: 0, confidence: 0, message: '' })
  imageRecognitionCandidates.value = []
}

function validateRecognitionFile(file) {
  const maxSize = 5 * 1024 * 1024
  const allowedTypes = ['image/png', 'image/jpeg', 'image/webp']
  const extension = String(file.name || '').split('.').pop()?.toLowerCase()
  if (!file || file.size <= 0) {
    ElMessage.warning('图片内容为空')
    throw new Error('图片内容为空')
  }
  if (file.size > maxSize) {
    ElMessage.warning('图片大小不能超过 5MB')
    throw new Error('图片过大')
  }
  if (!allowedTypes.includes(file.type) && !['png', 'jpg', 'jpeg', 'webp'].includes(extension)) {
    ElMessage.warning('仅支持 PNG、JPG、JPEG、WEBP 图片')
    throw new Error('图片格式不支持')
  }
}

function normalizeRecognitionCandidate(candidate = {}) {
  return {
    localId: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    barcode: candidate.barcode || '',
    modelCode: candidate.modelCode || '',
    spec: candidate.spec == null || candidate.spec === '' ? null : Number(candidate.spec),
    meters: candidate.meters == null || candidate.meters === '' ? null : Number(candidate.meters),
    manualVerified: false,
    customFields: createEmptyCustomFields()
  }
}

function createEmptyCustomFields() {
  return customInventoryFields.value.reduce((values, field) => {
    if (field?.key) values[field.key] = ''
    return values
  }, {})
}

function addRecognitionCandidate() {
  imageRecognitionCandidates.value.push(normalizeRecognitionCandidate())
}

function removeRecognitionCandidate(index) {
  imageRecognitionCandidates.value.splice(index, 1)
  if (imageRecognitionCandidates.value.length === 0) {
    addRecognitionCandidate()
  }
}

function recognitionConfidenceText(value) {
  const confidence = Number(value || 0)
  if (!Number.isFinite(confidence) || confidence <= 0) return '待核对'
  return `${Math.round(confidence * 100)}%`
}

function validateInboundPayload(payload, options = {}) {
  const { focusPrefix = '', labelPrefix = '' } = options
  const fieldName = (name) => (focusPrefix ? `${focusPrefix}.${name}` : '')
  const warn = (message, name) => {
    if (focusPrefix) {
      warnAndFocusField(message, fieldName(name))
    } else {
      ElMessage.warning(`${labelPrefix}${message}`)
    }
    return null
  }
  if (!payload.modelCode) return warn('请填写型号。', 'modelCode')
  if (Number(payload.spec) <= 0) return warn('请填写有效规格。', 'spec')
  if (Number(payload.meters) <= 0) return warn('请填写有效入库米数。', 'meters')
  const customFields = {}
  for (const field of customInventoryFields.value) {
    const value = String(payload.customFields?.[field.key] || '').trim()
    if (field.required && !value) {
      return warn(`请填写自定义信息：${field.label}`, `custom.${field.key}`)
    }
    if (field.fieldType === 'number' && value && !Number.isFinite(Number(value))) {
      return warn(`自定义信息必须是数字：${field.label}`, `custom.${field.key}`)
    }
    if (value) customFields[field.key] = value
  }
  return customFields
}

function formatBusinessDateTimePayload(value) {
  if (!value) {
    return ''
  }
  const text = String(value).trim()
  return text.length === 16 ? `${text}:00` : text
}

function validateInTimeInput(value) {
  if (!value) {
    return true
  }
  const payload = formatBusinessDateTimePayload(value)
  const date = new Date(payload)
  if (!Number.isFinite(date.getTime())) {
    warnAndFocusField('入库时间格式不正确，请选择完整日期和时间。', 'inventory.inTime')
    return false
  }
  if (date.getTime() > Date.now()) {
    warnAndFocusField('入库时间不能晚于当前时间。', 'inventory.inTime')
    return false
  }
  return true
}

async function submitIn() {
  if (!requireUiPermission('inventory:cloth:in')) return
  const customFields = validateInboundPayload(inForm, { focusPrefix: 'inventory' })
  if (customFields == null) return
  if (!validateInTimeInput(inForm.inTime)) return
  const result = await inCloth({
    barcode: inForm.barcode || undefined,
    modelCode: inForm.modelCode,
    spec: Number(inForm.spec),
    meters: Number(inForm.meters),
    inType: 'manual',
    inTime: formatBusinessDateTimePayload(inForm.inTime) || undefined,
    customFields
  })
  const hasLabelPrint = Boolean(result?.labelTask?.printTaskNo)
  ElMessage.success(hasLabelPrint ? '入库成功，标签已加入待打印' : '入库成功')
  closeInDrawer()
  await refreshAll()
}

async function submitRecognizedInventory() {
  if (!requireUiPermission('inventory:cloth:in')) return
  if (!imageRecognitionCandidates.value.length) {
    ElMessage.warning('请至少保留一条识别候选')
    return
  }
  const payloads = []
  for (let i = 0; i < imageRecognitionCandidates.value.length; i += 1) {
    const candidate = imageRecognitionCandidates.value[i]
    if (!candidate.manualVerified) {
      ElMessage.warning(`候选布匹 ${i + 1} 请先完成人工校验`)
      return
    }
    const customFields = validateInboundPayload(candidate, { labelPrefix: `候选布匹 ${i + 1}：` })
    if (customFields == null) return
    payloads.push({
      barcode: candidate.barcode || undefined,
      modelCode: candidate.modelCode,
      spec: Number(candidate.spec),
      meters: Number(candidate.meters),
      inType: 'image_recognition',
      manualVerified: true,
      customFields
    })
  }

  const results = []
  for (const payload of payloads) {
    results.push(await inCloth(payload))
  }
  const taskCount = results.filter((item) => item?.labelTask?.printTaskNo).length
  ElMessage.success(`图片识别入库成功 ${results.length} 匹${taskCount ? '，标签已加入待打印' : ''}`)
  imageRecognitionVisible.value = false
  await refreshAll()
}

async function submitOut() {
  if (!requireUiPermission('inventory:cloth:out')) return
  if (!outForm.barcode) return warnAndFocusField('请填写或扫描布匹条码。', 'inventory.outBarcode')
  if (Number(outForm.meters) <= 0) return warnAndFocusField('请填写有效出库米数。', 'inventory.outMeters')
  if (!outPreviewMatchesBarcode.value || outPreviewBarcode.value !== outForm.barcode) {
    ElMessage.warning('条码已变化，请重新查询并核对目标布匹')
    return
  }
  await outCloth({ barcode: outForm.barcode, meters: Number(outForm.meters) })
  ElMessage.success('出库成功')
  closeOutDrawer()
  await refreshAll()
}

async function handleTemplateDownload() {
  if (!requireUiPermission('inventory:import')) return
  const blob = await downloadInventoryImportTemplate()
  await downloadBlob(blob, '外部库存导入说明.xlsx')
}

function triggerImport() {
  if (!requireUiPermission('inventory:import')) return
  importInputRef.value?.click()
}

function requireUiPermission(permission) {
  if (userStore.hasPermission(permission)) return true
  ElMessage.warning('当前账号暂无权限')
  return false
}

function resolveLoadFailure(error, label) {
  const statusCode = Number(error?.code || error?.response?.data?.code || error?.response?.status || error?.status || 0)
  if (statusCode === 401) return { kind: 'authentication', message: `${label}加载失败：登录状态已失效，请重新登录。` }
  if (statusCode === 403) return { kind: 'permission', message: `${label}加载失败：当前账号暂无权限。` }
  if (statusCode >= 500) return { kind: 'server', message: `${label}加载失败：服务暂时不可用，请稍后重试。` }
  return { kind: 'network', message: `${label}加载失败：网络异常，请检查连接后重试。` }
}

function statusTagType(value) {
  if (Number(value) === 0) return 'success'
  if (Number(value) === 2) return 'warning'
  return 'info'
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
      ElMessage.error('下载失败：文件内容不是有效 Excel')
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
  z-index: 100;
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

.inventory-drawer-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 0.75rem;
}

.inventory-time-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border-radius: 999px;
  border: 1px solid rgb(251 191 36 / 0.45);
  background: rgb(255 251 235);
  padding: 0.45rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 800;
  color: rgb(146 64 14);
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.15s ease;
}

.inventory-time-btn:hover,
.inventory-time-btn.active {
  background: rgb(245 158 11);
  color: #fff;
  transform: translateY(-1px);
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
