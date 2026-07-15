<template>
  <main class="min-h-full bg-[#fbfcfe] px-4 py-10 text-[#0f172a]">
    <section class="mx-auto max-w-5xl overflow-hidden rounded-[2rem] border border-[#e2e8f0]/70 bg-white/95 shadow-xl shadow-slate-500/10">
      <header class="border-b border-[#e2e8f0]/60 bg-gradient-to-br from-[#ccfbf1] via-white to-[#ffffff] px-6 py-8 md:px-10">
        <div class="flex flex-wrap items-center justify-between gap-5">
          <div class="flex items-center gap-4">
            <img src="../../../images/logo.png" alt="蜂巢 Hive logo" class="h-14 w-14 object-contain drop-shadow-sm">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.24em] text-[#0f766e]">{{ siteConfig.productName }}</p>
              <h1 class="mt-2 text-3xl font-black tracking-tight md:text-4xl">{{ pageTitle }}</h1>
            </div>
          </div>
          <div class="rounded-2xl border border-[#ccfbf1] bg-white/80 px-4 py-3 text-xs font-bold text-[#134e4a]">
            生效日期：{{ effectiveDate }}
          </div>
        </div>
        <p class="mt-5 max-w-4xl text-sm leading-7 text-[#475569]">
          本页面说明 {{ siteConfig.companyName }} 在提供蜂巢 Hive 数字化工厂管理系统服务时，如何处理账号、业务、设备权限、日志与安全相关信息。我们坚持最小必要、组织内权限可控和安全审计原则。
        </p>
      </header>

      <article class="space-y-8 px-6 py-8 md:px-10">
        <section class="rounded-3xl border border-[#e2e8f0]/70 bg-[#ccfbf1] p-5 text-sm leading-7 text-[#475569]">
          <p>
            <strong class="text-[#101418]">适用范围：</strong>
            本{{ pageTitle }}适用于蜂巢 Hive 及配套服务。若企业基于自身业务另行制定内部制度，员工仍应同时遵守企业内部管理要求。
          </p>
        </section>

        <template v-if="type === 'privacy'">
          <section v-for="section in privacySections" :key="section.title" class="legal-section">
            <h2>{{ section.title }}</h2>
            <ul>
              <li v-for="item in section.items" :key="item">{{ item }}</li>
            </ul>
          </section>
        </template>

        <template v-else>
          <section v-for="section in termsSections" :key="section.title" class="legal-section">
            <h2>{{ section.title }}</h2>
            <ul>
              <li v-for="item in section.items" :key="item">{{ item }}</li>
            </ul>
          </section>
        </template>

        <section class="grid gap-4 rounded-3xl bg-[#f7fafc] p-5 text-sm leading-7 text-[#475569] md:grid-cols-2">
          <p><strong class="text-[#101418]">公司主体：</strong>{{ siteConfig.companyName }}</p>
          <p><strong class="text-[#101418]">联系邮箱：</strong>{{ siteConfig.supportEmail }}</p>
          <p>
            <strong class="text-[#101418]">ICP备案：</strong>
            <a :href="siteConfig.icpUrl" target="_blank" rel="noopener noreferrer" class="font-bold text-[#0f766e]">
              {{ siteConfig.icpNumber }}
            </a>
          </p>
          <p><strong class="text-[#101418]">最近更新：</strong>{{ effectiveDate }}</p>
        </section>

        <div class="flex flex-wrap gap-3">
          <router-link to="/login" class="rounded-2xl bg-[#0f766e] px-5 py-3 text-sm font-black text-white transition hover:bg-[#115e59]">
            返回登录页
          </router-link>
          <router-link :to="type === 'privacy' ? '/terms' : '/privacy'" class="rounded-2xl border border-[#e2e8f0] bg-white px-5 py-3 text-sm font-black text-[#0f172a] transition hover:bg-[#ccfbf1]">
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
const effectiveDate = '2026-05-15'
const type = computed(() => (route.path.includes('terms') ? 'terms' : 'privacy'))
const pageTitle = computed(() => (type.value === 'privacy' ? '隐私政策' : '服务条款'))

const privacySections = [
  {
    title: '一、我们处理的信息类型',
    items: [
      '账号与身份信息：登录账号、手机号、员工姓名、所属组织、部门、岗位、角色权限、直属负责人、账号状态等，用于身份认证、权限控制、组织协同和离职停用。',
      '业务管理信息：库存、订单、客户、供应商、生产、出入库、标签、质量异常、考勤、请假、财务审批、离职审批、文档等由企业或用户主动录入的数据。',
      '设备与权限信息：考勤打卡可能需要位置权限；微信一键登录会使用微信提供的手机号授权能力；订阅消息会记录用户对模板消息的授权状态。',
      '日志与安全信息：登录时间、服务访问、操作记录、异常日志、设备基础信息、IP 相关信息等，用于安全审计、问题排查、异常访问限制和服务稳定性保障。'
    ]
  },
  {
    title: '二、我们如何使用信息',
    items: [
      '提供基础功能：完成登录、权限校验、员工管理、库存流转、订单协同、审批处理、标签打印和考勤统计。',
      '保障安全与可靠性：进行异常检测、操作审计、访问频率控制、数据备份、故障定位和风险提醒。',
      '改进产品体验：统计功能使用情况、识别高频操作场景，优化页面响应速度、信息配置和流程闭环。',
      '发送必要通知：在用户主动授权的前提下，通过小程序订阅消息发送待办提醒；订阅消息不会用于无关营销。',
      '履行法律法规或监管要求：在必要范围内配合审计、安全事件处置、争议解决或主管机关依法提出的要求。'
    ]
  },
  {
    title: '三、第三方服务与信息共享',
    items: [
      '微信生态能力：小程序登录、手机号授权、订阅消息等能力由微信提供，相关能力会遵循微信平台规则。',
      '文件存储服务：上传的业务附件、标签相关文件或文档会存储到系统配置的文件目录中，并按权限控制访问。',
      '除实现上述功能、取得授权、履行法定义务或保护系统安全外，我们不会向无关第三方出售、出租或非法共享企业业务数据。'
    ]
  },
  {
    title: '四、数据保存、保护与删除',
    items: [
      '我们会根据业务连续性、审计追溯、财务合规和安全排查需要保存必要数据；超过必要期限的数据应通过清理策略或企业管理操作进行归档、删除或脱敏。',
      '系统会通过加密传输、权限控制、操作留痕、访问隔离和外部服务调用保护等措施降低泄露风险。',
      '员工离职、调岗或外包人员退出时，企业管理员应及时停用账号或调整权限；离职审批通过后系统会同步收敛相关权限。',
      '如需导出、删除或更正数据，可联系企业管理员处理；涉及系统级数据处理的，可通过本页面联系方式联系我们协助。'
    ]
  },
  {
    title: '五、用户权利与选择',
    items: [
      '你可以在授权弹窗中选择是否授权手机号、位置或订阅消息；拒绝部分授权可能导致微信登录、考勤打卡或待办提醒不可用。',
      '你可以要求企业管理员查询、更正、补充、停用或删除与你相关的账号和业务数据；系统会根据权限、合规留存和业务规则进行处理。',
      '如果你认为信息处理存在问题，可以通过企业管理员或本页面联系方式提出反馈，我们会在合理期限内核实并处理。'
    ]
  },
  {
    title: '六、未成年人保护与政策更新',
    items: [
      '本系统面向企业生产经营管理场景，原则上不面向未成年人提供个人消费类服务；企业不应为无业务必要的未成年人创建账号。',
      '当功能、第三方服务、法律法规或运营方式发生变化时，我们可能更新本政策，并通过页面展示、版本更新或企业通知等方式提示。',
      '如你继续使用系统，即表示你已阅读并理解更新后的政策；重大变更会尽量以更明显方式提示。'
    ]
  }
]

const termsSections = [
  {
    title: '一、服务内容',
    items: [
      '蜂巢 Hive 提供数字化工厂管理能力，包括库存、订单、客户、生产、考勤、审批、标签、文档和质量管理等模块。',
      '具体可用功能会根据企业版本、组织配置、用户权限和定制需求有所不同。'
    ]
  },
  {
    title: '二、账号与权限',
    items: [
      '用户应妥善保管账号和密码，不得借用、共享、冒用他人账号，不得绕过权限访问其他员工或组织内受限数据。',
      '企业管理员应按最小必要原则分配角色权限，并在员工离职、调岗、外包结束或职责变化时及时调整账号状态。',
      '系统管理账号应只分配给企业授权管理员，不应与普通员工账号混用。'
    ]
  },
  {
    title: '三、数据录入与业务责任',
    items: [
      '用户应确保录入的客户、订单、库存、考勤、财务、审批和文档信息真实、合法、必要、准确。',
      '电脑端和手机端的打印、导入、导出、审批、状态流转等操作会被记录用于追溯；请在提交前核对关键信息。'
    ]
  },
  {
    title: '四、禁止行为',
    items: [
      '不得上传违法违规、侵权、恶意代码、虚假业务或与企业经营无关的内容。',
      '不得攻击、扫描、压测、爬取、绕过权限、恶意批量请求、破解授权信息或干扰系统稳定运行。',
      '不得利用系统处理未获授权的个人信息、客户资料或其他企业商业秘密。'
    ]
  },
  {
    title: '五、服务变更与责任边界',
    items: [
      '我们会持续优化系统安全、备份、迁移、日志和自动化运维能力，但企业仍需自行管理内部人员、终端设备、账号权限和数据录入质量。',
      '因用户误操作、企业内部管理不当、第三方平台故障、网络波动、不可抗力或未按安全要求维护账号与授权信息造成的风险，应由相应责任方承担。',
      '如需商业定制、信息项扩展或流程改造，应通过双方确认的需求、验收和交付流程执行。'
    ]
  }
]
</script>

<style scoped>
.legal-section h2 {
  margin-bottom: 0.75rem;
  color: #0f172a;
  font-size: 1.125rem;
  font-weight: 900;
}

.legal-section ul {
  display: grid;
  gap: 0.6rem;
  margin: 0;
  padding-left: 1.2rem;
}

.legal-section li {
  color: #475569;
  font-size: 0.925rem;
  line-height: 1.85;
}
</style>
