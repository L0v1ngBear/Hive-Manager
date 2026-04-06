<template>
  <div class="h-[calc(100vh-8rem)] flex flex-col bg-surface-container-low -m-4 md:-m-8">

    <header class="h-16 bg-surface-container-lowest flex items-center justify-between px-6 shrink-0 relative z-10 shadow-sm ring-1 ring-outline-variant/10">
      <div class="flex items-center gap-4">
        <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white shadow-sm">
          <span class="material-symbols-outlined text-sm">terminal</span>
        </div>
        <div>
          <input v-model="templateName" class="text-lg font-bold text-on-surface bg-transparent border-none focus:ring-0 p-0 hover:bg-surface-container-highest rounded px-2 transition-colors cursor-text" placeholder="输入模板名称" />
          <p class="text-xs text-on-surface-variant px-2"></p>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <input type="file" ref="fileInputRef" accept=".prn,.txt" class="hidden" @change="handleFileUpload" />

        <button @click="triggerUpload" class="px-4 py-2 rounded-xl text-sm font-bold text-primary bg-primary-container hover:bg-primary-container/80 transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">upload_file</span>
          上传 Bartender 导出文件 (.prn)
        </button>

        <button @click="saveTemplate" class="bg-primary text-white px-5 py-2 rounded-xl text-sm font-bold shadow-sm hover:bg-primary/90 transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">save</span>
          保存系统模板
        </button>
      </div>
    </header>

    <main class="flex-1 flex overflow-hidden p-6 gap-6">

      <section class="flex-[2] flex flex-col bg-[#1e1e1e] rounded-2xl shadow-inner overflow-hidden ring-1 ring-gray-800">
        <div class="h-10 bg-[#2d2d2d] flex items-center px-4 border-b border-[#404040]">
          <span class="text-xs font-mono text-gray-400 flex items-center gap-2">
            <span class="material-symbols-outlined text-sm">code</span>
            TEMPLATE_SOURCE_CODE
          </span>
          <div class="ml-auto flex gap-2">
            <span class="w-3 h-3 rounded-full bg-error"></span>
            <span class="w-3 h-3 rounded-full bg-orange-400"></span>
            <span class="w-3 h-3 rounded-full bg-green-400"></span>
          </div>
        </div>

        <div class="flex-1 relative">
          <textarea
            v-model="templateCode"
            spellcheck="false"
            placeholder="请在此粘贴蓝牙打印机底层指令（如 TSPL），或点击右上角上传 Bartender 生成的 .prn 文件...&#10;&#10;使用示例：&#10;SIZE 70 mm, 50 mm&#10;GAP 2 mm, 0 mm&#10;CLS&#10;TEXT 20,30,&#34;TSS24.BF2&#34;,0,1,1,&#34;型号: ${modelCode}&#34;&#10;PRINT 1,1"
            class="absolute inset-0 w-full h-full bg-transparent text-[#4ade80] font-mono text-sm p-4 border-none focus:ring-0 resize-none leading-relaxed tracking-wide placeholder:text-gray-600"
          ></textarea>
        </div>
      </section>

      <aside class="flex-[1] bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 flex flex-col">
        <div class="p-5 border-b border-outline-variant/20">
          <h3 class="text-sm font-bold text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-base">data_object</span> 动态变量解析
          </h3>
          <p class="text-xs text-on-surface-variant mt-1">系统会自动提取代码中形如 <code class="bg-surface-container-high px-1 rounded text-primary">${xxx}</code> 的占位符。</p>
        </div>

        <div class="flex-1 overflow-y-auto p-5 space-y-4">
          <template v-if="detectedVariables.length > 0">
            <div v-for="variable in detectedVariables" :key="variable" class="p-4 bg-surface-container-low rounded-xl border-l-4 border-primary group">
              <div class="flex justify-between items-center mb-2">
                <span class="font-mono text-sm font-bold text-on-surface">${{ variable }}</span>
                <span class="text-[11px] px-2 py-0.5 rounded bg-primary-container text-on-primary-container">已捕获</span>
              </div>
              <div class="mt-2">
                <el-select size="small" placeholder="请选择要绑定的业务字段" class="w-full">
                  <el-option label="系统流水号 (id)" value="id" />
                  <el-option label="面料型号 (modelCode)" value="modelCode" />
                  <el-option label="批次号/条码 (batchNo)" value="batchNo" />
                  <el-option label="入库米数 (meters)" value="meters" />
                  <el-option label="当前操作人 (operator)" value="operator" />
                </el-select>
              </div>
            </div>
          </template>

          <div v-else class="h-full flex flex-col items-center justify-center text-on-surface-variant/40">
            <span class="material-symbols-outlined text-4xl mb-2">manage_search</span>
            <p class="text-sm">未检测到任何动态变量</p>
            <p class="text-xs mt-1 text-center">请在左侧代码中将需要动态变化的数据<br/>改为 ${变量名} 的格式</p>
          </div>
        </div>
      </aside>

    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';

defineOptions({ name: 'BartenderTemplate' });

// --- 状态 ---
const templateName = ref('面料入库标签(Bartender版)');
const templateCode = ref('');
const fileInputRef = ref<HTMLInputElement | null>(null);

// --- 计算属性：正则提取代码中的所有 ${xxx} 变量 ---
const detectedVariables = computed(() => {
  if (!templateCode.value) return [];
  // 正则匹配 ${...} 格式
  const regex = /\$\{([^}]+)\}/g;
  let match;
  const vars = new Set<string>(); // 使用 Set 自动去重

  while ((match = regex.exec(templateCode.value)) !== null) {
    vars.add(match[1]);
  }

  return Array.from(vars);
});

// --- 操作方法 ---
const triggerUpload = () => {
  fileInputRef.value?.click();
};

const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file) return;

  // 使用 FileReader 读取文本内容
  const reader = new FileReader();
  reader.onload = (e) => {
    const content = e.target?.result as string;
    templateCode.value = content;
    ElMessage.success(`成功读取文件: ${file.name}`);
    // 清空 input，以便下次可以重复上传同一个文件
    target.value = '';
  };

  reader.onerror = () => {
    ElMessage.error('文件读取失败');
  };

  reader.readAsText(file); // 按照 UTF-8 文本读取 .prn 或 .txt
};

const saveTemplate = () => {
  if (!templateCode.value.trim()) {
    ElMessage.warning('模板代码不能为空');
    return;
  }

  const finalPayload = {
    name: templateName.value,
    type: 'raw_command',
    content: templateCode.value, // 原封不动保存这串带有 ${xxx} 的字符串
    variables: detectedVariables.value
  };

  console.log('准备保存到后端的模板数据:', JSON.stringify(finalPayload, null, 2));
  ElMessage.success('模板保存成功，小程序端已可直接拉取该代码！');
};
</script>

<style scoped>
/* 隐藏原生 input 聚焦轮廓 */
textarea:focus, input:focus {
  outline: none;
}
</style>
