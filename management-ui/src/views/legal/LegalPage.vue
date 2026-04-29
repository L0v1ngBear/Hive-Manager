<template>
  <main class="min-h-full bg-[#fffdf8] px-4 py-10 text-[#101418]">
    <section class="mx-auto max-w-4xl overflow-hidden rounded-[2rem] border border-[#f0d48e]/70 bg-white/90 shadow-xl shadow-amber-500/10">
      <header class="border-b border-[#f0d48e]/60 bg-gradient-to-br from-[#fff8e6] to-white px-6 py-8 md:px-10">
        <div class="flex items-center gap-4">
          <img src="../../../images/logo.png" alt="蜂巢 logo" class="h-14 w-14 object-contain drop-shadow-sm">
          <div>
            <p class="text-xs font-black uppercase tracking-[0.24em] text-[#b56f00]">{{ siteConfig.productName }}</p>
            <h1 class="mt-2 text-3xl font-black tracking-tight md:text-4xl">{{ pageTitle }}</h1>
          </div>
        </div>
        <p class="mt-5 max-w-3xl text-sm leading-7 text-[#5f5a4e]">
          本页面用于说明 {{ siteConfig.companyName }} 在提供数字化工厂管理服务时对用户信息、数据安全与服务边界的处理方式。
        </p>
      </header>

      <article class="space-y-8 px-6 py-8 md:px-10">
        <template v-if="type === 'privacy'">
          <section v-for="section in privacySections" :key="section.title" class="legal-section">
            <h2>{{ section.title }}</h2>
            <p v-for="item in section.items" :key="item">{{ item }}</p>
          </section>
        </template>

        <template v-else>
          <section v-for="section in termsSections" :key="section.title" class="legal-section">
            <h2>{{ section.title }}</h2>
            <p v-for="item in section.items" :key="item">{{ item }}</p>
          </section>
        </template>

        <section class="rounded-3xl bg-[#fff8e6] p-5 text-sm leading-7 text-[#5f5a4e]">
          <p><strong class="text-[#101418]">公司主体：</strong>{{ siteConfig.companyName }}</p>
          <p><strong class="text-[#101418]">联系邮箱：</strong>{{ siteConfig.supportEmail }}</p>
          <p>
            <strong class="text-[#101418]">ICP备案：</strong>
            <a :href="siteConfig.icpUrl" target="_blank" rel="noopener noreferrer" class="font-bold text-[#b56f00]">
              {{ siteConfig.icpNumber }}
            </a>
          </p>
          <p><strong class="text-[#101418]">最近更新：</strong>2026-04-27</p>
        </section>

        <div class="flex flex-wrap gap-3">
          <router-link to="/login" class="rounded-2xl bg-[#101418] px-5 py-3 text-sm font-black text-white transition hover:bg-black">
            返回登录页
          </router-link>
          <router-link :to="type === 'privacy' ? '/terms' : '/privacy'" class="rounded-2xl border border-[#f0d48e] bg-white px-5 py-3 text-sm font-black text-[#101418] transition hover:bg-[#fff8e6]">
            查看{{ type === 'privacy' ? '服务条款' : '隐私政策' }}
          </router-link>
        </div>
      </article>
    </section>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { siteConfig } from '@/config/site'

defineOptions({ name: 'LegalPage' })

const route = useRoute()
const type = computed(() => (route.path.includes('terms') ? 'terms' : 'privacy'))
const pageTitle = computed(() => (type.value === 'privacy' ? '隐私政策' : '服务条款'))

const privacySections = [
  {
    title: '一、我们收集的信息',
    items: [
      '为完成登录、权限控制、租户隔离和业务处理，我们会处理账号、员工姓名、手机号、角色权限、租户编码等必要信息。',
      '为支持库存、订单、客户、考勤、审批、打印和质量管理，我们会保存用户主动录入的业务数据。',
      '为优化 AI 经营建议，我们会采集低敏行为元数据，例如页面访问、功能点击、建议反馈和通知打开记录；不会采集密码、token、银行卡号等敏感凭据。'
    ]
  },
  {
    title: '二、信息使用目的',
    items: [
      '用于身份认证、权限校验、跨租户隔离、业务流程流转、异常排查、审计追踪和数据分析。',
      '用于生成库存、订单、质量、客户和经营风险相关的智能建议，帮助管理人员做决策辅助。',
      '未经授权，我们不会将租户业务数据提供给无关第三方。'
    ]
  },
  {
    title: '三、数据安全措施',
    items: [
      '系统通过 HTTPS 传输、数据库低权限账号、租户隔离、操作日志、运维日志和服务器防火墙降低数据泄露风险。',
      '我们会尽量避免在日志和行为采集中记录密码、密钥、token、完整手机号等敏感字段。',
      '数据库备份、服务器权限和线上运维账号应由企业管理员妥善保管，并定期轮换密码和密钥。'
    ]
  },
  {
    title: '四、用户权利',
    items: [
      '企业管理员可以根据内部管理要求维护员工账号、角色权限、客户资料和业务数据。',
      '如需查询、更正或删除相关数据，可通过本页面联系方式与我们或企业管理员联系。',
      '若法律法规要求保留审计、财务或安全日志，我们会在合规期限内保留必要记录。'
    ]
  }
]

const termsSections = [
  {
    title: '一、服务内容',
    items: [
      '蜂巢 Hive 提供数字化工厂管理能力，包括库存、订单、客户、考勤、审批、打印、质量管理和 AI 经营建议等模块。',
      '系统功能会根据企业配置、用户权限和租户版本有所差异。'
    ]
  },
  {
    title: '二、账号与权限',
    items: [
      '用户应妥善保管账号和密码，不得将账号转借他人使用。',
      '企业管理员应按最小权限原则分配角色，离职、转岗或外包人员应及时停用或调整权限。'
    ]
  },
  {
    title: '三、数据与使用规范',
    items: [
      '用户应确保录入的客户、订单、库存、考勤和财务数据真实、合法、必要。',
      '不得利用系统上传违法内容、攻击系统、绕过权限限制或访问其他租户数据。'
    ]
  },
  {
    title: '四、服务安全与责任边界',
    items: [
      '我们会持续完善安全、备份、日志和审计能力，但企业仍需自行管理内部账号、终端设备和人员操作风险。',
      'AI 经营建议属于决策辅助信息，最终经营决策应由企业管理人员结合实际情况判断。'
    ]
  }
]
</script>

<style scoped>
.legal-section h2 {
  margin-bottom: 0.75rem;
  font-size: 1.125rem;
  font-weight: 900;
}

.legal-section p {
  margin-top: 0.5rem;
  color: #5f5a4e;
  font-size: 0.925rem;
  line-height: 1.8;
}
</style>
