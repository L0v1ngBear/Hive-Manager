<template>
  <div class="max-w-7xl mx-auto h-[calc(100vh-8rem)] flex flex-col lg:flex-row gap-6 pb-6">

    <section class="w-full lg:w-[400px] flex flex-col bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 overflow-hidden">
      <header class="p-5 border-b border-outline-variant/20 bg-surface flex justify-between items-center z-10">
        <div>
          <h2 class="text-lg font-black text-on-surface flex items-center gap-2">
            待打印任务队列
            <span class="bg-error/10 text-error text-[10px] px-2 py-0.5 rounded-full font-bold">
              {{ pendingOrders.length }}
            </span>
          </h2>
          <p class="text-xs text-on-surface-variant mt-1">选择下方出库单进行预览和打印</p>
        </div>
        <el-button circle icon="Refresh" @click="fetchPendingList" :loading="isFetchingList" />
      </header>

      <div class="flex-1 overflow-y-auto p-3 space-y-2 bg-surface-container-lowest">

        <div v-if="pendingOrders.length === 0 && !isFetchingList" class="h-full flex flex-col items-center justify-center text-on-surface-variant/50">
          <span class="material-symbols-outlined text-4xl mb-2">task_alt</span>
          <p class="text-sm font-medium">当前暂无待打印单据</p>
        </div>

        <div
          v-for="item in pendingOrders"
          :key="item.orderNo"
          @click="selectOrder(item)"
          :class="[
            'p-4 rounded-xl cursor-pointer transition-all border relative overflow-hidden',
            selectedOrder?.orderNo === item.orderNo
              ? 'bg-primary/5 border-primary/30 ring-1 ring-primary/30'
              : 'bg-surface hover:bg-surface-container border-outline-variant/20'
          ]"
        >
          <div v-if="selectedOrder?.orderNo === item.orderNo" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>

          <div class="flex justify-between items-start mb-2">
            <span class="text-sm font-black text-on-surface font-mono">{{ item.orderNo }}</span>
            <span class="text-xs text-on-surface-variant bg-surface-container px-2 py-0.5 rounded">{{ item.time }}</span>
          </div>
          <div class="text-xs text-on-surface-variant font-medium truncate mb-1">
            客户: <span class="text-on-surface">{{ item.customerName }}</span>
          </div>
          <div class="text-xs text-on-surface-variant">
            包含 <span class="font-bold text-primary">{{ item.itemCount }}</span> 条出库明细 | 经办: {{ item.operator }}
          </div>
        </div>
      </div>
    </section>

    <aside class="flex-1 bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 flex flex-col overflow-hidden relative">

      <div v-if="isLoadingDetail" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-20 flex flex-col items-center justify-center">
        <span class="material-symbols-outlined text-primary text-3xl animate-spin mb-2">progress_activity</span>
        <span class="text-sm text-primary font-bold">正在生成单据排版...</span>
      </div>

      <header class="p-4 border-b border-outline-variant/20 flex justify-between items-center bg-surface z-10">
        <h3 class="text-sm font-bold text-on-surface">针打预览区 (241mm × 140mm)</h3>
        <div class="flex gap-2">
          <el-button v-if="selectedOrder" plain type="danger" @click="handleSkipPrint">标记为已打印 (跳过)</el-button>
          <el-button type="success" icon="Printer" :disabled="!selectedOrder" @click="executePrint">
            开始打印
          </el-button>
        </div>
      </header>

      <div class="flex-1 overflow-y-auto bg-[#e5e7eb] p-8 flex justify-center">
        <div
          id="print-paper-area"
          class="bg-white shadow-xl relative overflow-hidden transition-opacity"
          :class="!selectedOrder ? 'opacity-0' : 'opacity-100'"
          style="width: 241mm; min-height: 140mm; padding: 10mm; font-family: 'SimSun', '宋体', serif; color: #000;"
        >
          <div v-if="!selectedOrder" class="absolute inset-0 flex flex-col items-center justify-center bg-white z-10 text-gray-400 print:hidden opacity-100">
            <span class="material-symbols-outlined text-5xl mb-2">touch_app</span>
            <p>请在左侧列表中选择单据进行打印</p>
          </div>

          <template v-if="selectedOrder">
            <div class="text-center relative mb-4">
              <h1 class="text-2xl font-black tracking-widest">星火服装厂</h1>
              <h2 class="text-lg font-bold tracking-widest mt-1 pb-2 border-b-2 border-black inline-block px-4">产品出库单</h2>
              <div class="absolute right-0 bottom-0 text-xs font-bold font-mono">
                单号: {{ selectedOrder.orderNo }}
              </div>
            </div>

            <div class="flex justify-between text-[13px] font-bold mb-2">
              <div>购货单位：{{ selectedOrder.customerName }}</div>
              <div>日期：{{ selectedOrder.time }}</div>
            </div>

            <table class="w-full text-[12px] border-collapse border border-black text-center mb-2" style="table-layout: fixed;">
              <thead>
              <tr>
                <th class="border border-black py-1.5 w-12">序号</th>
                <th class="border border-black py-1.5">商品型号</th>
                <th class="border border-black py-1.5">规格</th>
                <th class="border border-black py-1.5 w-20">数量(米)</th>
                <th class="border border-black py-1.5 w-24">单价</th>
                <th class="border border-black py-1.5 w-24">金额</th>
                <th class="border border-black py-1.5">备注</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(row, index) in displayTableData" :key="index" class="h-8">
                <td class="border border-black">{{ row.id ? index + 1 : '' }}</td>
                <td class="border border-black text-left px-2 truncate">{{ row.modelCode }}</td>
                <td class="border border-black">{{ row.spec }}</td>
                <td class="border border-black font-mono">{{ row.meters }}</td>
                <td class="border border-black font-mono">{{ row.price }}</td>
                <td class="border border-black font-mono">{{ row.total }}</td>
                <td class="border border-black text-left px-1 truncate">{{ row.remark }}</td>
              </tr>
              <tr class="h-8 font-bold">
                <td class="border border-black" colspan="3">合 计</td>
                <td class="border border-black font-mono">{{ totalMeters }}</td>
                <td class="border border-black"></td>
                <td class="border border-black font-mono">{{ totalAmount }}</td>
                <td class="border border-black"></td>
              </tr>
              </tbody>
            </table>

            <div class="text-[11px] mb-4">
              大写金额：<span class="font-bold border-b border-black inline-block w-64 pb-0.5">
                {{ convertCurrency(totalAmount) }}
              </span>
            </div>

            <div class="flex justify-between text-[13px] font-bold mt-auto pt-4">
              <div>制单人：{{ selectedOrder.operator }}</div>
              <div>库管员：________</div>
              <div>送货人：________</div>
              <div>收货人(签字)：________________</div>
            </div>

            <div class="text-[10px] text-gray-600 mt-2 text-center absolute bottom-2 w-full left-0">
              第一联：存根(白) &nbsp;&nbsp;&nbsp; 第二联：客户(红) &nbsp;&nbsp;&nbsp; 第三联：财务(蓝)
            </div>
          </template>
        </div>
      </div>
    </aside>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

defineOptions({ name: 'PendingPrintStation' });

// ================= 状态管理 =================
const isFetchingList = ref(false);
const isLoadingDetail = ref(false);

const pendingOrders = ref<any[]>([]);
const selectedOrder = ref<any>(null);
const tableData = ref<any[]>([]);

const ROWS_PER_PAGE = 5; // 控制单据每页行数

// ================= 生命周期 =================
onMounted(() => {
  fetchPendingList();
});

// ================= 获取待打印列表 =================
const fetchPendingList = async () => {
  isFetchingList.value = true;
  selectedOrder.value = null; // 刷新时清空右侧

  try {
    // 模拟后端请求：获取状态为“未打印”的出库单
    await new Promise(resolve => setTimeout(resolve, 500));

    pendingOrders.value = [
      { orderNo: 'CK20260405001', time: '2026-04-05 10:30', customerName: '杭州丝绸服装厂', itemCount: 3, operator: '张三' },
      { orderNo: 'CK20260405002', time: '2026-04-05 11:15', customerName: '广州越秀面料批发', itemCount: 1, operator: '李四' },
      { orderNo: 'CK20260405003', time: '2026-04-05 14:00', customerName: '江苏南通家纺城', itemCount: 5, operator: '张三' },
    ];
  } finally {
    isFetchingList.value = false;
  }
};

// ================= 选择订单并加载明细 =================
const selectOrder = async (order: any) => {
  if (selectedOrder.value?.orderNo === order.orderNo) return; // 重复点击拦截

  selectedOrder.value = order;
  isLoadingDetail.value = true;
  tableData.value = [];

  try {
    // 模拟后端请求：根据 orderNo 获取出库明细
    await new Promise(resolve => setTimeout(resolve, 400));

    // 针对不同的单号生成对应的模拟数据
    if (order.orderNo === 'CK20260405001') {
      tableData.value = [
        { id: 1, modelCode: 'T800-210A(白)', spec: '150cm', meters: '200', price: '12.50', total: '2500.00', remark: '' },
        { id: 2, modelCode: 'C300-纯棉', spec: '160cm', meters: '150', price: '18.00', total: '2700.00', remark: '' },
        { id: 3, modelCode: 'NY-防水尼龙', spec: '145cm', meters: '85', price: '20.00', total: '1700.00', remark: '急单' }
      ];
    } else {
      tableData.value = [
        { id: 1, modelCode: '默认测试面料', spec: '100cm', meters: '100', price: '10.00', total: '1000.00', remark: '' }
      ];
    }
  } finally {
    isLoadingDetail.value = false;
  }
};

// ================= 计算属性：处理数据 =================
const displayTableData = computed(() => {
  const rows = [...tableData.value];
  while (rows.length < ROWS_PER_PAGE) {
    rows.push({ id: '', modelCode: '', spec: '', meters: '', price: '', total: '', remark: '' });
  }
  return rows.slice(0, ROWS_PER_PAGE);
});

const totalMeters = computed(() => {
  if (tableData.value.length === 0) return '';
  return tableData.value.reduce((sum, item) => sum + (Number(item.meters) || 0), 0).toFixed(2);
});

const totalAmount = computed(() => {
  if (tableData.value.length === 0) return '';
  return tableData.value.reduce((sum, item) => sum + (Number(item.total) || 0), 0).toFixed(2);
});

const convertCurrency = (money: string | number) => {
  if (!money || Number(money) === 0) return '';
  return '模拟大写金额：' + money + '元整'; // 真实项目需引入转换工具函数
};

// ================= 打印与状态流转 =================
const executePrint = () => {
  if (!selectedOrder.value) return;

  const printContent = document.getElementById('print-paper-area')?.outerHTML;
  if (!printContent) return;

  const originalContent = document.body.innerHTML;

  // 1. 构建打印 DOM 并调用浏览器打印
  document.body.innerHTML = `<div class="print-container">${printContent}</div>`;
  window.print();

  // 2. 打印完毕后恢复页面 DOM (在部分浏览器里，window.print 是阻塞的，所以这里可以直接恢复)
  document.body.innerHTML = originalContent;

  // 必须重新挂载 Vue 实例，这里用重新加载页面简化处理（真实场景可以使用 iframe 隐藏打印避免页面刷新）
  // 为了用户体验，我们询问是否打印成功，如果成功，则从左侧列表中移除
  setTimeout(() => {
    window.location.reload();
    /* 真实工程最佳实践（无刷新）：
      ElMessageBox.confirm('打印是否成功且清晰？', '打印确认', {
        confirmButtonText: '打印成功，移除队列',
        cancelButtonText: '重新打印',
        type: 'warning'
      }).then(() => {
        // 请求后端更新状态： markPrinted(selectedOrder.value.orderNo)
        pendingOrders.value = pendingOrders.value.filter(item => item.orderNo !== selectedOrder.value.orderNo);
        selectedOrder.value = null;
        ElMessage.success('已标记为打印完成');
      }).catch(() => {});
    */
  }, 100);
};

const handleSkipPrint = () => {
  ElMessageBox.confirm(`确认将单据 ${selectedOrder.value.orderNo} 标记为已打印吗？它将从当前列表中移除。`, '跳过打印', {
    confirmButtonText: '确认标记',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    // 模拟向后端发送标记请求
    pendingOrders.value = pendingOrders.value.filter(item => item.orderNo !== selectedOrder.value.orderNo);
    selectedOrder.value = null;
    ElMessage.success('操作成功');
  }).catch(() => {});
};
</script>

<style scoped>
/* 滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background-color: #cbd5e1; /* Tailwind slate-300 */
}

/* 打印指令区 (严格保持这部分逻辑) */
@media print {
  @page {
    size: 241mm 140mm;
    margin: 0;
  }

  body {
    -webkit-print-color-adjust: exact;
    color: #000;
    background: #fff;
  }

  .print-container {
    display: block;
    width: 241mm;
    height: 140mm;
    page-break-after: always;
  }

  /* 打印时去除原本由于 flex 和 margin 导致的偏移 */
  #print-paper-area {
    box-shadow: none !important;
    border: none !important;
    margin: 0 !important;
    position: absolute !important;
    left: 0 !important;
    top: 0 !important;
  }

  table, th, td {
    border-color: #000 !important;
  }
}
</style>
