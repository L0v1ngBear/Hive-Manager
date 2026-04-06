<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 overflow-x-hidden">
    <div class="max-w-7xl mx-auto space-y-8">

      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
        <div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">角色管理</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-lg">
            管理 <span class="font-bold text-primary">星火服装厂</span> 的组织权限和系统访问层级。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="bg-surface-container-high text-on-surface flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-sm hover:bg-surface-container-highest transition-colors active:scale-95">
            <span class="material-symbols-outlined text-[20px]">filter_list</span>筛选
          </button>
          <button class="bg-primary text-on-primary flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-md hover:shadow-lg hover:opacity-90 transition-all active:scale-95">
            <span class="material-symbols-outlined text-[20px]">add</span>新增角色
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-8">
        <div class="col-span-1 lg:col-span-8 bg-surface-container-low rounded-xl p-6 md:p-8 flex flex-col justify-between relative overflow-hidden group">
          <div class="relative z-10">
            <h3 class="text-sm font-bold text-on-surface-variant mb-2">活跃角色总数</h3>
            <div class="flex items-baseline gap-4">
              <span class="text-5xl md:text-6xl font-black text-primary tracking-tighter">{{ totalRoles }}</span>
              <span v-if="newRolesCount > 0" class="text-xs md:text-sm font-bold text-on-tertiary-fixed-variant bg-tertiary-fixed px-2.5 py-1 rounded-md shadow-sm">
                本月新增 {{ newRolesCount }} 个
              </span>
            </div>
          </div>
          <div class="mt-8 flex gap-2" aria-hidden="true">
            <div class="w-1/4 h-1.5 bg-primary rounded-full"></div>
            <div class="w-1/2 h-1.5 bg-secondary-container rounded-full"></div>
            <div class="w-1/4 h-1.5 bg-outline-variant rounded-full"></div>
          </div>
          <div class="absolute -right-4 -bottom-10 opacity-5 group-hover:opacity-10 group-hover:scale-110 transition-all duration-500 pointer-events-none">
            <span class="material-symbols-outlined text-[10rem] md:text-[12rem]" style="font-variation-settings: 'FILL' 1;">verified_user</span>
          </div>
        </div>

        <div class="col-span-1 lg:col-span-4 bg-primary text-on-primary rounded-xl p-6 md:p-8 relative overflow-hidden">
          <h3 class="text-xs md:text-sm font-bold opacity-80 mb-4 uppercase tracking-widest flex items-center gap-2">
            <span class="material-symbols-outlined text-[18px]">history</span> 近期动态
          </h3>
          <div class="space-y-4 relative z-10">
            <div v-for="(activity, index) in recentActivities" :key="index"
                 :class="['flex items-start gap-3 border-l-2 pl-3 transition-colors', activity.isRecent ? 'border-primary-fixed-dim' : 'border-primary-fixed-dim/30 hover:border-primary-fixed-dim/60']">
              <div :class="['text-xs font-bold w-16 shrink-0 mt-0.5', activity.isRecent ? 'text-primary-fixed-dim' : 'text-primary-fixed-dim/60']">
                {{ activity.time }}
              </div>
              <div :class="['text-sm leading-snug', activity.isRecent ? 'text-white' : 'text-white/70']">
                {{ activity.desc }}
              </div>
            </div>
          </div>
          <div class="absolute right-0 top-0 w-32 h-full bg-gradient-to-l from-white/10 to-transparent pointer-events-none"></div>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border border-outline-variant/20">
        <div class="p-4 md:p-6 flex flex-col sm:flex-row sm:items-center justify-between border-b border-outline-variant/20 gap-4">
          <div>
            <h3 class="font-black text-primary text-lg">身份权限注册表</h3>
            <p class="text-xs text-on-surface-variant font-medium mt-1">展示当前系统的访问层级配置</p>
          </div>
          <div class="relative w-full sm:w-auto">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50 text-[18px]">search</span>
            <input type="text" placeholder="搜索角色名称..." class="w-full sm:w-64 pl-9 pr-4 py-2 bg-surface-container-low border-none rounded-lg text-sm focus:ring-2 focus:ring-primary transition-shadow" />
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse min-w-[800px]">
            <thead class="bg-surface-container-low/50">
            <tr>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">角色标识</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">分配人数</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">权限范围</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">创建日期</th>
              <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="role in roles" :key="role.id"
                :class="['group transition-colors', role.status === 'PENDING' ? 'bg-tertiary-fixed/10 hover:bg-tertiary-fixed/20 border-l-4 border-l-tertiary' : 'hover:bg-surface-container-high/30 border-l-4 border-l-transparent']">

              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div :class="['w-10 h-10 rounded-lg flex items-center justify-center shrink-0', getIconTheme(role.type).bg, getIconTheme(role.type).text]">
                    <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' 1;">{{ role.icon }}</span>
                  </div>
                  <div class="min-w-0">
                    <div class="font-bold text-primary leading-tight flex items-center gap-2 truncate">
                      {{ role.name }}
                      <span v-if="role.isNew" class="shrink-0 text-[10px] bg-tertiary-fixed text-on-tertiary-fixed-variant px-1.5 py-px rounded font-black uppercase">新</span>
                    </div>
                    <div class="text-xs text-on-surface-variant mt-1 truncate max-w-[250px]" :title="role.desc">{{ role.desc }}</div>
                  </div>
                </div>
              </td>

              <td :class="['px-6 py-4 text-sm font-bold', role.userCount === 0 ? 'text-on-surface-variant/50' : 'text-primary']">
                {{ role.userCount === 0 ? '未分配' : `${role.userCount} 人` }}
              </td>

              <td class="px-6 py-4">
                  <span :class="['text-[10px] font-bold px-2.5 py-1 rounded-md uppercase tracking-wide', getScopeTheme(role.scopeType)]">
                    {{ role.scopeName }}
                  </span>
              </td>

              <td class="px-6 py-4 text-sm text-on-surface-variant font-medium">
                {{ role.createdAt }}
              </td>

              <td class="px-6 py-4 text-right">
                <div v-if="role.status === 'PENDING'" class="flex justify-end gap-2 items-center">
                  <button class="bg-primary text-white px-4 py-1.5 rounded-lg text-sm font-bold shadow-md hover:bg-primary/90 transition-colors active:scale-95">完成配置</button>
                  <button class="p-1.5 hover:bg-error-container hover:text-error rounded-lg transition-colors text-on-surface-variant" aria-label="删除角色" title="删除">
                    <span class="material-symbols-outlined text-[20px]">delete</span>
                  </button>
                </div>
                <button v-else class="text-primary bg-primary/5 hover:bg-primary/10 px-4 py-1.5 rounded-lg text-sm font-bold transition-colors active:scale-95">
                  编辑权限
                </button>
              </td>

            </tr>

            <tr v-if="roles.length === 0">
              <td colspan="5" class="px-6 py-12 text-center text-on-surface-variant">
                <span class="material-symbols-outlined text-4xl mb-2 opacity-50">search_off</span>
                <p class="text-sm font-medium">暂无符合条件的角色数据</p>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 md:p-6 bg-surface-container-lowest flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-on-surface-variant font-medium border-t border-outline-variant/20">
          <span>当前显示 {{ roles.length }} 个核心角色，共 {{ totalRoles }} 个</span>
          <div class="flex gap-2">
            <button class="flex items-center justify-center w-8 h-8 rounded-lg hover:bg-surface-container-high transition-colors disabled:opacity-40 disabled:cursor-not-allowed" disabled aria-label="上一页">
              <span class="material-symbols-outlined text-[20px]">chevron_left</span>
            </button>
            <button class="flex items-center justify-center w-8 h-8 rounded-lg bg-primary text-white font-bold shadow-sm">1</button>
            <button class="flex items-center justify-center w-8 h-8 rounded-lg hover:bg-surface-container-high transition-colors text-primary font-bold">2</button>
            <button class="flex items-center justify-center w-8 h-8 rounded-lg hover:bg-surface-container-high transition-colors" aria-label="下一页">
              <span class="material-symbols-outlined text-[20px]">chevron_right</span>
            </button>
          </div>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// --- 数据模型 (模拟纯净的后端返回格式) ---
const recentActivities = ref([
  { time: '上午 10:45', desc: '创建了新角色 "仓库管理员"', isRecent: true },
  { time: '上午 09:12', desc: '更新了 "销售经理" 的数据权限', isRecent: false },
  { time: '昨天', desc: '为 "车间工人" 增加了系统查看权限', isRecent: false }
])

const roles = ref([
  {
    id: 1,
    name: '超级管理员',
    desc: '系统最高权限，可管理所有模块与配置',
    icon: 'security',
    type: 'ADMIN',
    userCount: 2,
    scopeName: '全系统',
    scopeType: 'GLOBAL',
    createdAt: '2024-01-12',
    isNew: false,
    status: 'ACTIVE'
  },
  {
    id: 2,
    name: '销售经理',
    desc: '客户关系管理与营收数据监督',
    icon: 'sell',
    type: 'STANDARD',
    userCount: 5,
    scopeName: '销售部门',
    scopeType: 'DEPT',
    createdAt: '2024-02-05',
    isNew: false,
    status: 'ACTIVE'
  },
  {
    id: 3,
    name: '仓库管理员',
    desc: '物流调度、出入库管理与库存控制',
    icon: 'inventory',
    type: 'SPECIAL',
    userCount: 0,
    scopeName: '库存中心',
    scopeType: 'DEPT',
    createdAt: '2024-04-06', // 模拟今天
    isNew: true,
    status: 'PENDING' // 对应"待配置"状态
  },
  {
    id: 4,
    name: '生产主管',
    desc: '工厂车间日常生产运营监控',
    icon: 'precision_manufacturing',
    type: 'STANDARD',
    userCount: 3,
    scopeName: '生产制造',
    scopeType: 'DEPT',
    createdAt: '2024-01-15',
    isNew: false,
    status: 'ACTIVE'
  }
])

// --- 逻辑与计算 (Computed & Methods) ---

// 自动计算统计数据
const totalRoles = computed(() => 14) // 实际业务中应为后端返回的 total
const newRolesCount = computed(() => roles.value.filter(r => r.isNew).length)

// 视图层样式映射函数 (将业务类型转换为UI类名)
const getIconTheme = (type) => {
  const themes = {
    'ADMIN': {bg: 'bg-primary-container', text: 'text-primary-fixed'},
    'SPECIAL': {bg: 'bg-tertiary', text: 'text-tertiary-fixed'},
    'STANDARD': {bg: 'bg-surface-container-highest', text: 'text-primary'}
  }
  return themes[type] || themes['STANDARD']
}

const getScopeTheme = (scopeType) => {
  const themes = {
    'GLOBAL': 'bg-secondary-container text-on-secondary-container',
    'DEPT': 'bg-outline-variant/20 text-on-surface-variant'
  }
  return themes[scopeType] || themes['DEPT']
}
</script>
