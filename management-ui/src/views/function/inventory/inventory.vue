<template>
  <div class="h-full min-h-0 bg-slate-50/50 text-slate-800 overflow-x-hidden font-sans">
    <div class="max-w-7xl mx-auto space-y-6 p-2 md:p-4">

      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <div class="inline-flex items-center gap-2 mb-2 px-3 py-1 rounded-full bg-blue-100/50 text-blue-700 text-xs font-bold tracking-widest uppercase">
            <span class="material-symbols-outlined text-[16px]">inventory_2</span>
            仓库调度中心
          </div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-slate-900 leading-tight">库存实时管理</h1>
          <p class="text-sm md:text-base text-slate-500 mt-2 max-w-2xl leading-relaxed">
            管理布匹入库、出库和库存预警，网页端已接入真实库存接口，方便仓库人员快速查条码和看流水。
          </p>
        </div>
        <div class="flex items-center gap-3">
          <button @click="openInDrawer" class="px-5 py-2.5 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-700 transition-all shadow-md shadow-blue-600/20 active:scale-95 flex items-center justify-center gap-1.5">
            <span class="material-symbols-outlined text-[20px]">add_circle</span>新增入库
          </button>
          <button @click="openOutDrawer()" class="px-5 py-2.5 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-900 transition-all shadow-md shadow-slate-800/20 active:scale-95 flex items-center justify-center gap-1.5">
            <span class="material-symbols-outlined text-[20px]">outbox</span>扫码出库
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-slate-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-blue-50 opacity-50 group-hover:scale-110 transition-transform">all_inbox</span>
          <p class="text-xs font-bold text-slate-500 uppercase tracking-widest relative z-10">可用总库存</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-blue-600 truncate" :title="meter(summary.totalMeters)">
              {{ formatBigNumber(summary.totalMeters) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.totalMeters, '米') }}</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-slate-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-slate-50 opacity-50 group-hover:scale-110 transition-transform">layers</span>
          <p class="text-xs font-bold text-slate-500 uppercase tracking-widest relative z-10">在库布匹</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-slate-800 truncate" :title="summary.clothCount">
              {{ formatBigNumber(summary.clothCount) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.clothCount, '卷/匹') }}</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-amber-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-amber-50 opacity-50 group-hover:scale-110 transition-transform">warning</span>
          <p class="text-xs font-bold text-amber-600/80 uppercase tracking-widest relative z-10">低库存预警</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-amber-600 truncate" :title="summary.warningCount">
              {{ summary.warningCount }}
            </h3>
            <span class="text-xs text-amber-500/70 font-medium whitespace-nowrap">低于 100 米</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-slate-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-emerald-50 opacity-50 group-hover:scale-110 transition-transform">move_to_inbox</span>
          <p class="text-xs font-bold text-slate-500 uppercase tracking-widest relative z-10">今日入库</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-emerald-500 truncate" :title="meter(summary.todayInMeters)">
              {{ formatBigNumber(summary.todayInMeters) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.todayInMeters, '米') }}</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-gradient-to-br from-slate-800 to-slate-900 text-white p-6 rounded-2xl shadow-lg shadow-slate-900/20 group hover:-translate-y-0.5 transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-white/5 group-hover:scale-110 transition-transform">outbox</span>
          <p class="text-xs font-bold text-slate-300 uppercase tracking-widest relative z-10">今日出库</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black truncate" :title="meter(summary.todayOutMeters)">
              {{ formatBigNumber(summary.todayOutMeters) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.todayOutMeters, '米') }}</span>
          </div>
        </div>
      </section>

      <section class="grid grid-cols-1 xl:grid-cols-[1fr_340px] gap-6">

        <div class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden flex flex-col min-h-[500px]">
          <div class="px-6 py-5 border-b border-slate-100 flex flex-wrap items-center justify-between gap-4 bg-slate-50/50">
            <div class="flex flex-wrap items-center gap-3">
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">search</span>
                <input
                    v-model.trim="query.keyword"
                    @keyup.enter="handleFilter"
                    class="w-64 max-w-full pl-10 pr-4 py-2.5 bg-white rounded-xl border border-slate-200 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 transition-all"
                    placeholder="搜索条码或型号"
                />
              </div>
              <div class="relative">
                <select v-model="query.status" class="appearance-none pl-4 pr-10 py-2.5 bg-white rounded-xl border border-slate-200 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 transition-all cursor-pointer min-w-[120px]">
                  <option value="">全部状态</option>
                  <option value="0">在库</option>
                  <option value="2">部分出库</option>
                  <option value="1">已出库</option>
                </select>
                <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">expand_more</span>
              </div>
              <button @click="handleFilter" class="px-5 py-2.5 bg-blue-50 text-blue-600 rounded-xl text-sm font-bold hover:bg-blue-100 transition-colors">查询</button>
              <button @click="resetFilter" class="px-5 py-2.5 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors">重置</button>
            </div>
            <span class="text-xs font-medium text-slate-500 bg-white px-3 py-1.5 rounded-lg border border-slate-100 shadow-sm">共 <b class="text-slate-800">{{ pagination.total }}</b> 条记录</span>
          </div>

          <div class="overflow-x-auto relative flex-1">
            <div v-if="loading" class="absolute inset-0 bg-white/70 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center gap-3">
              <span class="material-symbols-outlined text-blue-600 text-4xl animate-spin">progress_activity</span>
              <span class="text-sm font-medium text-blue-600">加载数据中...</span>
            </div>
            <table class="w-full text-left border-collapse min-w-[1040px]">
              <thead class="bg-slate-50/80 sticky top-0 z-0">
              <tr>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">型号</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">规格</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">总米数</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">剩余米数</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">状态</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">更新时间</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">操作</th>
              </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
              <tr v-for="item in rows" :key="item.id" class="cursor-pointer hover:bg-blue-50/40 transition-colors group" @click="openDetail(item)">
                <td class="px-6 py-4 text-sm font-bold text-slate-700">{{ item.modelCode }}</td>
                <td class="px-6 py-4 text-right text-sm text-slate-600">{{ meter(item.spec) }}</td>
                <td class="px-6 py-4 text-right text-sm text-slate-600">{{ meter(item.totalMeters) }}</td>
                <td class="px-6 py-4 text-right text-sm font-black text-blue-600">{{ meter(item.remainingMeters) }}</td>
                <td class="px-6 py-4">
                    <span :class="statusClass(item.status)" class="inline-flex px-2.5 py-1 rounded-md text-[11px] font-bold tracking-wider">
                      {{ item.statusName || statusLabel(item.status) }}
                    </span>
                </td>
                <td class="px-6 py-4 text-xs text-slate-500">{{ formatDateTime(item.updateTime) }}</td>
                <td class="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                  <button @click.stop="openDetail(item)" class="text-blue-600 hover:bg-blue-100/50 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors">详情</button>
                  <button
                      @click.stop="openOutDrawer(item)"
                      :disabled="Number(item.remainingMeters || 0) <= 0"
                      class="text-emerald-700 bg-emerald-50 hover:bg-emerald-100 disabled:opacity-40 disabled:hover:bg-emerald-50 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors"
                  >
                    出库
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && rows.length === 0">
                <td colspan="8" class="px-6 py-16 text-center">
                  <div class="flex flex-col items-center justify-center text-slate-400">
                    <span class="material-symbols-outlined text-5xl mb-2 opacity-50">search_off</span>
                    <p class="text-sm">暂无符合条件的库存记录</p>
                  </div>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="p-4 bg-slate-50 flex items-center justify-between text-sm text-slate-500 border-t border-slate-100">
            <span>第 <b class="text-slate-800">{{ query.pageNum }}</b> / {{ totalPages }} 页</span>
            <div class="flex gap-2">
              <button @click="changePage(query.pageNum - 1)" :disabled="query.pageNum <= 1" class="px-4 py-2 rounded-xl bg-white border border-slate-200 disabled:opacity-50 hover:bg-slate-50 transition-colors font-medium text-slate-700">上一页</button>
              <button @click="changePage(query.pageNum + 1)" :disabled="query.pageNum >= totalPages" class="px-4 py-2 rounded-xl bg-white border border-slate-200 disabled:opacity-50 hover:bg-slate-50 transition-colors font-medium text-slate-700">下一页</button>
            </div>
          </div>
        </div>

        <aside class="space-y-6">

          <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-base font-black text-slate-800 flex items-center gap-2">
                <span class="material-symbols-outlined text-blue-600 text-[20px]">monitoring</span>
                7 天出入库趋势
              </h2>
            </div>
            <div class="space-y-4">
              <div v-for="item in trendRows" :key="item.statDate" class="group">
                <div class="flex justify-between text-xs text-slate-500 mb-1.5 font-medium">
                  <span>{{ item.statDate }}</span>
                  <span class="text-slate-400">
                    <span class="text-emerald-600 font-bold">入 {{ meter(item.inMeters) }}</span> /
                    <span class="text-slate-800 font-bold">出 {{ meter(item.outMeters) }}</span>
                  </span>
                </div>
                <div class="h-2.5 bg-slate-100 rounded-full overflow-hidden flex ring-1 ring-inset ring-slate-200/50">
                  <div class="bg-emerald-400 transition-all duration-500" :style="{ width: trendWidth(item.inMeters) }"></div>
                  <div class="bg-slate-800 transition-all duration-500" :style="{ width: trendWidth(item.outMeters) }"></div>
                </div>
              </div>
              <div v-if="trendRows.length === 0" class="py-4 flex flex-col items-center justify-center text-slate-400 text-sm">
                <span class="material-symbols-outlined mb-1 opacity-50">bar_chart</span>暂无趋势数据
              </div>
            </div>
          </section>

          <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <h2 class="text-base font-black text-slate-800 mb-5 flex items-center gap-2">
              <span class="material-symbols-outlined text-amber-500 text-[20px]">error</span>
              低库存预警
            </h2>
            <div class="space-y-3">
              <div v-for="item in warningRows" :key="item.modelCode" class="p-4 rounded-xl bg-amber-50/50 border border-amber-100/80 hover:bg-amber-50 transition-colors">
                <div class="flex items-center justify-between">
                  <p class="text-sm font-black text-amber-900">{{ item.modelCode }}</p>
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-700">需补货</span>
                </div>
                <p class="text-xs text-amber-700/80 mt-2 flex items-center gap-1">
                  仅剩 <b class="text-amber-600 text-sm">{{ meter(item.totalMeters) }}</b> 米
                </p>
              </div>
              <div v-if="warningRows.length === 0" class="py-6 flex flex-col items-center justify-center text-slate-400 text-sm bg-slate-50 rounded-xl border border-dashed border-slate-200">
                <span class="material-symbols-outlined mb-1 opacity-50 text-emerald-500">check_circle</span>
                库位充足，暂无预警
              </div>
            </div>
          </section>

          <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <h2 class="text-base font-black text-slate-800 mb-5 flex items-center gap-2">
              <span class="material-symbols-outlined text-slate-400 text-[20px]">history</span>
              最近流水
            </h2>
            <div class="space-y-4 max-h-[320px] overflow-y-auto pr-2 custom-scrollbar">
              <div v-for="item in recordRows" :key="item.id" class="flex gap-4 group">
                <div class="relative flex flex-col items-center mt-1">
                  <div :class="item.operateType === 1 ? 'bg-slate-800 ring-slate-200' : 'bg-emerald-500 ring-emerald-100'" class="w-2.5 h-2.5 rounded-full ring-4 z-10"></div>
                  <div class="w-px h-full bg-slate-100 absolute top-3 group-last:hidden"></div>
                </div>
                <div class="flex-1 pb-1">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-bold text-slate-800">{{ item.operateTypeName }} <span :class="item.operateType === 1 ? 'text-slate-600' : 'text-emerald-600'">{{ meter(item.operateMeters) }}</span> 米</p>
                  </div>
                  <p class="text-xs text-slate-500 mt-1 font-medium">{{ item.modelCode }} <span class="mx-1 opacity-50">/</span> {{ item.barcode || '--' }}</p>
                  <p class="text-[11px] text-slate-400 mt-1 flex items-center gap-1">
                    <span class="material-symbols-outlined text-[12px]">person</span> {{ item.operatorName || '系统' }}
                    <span class="mx-1 opacity-50">·</span>
                    {{ formatDateTime(item.createTime) }}
                  </p>
                </div>
              </div>
              <div v-if="recordRows.length === 0" class="py-4 flex flex-col items-center justify-center text-slate-400 text-sm">
                暂无库存流水
              </div>
            </div>
          </section>

        </aside>
      </section>
    </div>

    <transition name="fade">
      <div v-if="detailVisible || inVisible || outVisible" class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-40 transition-opacity" @click="closePanels"></div>
    </transition>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[420px] bg-white shadow-2xl z-50 flex flex-col transition-transform duration-300 ease-in-out" :class="detailVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 bg-blue-600 w-full"></div>
      <div class="p-6 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
        <div>
          <h3 class="font-black text-slate-900 text-xl">库存详情</h3>
          <p class="text-xs text-slate-500 mt-1.5 font-medium font-mono bg-slate-200/50 inline-block px-2 py-0.5 rounded">{{ detailRecord?.barcode || '--' }}</p>
        </div>
        <button @click="detailVisible = false" class="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto" v-if="detailRecord">
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-blue-50/50 border border-blue-100 p-5 rounded-2xl">
            <span class="text-[11px] text-blue-600/80 font-bold uppercase tracking-wider">剩余米数</span>
            <p class="text-3xl font-black text-blue-600 mt-1">{{ meter(detailRecord.remainingMeters) }}</p>
          </div>
          <div class="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
            <span class="text-[11px] text-slate-500 font-bold uppercase tracking-wider">库存状态</span>
            <div class="mt-2">
               <span :class="statusClass(detailRecord.status)" class="inline-flex px-3 py-1 rounded-md text-sm font-bold">
                {{ detailRecord.statusName || statusLabel(detailRecord.status) }}
              </span>
            </div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-100 divide-y divide-slate-100">
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">型号</span>
            <span class="font-bold text-slate-800">{{ detailRecord.modelCode }}</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">规格</span>
            <span class="font-bold text-slate-800">{{ meter(detailRecord.spec) }} 米</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">总入库数</span>
            <span class="font-bold text-slate-800">{{ meter(detailRecord.totalMeters) }} 米</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">首次入库时间</span>
            <span class="font-medium text-slate-800">{{ formatDateTime(detailRecord.inTime) }}</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">最近出库时间</span>
            <span class="font-medium text-slate-800">{{ formatDateTime(detailRecord.outTime) || '--' }}</span>
          </div>
        </div>
      </div>
    </aside>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[420px] bg-white shadow-2xl z-50 flex flex-col transition-transform duration-300 ease-in-out" :class="inVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 bg-emerald-500 w-full"></div>
      <div class="p-6 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
        <div>
          <h3 class="font-black text-slate-900 text-xl flex items-center gap-2">
            <span class="material-symbols-outlined text-emerald-500">add_circle</span>新增入库
          </h3>
          <p class="text-xs text-slate-500 mt-1.5">条码不填时系统会自动生成唯一的标识号。</p>
        </div>
        <button @click="inVisible = false" class="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto">
        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
            <span class="material-symbols-outlined text-[16px] text-slate-400">qr_code</span> 条码
          </span>
          <input v-model.trim="inForm.barcode" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="可留空，由系统自动生成" />
        </label>

        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
            <span class="material-symbols-outlined text-[16px] text-slate-400">category</span> 布匹型号 <span class="text-rose-500">*</span>
          </span>
          <input v-model.trim="inForm.modelCode" @input="loadModelOptions" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="输入型号以检索" />
          <div v-if="modelOptions.length" class="mt-3 flex flex-wrap gap-2 p-3 bg-slate-50 border border-slate-100 rounded-xl">
            <button v-for="item in modelOptions" :key="`${item.modelCode}-${item.spec}`" @click="pickModel(item)" class="px-3 py-1.5 rounded-lg bg-white border border-slate-200 hover:border-emerald-400 hover:text-emerald-700 text-xs font-bold text-slate-600 transition-colors shadow-sm">
              {{ item.modelCode }} <span class="text-slate-300 mx-1">|</span> {{ meter(item.spec) }}
            </button>
          </div>
        </label>

        <div class="grid grid-cols-2 gap-4">
          <label class="block">
            <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
              <span class="material-symbols-outlined text-[16px] text-slate-400">straighten</span> 规格 <span class="text-rose-500">*</span>
            </span>
            <input v-model.trim="inForm.spec" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="0.00" />
          </label>
          <label class="block">
            <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
              <span class="material-symbols-outlined text-[16px] text-slate-400">input</span> 入库米数 <span class="text-rose-500">*</span>
            </span>
            <input v-model.trim="inForm.meters" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="0.00" />
          </label>
        </div>
      </div>
      <div class="p-6 border-t border-slate-100 flex gap-3 bg-slate-50">
        <button @click="inVisible = false" class="flex-1 px-4 py-3 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-100 transition-colors">取消</button>
        <button @click="submitIn" class="flex-1 px-4 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-sm shadow-md shadow-emerald-500/20 transition-all active:scale-95">确认入库</button>
      </div>
    </aside>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[420px] bg-white shadow-2xl z-50 flex flex-col transition-transform duration-300 ease-in-out" :class="outVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 bg-slate-800 w-full"></div>
      <div class="p-6 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
        <div>
          <h3 class="font-black text-slate-900 text-xl flex items-center gap-2">
            <span class="material-symbols-outlined text-slate-700">outbox</span>扫码出库
          </h3>
          <p class="text-xs text-slate-500 mt-1.5">扫描条形码以扣减对应的库存米数。</p>
        </div>
        <button @click="outVisible = false" class="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto">
        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
            <span class="material-symbols-outlined text-[16px] text-slate-400">barcode_scanner</span> 扫描条码 <span class="text-rose-500">*</span>
          </span>
          <div class="relative">
            <input v-model.trim="outForm.barcode" @change="lookupBarcode" class="w-full rounded-xl border border-slate-200 pl-4 pr-12 py-3 text-sm font-mono outline-none focus:ring-4 focus:ring-slate-800/10 focus:border-slate-800 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="请将光标置于此处扫码" autofocus />
            <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-300">qr_code_scanner</span>
          </div>
        </label>

        <div v-if="outPreview" class="rounded-xl bg-slate-50 border border-slate-200 p-4 relative overflow-hidden">
          <div class="absolute right-0 top-0 w-1 h-full bg-blue-500"></div>
          <div class="flex flex-col gap-2">
            <p class="text-sm flex justify-between"><span class="text-slate-500">识别型号</span><span class="font-bold text-slate-800">{{ outPreview.modelCode }}</span></p>
            <p class="text-sm flex justify-between"><span class="text-slate-500">当前可出</span><span class="font-bold text-blue-600">{{ meter(outPreview.remainingMeters) }} 米</span></p>
          </div>
        </div>

        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
            <span class="material-symbols-outlined text-[16px] text-slate-400">output</span> 出库米数 <span class="text-rose-500">*</span>
          </span>
          <input v-model.trim="outForm.meters" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-slate-800/10 focus:border-slate-800 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="请输入本次出库的数量" />
        </label>
      </div>
      <div class="p-6 border-t border-slate-100 flex gap-3 bg-slate-50">
        <button @click="outVisible = false" class="flex-1 px-4 py-3 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-100 transition-colors">取消</button>
        <button @click="submitOut" class="flex-1 px-4 py-3 rounded-xl bg-slate-800 hover:bg-slate-900 text-white font-bold text-sm shadow-md shadow-slate-800/20 transition-all active:scale-95">确认出库</button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getInventoryPage,
  getInventorySummary,
  getInventoryTrend,
  getInventoryWarnings,
  getRecentInventoryRecords,
  inCloth,
  outCloth,
  searchInventoryBarcode,
  searchInventoryModels
} from './api/inventory.js'

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
const outPreview = ref(null)
const inForm = reactive({ barcode: '', modelCode: '', spec: '', meters: '' })
const outForm = reactive({ barcode: '', meters: '' })

const totalPages = computed(() => Math.max(Number(pagination.pages || 1), 1))
const maxTrendValue = computed(() => Math.max(1, ...trendRows.value.flatMap((item) => [Number(item.inMeters || 0), Number(item.outMeters || 0)])))

refreshAll()

async function refreshAll() {
  await Promise.all([fetchData(), fetchSummary(), fetchWarnings(), fetchRecords(), fetchTrend()])
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getInventoryPage({
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

function openDetail(record) {
  detailRecord.value = record
  detailVisible.value = true
}

function openInDrawer() {
  Object.assign(inForm, { barcode: '', modelCode: '', spec: '', meters: '' })
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
  await inCloth({
    barcode: inForm.barcode || undefined,
    modelCode: inForm.modelCode,
    spec: Number(inForm.spec),
    meters: Number(inForm.meters),
    inType: 'manual'
  })
  ElMessage.success('入库成功')
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

function trendWidth(value) {
  return `${Math.min(100, Math.round((Number(value || 0) / maxTrendValue.value) * 100))}%`
}

function statusLabel(value) {
  if (Number(value) === 1) return '已出库'
  if (Number(value) === 2) return '部分出库'
  return '在库'
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

/* ---------------- 大数值处理函数 ---------------- */
function formatBigNumber(value) {
  const num = Number(value || 0)
  // 当数值大于等于 10000 时，降维转换为“万”，并保留两位小数
  if (num >= 10000) {
    return (num / 10000).toFixed(2)
  }
  // 如果是整数，直接返回，否则保留小数点后两位
  return Number.isInteger(num) ? num : num.toFixed(2)
}

function getUnit(value, defaultUnit = '米') {
  const num = Number(value || 0)
  // 根据数值动态改变单位
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

/* 自定义滚动条美化 */
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