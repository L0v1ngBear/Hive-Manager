<template>
  <div class="relative h-full flex flex-col min-w-0 bg-surface overflow-hidden">

    <div class="p-4 md:p-8 overflow-y-auto h-full space-y-8 pb-12">

      <div class="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <h2 class="text-3xl font-black text-primary tracking-tight">价格管理</h2>
          <p class="text-on-surface-variant mt-1">配置全局价格、阶梯批发折扣和面料估值规则。</p>
        </div>
        <div class="flex items-center gap-3">
          <button
            class="flex items-center gap-2 px-4 py-2 bg-surface-container-highest text-primary font-bold rounded-lg hover:bg-surface-container-high transition-colors text-sm">
            <span class="material-symbols-outlined text-lg">file_upload</span>
            导入价格表
          </button>

          <button
            class="flex items-center gap-2 px-4 py-2 bg-surface-container-highest text-primary font-bold rounded-lg hover:bg-surface-container-high transition-colors text-sm">
            <span class="material-symbols-outlined text-lg">file_download</span>
            导出价格表
          </button>

          <button
            @click="isCreateDrawerOpen = true"
            class="flex items-center gap-2 px-4 py-2 bg-primary text-white font-bold rounded-lg hover:bg-primary-container transition-colors text-sm shadow-md active:scale-95"
          >
            <span class="material-symbols-outlined text-lg">add_circle</span>
            追加产品价格
          </button>

          <PriceCreateDrawer
            :is-visible="isCreateDrawerOpen"
            @close="isCreateDrawerOpen = false"
            @success="handlePriceCreated"
          />
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 grid grid-cols-1 md:grid-cols-3 gap-4">
          <div
            class="bg-surface-container-lowest p-6 rounded-xl border border-transparent hover:border-outline-variant/20 transition-all shadow-sm">
            <div class="flex justify-between items-start">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">上架型号 (SKU)</span>
              <span class="material-symbols-outlined text-primary/40">inventory</span>
            </div>
            <div class="mt-4 flex flex-col">
              <span class="text-3xl font-black text-primary">1,284</span>
              <span class="text-xs text-green-600 font-medium flex items-center gap-1 mt-1">
                <span class="material-symbols-outlined text-xs">trending_up</span> 本月增长 12%
              </span>
            </div>
          </div>

          <div
            class="bg-surface-container-lowest p-6 rounded-xl border border-transparent shadow-sm">
            <div class="flex justify-between items-start">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">平均基准价</span>
              <span class="material-symbols-outlined text-primary/40">payments</span>
            </div>
            <div class="mt-4 flex flex-col">
              <span class="text-3xl font-black text-primary">¥42.50</span>
              <span class="text-xs text-on-surface-variant font-medium mt-1">按米数加权计算</span>
            </div>
          </div>

          <div
            class="bg-surface-container-lowest p-6 rounded-xl border border-transparent shadow-sm">
            <div class="flex justify-between items-start">
              <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">待生效变更</span>
              <span class="material-symbols-outlined text-orange-400/80">schedule</span>
            </div>
            <div class="mt-4 flex flex-col">
              <span class="text-3xl font-black text-primary">14</span>
              <span class="text-xs text-orange-600 font-medium mt-1">将于 10 月 1 日生效</span>
            </div>
          </div>
        </div>

        <div
          class="bg-[#1a365d] text-white p-6 rounded-xl flex flex-col relative overflow-hidden shadow-md">
          <div class="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full -mr-16 -mt-16"></div>

          <div class="flex justify-between items-center mb-4 relative z-10">
            <h3 class="text-sm font-bold uppercase tracking-wider opacity-80">阶梯批发折扣规则</h3>
            <button class="text-xs underline hover:no-underline opacity-80">管理全部</button>
          </div>
          <div class="space-y-3 relative z-10 flex-1">
            <div
              class="flex items-center justify-between p-3 bg-white/10 rounded-lg backdrop-blur-sm">
              <div class="flex flex-col">
                <span class="text-xs font-bold">梯队 01: 大批量客户</span>
                <span class="text-[10px] opacity-70">单笔订单量 > 500米</span>
              </div>
              <span class="text-lg font-black text-blue-200">享 95 折</span>
            </div>
            <div
              class="flex items-center justify-between p-3 bg-white/10 rounded-lg backdrop-blur-sm">
              <div class="flex flex-col">
                <span class="text-xs font-bold">梯队 02: 战略合作伙伴</span>
                <span class="text-[10px] opacity-70">单笔订单量 > 1000米</span>
              </div>
              <span class="text-lg font-black text-blue-200">享 9 折</span>
            </div>
          </div>
          <button
            class="mt-4 w-full py-2 bg-blue-100 text-[#1a365d] font-black text-xs rounded-lg uppercase tracking-widest hover:bg-white transition-colors relative z-10">
            创建折扣规则
          </button>
        </div>
      </div>

      <div
        class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden flex flex-col ring-1 ring-outline-variant/20">

        <div
          class="px-6 py-4 border-b border-surface-variant/50 flex flex-wrap items-center justify-between gap-4">
          <div class="flex items-center gap-3">
            <div
              class="flex items-center bg-surface-container-low rounded-lg px-3 py-1.5 border border-outline-variant/10">
              <span class="text-xs font-bold text-on-surface-variant mr-3">面料分类:</span>
              <select
                class="bg-transparent border-none p-0 text-xs font-bold text-primary focus:ring-0 cursor-pointer">
                <option>全部面料</option>
                <option>有机棉 (Organic Cotton)</option>
                <option>丝绸混纺 (Silk Blends)</option>
                <option>合成纤维 (Synthetic)</option>
              </select>
            </div>
            <div
              class="flex items-center bg-surface-container-low rounded-lg px-3 py-1.5 border border-outline-variant/10">
              <span class="text-xs font-bold text-on-surface-variant mr-3">状态:</span>
              <div class="flex gap-2">
                <span
                  class="text-[10px] px-2 py-0.5 rounded-full bg-primary/10 text-primary font-bold cursor-pointer">生效中</span>
                <span
                  class="text-[10px] px-2 py-0.5 rounded-full hover:bg-slate-200 text-on-surface-variant font-bold cursor-pointer transition-colors">计划中</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-2 text-xs font-medium text-on-surface-variant">
            <span class="material-symbols-outlined text-lg">calendar_today</span>
            <span>2024-09-01 至 2024-09-30</span>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
            <tr class="bg-surface-container-low/50">
              <th
                class="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                面料型号
              </th>
              <th
                class="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                规格说明
              </th>
              <th
                class="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-right">
                基准价
              </th>
              <th
                class="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                币种
              </th>
              <th
                class="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                生效日期
              </th>
              <th
                class="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                状态
              </th>
              <th
                class="px-6 py-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant"></th>
            </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/30">

            <tr
              v-for="item in tableData"
              :key="item.id"
              @click="openDrawer(item)"
              class="hover:bg-surface-container-high transition-colors cursor-pointer group"
              :class="{'bg-primary/5': activeRowId === item.id}"
            >
              <td class="px-6 py-4">
                <div class="flex items-center gap-4">
                  <div class="w-10 h-10 rounded-md overflow-hidden flex-shrink-0 bg-slate-200">
                    <img :src="item.image" :alt="item.model" class="w-full h-full object-cover"/>
                  </div>
                  <div>
                    <p class="text-sm font-bold text-primary">{{ item.model }}</p>
                    <p class="text-[10px] text-on-surface-variant">{{ item.lot }}</p>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 text-xs font-medium text-secondary">{{ item.spec }}</td>
              <td class="px-6 py-4 text-right text-sm font-black text-primary">
                ¥{{ item.price }} <span
                class="text-[10px] font-normal text-on-surface-variant italic">/m</span>
              </td>
              <td class="px-6 py-4 text-xs font-bold">{{ item.currency }}</td>
              <td class="px-6 py-4 text-xs text-on-surface-variant font-medium">{{ item.date }}</td>
              <td class="px-6 py-4">
                  <span v-if="item.status === 'Active'"
                        class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-green-100 text-green-700">
                    生效中
                  </span>
                <span v-else-if="item.status === 'Scheduled'"
                      class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700">
                    计划中
                  </span>
                <span v-else
                      class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-500">
                    已过期
                  </span>
              </td>
              <td class="px-6 py-4 text-right">
                <span
                  class="material-symbols-outlined text-primary/40 group-hover:text-primary transition-colors">chevron_right</span>
              </td>
            </tr>

            </tbody>
          </table>
        </div>
      </div>
    </div>

    <transition name="fade">
      <div v-if="isDrawerOpen" @click="closeDrawer"
           class="absolute inset-0 bg-black/20 backdrop-blur-[2px] z-40"></div>
    </transition>

    <div
      class="absolute top-0 right-0 h-full w-full sm:w-[400px] bg-white/80 backdrop-blur-2xl border-l border-outline-variant/30 shadow-2xl z-50 flex flex-col transition-transform duration-300 ease-out"
      :class="isDrawerOpen ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary w-full shrink-0"></div>

      <div
        class="p-6 border-b border-outline-variant/20 flex justify-between items-center bg-white/50">
        <div>
          <h3 class="font-black text-primary text-lg">价格详情洞察</h3>
          <p class="text-xs text-on-surface-variant font-medium">{{
              currentSku?.model || '未知型号'
            }}</p>
        </div>
        <button @click="closeDrawer"
                class="p-1 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <div class="flex-1 p-6 space-y-8 overflow-y-auto">

        <div class="grid grid-cols-2 gap-4">
          <div class="bg-surface-container-low p-4 rounded-xl ring-1 ring-outline-variant/10">
            <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-tighter">当前基准价</span>
            <p class="text-xl font-black text-primary">¥{{ currentSku?.price || '0.00' }}</p>
          </div>
          <div class="bg-surface-container-low p-4 rounded-xl ring-1 ring-outline-variant/10">
            <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-tighter">峰值价 (近12月)</span>
            <p class="text-xl font-black text-primary">¥48.20</p>
          </div>
        </div>

        <div class="space-y-3">
          <div class="flex justify-between items-center">
            <h4 class="text-xs font-bold text-primary uppercase tracking-widest">价格趋势
              (近12个月)</h4>
            <span class="text-[10px] text-green-600 font-bold bg-green-50 px-2 py-0.5 rounded">-6.6% 波动</span>
          </div>
          <div
            class="h-48 w-full bg-surface-container-low rounded-xl relative overflow-hidden flex items-end p-4 gap-2 border border-outline-variant/10">

            <svg class="absolute inset-0 w-full h-full px-4 pt-10" preserveAspectRatio="none"
                 viewBox="0 0 300 100">
              <path d="M0,80 Q30,70 60,75 T120,60 T180,40 T240,45 T300,55" fill="none"
                    stroke="#455f88" stroke-linecap="round" stroke-width="3"></path>
              <path d="M0,80 Q30,70 60,75 T120,60 T180,40 T240,45 T300,55 V100 H0 Z"
                    fill="url(#grad1)" opacity="0.1"></path>
              <defs>
                <linearGradient id="grad1" x1="0%" x2="0%" y1="0%" y2="100%">
                  <stop offset="0%" style="stop-color:#455f88;stop-opacity:1"></stop>
                  <stop offset="100%" style="stop-color:#455f88;stop-opacity:0"></stop>
                </linearGradient>
              </defs>
            </svg>

            <div class="flex-1 h-full flex items-end justify-between gap-1 relative z-10">
              <div
                class="w-full bg-primary/5 rounded-t-sm h-[60%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/5 rounded-t-sm h-[65%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/5 rounded-t-sm h-[62%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/5 rounded-t-sm h-[70%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/5 rounded-t-sm h-[75%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/5 rounded-t-sm h-[85%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/10 rounded-t-sm h-[80%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/10 rounded-t-sm h-[82%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/20 rounded-t-sm h-[88%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/20 rounded-t-sm h-[84%] hover:bg-primary/30 transition-colors"></div>
              <div
                class="w-full bg-primary/30 rounded-t-sm h-[80%] hover:bg-primary/50 transition-colors"></div>
              <div class="w-full bg-primary rounded-t-sm h-[78%]"></div>
            </div>
          </div>
          <div
            class="flex justify-between text-[10px] text-on-surface-variant font-bold uppercase px-1">
            <span>23年 9月</span>
            <span>24年 3月</span>
            <span>24年 8月</span>
          </div>
        </div>

        <div class="space-y-4">
          <h4 class="text-xs font-bold text-primary uppercase tracking-widest">价格调整日志</h4>
          <div class="space-y-3">
            <div
              class="flex gap-4 p-3 hover:bg-surface-container-low rounded-lg transition-colors border-l-2 border-primary bg-white shadow-sm">
              <div
                class="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                <span class="material-symbols-outlined text-primary text-sm">update</span>
              </div>
              <div class="flex-1">
                <p class="text-xs font-bold text-primary">基础价格更新</p>
                <p class="text-[10px] text-on-surface-variant mt-0.5">¥42.50 → ¥45.00 (操作人:
                  陈主管)</p>
                <p class="text-[10px] text-on-surface-variant mt-1.5 opacity-60">2024-08-12 • 14:20
                  PM</p>
              </div>
            </div>

            <div
              class="flex gap-4 p-3 hover:bg-surface-container-low rounded-lg transition-colors border-l-2 border-transparent bg-white shadow-sm">
              <div
                class="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center flex-shrink-0">
                <span class="material-symbols-outlined text-orange-700 text-sm">edit_note</span>
              </div>
              <div class="flex-1">
                <p class="text-xs font-bold text-primary">应用批量阶梯规则</p>
                <p class="text-[10px] text-on-surface-variant mt-0.5">
                  该型号已加入「战略合作伙伴」折扣梯队</p>
                <p class="text-[10px] text-on-surface-variant mt-1.5 opacity-60">2024-06-05 • 09:15
                  AM</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div
        class="p-6 bg-surface-container-lowest border-t border-outline-variant/20 grid grid-cols-2 gap-3 shrink-0">
        <button
          class="py-2.5 text-xs font-bold text-on-surface-variant bg-surface-container-high rounded-lg hover:bg-surface-variant transition-colors">
          对比其他 SKU
        </button>
        <button
          class="py-2.5 text-xs font-black text-white bg-primary rounded-lg hover:bg-primary/90 transition-colors shadow-md">
          修改定价规则
        </button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue';
import PriceCreateDrawer from './priceCreate.vue'; // 确保路径正确

defineOptions({name: 'PriceManagement'});

// ========================
// 状态控制变量
// ========================

// 1. 新增：控制新增价格抽屉的开关
const isCreateDrawerOpen = ref(false);

// 新增价格成功后的回调
const handlePriceCreated = () => {
  console.log('价格创建成功，刷新列表数据...');
  // TODO: 在这里添加刷新表格数据的 API 调用逻辑
};

// 2. 原有：控制详情抽屉的开关
const isDrawerOpen = ref(false);
const activeRowId = ref<number | null>(null);

// ========================
// 数据定义
// ========================

// 定义表格行数据类型
interface FabricItem {
  id: number;
  model: string;
  lot: string;
  spec: string;
  price: string;
  currency: string;
  date: string;
  status: 'Active' | 'Scheduled' | 'Expired';
  image: string;
}

// 当前选中的 SKU 详情
const currentSku = ref<FabricItem | null>(null);

// 模拟表格数据
const tableData = ref<FabricItem[]>([
  {
    id: 1,
    model: 'NV-2024-OXFORD',
    lot: '批号 #88219',
    spec: '100% 优质纯棉 (Oxford)',
    price: '45.00',
    currency: 'CNY',
    date: '2024-08-15',
    status: 'Active',
    image: 'https://placehold.co/100x100/e2e8f0/64748b?text=OXFORD'
  },
  {
    id: 2,
    model: 'SL-2024-LINEN',
    lot: '批号 #11204',
    spec: '80% 亚麻 / 20% 丝绸',
    price: '112.50',
    currency: 'CNY',
    date: '2024-09-01',
    status: 'Active',
    image: 'https://placehold.co/100x100/e2e8f0/64748b?text=LINEN'
  },
  {
    id: 3,
    model: 'PL-2025-TWILL',
    lot: '批号 #99321',
    spec: '再生聚酯斜纹布 (Twill)',
    price: '38.20',
    currency: 'CNY',
    date: '2024-10-12',
    status: 'Scheduled',
    image: 'https://placehold.co/100x100/e2e8f0/64748b?text=TWILL'
  },
  {
    id: 4,
    model: 'DT-2023-VOILE',
    lot: '批号 #10022',
    spec: '半透明纯棉薄纱 (Voile)',
    price: '29.00',
    currency: 'CNY',
    date: '2024-01-01',
    status: 'Expired',
    image: 'https://placehold.co/100x100/e2e8f0/64748b?text=VOILE'
  }
]);

// ========================
// 方法定义
// ========================

// 打开详情抽屉
const openDrawer = (item: FabricItem) => {
  currentSku.value = item;
  activeRowId.value = item.id;
  isDrawerOpen.value = true;
};

// 关闭详情抽屉
const closeDrawer = () => {
  isDrawerOpen.value = false;
  activeRowId.value = null;
};
</script>

<style scoped>
/* 渐变动画效果用于背景遮罩 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
