<template>
  <section class="manual-page">
    <div class="manual-hero">
      <div class="manual-hero__copy">
        <button type="button" class="manual-edit-btn manual-hero__edit" @click="openHeroEditor">
          <span class="material-symbols-outlined">edit</span>
          编辑
        </button>
        <span class="manual-kicker">{{ manualConfig.hero.kicker }}</span>
        <h1>{{ manualConfig.hero.title }}</h1>
        <p>{{ manualConfig.hero.intro }}</p>
        <div v-if="manualConfig.hero.badges?.length" class="manual-hero__badges">
          <span v-for="badge in manualConfig.hero.badges" :key="badge">{{ badge }}</span>
        </div>
      </div>
      <div class="manual-hero__card">
        <span class="material-symbols-outlined">{{ manualConfig.hero.cardIcon }}</span>
        <strong>{{ manualConfig.hero.cardTitle }}</strong>
        <p>{{ manualConfig.hero.cardText }}</p>
      </div>
    </div>

    <div class="manual-grid">
      <article v-for="(item, index) in quickGuides" :key="item.title" class="manual-card">
        <button type="button" class="manual-edit-btn manual-card__edit" @click.stop="openQuickGuideEditor(index)">
          <span class="material-symbols-outlined">edit</span>
          编辑
        </button>
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
        <button class="manual-toc__item" @click="scrollToSection('custom-manual')">
          <span class="material-symbols-outlined">edit_note</span>
          {{ manualConfig.customManual.title }}
        </button>
      </aside>

      <div class="manual-content">
        <section v-for="(section, sectionIndex) in sections" :id="section.id" :key="section.id" class="manual-section">
          <div class="manual-section__head">
            <span class="material-symbols-outlined">{{ section.icon }}</span>
            <div>
              <h2>{{ section.title }}</h2>
              <p>{{ section.summary }}</p>
            </div>
            <button type="button" class="manual-edit-btn manual-section__edit" @click="openSectionEditor(sectionIndex)">
              <span class="material-symbols-outlined">edit</span>
              编辑
            </button>
          </div>

          <div class="manual-steps">
            <div v-for="step in section.steps" :key="step.title" class="manual-step">
              <strong>{{ step.title }}</strong>
              <p>{{ step.content }}</p>
              <ul v-if="stepDetailList(section.id, step).length" class="manual-detail-list">
                <li v-for="detail in stepDetailList(section.id, step)" :key="detail">{{ detail }}</li>
              </ul>
            </div>
          </div>

          <div v-if="section.tips?.length" class="manual-tip-list">
            <span v-for="tip in section.tips" :key="tip">{{ tip }}</span>
          </div>

          <div v-if="section.route" class="manual-action">
            <button type="button" @click="goRoute(section.route)">
              进入{{ section.title }}
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
        </section>

        <section id="custom-manual" class="manual-section manual-custom-section">
          <div class="manual-section__head">
            <span class="material-symbols-outlined">edit_note</span>
            <div>
              <h2>{{ manualConfig.customManual.title }}</h2>
              <p>{{ manualConfig.customManual.summary }}</p>
            </div>
            <button type="button" class="manual-edit-btn manual-section__edit" @click="openCustomManualWindowEditor">
              <span class="material-symbols-outlined">edit</span>
              编辑
            </button>
          </div>

          <div v-loading="customManualLoading" class="manual-custom-editor">
            <div class="manual-custom-toolbar">
              <div>
                <strong>{{ manualConfig.customManual.label }}</strong>
                <p>{{ manualConfig.customManual.helper }}</p>
              </div>
              <span v-if="customManualSavedAt" class="manual-custom-saved">上次保存：{{ customManualSavedAt }}</span>
            </div>
            <textarea
              v-model="customManualDraft"
              rows="12"
              maxlength="120000"
              placeholder="示例：&#10;1. 新订单由销售负责人录入，提交前确认客户、项目、交期。&#10;2. 仓库入库后当天完成标签打印并贴标。&#10;3. 质量异常必须上传现场图片并填写处理结果。"
            ></textarea>
            <div class="manual-custom-actions">
              <button type="button" class="manual-secondary-btn" :disabled="customManualSaving" @click="resetCustomManualTemplate">填入推荐模板</button>
              <button type="button" class="manual-secondary-btn" :disabled="customManualSaving" @click="exportCustomManual">导出手册</button>
              <button type="button" class="manual-danger-btn" :disabled="customManualSaving" @click="clearCustomManual">清空</button>
              <button type="button" class="manual-primary-btn" :disabled="customManualSaving" @click="saveCustomManual">
                {{ customManualSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </section>

        <section class="manual-section faq-section">
          <div class="manual-section__head">
            <span class="material-symbols-outlined">help</span>
            <div>
              <h2>{{ manualConfig.faqWindow.title }}</h2>
              <p>{{ manualConfig.faqWindow.summary }}</p>
            </div>
            <div class="manual-section__actions">
              <button type="button" class="manual-edit-btn" @click="openFaqWindowEditor">
                <span class="material-symbols-outlined">edit</span>
                编辑
              </button>
              <button type="button" class="manual-edit-btn" @click="openNewFaqEditor">
                <span class="material-symbols-outlined">add</span>
                新增
              </button>
            </div>
          </div>

          <div class="faq-list">
            <details v-for="(faq, faqIndex) in faqs" :key="faq.question">
              <summary>{{ faq.question }}</summary>
              <p>{{ faq.answer }}</p>
              <button type="button" class="manual-inline-edit" @click="openFaqEditor(faqIndex)">
                <span class="material-symbols-outlined">edit</span>
                编辑
              </button>
            </details>
          </div>
        </section>
      </div>
    </div>

    <div v-if="manualEditor.open" class="manual-editor-mask" @click.self="closeManualEditor">
      <div class="manual-editor-dialog">
        <div class="manual-editor-head">
          <h3>{{ manualEditor.title }}</h3>
          <button type="button" @click="closeManualEditor">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="manual-editor-fields">
          <label v-for="field in manualEditor.fields" :key="field.key" class="manual-editor-field">
            <span>{{ field.label }}</span>
            <textarea
              v-if="field.type === 'textarea'"
              v-model="manualEditor.form[field.key]"
              :rows="field.rows || 4"
            ></textarea>
            <input v-else v-model="manualEditor.form[field.key]" type="text" />
            <small v-if="field.hint">{{ field.hint }}</small>
          </label>
        </div>

        <div class="manual-editor-actions">
          <button type="button" class="manual-secondary-btn" :disabled="customManualSaving" @click="closeManualEditor">取消</button>
          <button type="button" class="manual-primary-btn" :disabled="customManualSaving" @click="saveManualEditor">
            {{ customManualSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCustomManual, saveCustomManualContent } from './api/manual'

defineOptions({ name: 'UserManual' })

const router = useRouter()

const customManualDraft = ref('')
const customManualSavedAt = ref('')
const customManualLoading = ref(false)
const customManualSaving = ref(false)

const recommendedCustomManual = `# 企业内部操作补充

## 订单
1. 新订单由销售负责人录入，提交前确认客户、项目、信息渠道和订单小项。
2. 需要审批的订单先提交审批，审批通过后再进入后续流转。
3. 发货前必须确认物流信息，并把异常情况写入备注。

## 库存
1. 入库后当天完成标签打印并贴标。
2. 部分出库后必须补打剩余标签，避免后续扫码米数不一致。
3. 库存预警由仓库负责人每日处理。

## 质量与售后
1. 质量异常必须填写问题类型、责任人、处理结果。
2. 需要留证时上传现场图片或附件。
3. 高频问题每周复盘一次，并写明改进措施。

## 审批
1. 审批人收到待办后应在当天处理。
2. 驳回时必须写清楚原因，方便业务人员修改。
3. 离职审批通过后，管理员及时回收权限。`

onMounted(() => {
  loadCustomManual()
})

const DEFAULT_QUICK_GUIDES = [
  {
    icon: 'verified_user',
    title: '账号安全先完成',
    desc: '首次登录按系统提示修改初始密码；忘记密码可通过短信验证码找回，避免共用账号。'
  },
  {
    icon: 'groups',
    title: '人员权限先对齐',
    desc: '先维护部门、员工、直属负责人和角色权限，小程序加入组织后再由管理员分配业务权限。'
  },
  {
    icon: 'qr_code_scanner',
    title: '现场优先扫码',
    desc: '订单流转、库存出入库、设备巡检和标签打印尽量走扫码，减少人工输入错误。'
  },
  {
    icon: 'notifications_active',
    title: '异常统一待办',
    desc: '订单预警、库存水位、审批事项和企业通知统一进入待办/通知，负责人按优先级处理。'
  }
]

const DEFAULT_SECTIONS = [
  {
    id: 'start',
    icon: 'rocket_launch',
    title: '首次使用',
    summary: '适合负责人、管理员和新员工快速熟悉系统。',
    route: '/dashboard',
    steps: [
      {
        title: '1. 登录并修改密码',
        content: '使用企业分配的账号密码登录。首次登录如提示修改密码，请先完成修改；后续忘记密码可在登录页通过短信验证码重置。'
      },
      {
        title: '2. 员工加入组织',
        content: '员工可通过组织码加入，加入时需要填写真实姓名。组织码由管理员生成，短时间有效，过期后需重新生成。'
      },
      {
        title: '3. 管理员分配权限',
        content: '新加入员工默认只有普通员工基础身份。需要使用订单、库存、审批、打印等功能时，由管理员在员工管理中分配角色和权限。'
      }
    ],
    tips: ['建议每个员工使用自己的手机号和账号', '不要多人共用管理员账号', '离职员工及时改为已离职并回收权限']
  },
  {
    id: 'base',
    icon: 'account_tree',
    title: '基础资料',
    summary: '基础资料决定后续订单、审批、考勤和通知能否自动对齐。',
    route: '/function/employee',
    steps: [
      {
        title: '部门与负责人',
        content: '先维护部门，再维护员工的职位、直属负责人和在职状态。审批、待办和组织结构会依赖这些关系。'
      },
      {
        title: '角色与权限',
        content: '角色管理用于控制菜单和操作权限。普通员工只给基础查看和本人业务权限，管理类权限只分配给对应负责人。'
      },
      {
        title: '客户与项目',
        content: '客户管理中维护客户名称、联系人、电话、项目和项目负责人。订单选择客户后，会优先从客户项目中带出业务信息。'
      }
    ],
    tips: ['客户、项目、员工姓名尽量使用正式名称', '项目负责人会影响后续跟进责任', '信息展示可按企业习惯配置']
  },
  {
    id: 'order',
    icon: 'list_alt',
    title: '订单管理',
    summary: '订单是业务主线，按状态和工序推进，形成从下单到发货的完整闭环。',
    route: '/function/order',
    steps: [
      {
        title: '1. 新建订单',
        content: '填写客户、项目、品牌、订单小项、信息渠道和商品明细。普通销售订单必须填写信息渠道，图纸预算订单可留空；订单小项分为样板间、大货和补单，便于后续统计。'
      },
      {
        title: '2. 审批与状态流转',
        content: '待确认、待收款、备料中、生产中、待发货、已发货、已完成按顺序推进；待收款转备料中需要订单审批。'
      },
      {
        title: '3. 生产工序',
        content: '生产中订单按原料入库、原料检验、尺寸裁剪、窗帘缝制、窗帘熨烫、成品检验、高温定型、打包装箱、成品入库、成品发货推进。'
      }
    ],
    tips: ['一个订单只使用一个流转码', '审批通过后自动生成待打印流转码任务', '扫码后按当前状态推进到下一环节']
  },
  {
    id: 'scan',
    icon: 'qr_code_2',
    title: '扫码流转',
    summary: '扫码功能用于减少人工录入，让现场人员按最短路径完成操作。',
    steps: [
      {
        title: '订单流转码',
        content: '订单审批通过后，系统自动创建流转码待打印任务。小程序打印后贴到订单资料上，后续扫码即可更新订单状态或生产工序。'
      },
      {
        title: '库存条码',
        content: '每匹布都有唯一条码。扫码可查看库存详情、执行出库、部分出库和标签补打。部分出库后必须给剩余布匹重新打印标签。'
      },
      {
        title: '设备巡检码',
        content: '设备巡检码是固定码，打印一次贴在设备上即可。现场人员扫码后填写巡检结果，负责人可查看巡检记录。'
      }
    ],
    tips: ['二维码和条形码都可用于扫码', '条码内容只保留业务识别信息', '扫码失败时先确认网络和权限']
  },
  {
    id: 'inventory',
    icon: 'inventory_2',
    title: '库存管理',
    summary: '库存页面按型号聚合总米数，进入详情后查看每匹布的剩余米数和出入库流水。',
    route: '/function/inventory',
    steps: [
      {
        title: '入库',
        content: '支持手动入库、批量导入和图片识别入库。入库后按每匹布生成条码，标签由小程序蓝牙打印。'
      },
      {
        title: '出库',
        content: '出库时优先扫码选择布匹，可按先进先出或先进后出排序。出库后会记录流水，剩余布匹可继续追溯。'
      },
      {
        title: '库存预警',
        content: '用户可设置型号库存低于多少米时预警。系统采用按需判断，进入相关页面或业务触发时生成预警，避免无意义轮询。'
      }
    ],
    tips: ['导入第三方库存前先下载导入说明', '导入失败会给出错误原因', '库存流水是排查账实差异的第一依据']
  },
  {
    id: 'print',
    icon: 'print',
    title: '打印与模板',
    summary: '标签、订单流转码和出库单均可追溯，电脑端负责配置和预览，手机端负责现场蓝牙标签打印。',
    route: '/function/receipt',
    steps: [
      {
        title: '标签模板',
        content: '标签模板页面配置打印内容、条码、二维码、字号和位置。库存标签、订单流转码会读取模板生成打印任务。'
      },
      {
        title: '出库单打印',
        content: '出库单打印页面选择待打印单据，可在打印前修正业务内容并保存，保存后会回写单据用于后续追溯。'
      },
      {
        title: '打印确认',
        content: '小程序完成蓝牙打印或网页完成出库单打印后，需要确认已打印。失败、跳过和补打都会记录状态，便于排查。'
      }
    ],
    tips: ['电脑端负责配置模板', '标签打印统一使用现场蓝牙打印', '出库单适合浏览器连接纸张打印机']
  },
  {
    id: 'approval',
    icon: 'approval',
    title: '审批中心',
    summary: '订单、财务、请假和离职审批集中处理，支持多个审批人同时参与。',
    route: '/function/approval',
    steps: [
      {
        title: '审批顺序',
        content: '审批中心按订单审批、财务审批、请假审批、离职审批展示。每类审批和入口会显示待处理数量红点。'
      },
      {
        title: '订单审批',
        content: '待收款订单需要进入备料中时发起订单审批。审批通过后订单状态推进，并自动生成订单流转码打印任务。'
      },
      {
        title: '员工相关审批',
        content: '请假审批通过后会同步影响考勤判断；离职审批通过后会调整员工状态，避免离职人员继续使用业务权限。'
      }
    ],
    tips: ['审批通过会形成业务记录', '驳回后请回到对应业务单据调整', '待办通知会同步提醒审批人']
  },
  {
    id: 'attendance',
    icon: 'fingerprint',
    title: '考勤管理',
    summary: '考勤规则由管理员维护，手机端用于员工现场打卡。',
    route: '/function/attendance',
    steps: [
      {
        title: '配置规则',
        content: '设置上班、下班和加班时间段。公司位置需要由管理员维护，手机端按规则进行打卡判断。'
      },
      {
        title: '查看异常',
        content: '考勤管理页展示打卡记录、异常人员和统计结果。总览大盘也会展示今日考勤异常。'
      },
      {
        title: '导出记录',
        content: '列表支持导出当前页或全部页。数据量较大时建议先筛选日期、部门或员工后再导出。'
      }
    ],
    tips: ['请假审批会影响考勤状态', '员工定位异常时先确认手机权限', '考勤数据建议按月归档']
  },
  {
    id: 'quality',
    icon: 'fact_check',
    title: '质量与售后',
    summary: '质量管理用于记录次品、售后、损失金额、责任人和改进方案。',
    route: '/function/badProduct',
    steps: [
      {
        title: '登记质量问题',
        content: '填写关联订单、质量类型、异常数量、损失金额、负责人、处理措施和改进方案，便于后续复盘。'
      },
      {
        title: '售后管理',
        content: '售后问题与质量问题统一进入质量管理，区分问题来源、处理进度和客户反馈。'
      },
      {
        title: '附件留痕',
        content: '需要留证的页面支持上传图片或文件。手机端通常上传现场图片，电脑端可上传文档和图片。'
      }
    ],
    tips: ['质量问题尽量关联订单', '处理后补充改进方案', '高频问题应在周会复盘']
  },
  {
    id: 'dashboard',
    icon: 'dashboard',
    title: '总览与通知',
    summary: '总览大盘展示经营概况、企业公告、重要公告、业务提醒和考勤异常。',
    route: '/dashboard',
    steps: [
      {
        title: '查看今日概况',
        content: '进入总览大盘后先看订单、库存、待办、考勤和业务提醒。页面数据用于快速判断当天重点。'
      },
      {
        title: '发布企业通知',
        content: '企业通知公告用于面向员工发布消息，并查看已读和未读状态，方便确认通知是否触达。'
      },
      {
        title: '处理待办预警',
        content: '订单超时、库存水位、待打印、审批事项等需要处理的内容会进入通知/待办，建议每天固定查看。'
      }
    ],
    tips: ['总览用于发现问题，不替代明细页', '点击提醒可进入对应业务页面', '通知未读人员需要及时跟进']
  },
  {
    id: 'import-export',
    icon: 'file_download',
    title: '导入导出',
    summary: '各业务列表支持 Excel 导出，库存、员工等页面支持按模板导入。',
    steps: [
      {
        title: '导入前检查',
        content: '先下载导入说明或模板，确认必填信息、格式、日期和数字单位。导入第三方系统数据时，优先整理信息再上传。'
      },
      {
        title: '导入失败处理',
        content: '如果文件格式、信息或数据不合法，页面会提示错误原因。根据错误行修正后重新上传，不要直接覆盖原始文件。'
      },
      {
        title: '导出数据',
        content: '列表支持导出当前页和全部页。全部页数据较多时建议先加筛选条件，避免生成时间过长。'
      }
    ],
    tips: ['导出文件为标准 Excel 格式', '导入数据不会随意覆盖已有业务数据', '敏感数据请妥善保存']
  },
  {
    id: 'mini',
    icon: 'phone_iphone',
    title: '小程序联动',
    summary: '手机端适合现场人员使用，电脑端适合管理配置和经营分析。',
    steps: [
      {
        title: '登录与加入',
        content: '小程序支持账号密码登录和微信一键登录。新手机号可先登录，但没有加入组织和权限前不能使用业务功能。'
      },
      {
        title: '现场操作',
        content: '员工可在小程序完成考勤打卡、订单扫码流转、库存扫码出入库、质量登记、待办处理和蓝牙打印。'
      },
      {
        title: '双端同步',
        content: '电脑端维护客户、订单、库存、审批和模板后，手机端会按权限展示对应功能。现场操作会同步回电脑端。'
      }
    ],
    tips: ['上线前确认员工可正常登录和访问', '正式使用前确认业务入口正确', '手机端样式会按机型自适应']
  }
]

const DEFAULT_DETAILED_STEPS = {
  'start::1. 登录并修改密码': [
    '电脑端登录后如果系统提示修改初始密码，请先完成新密码设置，再进入业务页面。',
    '密码建议包含大小写字母、数字或符号，不要使用姓名、手机号、生日等容易被猜到的信息。',
    '忘记密码时，在登录页点击忘记密码，通过手机号短信验证码完成身份校验后重置密码。',
    '如果手机号无法收到验证码，先确认员工资料中手机号是否正确，再联系管理员处理。'
  ],
  'start::2. 员工加入组织': [
    '管理员生成组织码后，员工在手机端加入组织页面输入组织码和本人姓名。',
    '组织码有短时有效期，过期后不能继续使用，需要管理员重新生成。',
    '员工填写的姓名会同步到人员信息，便于管理员识别和后续分配权限。',
    '一个手机号默认对应一个员工账号，离职后应由管理员处理离职状态，不建议重复创建新账号。'
  ],
  'start::3. 管理员分配权限': [
    '员工加入组织后默认只具备普通员工基础身份，不能直接访问管理功能。',
    '管理员进入员工管理，编辑员工角色、部门、直属负责人和在职状态。',
    '仓库、销售、生产、财务、审批、质量等角色应按岗位最小权限分配。',
    '分配后让员工重新进入手机端或刷新电脑端，确认功能入口是否出现。'
  ],

  'base::部门与负责人': [
    '先建立部门，再将员工绑定到对应部门，避免后续审批和统计无法按部门归类。',
    '直属负责人用于审批、离职流转、业务责任追踪和组织结构展示。',
    '负责人变更时应同步检查该负责人名下员工，防止待办和审批流转到错误人员。',
    '员工离职时不要直接删除，建议改为已离职，保留历史业务记录可追溯。'
  ],
  'base::角色与权限': [
    '角色代表一组权限，例如普通员工、仓库管理员、订单管理员、审批负责人等。',
    '不要把高级管理权限或关键配置权限分配给普通业务人员。',
    '新建角色后，应先用普通账号验证菜单、操作和小程序入口是否符合预期。',
    '权限调整后，建议让员工重新登录，避免旧登录状态缓存导致看不到最新权限。'
  ],
  'base::客户与项目': [
    '客户名称建议使用营业执照或业务合同中的正式名称，避免同一客户出现多个写法。',
    '同一客户下可维护多个项目，项目负责人用于后续订单跟进和责任归属。',
    '订单录入时如果选择已有客户，项目下拉会优先显示该客户名下项目。',
    '如果订单中输入了新的客户或项目，系统会同步维护到客户资料，方便下次选择。'
  ],

  'order::1. 新建订单': [
    '进入订单管理，点击新建订单，按要求填写客户名称、项目名称、信息渠道、订单小项和商品明细。普通销售订单必须填写信息渠道，图纸预算订单可留空。',
    '客户和项目支持下拉选择；如果没有对应客户或项目，可直接输入新内容，保存后会沉淀到客户管理。',
    '订单小项用于区分样板间、大货和补单，后续统计、筛选和流转都会使用该信息。',
    '品牌、型号、克重、规格、数量和金额尽量填写完整，减少后续生产和出库环节反复确认。',
    '必填项未填写时页面会提示并定位到对应输入框。'
  ],
  'order::2. 审批与状态流转': [
    '新订单一般从待确认开始，确认后进入待收款或后续环节。',
    '待收款订单如果需要转入备料中，必须走订单审批，审批通过后系统才允许状态推进。',
    '审批通过后会自动生成订单流转码打印任务，现场人员打印并贴到订单资料上。',
    '已发货状态需要填写物流公司和物流单号，否则系统会拦截，避免发货信息缺失。',
    '取消订单前应确认是否已经产生库存出库、打印任务或财务记录。'
  ],
  'order::3. 生产工序': [
    '订单进入生产中后，按工序逐步推进，不支持跳级或倒退。',
    '每个工序完成后可通过小程序扫码更新，减少工人手动查找订单。',
    '如果某个工序操作错误，应由有权限的管理人员在电脑端核对后处理。',
    '生产工序完成到成品发货后，订单才适合进入待发货或已发货流程。',
    '工序记录会成为后续交付延误、质量问题和责任复盘的重要依据。'
  ],

  'scan::订单流转码': [
    '一个订单只使用一个流转码，码内只保存业务识别信息，不展示无关技术内容。',
    '流转码不是创建订单时生成，而是在审批通过后自动生成待打印任务。',
    '小程序打印后贴在订单资料、生产单或现场流转卡上。',
    '现场扫码后，系统根据当前订单状态判断下一步动作，生产中则推进具体工序。',
    '如果码丢失或损坏，可在订单列表点击流转码按钮生成补打任务。'
  ],
  'scan::库存条码': [
    '每匹布入库后生成唯一条码，条码代表这一匹布，而不是一个型号总库存。',
    '扫码可以查看型号、规格、总米数、剩余米数、入库时间和最近出库记录。',
    '部分出库后，剩余米数会变化，必须打印新的剩余标签贴回布匹。',
    '如果条码无法识别，先确认标签是否清晰，再在库存详情中按条码或型号搜索。'
  ],
  'scan::设备巡检码': [
    '设备巡检码是固定码，只需打印一次贴在设备明显位置。',
    '员工扫码后填写巡检结果、异常描述和图片，负责人可查看巡检记录。',
    '设备更换、报废或迁移位置时，应同步更新设备资料，避免扫码后关联错误设备。',
    '巡检异常建议进入通知/待办，确保维修或负责人及时处理。'
  ],

  'inventory::入库': [
    '手动入库适合少量新增；批量导入适合客户已有库存一次性迁入。',
    '第三方系统导出的库存文件要先核对信息项，确认型号、规格、米数、条码等信息。',
    '图片识别入库适合现场拍照提取信息，识别结果保存前仍需人工确认。',
    '条码为空时系统自动生成；如果客户已有条码，可录入原条码，但要保证唯一。',
    '入库完成后建议立即打印标签，避免布匹已上架但没有可扫码标签。'
  ],
  'inventory::出库': [
    '出库可按先进先出或先进后出排序，页面文案分别展示为先进先出、先进后出。',
    '扫码出库默认读取当前布匹剩余米数，输入出库米数后系统会校验不能超出可用米数。',
    '整匹出库后该布匹状态变化；部分出库后保留剩余库存并要求补打剩余标签。',
    '出库记录会进入库存流水，可按时间、型号、条码、操作类型进行排查。'
  ],
  'inventory::库存预警': [
    '库存水位由用户自己设置，例如某型号低于 100 米预警。',
    '系统不做无意义轮询，而是在查询库存、进入页面或业务触发时按需判断。',
    '预警会进入待办/通知，负责人可以根据型号、剩余米数和近期出库情况安排补货。',
    '低频型号可设置较低水位，高频型号建议设置更高水位，避免热销型号断货。'
  ],

  'print::标签模板': [
    '进入标签模板页面，可配置文本、条码、二维码、字号、宽高和内容位置。',
    '库存标签、订单流转码和其他标签应使用业务信息，不要把无关内容打印给客户。',
    '模板调整后建议用一张预览标签确认位置、字体和扫描效果。',
    '如果打印偏移，优先检查模板尺寸、打印机纸张尺寸和小程序打印参数。'
  ],
  'print::出库单打印': [
    '出库单打印页面会列出待打印出库单，选择单据后右侧预览。',
    '打印前可修正客户名称、项目、明细、米数、单价、金额、物流和业务日期。',
    '修正保存后会回写单据并保留追溯记录，方便后续核对是谁在什么时间修改了内容。',
    '出库单适合使用浏览器连接纸张打印机，不走小程序蓝牙。'
  ],
  'print::打印确认': [
    '打印成功后点击确认已打印，系统会把任务移出待打印队列。',
    '打印失败时保留失败状态和错误信息，方便重新打印或排查设备问题。',
    '补打不应改变原始业务单据，只新增打印任务和打印记录。',
    '如果连续打印失败，先检查蓝牙连接、纸张、模板尺寸和打印机电量。'
  ],

  'approval::审批顺序': [
    '审批中心按订单审批、财务审批、请假审批、离职审批排列。',
    '审批入口和每个审批类型都会显示待处理数量红点。',
    '多个审批人可同时看到待办，系统会校验当前用户是否有审批资格。',
    '已处理的审批不会重复处理，避免多人重复审批同一单据。'
  ],
  'approval::订单审批': [
    '待收款订单转备料中时，需要走订单审批。',
    '审批通过后订单进入下一状态，同时自动生成订单流转码待打印任务。',
    '审批驳回后应回到订单管理修改客户、金额、交付或备注等业务信息。',
    '订单审批记录用于复盘为什么某个订单进入后续阶段或被拦截。'
  ],
  'approval::员工相关审批': [
    '请假审批通过后会同步到考勤判断，避免员工请假当天被误判为缺勤。',
    '财务审批用于费用、采购、报销等资金事项，建议附加说明和附件。',
    '离职审批通过后员工状态会调整，管理员应及时回收业务权限。',
    '涉及多级负责人时，按组织关系和权限自动流转给合适审批人。'
  ],

  'attendance::配置规则': [
    '考勤规则包括上班打卡时间段、下班打卡时间段和加班时间段。',
    '公司位置由管理员维护，员工打卡时按位置进行判断。',
    '规则修改后建议通知员工，避免员工按旧时间打卡导致异常。',
    '如果企业有多个地点，建议先统一规则，再根据实际业务扩展地点配置。'
  ],
  'attendance::查看异常': [
    '考勤页面可按日期、员工、部门查看打卡记录和异常。',
    '今日考勤异常会在总览大盘展示，方便负责人当天处理。',
    '员工反馈定位异常时，先确认手机定位权限、网络和小程序版本。',
    '请假、外勤或特殊情况应走审批或备注，不建议直接改原始打卡记录。'
  ],
  'attendance::导出记录': [
    '导出前建议先选择月份、部门或员工，减少文件生成时间。',
    '导出支持当前页和全部页，大数据量全部导出可能需要等待。',
    '导出的 Excel 用于对账、工资核算或线下归档，应妥善保存。',
    '如果导出失败，先缩小筛选范围，再联系管理员查看错误提示。'
  ],

  'quality::登记质量问题': [
    '质量问题建议尽量关联订单，方便从客户、项目、型号和生产环节追溯原因。',
    '数量、损失金额、责任人、处理措施和改进方案越完整，复盘价值越高。',
    '负责人应及时更新处理结果，避免质量记录只登记不闭环。',
    '严重质量问题可同步进入通知或待办，提醒相关负责人处理。'
  ],
  'quality::售后管理': [
    '售后问题可记录客户反馈、处理方式、补偿方案和最终结果。',
    '售后与质量统一管理，便于区分内部质量问题和客户服务问题。',
    '涉及金额或赔付的售后问题，建议关联财务审批或备注资金影响。',
    '高频售后问题应按客户、型号和生产工序维度复盘。'
  ],
  'quality::附件留痕': [
    '电脑端可上传图片、文档或表格；手机端通常上传现场图片。',
    '附件用于证明问题、处理过程和客户反馈，不要上传无关或敏感文件。',
    '上传失败时检查文件大小、格式和网络，再重新上传。',
    '文件统一进入系统上传目录管理，下载和查看都走系统权限校验。'
  ],

  'dashboard::查看今日概况': [
    '总览大盘适合每天开工前快速查看订单、库存、待办和考勤。',
    '大盘只展示关键趋势和异常，不替代订单、库存等明细页面。',
    '切换页面后回来如果看到空白或加载慢，可先刷新页面确认网络和服务状态。',
    '经营指标需要真实业务数据支撑，新系统刚启用时部分图表为空是正常情况。'
  ],
  'dashboard::发布企业通知': [
    '企业通知公告用于发布制度、生产安排、重要提醒和临时事项。',
    '通知发布后可以查看已读和未读人员，便于确认是否触达。',
    '重要通知建议内容简洁、责任明确，并注明截止时间或处理要求。',
    '未读人员较多时，负责人应线下或小程序再次提醒。'
  ],
  'dashboard::处理待办预警': [
    '待办包括审批、库存低水位、订单长时间未更新、待打印出库单等。',
    '预警不是错误，而是提醒负责人及时处理业务风险。',
    '处理后应更新订单、库存、审批或打印状态，让待办自然关闭。',
    '如果预警重复出现，说明规则或业务流程需要复盘优化。'
  ],

  'import-export::导入前检查': [
    '导入前先下载导入说明，确认哪些信息必填、哪些信息可选。',
    '日期建议统一为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss，数字信息不要带中文单位。',
    '第三方系统导出的库存、员工或客户数据，建议先另存为 xlsx。',
    '导入前保留原始文件副本，避免处理失败后无法恢复原始数据。'
  ],
  'import-export::导入失败处理': [
    '系统会尽量提示具体错误原因，例如信息缺失、数值不合法、重复条码、日期格式错误。',
    '根据错误提示修正对应行，不要盲目删除数据或覆盖线上数据。',
    '如果错误较多，先导入少量样例验证格式，再批量导入。',
    '重要历史数据导入前建议先在独立环境验证一次。'
  ],
  'import-export::导出数据': [
    '当前页导出适合快速查看；全部页导出适合归档和对账。',
    '全部页导出会生成标准 Excel，避免浏览器表格格式导致文件打不开。',
    '如果数据量很大，先按日期、状态、客户、型号等条件筛选。',
    '导出的文件包含业务数据，请按公司数据安全要求保存。'
  ],

  'mini::登录与加入': [
    '微信一键登录授权手机号后，系统会查找或创建用户信息。',
    '没有加入组织的用户只能看到加入组织入口，不能使用业务功能。',
    '加入组织时填写真实姓名，管理员审核或分配权限后才能看到对应菜单。',
    '账号密码登录与电脑端账号体系对齐，不再要求用户输入额外组织信息。'
  ],
  'mini::现场操作': [
    '考勤打卡适合员工每天使用，订单扫码适合生产和发货现场使用。',
    '库存扫码出入库适合仓库人员，质量登记适合质检或售后人员。',
    '蓝牙打印用于库存标签和订单流转码，电脑端负责生成任务和模板。',
    '现场操作失败时先看提示，再确认网络、权限、扫码内容和打印机连接。'
  ],
  'mini::双端同步': [
    '电脑端创建或修改的订单、库存、模板、权限会同步影响手机端展示。',
    '手机端产生的打卡、扫码、打印和质量记录会同步回电脑端。',
    '如果双端显示不一致，优先重新登录或下拉刷新小程序页面。',
    '上线前应确认员工使用的是正式入口。'
  ]
}

const DEFAULT_FAQS = [
  {
    question: '为什么我看不到某个菜单？',
    answer: '菜单和操作都受角色权限控制。请联系管理员确认员工已加入组织、状态为在职，并绑定了对应角色。'
  },
  {
    question: '小程序显示已加入但仍提示无权限怎么办？',
    answer: '加入组织只代表账号已进入企业，不代表拥有业务权限。需要管理员在员工管理中分配普通员工或对应岗位角色。'
  },
  {
    question: '为什么订单流转码不是创建订单时就打印？',
    answer: '订单流转码在审批通过后自动生成，避免未确认订单提前进入现场流转。需要补打时可在订单列表点击流转码按钮。'
  },
  {
    question: '生产中订单为什么不能跳过工序？',
    answer: '生产工序需要按顺序推进，避免现场扫码直接跳到后续环节导致责任和进度不可追溯。'
  },
  {
    question: '部分出库后为什么要重新打印标签？',
    answer: '部分出库后原标签上的米数已经不准确，必须给剩余布匹重新贴标签，后续扫码才能得到正确可用米数。'
  },
  {
    question: '导入文件打不开或提示格式不对怎么办？',
    answer: '请确认文件为标准 Excel 格式，不要把网页表格伪装成 xls。建议先下载导入说明，再用 Excel 另存为 xlsx 后导入。'
  },
  {
    question: '微信一键登录为什么不需要短信验证码？',
    answer: '微信一键登录使用小程序手机号授权能力，系统校验手机号后匹配用户，不走短信验证码。'
  },
  {
    question: '大盘数据为空怎么办？',
    answer: '先确认是否已有真实订单、库存、审批或考勤数据；再确认提醒任务和服务是否正常。没有业务数据时，大盘会展示空状态。'
  },
  {
    question: '打印失败怎么办？',
    answer: '先确认小程序蓝牙连接、打印机纸张、模板尺寸和任务状态。失败任务会保留，可重新打印或由管理员补打。'
  },
  {
    question: '线上访问异常或数据不对怎么办？',
    answer: '先刷新页面并确认当前账号权限。如果仍异常，请提供页面名称、操作时间、业务编号和错误提示，方便定位日志。'
  }
]

const MANUAL_CONFIG_TYPE = 'hive-full-manual'
const manualConfig = ref(createDefaultManualConfig())
const manualEditor = ref(createManualEditorState())

const quickGuides = computed(() => manualConfig.value.quickGuides || [])
const sections = computed(() => manualConfig.value.sections || [])
const faqs = computed(() => manualConfig.value.faqs || [])

function createManualEditorState() {
  return {
    open: false,
    type: '',
    title: '',
    index: -1,
    fields: [],
    form: {}
  }
}

function createDefaultManualConfig() {
  const defaultSections = cloneManual(DEFAULT_SECTIONS).map((section) => ({
    ...section,
    steps: (section.steps || []).map((step) => ({
      ...step,
      details: cloneManual(DEFAULT_DETAILED_STEPS[`${section.id}::${step.title}`] || [])
    }))
  }))

  return {
    type: MANUAL_CONFIG_TYPE,
    version: 1,
    hero: {
      kicker: '使用手册',
      title: '蜂巢 Hive 操作指南',
      intro: '按照“基础资料先完善、现场扫码少录入、审批打印可追溯、异常预警集中处理”的方式使用，能让订单、库存、考勤、审批、质量和打印形成稳定闭环。',
      badges: ['订单流转', '库存扫码', '审批待办', '打印追溯', '经营提醒'],
      cardIcon: 'route',
      cardTitle: '推荐首次使用顺序',
      cardText: '账号登录 → 修改初始密码 → 员工/部门/角色 → 客户项目 → 订单 → 库存入库 → 模板打印 → 审批与通知'
    },
    quickGuides: cloneManual(DEFAULT_QUICK_GUIDES),
    sections: defaultSections,
    faqWindow: {
      title: '常见问题',
      summary: '遇到登录、权限、扫码、打印、导入导出或数据不一致时，优先从这里排查。'
    },
    faqs: cloneManual(DEFAULT_FAQS),
    customManual: {
      title: '企业自定义手册',
      summary: '这里用于维护企业自己的操作约定、岗位分工、打印规范和内部注意事项，保存后同组织用户可见。',
      label: '内部补充说明',
      helper: '建议写给员工能直接照做的步骤，不放内部编号、密钥、服务地址等敏感信息。',
      content: ''
    }
  }
}

function cloneManual(value) {
  return JSON.parse(JSON.stringify(value))
}

function normalizeManualConfig(content) {
  const defaults = createDefaultManualConfig()
  const source = String(content || '').trim()
  if (!source) {
    return defaults
  }

  let parsed = null
  try {
    parsed = JSON.parse(source)
  } catch {
    defaults.customManual.content = source
    return defaults
  }

  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    defaults.customManual.content = source
    return defaults
  }

  const looksStructured = parsed.type === MANUAL_CONFIG_TYPE || parsed.hero || parsed.quickGuides || parsed.sections || parsed.faqWindow || parsed.faqs || parsed.customManual
  if (!looksStructured) {
    defaults.customManual.content = source
    return defaults
  }

  return {
    type: MANUAL_CONFIG_TYPE,
    version: 1,
    hero: normalizeHero(parsed.hero, defaults.hero),
    quickGuides: normalizeQuickGuides(parsed.quickGuides, defaults.quickGuides),
    sections: normalizeSections(parsed.sections, defaults.sections, parsed.detailedSteps),
    faqWindow: normalizeFaqWindow(parsed.faqWindow, defaults.faqWindow),
    faqs: normalizeFaqs(parsed.faqs, defaults.faqs),
    customManual: normalizeCustomManual(parsed.customManual, defaults.customManual)
  }
}

function normalizeHero(hero, fallback) {
  return {
    kicker: cleanText(hero?.kicker, fallback.kicker),
    title: cleanText(hero?.title, fallback.title),
    intro: cleanText(hero?.intro, fallback.intro),
    badges: normalizeTextArray(hero?.badges, fallback.badges),
    cardIcon: cleanText(hero?.cardIcon, fallback.cardIcon),
    cardTitle: cleanText(hero?.cardTitle, fallback.cardTitle),
    cardText: cleanText(hero?.cardText, fallback.cardText)
  }
}

function normalizeQuickGuides(items, fallback) {
  const source = Array.isArray(items) && items.length ? items : fallback
  return source.map((item, index) => {
    const base = fallback[index] || { icon: 'article', title: `快捷入口${index + 1}`, desc: '' }
    return {
      icon: cleanText(item?.icon, base.icon),
      title: cleanText(item?.title, base.title),
      desc: cleanText(item?.desc, base.desc)
    }
  })
}

function normalizeSections(items, fallback, legacyDetails = {}) {
  const source = Array.isArray(items) && items.length ? items : fallback
  return source.map((item, index) => {
    const base = fallback[index] || {
      id: `manual-section-${index + 1}`,
      icon: 'article',
      title: `章节${index + 1}`,
      summary: '',
      route: '',
      steps: [],
      tips: []
    }
    const id = cleanText(item?.id, base.id || `manual-section-${index + 1}`)

    return {
      id,
      icon: cleanText(item?.icon, base.icon),
      title: cleanText(item?.title, base.title),
      summary: cleanText(item?.summary, base.summary),
      route: typeof item?.route === 'string' ? item.route.trim() : (base.route || ''),
      steps: normalizeSteps(item?.steps, base.steps || [], id, base.id, legacyDetails),
      tips: normalizeTextArray(item?.tips, base.tips || [])
    }
  })
}

function normalizeSteps(items, fallback, sectionId, fallbackSectionId, legacyDetails = {}) {
  const source = Array.isArray(items) && items.length ? items : fallback
  return source.map((item, index) => {
    const base = fallback[index] || { title: `步骤${index + 1}`, content: '', details: [] }
    const title = cleanText(item?.title, base.title)
    const details = item?.details
      || legacyDetails?.[`${sectionId}::${title}`]
      || legacyDetails?.[`${fallbackSectionId}::${base.title}`]
      || base.details
      || []

    return {
      title,
      content: cleanText(item?.content, base.content),
      details: normalizeTextArray(details, [])
    }
  })
}

function normalizeFaqs(items, fallback) {
  const source = Array.isArray(items) && items.length ? items : fallback
  return source.map((item, index) => {
    const base = fallback[index] || { question: `问题${index + 1}`, answer: '' }
    return {
      question: cleanText(item?.question, base.question),
      answer: cleanText(item?.answer, base.answer)
    }
  })
}

function normalizeFaqWindow(value, fallback) {
  return {
    title: cleanText(value?.title, fallback.title),
    summary: cleanText(value?.summary, fallback.summary)
  }
}

function normalizeCustomManual(value, fallback) {
  return {
    title: cleanText(value?.title, fallback.title),
    summary: cleanText(value?.summary, fallback.summary),
    label: cleanText(value?.label, fallback.label),
    helper: cleanText(value?.helper, fallback.helper),
    content: typeof value?.content === 'string' ? value.content : ''
  }
}

function cleanText(value, fallback = '') {
  const text = String(value ?? '').trim()
  return text || fallback
}

function normalizeTextArray(value, fallback = []) {
  if (Array.isArray(value)) {
    const items = value.map((item) => String(item ?? '').trim()).filter(Boolean)
    return items
  }
  if (typeof value === 'string') {
    const items = splitTextLines(value)
    return items
  }
  return cloneManual(fallback)
}

function splitTextLines(value) {
  return String(value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function openManualEditor(options) {
  manualEditor.value = {
    open: true,
    type: options.type,
    title: options.title,
    index: typeof options.index === 'number' ? options.index : -1,
    fields: options.fields || [],
    form: { ...options.form }
  }
}

function closeManualEditor() {
  manualEditor.value = createManualEditorState()
}

function openHeroEditor() {
  const hero = manualConfig.value.hero
  openManualEditor({
    type: 'hero',
    title: '编辑顶部说明',
    fields: [
      { key: 'kicker', label: '角标' },
      { key: 'title', label: '标题' },
      { key: 'intro', label: '说明', type: 'textarea', rows: 4 },
      { key: 'badgesText', label: '标签', type: 'textarea', rows: 4, hint: '每行一个标签' },
      { key: 'cardIcon', label: '右侧图标' },
      { key: 'cardTitle', label: '右侧标题' },
      { key: 'cardText', label: '右侧说明', type: 'textarea', rows: 4 }
    ],
    form: {
      kicker: hero.kicker,
      title: hero.title,
      intro: hero.intro,
      badgesText: (hero.badges || []).join('\n'),
      cardIcon: hero.cardIcon,
      cardTitle: hero.cardTitle,
      cardText: hero.cardText
    }
  })
}

function openQuickGuideEditor(index) {
  const item = quickGuides.value[index]
  openManualEditor({
    type: 'quick-guide',
    title: '编辑快捷卡片',
    index,
    fields: [
      { key: 'icon', label: '图标' },
      { key: 'title', label: '标题' },
      { key: 'desc', label: '说明', type: 'textarea', rows: 4 }
    ],
    form: {
      icon: item?.icon || 'article',
      title: item?.title || '',
      desc: item?.desc || ''
    }
  })
}

function openSectionEditor(index) {
  const section = sections.value[index]
  openManualEditor({
    type: 'section',
    title: '编辑手册章节',
    index,
    fields: [
      { key: 'icon', label: '图标' },
      { key: 'title', label: '章节标题' },
      { key: 'summary', label: '章节说明', type: 'textarea', rows: 3 },
      { key: 'route', label: '跳转路径' },
      { key: 'stepsText', label: '操作步骤', type: 'textarea', rows: 7, hint: '每行一个步骤，格式：步骤标题 | 步骤说明' },
      { key: 'detailsText', label: '步骤明细', type: 'textarea', rows: 8, hint: '每行一个明细，格式：步骤标题 | 明细内容' },
      { key: 'tipsText', label: '提示标签', type: 'textarea', rows: 4, hint: '每行一个提示' }
    ],
    form: {
      icon: section?.icon || 'article',
      title: section?.title || '',
      summary: section?.summary || '',
      route: section?.route || '',
      stepsText: formatStepsForEditor(section?.steps || []),
      detailsText: formatDetailsForEditor(section?.steps || []),
      tipsText: (section?.tips || []).join('\n')
    }
  })
}

function openCustomManualWindowEditor() {
  const customManual = manualConfig.value.customManual
  openManualEditor({
    type: 'custom-window',
    title: '编辑自定义手册窗口',
    fields: [
      { key: 'title', label: '标题' },
      { key: 'summary', label: '说明', type: 'textarea', rows: 4 },
      { key: 'label', label: '编辑区标题' },
      { key: 'helper', label: '编辑区说明', type: 'textarea', rows: 4 }
    ],
    form: {
      title: customManual.title,
      summary: customManual.summary,
      label: customManual.label,
      helper: customManual.helper
    }
  })
}

function openFaqEditor(index) {
  const faq = faqs.value[index]
  openManualEditor({
    type: 'faq',
    title: '编辑常见问题',
    index,
    fields: [
      { key: 'question', label: '问题' },
      { key: 'answer', label: '回答', type: 'textarea', rows: 6 }
    ],
    form: {
      question: faq?.question || '',
      answer: faq?.answer || ''
    }
  })
}

function openFaqWindowEditor() {
  const faqWindow = manualConfig.value.faqWindow
  openManualEditor({
    type: 'faq-window',
    title: '编辑常见问题窗口',
    fields: [
      { key: 'title', label: '标题' },
      { key: 'summary', label: '说明', type: 'textarea', rows: 4 }
    ],
    form: {
      title: faqWindow.title,
      summary: faqWindow.summary
    }
  })
}

function openNewFaqEditor() {
  openManualEditor({
    type: 'faq-create',
    title: '新增常见问题',
    fields: [
      { key: 'question', label: '问题' },
      { key: 'answer', label: '回答', type: 'textarea', rows: 6 }
    ],
    form: {
      question: '',
      answer: ''
    }
  })
}

function formatStepsForEditor(steps) {
  return steps.map((step) => `${step.title || ''} | ${step.content || ''}`).join('\n')
}

function formatDetailsForEditor(steps) {
  return steps
    .flatMap((step) => (step.details || []).map((detail) => `${step.title || ''} | ${detail}`))
    .join('\n')
}

async function saveManualEditor() {
  const editor = manualEditor.value
  if (!editor.open || customManualSaving.value) {
    return
  }
  applyManualEditor(editor)
  const saved = await saveManualConfig('使用手册已保存')
  if (saved) {
    closeManualEditor()
  }
}

function applyManualEditor(editor) {
  const form = editor.form || {}

  if (editor.type === 'hero') {
    manualConfig.value = {
      ...manualConfig.value,
      hero: {
        kicker: cleanText(form.kicker, manualConfig.value.hero.kicker),
        title: cleanText(form.title, manualConfig.value.hero.title),
        intro: cleanText(form.intro, manualConfig.value.hero.intro),
        badges: normalizeTextArray(form.badgesText, manualConfig.value.hero.badges),
        cardIcon: cleanText(form.cardIcon, manualConfig.value.hero.cardIcon),
        cardTitle: cleanText(form.cardTitle, manualConfig.value.hero.cardTitle),
        cardText: cleanText(form.cardText, manualConfig.value.hero.cardText)
      }
    }
    return
  }

  if (editor.type === 'quick-guide') {
    const items = cloneManual(quickGuides.value)
    items[editor.index] = {
      icon: cleanText(form.icon, 'article'),
      title: cleanText(form.title, `快捷入口${editor.index + 1}`),
      desc: cleanText(form.desc, '')
    }
    manualConfig.value = { ...manualConfig.value, quickGuides: items }
    return
  }

  if (editor.type === 'section') {
    const items = cloneManual(sections.value)
    const current = items[editor.index] || {}
    const steps = parseStepsEditor(form.stepsText, current.steps || [])
    const detailsMap = parseStepDetailsEditor(form.detailsText)
    items[editor.index] = {
      ...current,
      icon: cleanText(form.icon, current.icon || 'article'),
      title: cleanText(form.title, current.title || `章节${editor.index + 1}`),
      summary: cleanText(form.summary, current.summary || ''),
      route: typeof form.route === 'string' ? form.route.trim() : '',
      steps: steps.map((step) => ({
        ...step,
        details: detailsMap.get(step.title) || []
      })),
      tips: normalizeTextArray(form.tipsText, [])
    }
    manualConfig.value = { ...manualConfig.value, sections: items }
    return
  }

  if (editor.type === 'custom-window') {
    manualConfig.value = {
      ...manualConfig.value,
      customManual: {
        ...manualConfig.value.customManual,
        title: cleanText(form.title, manualConfig.value.customManual.title),
        summary: cleanText(form.summary, manualConfig.value.customManual.summary),
        label: cleanText(form.label, manualConfig.value.customManual.label),
        helper: cleanText(form.helper, manualConfig.value.customManual.helper)
      }
    }
    return
  }

  if (editor.type === 'faq-window') {
    manualConfig.value = {
      ...manualConfig.value,
      faqWindow: {
        title: cleanText(form.title, manualConfig.value.faqWindow.title),
        summary: cleanText(form.summary, manualConfig.value.faqWindow.summary)
      }
    }
    return
  }

  if (editor.type === 'faq' || editor.type === 'faq-create') {
    const items = cloneManual(faqs.value)
    const faq = {
      question: cleanText(form.question, `问题${items.length + 1}`),
      answer: cleanText(form.answer, '')
    }
    if (editor.type === 'faq-create') {
      items.push(faq)
    } else {
      items[editor.index] = faq
    }
    manualConfig.value = { ...manualConfig.value, faqs: items }
  }
}

function parseStepsEditor(value, fallbackSteps) {
  const lines = splitTextLines(value)
  if (!lines.length) {
    return cloneManual(fallbackSteps).map((step) => ({ ...step, details: [] }))
  }

  return lines.map((line, index) => {
    const [rawTitle, ...contentParts] = line.split('|')
    return {
      title: cleanText(rawTitle, `步骤${index + 1}`),
      content: cleanText(contentParts.join('|'), fallbackSteps[index]?.content || ''),
      details: []
    }
  })
}

function parseStepDetailsEditor(value) {
  const detailsMap = new Map()
  splitTextLines(value).forEach((line) => {
    const [rawTitle, ...detailParts] = line.split('|')
    const title = cleanText(rawTitle, '')
    const detail = cleanText(detailParts.join('|'), '')
    if (!title || !detail) {
      return
    }
    const existing = detailsMap.get(title) || []
    existing.push(detail)
    detailsMap.set(title, existing)
  })
  return detailsMap
}

function createManualPayload() {
  return {
    ...cloneManual(manualConfig.value),
    type: MANUAL_CONFIG_TYPE,
    version: 1,
    customManual: {
      ...cloneManual(manualConfig.value.customManual),
      content: customManualDraft.value || ''
    }
  }
}

async function saveManualConfig(successMessage = '使用手册已保存') {
  if (customManualSaving.value) {
    return false
  }
  customManualSaving.value = true
  try {
    const payload = createManualPayload()
    const content = JSON.stringify(payload)
    const manual = await saveCustomManualContent(content)
    manualConfig.value = normalizeManualConfig(manual?.content || content)
    customManualDraft.value = manualConfig.value.customManual.content || ''
    customManualSavedAt.value = manual?.savedAt || formatLocalDateTime(new Date())
    ElMessage.success(successMessage)
    return true
  } catch {
    ElMessage.error('保存失败，请稍后重试')
    return false
  } finally {
    customManualSaving.value = false
  }
}

function buildManualMarkdown() {
  const manual = createManualPayload()
  const lines = [
    `# ${manual.hero.title}`,
    '',
    manual.hero.intro,
    '',
    `> ${manual.hero.cardTitle}：${manual.hero.cardText}`,
    ''
  ]

  if (manual.hero.badges?.length) {
    lines.push(`标签：${manual.hero.badges.join('、')}`, '')
  }

  lines.push('## 快捷说明', '')
  manual.quickGuides.forEach((item) => {
    lines.push(`### ${item.title}`, item.desc, '')
  })

  manual.sections.forEach((section) => {
    lines.push(`## ${section.title}`, '', section.summary, '')
    section.steps.forEach((step) => {
      lines.push(`### ${step.title}`, step.content)
      ;(step.details || []).forEach((detail) => lines.push(`- ${detail}`))
      lines.push('')
    })
    if (section.tips?.length) {
      lines.push(`提示：${section.tips.join('、')}`, '')
    }
  })

  if (manual.customManual.content?.trim()) {
    lines.push(`## ${manual.customManual.title}`, '', manual.customManual.content.trim(), '')
  }

  if (manual.faqs?.length) {
    lines.push('## 常见问题', '')
    manual.faqs.forEach((faq) => {
      lines.push(`### ${faq.question}`, faq.answer, '')
    })
  }

  return lines.join('\n').replace(/\n{3,}/g, '\n\n')
}

function scrollToSection(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goRoute(path) {
  router.push(path)
}

function stepDetailList(sectionId, step) {
  if (typeof step === 'string') {
    return []
  }
  return Array.isArray(step?.details) ? step.details.filter(Boolean) : []
}

async function loadCustomManual() {
  customManualLoading.value = true
  try {
    const manual = await getCustomManual()
    manualConfig.value = normalizeManualConfig(manual?.content || '')
    customManualDraft.value = manualConfig.value.customManual.content || ''
    customManualSavedAt.value = manual?.savedAt || ''
  } catch {
    ElMessage.warning('企业自定义手册加载失败，请稍后重试')
  } finally {
    customManualLoading.value = false
  }
}

async function saveCustomManual() {
  await saveManualConfig('企业自定义手册已保存')
}

function resetCustomManualTemplate() {
  if (customManualDraft.value.trim() && !window.confirm('当前内容会被推荐模板覆盖，确认继续吗？')) {
    return
  }
  customManualDraft.value = recommendedCustomManual
  ElMessage.success('已填入推荐模板，可继续编辑后保存')
}

function exportCustomManual() {
  const content = buildManualMarkdown().trim()
  if (!content) {
    ElMessage.warning('暂无可导出的手册内容')
    return
  }
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `企业自定义使用手册_${formatFileDate(new Date())}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('使用手册已导出')
}

function clearCustomManual() {
  if (!customManualDraft.value.trim()) {
    return
  }
  if (!window.confirm('确认清空当前企业自定义手册吗？')) {
    return
  }
  customManualDraft.value = ''
  saveCustomManual()
}

function formatLocalDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatFileDate(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}`
}
</script>

<style scoped>
.manual-page {
  min-height: 100%;
  padding: clamp(1rem, 2vw, 2rem);
  color: #0f172a;
}

.manual-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(18rem, 28rem);
  gap: 1.5rem;
  align-items: stretch;
  padding: clamp(1.5rem, 4vw, 3rem);
  border: 1px solid rgba(200, 211, 223, 0.58);
  border-radius: 2rem;
  background:
    radial-gradient(circle at 78% 18%, rgba(31, 63, 95, 0.18), transparent 28%),
    linear-gradient(135deg, rgba(251, 252, 254, 0.98), rgba(238, 244, 251, 0.92));
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.10);
}

.manual-hero__copy,
.manual-card,
.manual-section {
  position: relative;
}

.manual-edit-btn,
.manual-inline-edit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.28rem;
  border: 1px solid rgba(31, 63, 95, 0.14);
  border-radius: 999px;
  padding: 0.36rem 0.62rem;
  color: #1f3f5f;
  background: rgba(255, 255, 255, 0.88);
  font-size: 0.76rem;
  font-weight: 950;
  transition: 0.18s ease;
}

.manual-edit-btn .material-symbols-outlined,
.manual-inline-edit .material-symbols-outlined {
  font-size: 1rem;
}

.manual-edit-btn:hover,
.manual-inline-edit:hover {
  transform: translateY(-1px);
  border-color: rgba(31, 63, 95, 0.28);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.10);
}

.manual-hero__edit,
.manual-card__edit {
  position: absolute;
  right: 0;
  top: 0;
}

.manual-kicker {
  display: inline-flex;
  margin-bottom: 1rem;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  background: rgba(31, 63, 95, 0.10);
  color: #1f3f5f;
  font-size: 0.75rem;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.manual-hero h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(2.2rem, 5vw, 4.4rem);
  font-weight: 1000;
  letter-spacing: 0;
  line-height: 0.98;
}

.manual-hero p {
  margin-top: 1rem;
  max-width: 52rem;
  color: #64748b;
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.9;
}

.manual-hero__badges,
.manual-tip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin-top: 1rem;
}

.manual-hero__badges span,
.manual-tip-list span {
  border-radius: 999px;
  border: 1px solid rgba(31, 63, 95, 0.16);
  background: rgba(255, 255, 255, 0.72);
  padding: 0.34rem 0.72rem;
  color: #1f3f5f;
  font-size: 0.76rem;
  font-weight: 900;
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
  box-shadow: inset 0 0 0 1px rgba(200, 211, 223, 0.56);
}

.manual-hero__card .material-symbols-outlined {
  color: #1f3f5f;
  font-size: 3rem;
}

.manual-hero__card strong {
  font-size: 1.2rem;
  font-weight: 1000;
}

.manual-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.manual-card,
.manual-section,
.manual-toc {
  border: 1px solid rgba(200, 211, 223, 0.52);
  background: rgba(251, 252, 254, 0.92);
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
  background: linear-gradient(135deg, #0b1f33, #1f3f5f 58%, #4b7395);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.18);
}

.manual-card h2,
.manual-section h2 {
  margin: 0;
  color: #0f172a;
  font-size: 1.05rem;
  font-weight: 1000;
}

.manual-card p,
.manual-section p,
.manual-step p,
.faq-list p {
  margin: 0.35rem 0 0;
  color: #64748b;
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
  max-height: calc(100vh - 2rem);
  overflow-y: auto;
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
  color: #64748b;
  background: transparent;
  font-size: 0.88rem;
  font-weight: 900;
  text-align: left;
  transition: 0.18s ease;
}

.manual-toc__item:hover {
  color: #0f172a;
  background: #e8eef6;
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

.manual-section__head > div {
  flex: 1;
  min-width: 0;
}

.manual-section__edit {
  flex-shrink: 0;
}

.manual-section__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
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
  color: #0f172a;
  font-size: 0.92rem;
  font-weight: 1000;
}

.manual-detail-list {
  display: grid;
  gap: 0.45rem;
  margin: 0.8rem 0 0;
  padding: 0;
  list-style: none;
}

.manual-detail-list li {
  position: relative;
  padding-left: 1rem;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 650;
  line-height: 1.7;
}

.manual-detail-list li::before {
  position: absolute;
  left: 0;
  top: 0.65em;
  width: 0.35rem;
  height: 0.35rem;
  border-radius: 999px;
  background: #1f3f5f;
  content: '';
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
  background: #0f172a;
  font-weight: 900;
  transition: 0.18s ease;
}

.manual-action button:hover {
  transform: translateY(-1px);
  background: #1f3f5f;
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

.manual-inline-edit {
  margin-top: 0.65rem;
}

.manual-custom-section {
  background:
    radial-gradient(circle at 90% 10%, rgba(31, 63, 95, 0.10), transparent 30%),
    rgba(251, 252, 254, 0.96);
}

.manual-custom-editor {
  display: grid;
  gap: 1rem;
  border-radius: 1.5rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.82);
}

.manual-custom-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.manual-custom-toolbar strong {
  color: #0f172a;
  font-size: 1rem;
  font-weight: 1000;
}

.manual-custom-toolbar p {
  margin-top: 0.25rem;
}

.manual-custom-saved {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 0.36rem 0.72rem;
  background: rgba(31, 63, 95, 0.10);
  color: #1f3f5f;
  font-size: 0.76rem;
  font-weight: 900;
}

.manual-custom-editor textarea {
  width: 100%;
  min-height: 18rem;
  resize: vertical;
  border: 1px solid rgba(200, 211, 223, 0.82);
  border-radius: 1.1rem;
  padding: 1rem;
  color: #0f172a;
  background: rgba(248, 250, 252, 0.92);
  font-size: 0.95rem;
  font-weight: 700;
  line-height: 1.8;
  outline: none;
  transition: 0.18s ease;
}

.manual-custom-editor textarea:focus {
  border-color: rgba(31, 63, 95, 0.55);
  box-shadow: 0 0 0 4px rgba(31, 63, 95, 0.08);
}

.manual-custom-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.65rem;
}

.manual-primary-btn,
.manual-secondary-btn,
.manual-danger-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0.72rem 1.05rem;
  font-weight: 1000;
  transition: 0.18s ease;
}

.manual-primary-btn {
  border: none;
  color: #ffffff;
  background: #0f172a;
}

.manual-secondary-btn {
  border: 1px solid rgba(31, 63, 95, 0.16);
  color: #1f3f5f;
  background: #ffffff;
}

.manual-danger-btn {
  border: 1px solid rgba(239, 68, 68, 0.18);
  color: #b91c1c;
  background: rgba(254, 242, 242, 0.86);
}

.manual-primary-btn:hover,
.manual-secondary-btn:hover,
.manual-danger-btn:hover {
  transform: translateY(-1px);
}

.manual-editor-mask {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.42);
}

.manual-editor-dialog {
  display: grid;
  width: min(42rem, 100%);
  max-height: calc(100vh - 2rem);
  overflow: hidden;
  border-radius: 1.25rem;
  background: #ffffff;
  box-shadow: 0 26px 78px rgba(15, 23, 42, 0.22);
}

.manual-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.86);
  padding: 1rem 1.15rem;
}

.manual-editor-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 1.05rem;
  font-weight: 1000;
}

.manual-editor-head button {
  display: inline-flex;
  width: 2.2rem;
  height: 2.2rem;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  color: #334155;
  background: #f1f5f9;
}

.manual-editor-fields {
  display: grid;
  gap: 0.9rem;
  overflow-y: auto;
  padding: 1.1rem;
}

.manual-editor-field {
  display: grid;
  gap: 0.45rem;
}

.manual-editor-field > span {
  color: #334155;
  font-size: 0.84rem;
  font-weight: 950;
}

.manual-editor-field input,
.manual-editor-field textarea {
  width: 100%;
  border: 1px solid rgba(200, 211, 223, 0.92);
  border-radius: 0.95rem;
  padding: 0.72rem 0.85rem;
  color: #0f172a;
  background: #f8fafc;
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.7;
  outline: none;
  transition: 0.18s ease;
}

.manual-editor-field textarea {
  resize: vertical;
}

.manual-editor-field input:focus,
.manual-editor-field textarea:focus {
  border-color: rgba(31, 63, 95, 0.48);
  box-shadow: 0 0 0 4px rgba(31, 63, 95, 0.08);
}

.manual-editor-field small {
  color: #64748b;
  font-size: 0.76rem;
  font-weight: 750;
}

.manual-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  border-top: 1px solid rgba(226, 232, 240, 0.86);
  padding: 0.9rem 1.1rem;
}

@media (max-width: 1180px) {
  .manual-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
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
    max-height: none;
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

  .manual-custom-toolbar {
    flex-direction: column;
  }

  .manual-custom-actions {
    justify-content: stretch;
  }

  .manual-section__head {
    flex-wrap: wrap;
  }

  .manual-section__edit {
    width: 100%;
  }

  .manual-section__actions {
    width: 100%;
    justify-content: stretch;
  }

  .manual-section__actions .manual-edit-btn {
    flex: 1;
  }

  .manual-hero__edit,
  .manual-card__edit {
    position: static;
    justify-self: flex-start;
    margin-bottom: 0.65rem;
  }

  .manual-editor-dialog {
    max-height: calc(100vh - 1rem);
  }

  .manual-editor-actions {
    flex-direction: column-reverse;
  }

  .manual-primary-btn,
  .manual-secondary-btn,
  .manual-danger-btn {
    width: 100%;
  }
}
</style>
