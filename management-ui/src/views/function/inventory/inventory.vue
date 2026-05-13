<template>
  <div class="function-page-shell h-full min-h-0 font-sans">
    <div class="function-page-container space-y-6 p-2 md:p-4">

      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined text-[16px]">inventory_2</span>
            浠撳簱璋冨害涓績
          </div>
          <h1 class="function-page-title">搴撳瓨瀹炴椂绠＄悊</h1>
          <p class="function-page-desc">
            绠＄悊甯冨尮鍏ュ簱銆佸嚭搴撳拰搴撳瓨棰勮锛岀綉椤电宸叉帴鍏ョ湡瀹炲簱瀛樻帴鍙ｏ紝鏂逛究浠撳簱浜哄憳蹇€熸煡鏉＄爜鍜岀湅娴佹按銆?          </p>
        </div>
        <div class="flex items-center gap-3">
          <button
              v-permission="'inventory:cloth:in'"
              @click="handleTemplateDownload"
              class="px-4 py-2 bg-white border border-slate-200 text-slate-700 font-bold rounded-xl flex items-center gap-2 hover:bg-slate-50 transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">description</span>瀛楁璇存槑
          </button>
          <button
              v-permission="'inventory:cloth:in'"
              @click="triggerImport"
              class="px-4 py-2 bg-white border border-slate-200 text-slate-700 font-bold rounded-xl flex items-center gap-2 hover:bg-slate-50 transition-colors text-sm"
          >
            <span class="material-symbols-outlined text-[20px]">file_upload</span>瀵煎叆澶栭儴搴撳瓨
          </button>
          <button @click="openInDrawer" class="function-action-primary">
            <span class="material-symbols-outlined text-[20px]">add_circle</span>鏂板鍏ュ簱
          </button>
          <button @click="openOutDrawer()" class="function-action-dark">
            <span class="material-symbols-outlined text-[20px]">outbox</span>鎵爜鍑哄簱
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-slate-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-blue-50 opacity-50 group-hover:scale-110 transition-transform">all_inbox</span>
          <p class="text-xs font-bold text-slate-500 uppercase tracking-widest relative z-10">鍙敤鎬诲簱瀛</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-blue-600 truncate" :title="meter(summary.totalMeters)">
              {{ formatBigNumber(summary.totalMeters) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.totalMeters, 'm') }}</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-slate-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-slate-50 opacity-50 group-hover:scale-110 transition-transform">layers</span>
          <p class="text-xs font-bold text-slate-500 uppercase tracking-widest relative z-10">鍦ㄥ簱甯冨尮</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-slate-800 truncate" :title="summary.clothCount">
              {{ formatBigNumber(summary.clothCount) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.clothCount, 'rolls') }}</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-amber-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-amber-50 opacity-50 group-hover:scale-110 transition-transform">warning</span>
          <p class="text-xs font-bold text-amber-600/80 uppercase tracking-widest relative z-10">浣庡簱瀛橀璀</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-amber-600 truncate" :title="summary.warningCount">
              {{ summary.warningCount }}
            </h3>
            <span class="text-xs text-amber-500/70 font-medium whitespace-nowrap">浣庝簬 100 绫</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-white p-6 rounded-2xl shadow-sm border border-slate-100 group hover:shadow-md transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-emerald-50 opacity-50 group-hover:scale-110 transition-transform">move_to_inbox</span>
          <p class="text-xs font-bold text-slate-500 uppercase tracking-widest relative z-10">浠婃棩鍏ュ簱</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black text-emerald-500 truncate" :title="meter(summary.todayInMeters)">
              {{ formatBigNumber(summary.todayInMeters) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.todayInMeters, 'm') }}</span>
          </div>
        </div>

        <div class="relative overflow-hidden bg-gradient-to-br from-slate-800 to-slate-900 text-white p-6 rounded-2xl shadow-lg shadow-slate-900/20 group hover:-translate-y-0.5 transition-all">
          <span class="material-symbols-outlined absolute -right-4 -bottom-4 text-[100px] text-white/5 group-hover:scale-110 transition-transform">outbox</span>
          <p class="text-xs font-bold text-slate-300 uppercase tracking-widest relative z-10">浠婃棩鍑哄簱</p>
          <div class="mt-3 flex items-baseline gap-1 relative z-10 min-w-0">
            <h3 class="text-3xl xl:text-4xl font-black truncate" :title="meter(summary.todayOutMeters)">
              {{ formatBigNumber(summary.todayOutMeters) }}
            </h3>
            <span class="text-xs text-slate-400 font-medium whitespace-nowrap">{{ getUnit(summary.todayOutMeters, 'm') }}</span>
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
                    placeholder="Search barcode or model"
                />
              </div>
              <div class="relative">
                <select v-model="query.status" class="appearance-none pl-4 pr-10 py-2.5 bg-white rounded-xl border border-slate-200 text-sm outline-none focus:ring-4 focus:ring-blue-600/10 focus:border-blue-500 transition-all cursor-pointer min-w-[120px]">
                  <option value="">鍏ㄩ儴鐘舵€</option>
                  <option value="0">鍦ㄥ簱</option>
                  <option value="2">閮ㄥ垎鍑哄簱</option>
                  <option value="1">宸插嚭搴</option>
                </select>
                <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">expand_more</span>
              </div>
              <button @click="handleFilter" class="px-5 py-2.5 bg-blue-50 text-blue-600 rounded-xl text-sm font-bold hover:bg-blue-100 transition-colors">鏌ヨ</button>
              <button @click="resetFilter" class="px-5 py-2.5 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-bold hover:bg-slate-50 transition-colors">閲嶇疆</button>
            </div>
            <span class="text-xs font-medium text-slate-500 bg-white px-3 py-1.5 rounded-lg border border-slate-100 shadow-sm">鍏?<b class="text-slate-800">{{ pagination.total }}</b> 鏉¤褰</span>
          </div>

          <div class="overflow-x-auto relative flex-1">
            <div v-if="loading" class="absolute inset-0 bg-white/70 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center gap-3">
              <span class="material-symbols-outlined text-blue-600 text-4xl animate-spin">progress_activity</span>
              <span class="text-sm font-medium text-blue-600">鍔犺浇鏁版嵁涓?..</span>
            </div>
            <table class="w-full text-left border-collapse min-w-[1040px]">
              <thead class="bg-slate-50/80 sticky top-0 z-0">
              <tr>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">{{ fieldLabel('modelCode', '型号') }}</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">{{ fieldLabel('spec', '规格') }}</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">{{ fieldLabel('totalMeters', '总米数') }}</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">{{ fieldLabel('remainingMeters', '剩余米数') }}</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">{{ fieldLabel('status', '库存状态') }}</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">{{ fieldLabel('updateTime', '更新时间') }}</th>
                <th class="px-6 py-4 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">鎿嶄綔</th>
              </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
              <tr v-for="item in rows" :key="`${item.modelCode}-${item.spec}`" class="cursor-pointer hover:bg-blue-50/40 transition-colors group" @click="openDetail(item)">
                <td class="px-6 py-4 text-sm font-bold text-slate-700">{{ item.modelCode }}</td>
                <td class="px-6 py-4 text-right text-sm text-slate-600">{{ meter(item.spec) }}</td>
                <td class="px-6 py-4 text-right text-sm text-slate-600">{{ meter(item.totalMeters) }}</td>
                <td class="px-6 py-4 text-right text-sm font-black text-blue-600">{{ meter(item.remainingMeters) }}</td>
                <td class="px-6 py-4">
                    <span :class="statusClass(item.status)" class="inline-flex px-2.5 py-1 rounded-md text-[11px] font-bold tracking-wider">
                      {{ item.statusName || statusLabel(item.status) }}
                    </span>
                </td>
                <td class="px-6 py-4 text-xs text-slate-500">{{ formatDateTime(item.latestTime || item.updateTime) }}</td>
                <td class="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                  <button @click.stop="openDetail(item)" class="text-blue-600 hover:bg-blue-100/50 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors">璇︽儏</button>
                  <button
                      v-if="item.barcode"
                      @click.stop="openOutDrawer(item)"
                      :disabled="Number(item.remainingMeters || 0) <= 0"
                      class="text-emerald-700 bg-emerald-50 hover:bg-emerald-100 disabled:opacity-40 disabled:hover:bg-emerald-50 px-3 py-1.5 rounded-lg text-xs font-bold transition-colors"
                  >
                    鍑哄簱
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && rows.length === 0">
                <td colspan="8" class="px-6 py-16 text-center">
                  <div class="flex flex-col items-center justify-center text-slate-400">
                    <span class="material-symbols-outlined text-5xl mb-2 opacity-50">search_off</span>
                    <p class="text-sm">鏆傛棤绗﹀悎鏉′欢鐨勫簱瀛樿褰</p>
                  </div>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="p-4 bg-slate-50 flex items-center justify-between text-sm text-slate-500 border-t border-slate-100">
            <span>绗?<b class="text-slate-800">{{ query.pageNum }}</b> / {{ totalPages }} 椤</span>
            <div class="flex gap-2">
              <button @click="changePage(query.pageNum - 1)" :disabled="query.pageNum <= 1" class="px-4 py-2 rounded-xl bg-white border border-slate-200 disabled:opacity-50 hover:bg-slate-50 transition-colors font-medium text-slate-700">涓婁竴椤</button>
              <button @click="changePage(query.pageNum + 1)" :disabled="query.pageNum >= totalPages" class="px-4 py-2 rounded-xl bg-white border border-slate-200 disabled:opacity-50 hover:bg-slate-50 transition-colors font-medium text-slate-700">涓嬩竴椤</button>
            </div>
          </div>
        </div>

        <aside class="space-y-6">

          <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-base font-black text-slate-800 flex items-center gap-2">
                <span class="material-symbols-outlined text-blue-600 text-[20px]">monitoring</span>
                7 澶╁嚭鍏ュ簱瓒嬪娍
              </h2>
            </div>
            <div class="space-y-4">
              <div v-for="item in trendRows" :key="item.statDate" class="group">
                <div class="flex justify-between text-xs text-slate-500 mb-1.5 font-medium">
                  <span>{{ item.statDate }}</span>
                  <span class="text-slate-400">
                    <span class="text-emerald-600 font-bold">鍏?{{ meter(item.inMeters) }}</span> /
                    <span class="text-slate-800 font-bold">鍑?{{ meter(item.outMeters) }}</span>
                  </span>
                </div>
                <div class="h-2.5 bg-slate-100 rounded-full overflow-hidden flex ring-1 ring-inset ring-slate-200/50">
                  <div class="bg-emerald-400 transition-all duration-500" :style="{ width: trendWidth(item.inMeters) }"></div>
                  <div class="bg-slate-800 transition-all duration-500" :style="{ width: trendWidth(item.outMeters) }"></div>
                </div>
              </div>
              <div v-if="trendRows.length === 0" class="py-4 flex flex-col items-center justify-center text-slate-400 text-sm">
                <span class="material-symbols-outlined mb-1 opacity-50">bar_chart</span>鏆傛棤瓒嬪娍鏁版嵁
              </div>
            </div>
          </section>

          <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <h2 class="text-base font-black text-slate-800 mb-5 flex items-center gap-2">
              <span class="material-symbols-outlined text-amber-500 text-[20px]">error</span>
              浣庡簱瀛橀璀?            </h2>
            <div class="space-y-3">
              <div v-for="item in warningRows" :key="item.modelCode" class="p-4 rounded-xl bg-amber-50/50 border border-amber-100/80 hover:bg-amber-50 transition-colors">
                <div class="flex items-center justify-between">
                  <p class="text-sm font-black text-amber-900">{{ item.modelCode }}</p>
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-700">闇€琛ヨ揣</span>
                </div>
                <p class="text-xs text-amber-700/80 mt-2 flex items-center gap-1">
                  浠呭墿 <b class="text-amber-600 text-sm">{{ meter(item.totalMeters) }}</b> 绫?                </p>
              </div>
              <div v-if="warningRows.length === 0" class="py-6 flex flex-col items-center justify-center text-slate-400 text-sm bg-slate-50 rounded-xl border border-dashed border-slate-200">
                <span class="material-symbols-outlined mb-1 opacity-50 text-emerald-500">check_circle</span>
                搴撲綅鍏呰冻锛屾殏鏃犻璀?              </div>
            </div>
          </section>

          <section class="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
            <h2 class="text-base font-black text-slate-800 mb-5 flex items-center gap-2">
              <span class="material-symbols-outlined text-slate-400 text-[20px]">history</span>
              鏈€杩戞祦姘?            </h2>
            <div class="space-y-4 max-h-[320px] overflow-y-auto pr-2 custom-scrollbar">
              <div v-for="item in recordRows" :key="item.id" class="flex gap-4 group">
                <div class="relative flex flex-col items-center mt-1">
                  <div :class="item.operateType === 1 ? 'bg-slate-800 ring-slate-200' : 'bg-emerald-500 ring-emerald-100'" class="w-2.5 h-2.5 rounded-full ring-4 z-10"></div>
                  <div class="w-px h-full bg-slate-100 absolute top-3 group-last:hidden"></div>
                </div>
                <div class="flex-1 pb-1">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-bold text-slate-800">{{ item.operateTypeName }} <span :class="item.operateType === 1 ? 'text-slate-600' : 'text-emerald-600'">{{ meter(item.operateMeters) }}</span> 绫</p>
                  </div>
                  <p class="text-xs text-slate-500 mt-1 font-medium">鍨嬪彿锛歿{ item.modelCode || '--' }}</p>
                  <p class="text-[11px] text-slate-400 mt-1 flex items-center gap-1">
                    <span class="material-symbols-outlined text-[12px]">person</span> {{ item.operatorName || '绯荤粺' }}
                    <span class="mx-1 opacity-50">路</span>
                    {{ formatDateTime(item.createTime) }}
                  </p>
                </div>
              </div>
              <div v-if="recordRows.length === 0" class="py-4 flex flex-col items-center justify-center text-slate-400 text-sm">
                鏆傛棤搴撳瓨娴佹按
              </div>
            </div>
          </section>

        </aside>
      </section>
    </div>

    <transition name="fade">
      <div v-if="detailVisible || inVisible || outVisible" class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-40 transition-opacity" @click="closePanels"></div>
    </transition>

    <input ref="importInputRef" type="file" accept=".xlsx,.xls,.csv" class="hidden" @change="handleImportChange" />

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[420px] bg-white shadow-2xl z-50 flex flex-col transition-transform duration-300 ease-in-out" :class="detailVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 bg-blue-600 w-full"></div>
      <div class="p-6 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
        <div>
          <h3 class="font-black text-slate-900 text-xl">搴撳瓨璇︽儏</h3>
          <p class="text-xs text-slate-500 mt-1.5 font-medium font-mono bg-slate-200/50 inline-block px-2 py-0.5 rounded">{{ detailRecord?.modelCode || '--' }} / {{ meter(detailRecord?.spec) }}</p>
        </div>
        <button @click="detailVisible = false" class="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto" v-if="detailRecord">
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-blue-50/50 border border-blue-100 p-5 rounded-2xl">
            <span class="text-[11px] text-blue-600/80 font-bold uppercase tracking-wider">鍓╀綑绫虫暟</span>
            <p class="text-3xl font-black text-blue-600 mt-1">{{ meter(detailRecord.remainingMeters) }}</p>
          </div>
          <div class="bg-slate-50 border border-slate-100 p-5 rounded-2xl">
            <span class="text-[11px] text-slate-500 font-bold uppercase tracking-wider">布匹数</span>
            <div class="mt-2">
              <span class="text-3xl font-black text-slate-800">{{ detailRecord.rollCount || detailRows.length || 0 }}</span>
            </div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-100 divide-y divide-slate-100">
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">{{ fieldLabel('modelCode', '型号') }}</span>
            <span class="font-bold text-slate-800">{{ detailRecord.modelCode }}</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">{{ fieldLabel('spec', '规格') }}</span>
            <span class="font-bold text-slate-800">{{ meter(detailRecord.spec) }} 绫</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">{{ fieldLabel('totalMeters', '总米数') }}</span>
            <span class="font-bold text-slate-800">{{ meter(detailRecord.totalMeters) }} 绫</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">棣栨鍏ュ簱鏃堕棿</span>
            <span class="font-medium text-slate-800">{{ formatDateTime(detailRecord.inTime) }}</span>
          </div>
          <div class="p-4 flex items-center justify-between text-sm">
            <span class="text-slate-500">鏈€杩戝嚭搴撴椂闂</span>
            <span class="font-medium text-slate-800">{{ formatDateTime(detailRecord.outTime) || '--' }}</span>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-100 overflow-hidden">
          <div class="px-4 py-3 bg-slate-50 flex items-center justify-between">
            <span class="text-sm font-black text-slate-800">单匹布明细</span>
            <span v-if="detailLoading" class="text-xs text-blue-600">loading...</span>
          </div>
          <div class="divide-y divide-slate-100 max-h-[360px] overflow-y-auto">
            <div v-for="cloth in detailRows" :key="cloth.id" class="p-4 text-sm space-y-2">
              <div class="flex items-center justify-between gap-3">
                <span class="font-mono font-bold text-slate-800 break-all">{{ cloth.barcode }}</span>
                <span :class="statusClass(cloth.status)" class="shrink-0 inline-flex px-2 py-0.5 rounded text-[11px] font-bold">{{ cloth.statusName || statusLabel(cloth.status) }}</span>
              </div>
              <div class="grid grid-cols-2 gap-2 text-xs text-slate-500">
                <span>{{ fieldLabel('totalMeters', '总米数') }}: <b class="text-slate-700">{{ meter(cloth.totalMeters) }}</b></span>
                <span>{{ fieldLabel('remainingMeters', '剩余米数') }}: <b class="text-blue-600">{{ meter(cloth.remainingMeters) }}</b></span>
                <span>入库: {{ formatDateTime(cloth.inTime) }}</span>
                <span>更新: {{ formatDateTime(cloth.updateTime) }}</span>
              </div>
              <div v-if="customInventoryFields.length" class="grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-3 text-xs text-slate-500">
                <span v-for="field in customInventoryFields" :key="`${cloth.id}-${field.key}`">
                  {{ field.label }}:
                  <b class="text-slate-700">{{ customFieldValue(cloth, field) }}</b>
                </span>
              </div>
              <button
                  @click="openOutDrawer(cloth)"
                  :disabled="Number(cloth.remainingMeters || 0) <= 0"
                  class="w-full mt-2 text-emerald-700 bg-emerald-50 hover:bg-emerald-100 disabled:opacity-40 px-3 py-2 rounded-lg text-xs font-bold transition-colors"
              >
                扫码出库
              </button>
            </div>
            <div v-if="!detailLoading && detailRows.length === 0" class="p-8 text-center text-sm text-slate-400">暂无单匹布明细</div>
          </div>
        </div>
      </div>
    </aside>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[420px] bg-white shadow-2xl z-50 flex flex-col transition-transform duration-300 ease-in-out" :class="inVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 bg-emerald-500 w-full"></div>
      <div class="p-6 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
        <div>
          <h3 class="font-black text-slate-900 text-xl flex items-center gap-2">
            <span class="material-symbols-outlined text-emerald-500">add_circle</span>鏂板鍏ュ簱
          </h3>
          <p class="text-xs text-slate-500 mt-1.5">鏉＄爜涓嶅～鏃剁郴缁熶細鑷姩鐢熸垚鍞竴鐨勬爣璇嗗彿銆</p>
        </div>
        <button @click="inVisible = false" class="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto">
        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
            <span class="material-symbols-outlined text-[16px] text-slate-400">qr_code</span> {{ fieldLabel('barCode', '条码') }}
          </span>
          <input v-model.trim="inForm.barcode" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="Leave empty to auto-generate" />
        </label>

        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
            <span class="material-symbols-outlined text-[16px] text-slate-400">category</span> {{ fieldLabel('modelCode', '型号') }} <span v-if="fieldRequired('modelCode')" class="text-rose-500">*</span>
          </span>
          <input v-model.trim="inForm.modelCode" @input="loadModelOptions" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="Search model" />
          <div v-if="modelOptions.length" class="mt-3 flex flex-wrap gap-2 p-3 bg-slate-50 border border-slate-100 rounded-xl">
            <button v-for="item in modelOptions" :key="`${item.modelCode}-${item.spec}`" @click="pickModel(item)" class="px-3 py-1.5 rounded-lg bg-white border border-slate-200 hover:border-emerald-400 hover:text-emerald-700 text-xs font-bold text-slate-600 transition-colors shadow-sm">
              {{ item.modelCode }} <span class="text-slate-300 mx-1">|</span> {{ meter(item.spec) }}
            </button>
          </div>
        </label>

        <div class="grid grid-cols-2 gap-4">
          <label class="block">
            <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
              <span class="material-symbols-outlined text-[16px] text-slate-400">straighten</span> {{ fieldLabel('spec', '规格') }} <span v-if="fieldRequired('spec')" class="text-rose-500">*</span>
            </span>
            <input v-model.trim="inForm.spec" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="0.00" />
          </label>
          <label class="block">
            <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
              <span class="material-symbols-outlined text-[16px] text-slate-400">input</span> {{ fieldLabel('totalMeters', '入库米数') }} <span v-if="fieldRequired('totalMeters')" class="text-rose-500">*</span>
            </span>
            <input v-model.trim="inForm.meters" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="0.00" />
          </label>
        </div>

        <div v-if="customInventoryFields.length" class="rounded-2xl border border-slate-100 bg-slate-50/70 p-4 space-y-4">
          <div>
            <h4 class="text-sm font-black text-slate-800">租户自定义字段</h4>
            <p class="mt-1 text-xs text-slate-500">这些字段只属于当前租户，用于适配客户原有库存台账。</p>
          </div>
          <label v-for="field in customInventoryFields" :key="field.key" class="block">
            <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
              {{ field.label }} <span v-if="field.required" class="text-rose-500">*</span>
            </span>
            <input
              v-model.trim="inForm.customFields[field.key]"
              :type="customFieldInputType(field)"
              class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 transition-all placeholder:text-slate-300 bg-white"
              :placeholder="`请输入${field.label}`"
            />
          </label>
        </div>
      </div>
      <div class="p-6 border-t border-slate-100 flex gap-3 bg-slate-50">
        <button @click="inVisible = false" class="flex-1 px-4 py-3 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-100 transition-colors">鍙栨秷</button>
        <button @click="submitIn" class="flex-1 px-4 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-sm shadow-md shadow-emerald-500/20 transition-all active:scale-95">纭鍏ュ簱</button>
      </div>
    </aside>

    <aside class="fixed top-0 right-0 h-full w-full sm:w-[420px] bg-white shadow-2xl z-50 flex flex-col transition-transform duration-300 ease-in-out" :class="outVisible ? 'translate-x-0' : 'translate-x-full'">
      <div class="h-1.5 bg-slate-800 w-full"></div>
      <div class="p-6 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
        <div>
          <h3 class="font-black text-slate-900 text-xl flex items-center gap-2">
            <span class="material-symbols-outlined text-slate-700">outbox</span>鎵爜鍑哄簱
          </h3>
          <p class="text-xs text-slate-500 mt-1.5">鎵弿鏉″舰鐮佷互鎵ｅ噺瀵瑰簲鐨勫簱瀛樼背鏁般€</p>
        </div>
        <button @click="outVisible = false" class="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"><span class="material-symbols-outlined">close</span></button>
      </div>
      <div class="flex-1 p-6 space-y-6 overflow-y-auto">
        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
            <span class="material-symbols-outlined text-[16px] text-slate-400">barcode_scanner</span> 鎵弿鏉＄爜 <span class="text-rose-500">*</span>
          </span>
          <div class="relative">
            <input v-model.trim="outForm.barcode" @change="lookupBarcode" class="w-full rounded-xl border border-slate-200 pl-4 pr-12 py-3 text-sm font-mono outline-none focus:ring-4 focus:ring-slate-800/10 focus:border-slate-800 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="璇峰皢鍏夋爣缃簬姝ゅ鎵爜" autofocus />
            <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-300">qr_code_scanner</span>
          </div>
        </label>

        <div v-if="outPreview" class="rounded-xl bg-slate-50 border border-slate-200 p-4 relative overflow-hidden">
          <div class="absolute right-0 top-0 w-1 h-full bg-blue-500"></div>
          <div class="flex flex-col gap-2">
            <p class="text-sm flex justify-between"><span class="text-slate-500">{{ fieldLabel('modelCode', '型号') }}</span><span class="font-bold text-slate-800">{{ outPreview.modelCode }}</span></p>
            <p class="text-sm flex justify-between"><span class="text-slate-500">{{ fieldLabel('remainingMeters', '当前可出') }}</span><span class="font-bold text-blue-600">{{ meter(outPreview.remainingMeters) }} 绫</span></p>
          </div>
        </div>

        <label class="block">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1 mb-2">
              <span class="material-symbols-outlined text-[16px] text-slate-400">output</span> {{ fieldLabel('remainingMeters', '出库米数') }} <span class="text-rose-500">*</span>
          </span>
          <input v-model.trim="outForm.meters" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:ring-4 focus:ring-slate-800/10 focus:border-slate-800 transition-all placeholder:text-slate-300 bg-slate-50/50 focus:bg-white" placeholder="璇疯緭鍏ユ湰娆″嚭搴撶殑鏁伴噺" />
        </label>
      </div>
      <div class="p-6 border-t border-slate-100 flex gap-3 bg-slate-50">
        <button @click="outVisible = false" class="flex-1 px-4 py-3 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-100 transition-colors">鍙栨秷</button>
        <button @click="submitOut" class="flex-1 px-4 py-3 rounded-xl bg-slate-800 hover:bg-slate-900 text-white font-bold text-sm shadow-md shadow-slate-800/20 transition-all active:scale-95">纭鍑哄簱</button>
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
  importInventory,
  getRecentInventoryRecords,
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
    const rows = await getCurrentTenantFieldConfig('inventory')
    inventoryFieldConfig.value = mergeInventoryFieldConfig(Array.isArray(rows) ? rows : [])
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
    ElMessage.warning('璇峰～鍐欏瀷鍙枫€佽鏍煎拰鏈夋晥鍏ュ簱绫虫暟')
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
  if (taskNo) {
    ElMessage.success('In stock success, mini-program label task: ' + taskNo)
  }
  if (!taskNo) { ElMessage.success('In stock success') }
  inVisible.value = false
  await refreshAll()
}

async function submitOut() {
  if (!outForm.barcode || Number(outForm.meters) <= 0) {
    ElMessage.warning('璇峰～鍐欐潯鐮佸拰鏈夋晥鍑哄簱绫虫暟')
    return
  }
  await outCloth({ barcode: outForm.barcode, meters: Number(outForm.meters) })
  ElMessage.success('鍑哄簱鎴愬姛')
  outVisible.value = false
  await refreshAll()
}

async function handleTemplateDownload() {
  const blob = await downloadInventoryImportTemplate()
  await downloadBlob(blob, '澶栭儴搴撳瓨瀵煎叆瀛楁璇存槑.xlsx')
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
        'Import result: success ' + (result.successCount || 0) + ', failed ' + (result.failCount || 0) + ', label tasks ' + (result.printTaskCount || 0) + '.' + (failText ? '\n\nFailed rows:\n' + failText : ''),
        'Inventory import result'
    )
    await refreshAll()
  } finally {
    event.target.value = ''
  }
}

async function downloadBlob(blob, fileName) {
  if (!blob || blob.size === 0) {
    ElMessage.error('Download failed: empty file')
    return
  }
  const contentType = String(blob.type || '').toLowerCase()
  if (contentType.includes('application/json') || contentType.includes('text/plain')) {
    const text = await blob.text()
    let message = text || '涓嬭浇澶辫触'
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
    const isZipBasedExcel = header.length >= 2 && header[0] === 0x50 && header[1] === 0x4B
    if (!isZipBasedExcel) {
      ElMessage.error('涓嬭浇澶辫触锛氭帴鍙ｆ病鏈夎繑鍥炴湁鏁堢殑 Excel 鏂囦欢')
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
  if (Number(value) === 1) return 'Out'
  if (Number(value) === 2) return 'Partial out'
  return 'In stock'
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

function getUnit(value, defaultUnit = 'm') {
  const num = Number(value || 0)
  return num >= 10000 ? '10k ' + defaultUnit : defaultUnit
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

/* 鑷畾涔夋粴鍔ㄦ潯缇庡寲 */
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
