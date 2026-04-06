<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8">

    <div class="max-w-7xl mx-auto space-y-8">

      <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 class="text-3xl font-black text-primary tracking-tight">员工名册</h2>
          <p class="text-on-surface-variant mt-1">管理所有厂区的劳动力运营和生产人员编制。</p>
        </div>
        <div class="flex gap-3">
          <button class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm">
            <span class="material-symbols-outlined text-[20px]">upload_file</span>导出列表
          </button>
          <button class="px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded-lg flex items-center gap-2 hover:bg-surface-variant transition-colors text-sm">
            <span class="material-symbols-outlined text-[20px]">edit_square</span>批量编辑
          </button>
          <button class="px-5 py-2 bg-primary text-white font-bold rounded-lg flex items-center gap-2 shadow-md hover:bg-primary/90 transition-all text-sm">
            <span class="material-symbols-outlined text-[20px]">person_add</span>添加员工
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-primary-container p-6 rounded-xl relative overflow-hidden group">
          <div class="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-[80px]">groups</span>
          </div>
          <p class="text-on-primary-container text-sm font-bold uppercase tracking-widest">员工总数</p>
          <h3 class="text-4xl font-black text-white mt-2">1,248</h3>
          <p class="text-on-primary-container text-xs mt-3 flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">trending_up</span> 较上月增长 12%
          </p>
        </div>

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary">
          <p class="text-on-surface-variant text-sm font-bold uppercase tracking-widest">今日出勤率</p>
          <div class="flex items-end justify-between mt-2">
            <h3 class="text-4xl font-black text-primary">94.2%</h3>
            <div class="h-10 w-20 bg-surface-container-high rounded flex items-end p-1 gap-0.5">
              <div class="w-full bg-primary/20 h-[60%] rounded-t-sm"></div>
              <div class="w-full bg-primary/20 h-[70%] rounded-t-sm"></div>
              <div class="w-full bg-primary/20 h-[85%] rounded-t-sm"></div>
              <div class="w-full bg-primary h-full rounded-t-sm"></div>
            </div>
          </div>
          <p class="text-on-surface-variant text-xs mt-3 uppercase font-bold tracking-tighter">当前班次: 早班 (A1)</p>
        </div>

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <p class="text-on-surface-variant text-sm font-bold uppercase tracking-widest">部门编制</p>
          <h3 class="text-4xl font-black text-primary mt-2">12</h3>
          <div class="flex -space-x-2 mt-4 overflow-hidden">
            <span class="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center text-[10px] font-bold border-2 border-white">产</span>
            <span class="w-6 h-6 rounded-full bg-amber-100 flex items-center justify-center text-[10px] font-bold border-2 border-white">销</span>
            <span class="w-6 h-6 rounded-full bg-emerald-100 flex items-center justify-center text-[10px] font-bold border-2 border-white">政</span>
            <span class="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center text-[10px] font-bold border-2 border-white">+9</span>
          </div>
        </div>

        <div class="bg-white/70 backdrop-blur-md p-6 rounded-xl shadow-sm border border-orange-200">
          <p class="text-orange-900 text-sm font-bold uppercase tracking-widest">待入职</p>
          <h3 class="text-4xl font-black text-orange-700 mt-2">08</h3>
          <p class="text-orange-900/70 text-xs mt-3 flex items-center gap-1 font-medium">
            <span class="material-symbols-outlined text-xs">schedule</span> 下批次周一办理
          </p>
        </div>
      </div>

      <div class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden flex flex-col border border-surface-variant/50">

        <div class="p-4 bg-surface-container-low flex flex-wrap items-center gap-4 border-b border-surface-variant/50">
          <div class="flex-1 min-w-[300px] relative">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant">search</span>
            <input type="text" class="w-full pl-10 pr-4 py-2 bg-white border-none ring-1 ring-outline-variant/30 focus:ring-2 focus:ring-primary rounded-lg text-sm transition-all" placeholder="按姓名、工号或职位搜索..." />
          </div>
          <div class="flex items-center gap-3">
            <select class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[120px] font-medium appearance-none">
              <option>所有部门</option>
              <option>生产部</option>
              <option>销售部</option>
              <option>行政部</option>
              <option>质检部</option>
            </select>
            <select class="pl-3 pr-8 py-2 bg-white border-none ring-1 ring-outline-variant/30 rounded-lg text-sm focus:ring-2 focus:ring-primary min-w-[120px] font-medium appearance-none">
              <option>状态: 全部</option>
              <option>在职</option>
              <option>请假中</option>
              <option>已离职</option>
            </select>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
            <tr class="bg-surface-container/30 text-on-surface-variant border-b border-surface-variant/50">
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">员工</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">工号</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">部门</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">职位</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">联系方式</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">状态</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider whitespace-nowrap">入职时间</th>
              <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-right whitespace-nowrap">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-surface-variant/50">
            <tr v-for="emp in employees" :key="emp.id" class="hover:bg-surface-container-high/50 transition-colors group">
              <td class="px-6 py-3">
                <div class="flex items-center gap-3">
                  <img :src="emp.avatar" class="w-9 h-9 rounded-full object-cover bg-slate-200 shrink-0" />
                  <div>
                    <p class="font-bold text-primary leading-none whitespace-nowrap">{{ emp.name }}</p>
                    <p class="text-[10px] text-on-surface-variant uppercase mt-1">{{ emp.type }}</p>
                  </div>
                </div>
              </td>
              <td class="px-6 py-3 font-mono text-sm text-secondary whitespace-nowrap">{{ emp.empId }}</td>
              <td class="px-6 py-3 whitespace-nowrap">
                  <span :class="`px-2 py-0.5 rounded text-[11px] font-bold border ${emp.deptColor}`">
                    {{ emp.dept }}
                  </span>
              </td>
              <td class="px-6 py-3 text-sm font-bold text-primary whitespace-nowrap">{{ emp.role }}</td>
              <td class="px-6 py-3 whitespace-nowrap">
                <div class="text-xs font-medium text-primary">{{ emp.email }}</div>
                <div class="text-xs text-on-surface-variant mt-0.5">{{ emp.phone }}</div>
              </td>
              <td class="px-6 py-3 whitespace-nowrap">
                <div :class="`flex items-center gap-1.5 font-bold text-xs ${emp.statusColor}`">
                  <span :class="`w-1.5 h-1.5 rounded-full ${emp.dotColor}`"></span>
                  {{ emp.status }}
                </div>
              </td>
              <td class="px-6 py-3 text-sm text-on-surface-variant font-medium whitespace-nowrap">{{ emp.joinDate }}</td>
              <td class="px-6 py-3 text-right">
                <div class="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button class="p-1.5 hover:bg-white rounded-md text-primary" title="查看"><span class="material-symbols-outlined text-[18px]">visibility</span></button>
                  <button class="p-1.5 hover:bg-white rounded-md text-primary" title="权限"><span class="material-symbols-outlined text-[18px]">lock_open</span></button>
                  <button class="p-1.5 hover:bg-white rounded-md text-primary" title="更多"><span class="material-symbols-outlined text-[18px]">more_vert</span></button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="p-4 bg-surface-container/20 flex flex-wrap items-center justify-between gap-4 text-sm text-on-surface-variant border-t border-surface-variant/50">
          <div class="flex items-center gap-2">
            <span>每页显示</span>
            <select class="bg-white border border-surface-variant/50 rounded-md py-1 px-2 text-xs focus:outline-none">
              <option>10</option>
              <option>25</option>
              <option>50</option>
            </select>
            <span>条记录</span>
          </div>
          <div class="flex items-center gap-4">
            <p class="hidden sm:block">显示第 1 到 10 条，共 1,248 条记录</p>
            <div class="flex gap-1">
              <button class="w-8 h-8 flex items-center justify-center rounded bg-white border border-surface-variant/50 disabled:opacity-50 hover:bg-slate-50 transition-colors">
                <span class="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <button class="w-8 h-8 flex items-center justify-center rounded bg-primary text-white font-bold">1</button>
              <button class="w-8 h-8 flex items-center justify-center rounded bg-white border border-surface-variant/50 hover:bg-slate-50 transition-colors">2</button>
              <button class="w-8 h-8 flex items-center justify-center rounded bg-white border border-surface-variant/50 hover:bg-slate-50 transition-colors">3</button>
              <button class="w-8 h-8 flex items-center justify-center rounded bg-white border border-surface-variant/50 hover:bg-slate-50 transition-colors">
                <span class="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 模拟员工数据保留不变
const employees = ref([
  {
    id: 1,
    name: '马库斯 (Marcus)',
    type: '全职',
    empId: '#LA-2024-089',
    dept: '生产部',
    deptColor: 'bg-blue-50 text-blue-700 border-blue-200',
    role: '生产主管',
    email: 'm.sterling@loom-erp.com',
    phone: '+86 138 0012 3456',
    status: '在职',
    statusColor: 'text-emerald-600',
    dotColor: 'bg-emerald-600',
    joinDate: '2021-01-12',
    avatar: 'https://placehold.co/100x100/e2e8f0/64748b?text=M'
  },
  {
    id: 2,
    name: '陈莎拉 (Sarah)',
    type: '合同工',
    empId: '#LA-2024-112',
    dept: '销售部',
    deptColor: 'bg-amber-50 text-amber-700 border-amber-200',
    role: '区域负责人',
    email: 's.chen@loom-erp.com',
    phone: '+86 139 0987 6543',
    status: '请假中',
    statusColor: 'text-amber-600',
    dotColor: 'bg-amber-600',
    joinDate: '2022-11-04',
    avatar: 'https://placehold.co/100x100/e2e8f0/64748b?text=S'
  },
  {
    id: 3,
    name: '朱利安 (Julian)',
    type: '全职',
    empId: '#LA-2023-442',
    dept: '行政部',
    deptColor: 'bg-slate-100 text-slate-700 border-slate-200',
    role: '人事经理',
    email: 'j.vane@loom-erp.com',
    phone: '+86 137 0444 5566',
    status: '在职',
    statusColor: 'text-emerald-600',
    dotColor: 'bg-emerald-600',
    joinDate: '2023-05-22',
    avatar: 'https://placehold.co/100x100/e2e8f0/64748b?text=J'
  },
  {
    id: 4,
    name: '凯文 (Kevin)',
    type: '试用期',
    empId: '#LA-2024-501',
    dept: '质检部',
    deptColor: 'bg-purple-50 text-purple-700 border-purple-200',
    role: '纺织质检员',
    email: 'k.ortiz@loom-erp.com',
    phone: '+86 150 0222 1111',
    status: '在职',
    statusColor: 'text-emerald-600',
    dotColor: 'bg-emerald-600',
    joinDate: '2024-09-15',
    avatar: 'https://placehold.co/100x100/e2e8f0/64748b?text=K'
  }
])
</script>
