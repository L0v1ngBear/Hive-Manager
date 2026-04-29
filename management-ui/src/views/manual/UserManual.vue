<template>
  <section class="manual-page">
    <div class="manual-hero">
      <div class="manual-hero__copy">
        <span class="manual-kicker">使用手册</span>
        <h1>蜂巢 Hive 操作指南</h1>
        <p>
          按照“先配置基础资料、再跑业务流程、最后看经营洞察”的顺序使用，能让订单、库存、考勤、审批和打印链路更稳定地闭环。
        </p>
      </div>
      <div class="manual-hero__card">
        <span class="material-symbols-outlined">menu_book</span>
        <strong>推荐首次使用顺序</strong>
        <p>租户资料 → 员工与角色 → 客户项目 → 订单 → 库存与打印 → 大盘与 AI 建议</p>
      </div>
    </div>

    <div class="manual-grid">
      <article v-for="item in quickGuides" :key="item.title" class="manual-card">
        <div class="manual-card__icon">
          <span class="material-symbols-outlined">{{ item.icon }}</span>
        </div>
        <div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.desc }}</p>
        </div>
      </article>
    </div>

    <div class="manual-layout">
      <aside class="manual-toc">
        <button
          v-for="section in sections"
          :key="section.id"
          class="manual-toc__item"
          @click="scrollToSection(section.id)"
        >
          <span class="material-symbols-outlined">{{ section.icon }}</span>
          {{ section.title }}
        </button>
      </aside>

      <div class="manual-content">
        <section v-for="section in sections" :id="section.id" :key="section.id" class="manual-section">
          <div class="manual-section__head">
            <span class="material-symbols-outlined">{{ section.icon }}</span>
            <div>
              <h2>{{ section.title }}</h2>
              <p>{{ section.summary }}</p>
            </div>
          </div>

          <div class="manual-steps">
            <div v-for="step in section.steps" :key="step.title" class="manual-step">
              <strong>{{ step.title }}</strong>
              <p>{{ step.content }}</p>
            </div>
          </div>

          <div v-if="section.route" class="manual-action">
            <button type="button" @click="goRoute(section.route)">
              进入{{ section.title }}
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
        </section>

        <section class="manual-section faq-section">
          <div class="manual-section__head">
            <span class="material-symbols-outlined">help</span>
            <div>
              <h2>常见问题</h2>
              <p>遇到看不到页面、数据不一致、打印异常时，优先从这里排查。</p>
            </div>
          </div>

          <div class="faq-list">
            <details v-for="faq in faqs" :key="faq.question">
              <summary>{{ faq.question }}</summary>
              <p>{{ faq.answer }}</p>
            </details>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineOptions({ name: 'UserManual' })

const router = useRouter()

const quickGuides = [
  {
    icon: 'flag',
    title: '先建基础资料',
    desc: '员工、角色、客户、项目和模板先维护好，后续订单、库存和打印才不会反复补数据。'
  },
  {
    icon: 'sync_alt',
    title: '按流程闭环',
    desc: '销售订单、生产订单、库存出入库、出库单打印和状态流转要按业务顺序推进。'
  },
  {
    icon: 'insights',
    title: '用数据辅助决策',
    desc: '总览大盘、待办通知和 AI 经营建议会把异常、风险和建议集中展示给管理层。'
  }
]

const sections = [
  {
    id: 'start',
    icon: 'rocket_launch',
    title: '首次使用',
    summary: '适合老板、管理员或刚接手系统的同事。',
    route: '/dashboard',
    steps: [
      {
        title: '1. 登录并确认租户',
        content: '使用管理员分配的账号密码登录。进入系统后先看右上角租户信息，确认当前进入的是自己的企业组织。'
      },
      {
        title: '2. 配置角色与权限',
        content: '进入角色管理，给老板、仓库、销售、财务、生产、质检等岗位分配对应权限，再把员工绑定到角色。'
      },
      {
        title: '3. 维护员工与组织架构',
        content: '进入员工管理新增员工，维护上级关系、部门、岗位和手机号。组织架构抽屉会按上下级关系展示人员结构。'
      }
    ]
  },
  {
    id: 'customer',
    icon: 'handshake',
    title: '客户与项目',
    summary: '客户资料决定销售订单、项目名称和施工区域等业务字段。',
    route: '/function/customer',
    steps: [
      {
        title: '新建客户',
        content: '在客户管理中维护客户名称、联系人、联系电话等基础信息，后续销售订单会从这里搜索客户。'
      },
      {
        title: '添加合作项目',
        content: '每个客户可以维护多个合作项目，施工区域跟随项目维护，避免把不同项目固定成同一个区域。'
      },
      {
        title: '订单引用项目',
        content: '销售订单选择客户后，只展示该客户下的合作项目。若输入新客户或新项目，系统会同步维护到客户资料。'
      }
    ]
  },
  {
    id: 'order',
    icon: 'list_alt',
    title: '订单管理',
    summary: '订单是业务主线，销售单和生产单通过状态流转形成闭环。',
    route: '/function/order',
    steps: [
      {
        title: '新建销售订单',
        content: '填写客户、项目、交付日期和商品明细。新订单默认待确认，交付日期为必填项。'
      },
      {
        title: '新建生产订单',
        content: '填写面料型号、规格、克重、数量和交付日期。只有订单进入生产中状态后，生产工序流转才会展示。'
      },
      {
        title: '状态与物流',
        content: '销售订单只有变更为已发货时才需要填写物流公司和单号；有关联订单时，状态会同步更新。'
      }
    ]
  },
  {
    id: 'inventory',
    icon: 'inventory_2',
    title: '库存管理',
    summary: '库存链路是系统最重要的服务，入库、出库、标签打印要保持可靠。',
    route: '/function/inventory',
    steps: [
      {
        title: '首次入库',
        content: '录入型号、规格和米数后生成布匹库存。首次入库需要打印标签并贴到布匹上，方便后续扫码出库。'
      },
      {
        title: '扫码出库',
        content: '小程序扫码会默认显示可用米数。如果出库米数小于可用米数，系统视为部分出库，并要求重打剩余布匹标签。'
      },
      {
        title: '库存预警',
        content: '库存管理会展示库存水位、最近流水和预警信息。建议仓库每天核对出入库流水，避免账实不一致。'
      }
    ]
  },
  {
    id: 'print',
    icon: 'print',
    title: '打印与模板',
    summary: '标签和出库单都支持模板化，打印结果会回传形成审计记录。',
    route: '/function/receipt',
    steps: [
      {
        title: '标签模板',
        content: '进入标签模板页面，可视化配置字段位置、字号和变量。小程序蓝牙打印会读取管理端模板。'
      },
      {
        title: '出库单模板',
        content: '出库单打印页面可选择模板并预览。连续纸建议按 241-1 纸张规格配置打印机，超出一页会自动生成完整下一页。'
      },
      {
        title: '打印确认',
        content: '打印成功后点击确认已打印，失败或跳过会记录状态，方便后续补打和运维排查。'
      }
    ]
  },
  {
    id: 'attendance',
    icon: 'fingerprint',
    title: '考勤与审批',
    summary: '考勤规则给小程序打卡使用，审批中心处理请假和财务事项。',
    route: '/function/attendance',
    steps: [
      {
        title: '配置考勤规则',
        content: '设置上班打卡时间段、下班打卡时间段和加班时间段。小程序打卡会按这些规则判断状态。'
      },
      {
        title: '查看打卡记录',
        content: '考勤管理页面可查看员工打卡记录、异常情况和规则配置，便于管理人员跟进。'
      },
      {
        title: '处理审批',
        content: '审批中心集中处理请假、财务等待办。小程序和网页端待办会联动展示与当前用户相关的事项。'
      }
    ]
  },
  {
    id: 'ai',
    icon: 'psychology',
    title: 'AI 经营建议',
    summary: 'AI 建议面向老板和管理层，帮助发现库存、订单、客户、质量和成本风险。',
    route: '/dashboard/ai-advices',
    steps: [
      {
        title: '查看大盘洞察',
        content: '总览大盘会展示关键指标、待办和 AI 经营洞察，适合每天早会前快速看整体情况。'
      },
      {
        title: '进入建议中心',
        content: '点击查看更多 AI 建议，可按库存、订单、客户、质量、财务和生产运营维度查看异常诊断。'
      },
      {
        title: '形成闭环',
        content: '建议不只是展示，后续可沉淀为通知、待办或处理记录，推动负责人跟进并持续优化建议准确度。'
      }
    ]
  },
  {
    id: 'mini',
    icon: 'phone_iphone',
    title: '小程序联动',
    summary: '小程序适合现场人员使用，网页端适合管理配置和经营分析。',
    steps: [
      {
        title: '登录小程序',
        content: '小程序支持账号密码登录和微信一键登录。微信一键登录不需要短信验证码，用户授权手机号后由后端匹配系统用户。'
      },
      {
        title: '现场操作',
        content: '员工可以在小程序完成考勤打卡、订单查看、库存扫码出入库、次品登记和待办处理。'
      },
      {
        title: '模板同步',
        content: '管理端维护标签模板，小程序打印时自动读取模板变量，确保现场打印和后台配置保持一致。'
      }
    ]
  }
]

const faqs = [
  {
    question: '为什么我看不到某个菜单？',
    answer: '大多数菜单受角色权限控制。请让管理员进入角色管理，为你的角色分配对应权限，再确认员工已绑定该角色。'
  },
  {
    question: '为什么订单里没有想选的客户或项目？',
    answer: '先到客户管理维护客户和合作项目。销售订单选择客户后，项目下拉列表只展示该客户维护过的项目。'
  },
  {
    question: '部分出库后为什么要重新打印标签？',
    answer: '部分出库后原标签米数已经不准确，必须给剩余布匹重新贴标签，后续扫码才能得到正确可用米数。'
  },
  {
    question: '微信一键登录为什么不需要短信验证码？',
    answer: '微信一键登录使用小程序手机号授权能力，微信返回一次性授权 code，后端换取手机号后匹配系统用户，不走短信验证码。'
  },
  {
    question: '线上访问异常或数据不对怎么办？',
    answer: '先刷新页面并确认当前租户。如果仍异常，请联系管理员查看运维日志，并提供操作时间、页面名称和业务编号。'
  }
]

function scrollToSection(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goRoute(path) {
  router.push(path)
}
</script>

<style scoped>
.manual-page {
  min-height: 100%;
  padding: clamp(1rem, 2vw, 2rem);
  color: #101418;
}

.manual-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(18rem, 26rem);
  gap: 1.5rem;
  align-items: stretch;
  padding: clamp(1.5rem, 4vw, 3rem);
  border: 1px solid rgba(233, 197, 109, 0.42);
  border-radius: 2rem;
  background:
    radial-gradient(circle at 78% 18%, rgba(255, 196, 41, 0.32), transparent 28%),
    linear-gradient(135deg, rgba(255, 253, 248, 0.98), rgba(255, 248, 228, 0.92));
  box-shadow: 0 24px 70px rgba(245, 164, 0, 0.14);
}

.manual-kicker {
  display: inline-flex;
  margin-bottom: 1rem;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  background: rgba(245, 164, 0, 0.12);
  color: #9a6500;
  font-size: 0.75rem;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.manual-hero h1 {
  margin: 0;
  color: #101418;
  font-size: clamp(2.2rem, 5vw, 4.4rem);
  font-weight: 1000;
  letter-spacing: -0.08em;
  line-height: 0.98;
}

.manual-hero p {
  margin-top: 1rem;
  max-width: 48rem;
  color: #5f5a4e;
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.9;
}

.manual-hero__card {
  display: flex;
  min-height: 14rem;
  flex-direction: column;
  justify-content: center;
  gap: 0.85rem;
  border-radius: 1.5rem;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 0 0 1px rgba(233, 197, 109, 0.36);
}

.manual-hero__card .material-symbols-outlined {
  color: #f5a400;
  font-size: 3rem;
}

.manual-hero__card strong {
  font-size: 1.2rem;
  font-weight: 1000;
}

.manual-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.manual-card,
.manual-section,
.manual-toc {
  border: 1px solid rgba(233, 197, 109, 0.28);
  background: rgba(255, 253, 248, 0.9);
  box-shadow: 0 16px 42px rgba(15, 23, 42, 0.06);
}

.manual-card {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  padding: 1.25rem;
  border-radius: 1.5rem;
}

.manual-card__icon,
.manual-section__head > .material-symbols-outlined {
  display: inline-flex;
  width: 3rem;
  height: 3rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  color: #ffffff;
  background: linear-gradient(135deg, #ffd43b, #f5a400 58%, #f08a00);
  box-shadow: 0 12px 24px rgba(245, 164, 0, 0.24);
}

.manual-card h2,
.manual-section h2 {
  margin: 0;
  color: #101418;
  font-size: 1.05rem;
  font-weight: 1000;
}

.manual-card p,
.manual-section p,
.manual-step p,
.faq-list p {
  margin: 0.35rem 0 0;
  color: #5f5a4e;
  font-size: 0.92rem;
  font-weight: 650;
  line-height: 1.8;
}

.manual-layout {
  display: grid;
  grid-template-columns: 16rem minmax(0, 1fr);
  gap: 1.25rem;
  margin-top: 1.25rem;
}

.manual-toc {
  position: sticky;
  top: 1rem;
  align-self: start;
  border-radius: 1.5rem;
  padding: 0.75rem;
}

.manual-toc__item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.7rem;
  border: none;
  border-radius: 1rem;
  padding: 0.9rem 1rem;
  color: #5f5a4e;
  background: transparent;
  font-size: 0.88rem;
  font-weight: 900;
  text-align: left;
  transition: 0.18s ease;
}

.manual-toc__item:hover {
  color: #101418;
  background: #fff3cc;
}

.manual-content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.manual-section {
  scroll-margin-top: 1.25rem;
  border-radius: 1.75rem;
  padding: clamp(1.25rem, 3vw, 2rem);
}

.manual-section__head {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}

.manual-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.manual-step {
  border-radius: 1.25rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.78);
}

.manual-step strong {
  display: block;
  color: #101418;
  font-size: 0.92rem;
  font-weight: 1000;
}

.manual-action {
  margin-top: 1rem;
}

.manual-action button {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  border: none;
  border-radius: 999px;
  padding: 0.78rem 1.2rem;
  color: #ffffff;
  background: #101418;
  font-weight: 900;
  transition: 0.18s ease;
}

.manual-action button:hover {
  transform: translateY(-1px);
  background: #f5a400;
}

.faq-list {
  display: grid;
  gap: 0.8rem;
}

.faq-list details {
  border-radius: 1.1rem;
  padding: 1rem 1.1rem;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.78);
}

.faq-list summary {
  cursor: pointer;
  color: #101418;
  font-weight: 1000;
}

@media (max-width: 1100px) {
  .manual-hero,
  .manual-layout {
    grid-template-columns: 1fr;
  }

  .manual-toc {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .manual-page {
    padding: 0.75rem;
  }

  .manual-grid,
  .manual-steps,
  .manual-toc {
    grid-template-columns: 1fr;
  }

  .manual-hero {
    border-radius: 1.5rem;
    padding: 1.25rem;
  }
}
</style>
