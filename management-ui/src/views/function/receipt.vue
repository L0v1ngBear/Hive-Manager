<template>
  <div class="max-w-7xl mx-auto h-[calc(100vh-8rem)] flex flex-col md:flex-row gap-6 pb-6">

    <section class="flex-1 bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 flex flex-col overflow-hidden">
      <header class="p-6 border-b border-outline-variant/20 flex justify-between items-center bg-surface">
        <div>
          <h2 class="text-lg font-black text-on-surface">三联单/票据 模板配置</h2>
          <p class="text-xs text-on-surface-variant mt-1">ESC/POS 流式排版，适用于出库单、生产领料单等</p>
        </div>
        <el-button type="primary" icon="Check" @click="handleSave">保存模板</el-button>
      </header>

      <div class="flex-1 overflow-y-auto p-6 space-y-8">

        <div class="space-y-4">
          <h3 class="text-sm font-bold text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-base">title</span> 表头配置 (Header)
          </h3>
          <div class="grid grid-cols-2 gap-4 p-4 bg-surface-container-low rounded-xl">
            <div>
              <label class="block text-xs font-medium text-on-surface-variant mb-1">单据标题</label>
              <input v-model="config.header.title" type="text" class="w-full bg-surface-container-lowest border-none rounded-lg text-sm px-3 py-2 focus:ring-2 focus:ring-primary/20" />
            </div>
            <div>
              <label class="block text-xs font-medium text-on-surface-variant mb-1">对齐方式</label>
              <el-radio-group v-model="config.header.align" size="small">
                <el-radio-button label="left">居左</el-radio-button>
                <el-radio-button label="center">居中</el-radio-button>
              </el-radio-group>
            </div>
            <div>
              <label class="block text-xs font-medium text-on-surface-variant mb-1">字体大小</label>
              <el-radio-group v-model="config.header.size" size="small">
                <el-radio-button label="normal">常规</el-radio-button>
                <el-radio-button label="large">放大加粗</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </div>

        <div class="space-y-4">
          <div class="flex justify-between items-center">
            <h3 class="text-sm font-bold text-primary flex items-center gap-2">
              <span class="material-symbols-outlined text-base">view_column</span> 表格列配置 (Table)
            </h3>
            <el-button size="small" plain type="primary" icon="Plus" @click="addColumn">添加列</el-button>
          </div>

          <div class="space-y-2">
            <div v-for="(col, index) in config.table.columns" :key="index" class="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl group transition-all">
              <div class="flex-1">
                <input v-model="col.title" placeholder="列名 (如: 型号)" class="w-full bg-surface-container-lowest border-none rounded text-sm px-2 py-1" />
              </div>
              <div class="flex-1">
                <el-select v-model="col.field" placeholder="绑定字段" size="small" class="w-full">
                  <el-option label="面料型号 (modelCode)" value="modelCode" />
                  <el-option label="规格 (spec)" value="spec" />
                  <el-option label="米数 (meters)" value="meters" />
                  <el-option label="重量 (weight)" value="weight" />
                  <el-option label="备注 (remark)" value="remark" />
                </el-select>
              </div>
              <div class="w-24">
                <el-input-number v-model="col.widthRatio" :min="1" :max="10" size="small" controls-position="right" placeholder="宽度比例" class="!w-full" />
              </div>
              <button @click="removeColumn(index)" class="text-on-surface-variant hover:text-error transition-colors p-1">
                <span class="material-symbols-outlined text-lg">delete</span>
              </button>
            </div>
          </div>
        </div>

        <div class="space-y-4">
          <h3 class="text-sm font-bold text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-base">horizontal_rule</span> 表尾配置 (Footer)
          </h3>
          <div class="grid grid-cols-2 gap-4 p-4 bg-surface-container-low rounded-xl">
            <div>
              <label class="block text-xs font-medium text-on-surface-variant mb-1">签字文本</label>
              <input v-model="config.footer.signText" type="text" class="w-full bg-surface-container-lowest border-none rounded-lg text-sm px-3 py-2 focus:ring-2 focus:ring-primary/20" />
            </div>
            <div>
              <label class="block text-xs font-medium text-on-surface-variant mb-1">底部寄语</label>
              <input v-model="config.footer.memo" type="text" placeholder="如: 感谢您的信任" class="w-full bg-surface-container-lowest border-none rounded-lg text-sm px-3 py-2 focus:ring-2 focus:ring-primary/20" />
            </div>
          </div>
        </div>

      </div>
    </section>

    <aside class="w-full md:w-96 flex flex-col gap-4">
      <div class="bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 p-4 flex-1 flex flex-col">
        <h3 class="text-sm font-bold text-on-surface mb-4">打印机效果预览</h3>

        <div class="flex-1 bg-[#fffdf9] border border-gray-200 shadow-inner p-6 font-mono text-gray-800 text-[13px] leading-relaxed overflow-y-auto">

          <div class="mb-4" :class="{ 'text-center': config.header.align === 'center', 'text-left': config.header.align === 'left' }">
            <div :class="config.header.size === 'large' ? 'text-lg font-bold' : 'text-sm'">
              {{ config.header.title || '（未设置标题）' }}
            </div>
            <div class="text-[11px] text-gray-500 mt-1">单号：CK202604050001</div>
            <div class="text-[11px] text-gray-500">时间：2026-04-05 14:30</div>
          </div>

          <div class="border-b border-dashed border-gray-400 mb-2"></div>

          <div class="flex font-bold pb-2">
            <div v-for="(col, i) in config.table.columns" :key="'h'+i"
                 :style="{ flex: col.widthRatio }" class="pr-2 truncate">
              {{ col.title }}
            </div>
          </div>

          <div class="border-b border-dashed border-gray-400 mb-2"></div>

          <div class="space-y-2">
            <div v-for="row in mockData" :key="row.id" class="flex">
              <div v-for="(col, i) in config.table.columns" :key="'d'+i"
                   :style="{ flex: col.widthRatio }" class="pr-2 break-all">
                {{ row[col.field] || '-' }}
              </div>
            </div>
          </div>

          <div class="border-b border-dashed border-gray-400 my-4"></div>

          <div class="space-y-6">
            <div v-if="config.footer.memo" class="text-center text-[11px]">{{ config.footer.memo }}</div>
            <div class="flex justify-between mt-8">
              <div>操作员：管理员</div>
              <div>{{ config.footer.signText }} _________</div>
            </div>
          </div>

        </div>
      </div>
    </aside>

  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

defineOptions({ name: 'ReceiptDesigner' });

// --- 核心 JSON 结构 ---
const config = reactive({
  type: 'receipt',
  header: {
    title: '星火工坊-出库单',
    align: 'center',
    size: 'large'
  },
  table: {
    columns: [
      { field: 'modelCode', title: '型号', widthRatio: 4 },
      { field: 'spec', title: '规格', widthRatio: 3 },
      { field: 'meters', title: '米数', widthRatio: 2 }
    ]
  },
  footer: {
    signText: '客户签字：',
    align: 'left',
    memo: '白联:存根 红联:客户 蓝联:财务'
  }
});

// 用于右侧效果预览的假数据
const mockData = ref([
  { id: 1, modelCode: 'T800-210A', spec: '150cm', meters: '200.5', weight: '280' },
  { id: 2, modelCode: 'C300-棉麻', spec: '160cm', meters: '150.0', weight: '320' },
  { id: 3, modelCode: 'Nylon-防水', spec: '145cm', meters: '85.0', weight: '180' }
]);

// --- 操作方法 ---
const addColumn = () => {
  if (config.table.columns.length >= 5) {
    ElMessage.warning('小票宽度有限，建议最多设置5列');
    return;
  }
  config.table.columns.push({ field: 'remark', title: '新列', widthRatio: 2 });
};

const removeColumn = (index: number) => {
  if (config.table.columns.length <= 1) {
    ElMessage.warning('至少需要保留一列');
    return;
  }
  config.table.columns.splice(index, 1);
};

const handleSave = () => {
  // 这里将得到最干净、最标准的 JSON 描述文件，发送给后端保存即可
  const finalJson = JSON.parse(JSON.stringify(config));
  console.log('即将保存的小票模板 JSON:', finalJson);
  ElMessage.success('JSON 生成成功，请查看控制台');

  // 实际业务：
  // request.post('/api/print/template', { type: 'esc', content: JSON.stringify(finalJson) })
};
</script>

<style scoped>
/* 隐藏原生 input 的焦点黑框，使用 Tailwind 的 ring */
input:focus {
  outline: none;
}
</style>
