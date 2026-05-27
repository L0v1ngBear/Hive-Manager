<template>
  <section class="manual-page">
    <div class="manual-hero">
      <div class="manual-hero__copy">
        <span class="manual-kicker">使用手册</span>
        <h1>蜂巢 Hive 操作指南</h1>
        <p>
          按照“基础资料先完善、现场扫码少录入、审批打印可追溯、异常预警集中处理”的方式使用，
          能让订单、库存、考勤、审批、质量和打印形成稳定闭环。
        </p>
        <div class="manual-hero__badges">
          <span>订单流转</span>
          <span>库存扫码</span>
          <span>审批待办</span>
          <span>打印追溯</span>
          <span>经营提醒</span>
        </div>
      </div>
      <div class="manual-hero__card">
        <span class="material-symbols-outlined">route</span>
        <strong>推荐首次使用顺序</strong>
        <p>账号登录 → 修改初始密码 → 员工/部门/角色 → 客户项目 → 订单 → 库存入库 → 模板打印 → 审批与通知</p>
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
              <ul v-if="stepDetailList(section.id, step.title).length" class="manual-detail-list">
                <li v-for="detail in stepDetailList(section.id, step.title)" :key="detail">{{ detail }}</li>
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

        <section class="manual-section faq-section">
          <div class="manual-section__head">
            <span class="material-symbols-outlined">help</span>
            <div>
              <h2>常见问题</h2>
              <p>遇到登录、权限、扫码、打印、导入导出或数据不一致时，优先从这里排查。</p>
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

const sections = [
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
        content: '小程序用户可通过组织码加入，加入时需要填写真实姓名。组织码由网页端管理员生成，短时间有效，过期后需重新生成。'
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
        content: '角色管理用于控制菜单和接口权限。普通员工只给基础查看和本人业务权限，管理类权限只分配给对应负责人。'
      },
      {
        title: '客户与项目',
        content: '客户管理中维护客户名称、联系人、电话、项目和项目负责人。订单选择客户后，会优先从客户项目中带出业务信息。'
      }
    ],
    tips: ['客户、项目、员工姓名尽量使用正式名称', '项目负责人会影响后续跟进责任', '字段展示可按企业习惯配置']
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
        content: '填写客户、项目、品牌、订单小项、交付日期和商品明细。订单小项分为样板间、大货和补单，便于后续统计。'
      },
      {
        title: '2. 审批与状态流转',
        content: '待确认、待收款、备料中、生产中、待发货、已发货、已完成按顺序推进；待收款转生产中需要订单审批。'
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
        content: '设备巡检码是固定码，打印一次贴在设备上即可。现场人员扫码后填写巡检结果，管理端可查看巡检记录。'
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
    tips: ['导入第三方库存前先下载字段说明', '导入失败会给出错误原因', '库存流水是排查账实差异的第一依据']
  },
  {
    id: 'print',
    icon: 'print',
    title: '打印与模板',
    summary: '标签、订单流转码和出库单均可追溯，网页端负责配置和预览，小程序负责蓝牙标签打印。',
    route: '/function/receipt',
    steps: [
      {
        title: '标签模板',
        content: '标签模板页面配置字段、条码、二维码、字号和位置。库存标签、订单流转码会读取模板生成打印任务。'
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
    tips: ['网页端不连接蓝牙打印机', '标签打印统一走小程序蓝牙', '出库单适合浏览器连接纸张打印机']
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
        content: '待收款订单需要进入生产中时发起订单审批。审批通过后订单状态推进，并自动生成订单流转码打印任务。'
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
    summary: '考勤规则由管理端维护，小程序用于员工现场打卡。',
    route: '/function/attendance',
    steps: [
      {
        title: '配置规则',
        content: '设置上班、下班和加班时间段。公司经纬度需要在管理端手动维护，小程序按规则进行打卡判断。'
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
        content: '需要留证的页面支持上传图片或文件。小程序端通常上传图片，网页端可上传文档和图片。'
      }
    ],
    tips: ['质量问题尽量关联订单', '处理后补充改进方案', '高频问题应在周会复盘']
  },
  {
    id: 'dashboard',
    icon: 'dashboard',
    title: '总览与通知',
    summary: '总览大盘展示经营概况、企业公告、库存趋势、业务提醒和考勤异常。',
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
    id: 'ai',
    icon: 'psychology',
    title: '经营建议',
    summary: '经营建议面向老板和管理层，用于辅助分析订单、客户、员工、质量和经营风险。',
    route: '/dashboard/ai-advices',
    steps: [
      {
        title: '建议生成',
        content: '系统会按业务数据定期生成建议，也可由管理层进入建议中心查看最新建议。建议内容会尽量聚焦业务问题和处理动作。'
      },
      {
        title: '使用方式',
        content: '先看建议标题和风险等级，再看涉及部门、原因和处理建议。需要执行的事项应转为责任人跟进。'
      },
      {
        title: '持续优化',
        content: '建议处理结果会沉淀为业务反馈，后续用于优化建议质量。经营建议只辅助决策，最终处理以业务负责人判断为准。'
      }
    ],
    tips: ['高权限用户才能查看高维建议', '建议不展示无关技术信息', '重要建议要形成闭环记录']
  },
  {
    id: 'import-export',
    icon: 'file_download',
    title: '导入导出',
    summary: '各业务列表支持 Excel 导出，库存、员工等页面支持按模板导入。',
    steps: [
      {
        title: '导入前检查',
        content: '先下载字段说明或模板，确认必填字段、格式、日期和数字单位。导入第三方系统数据时，优先整理字段再上传。'
      },
      {
        title: '导入失败处理',
        content: '如果文件格式、字段或数据不合法，页面会提示错误原因。根据错误行修正后重新上传，不要直接覆盖原始文件。'
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
    summary: '小程序适合现场人员使用，网页端适合管理配置和经营分析。',
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
        content: '网页端维护客户、订单、库存、审批和模板后，小程序会按权限展示对应功能。现场操作会同步回网页端。'
      }
    ],
    tips: ['发布小程序前确认接口环境', '体验版和正式版应连接公网接口', '小程序样式会按机型自适应']
  },
  {
    id: 'hidden-time',
    icon: 'edit_calendar',
    title: '业务时间修正',
    summary: '少数历史补录或业务修正场景需要调整创建时间，入口默认隐藏。',
    steps: [
      {
        title: '开启方式',
        content: '在订单、库存入库、质量记录或出库单打印编辑区域，按 Ctrl + Alt + T 开启业务时间修正，再按一次关闭。'
      },
      {
        title: '适用场景',
        content: '用于历史单据补录、业务日期修正或打印日期修正。普通新增和编辑不需要使用该模式。'
      },
      {
        title: '注意事项',
        content: '时间修正属于受控操作，请确认业务原因后再保存。不要用它修改无关字段或规避正常审批流程。'
      }
    ],
    tips: ['该入口不会摆在页面明面上', '只给确有需要的管理人员使用', '修正前建议确认原始业务凭证']
  }
]

const detailedSteps = {
  'start::1. 登录并修改密码': [
    '网页端登录后如果系统提示修改初始密码，请先完成新密码设置，再进入业务页面。',
    '密码建议包含大小写字母、数字或符号，不要使用姓名、手机号、生日等容易被猜到的信息。',
    '忘记密码时，在登录页点击忘记密码，通过手机号短信验证码完成身份校验后重置密码。',
    '如果手机号无法收到验证码，先确认员工资料中手机号是否正确，再联系管理员处理。'
  ],
  'start::2. 员工加入组织': [
    '管理员在网页端生成组织码后，员工在小程序加入组织页面输入组织码和本人姓名。',
    '组织码有短时有效期，过期后不能继续使用，需要管理员重新生成。',
    '员工填写的姓名会同步到系统人员信息，便于管理端识别和后续分配权限。',
    '一个手机号默认对应一个员工账号，离职后应由管理员处理离职状态，不建议重复创建新账号。'
  ],
  'start::3. 管理员分配权限': [
    '员工加入组织后默认只具备普通员工基础身份，不能直接访问管理功能。',
    '管理员进入员工管理，编辑员工角色、部门、直属负责人和在职状态。',
    '仓库、销售、生产、财务、审批、质量等角色应按岗位最小权限分配。',
    '分配后让员工重新进入小程序或刷新网页端，确认功能入口是否出现。'
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
    '新建角色后，应先用普通账号验证菜单、接口和小程序入口是否符合预期。',
    '权限调整后，建议让员工重新登录，避免旧登录状态缓存导致看不到最新权限。'
  ],
  'base::客户与项目': [
    '客户名称建议使用营业执照或业务合同中的正式名称，避免同一客户出现多个写法。',
    '同一客户下可维护多个项目，项目负责人用于后续订单跟进和责任归属。',
    '订单录入时如果选择已有客户，项目下拉会优先显示该客户名下项目。',
    '如果订单中输入了新的客户或项目，系统会同步维护到客户资料，方便下次选择。'
  ],

  'order::1. 新建订单': [
    '进入订单管理，点击新建订单，按要求填写客户名称、项目名称、交付日期、订单小项和商品明细。',
    '客户和项目支持下拉选择；如果没有对应客户或项目，可直接输入新内容，保存后会沉淀到客户管理。',
    '订单小项用于区分样板间、大货和补单，后续统计、筛选和流转都会使用该字段。',
    '品牌、型号、克重、规格、数量和金额尽量填写完整，减少后续生产和出库环节反复确认。',
    '必填项未填写时页面会提示并定位到对应输入框。'
  ],
  'order::2. 审批与状态流转': [
    '新订单一般从待确认开始，确认后进入待收款或后续环节。',
    '待收款订单如果需要转入生产中，必须走订单审批，审批通过后系统才允许状态推进。',
    '审批通过后会自动生成订单流转码打印任务，现场人员打印并贴到订单资料上。',
    '已发货状态需要填写物流公司和物流单号，否则系统会拦截，避免发货信息缺失。',
    '取消订单前应确认是否已经产生库存出库、打印任务或财务记录。'
  ],
  'order::3. 生产工序': [
    '订单进入生产中后，按工序逐步推进，不支持跳级或倒退。',
    '每个工序完成后可通过小程序扫码更新，减少工人手动查找订单。',
    '如果某个工序操作错误，应由有权限的管理人员在网页端核对后处理。',
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
    '员工扫码后填写巡检结果、异常描述和图片，管理端可查看巡检记录。',
    '设备更换、报废或迁移位置时，应同步更新设备资料，避免扫码后关联错误设备。',
    '巡检异常建议进入通知/待办，确保维修或负责人及时处理。'
  ],

  'inventory::入库': [
    '手动入库适合少量新增；批量导入适合客户已有库存一次性迁入。',
    '第三方系统导出的库存文件要先核对字段，确认型号、规格、米数、条码等信息。',
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
    '进入标签模板页面，可配置文本、条码、二维码、字号、宽高和字段位置。',
    '库存标签、订单流转码和其他标签应使用业务字段，不要把非业务字段打印给客户。',
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
    '待收款订单转生产中时，需要走订单审批。',
    '审批通过后订单进入下一状态，同时自动生成订单流转码待打印任务。',
    '审批驳回后应回到订单管理修改客户、金额、交付或备注等业务信息。',
    '订单审批记录用于复盘为什么某个订单进入生产或被拦截。'
  ],
  'approval::员工相关审批': [
    '请假审批通过后会同步到考勤判断，避免员工请假当天被误判为缺勤。',
    '财务审批用于费用、采购、报销等资金事项，建议附加说明和附件。',
    '离职审批通过后员工状态会调整，管理员应及时回收业务权限。',
    '涉及多级负责人时，按组织关系和权限自动流转给合适审批人。'
  ],

  'attendance::配置规则': [
    '考勤规则包括上班打卡时间段、下班打卡时间段和加班时间段。',
    '公司经纬度由管理员在管理端手动维护，小程序打卡时按位置进行判断。',
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
    '网页端可上传图片、文档或表格；小程序端通常上传现场图片。',
    '附件用于证明问题、处理过程和客户反馈，不要上传无关或敏感文件。',
    '上传失败时检查文件大小、格式和网络，再重新上传。',
    '后续如果使用 OSS，文件会由统一上传体系管理。'
  ],

  'dashboard::查看今日概况': [
    '总览大盘适合每天开工前快速查看订单、库存、待办和考勤。',
    '大盘只展示关键趋势和异常，不替代订单、库存等明细页面。',
    '切换页面后回来如果看到空白或加载慢，可先刷新页面确认网络和接口状态。',
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

  'ai::建议生成': [
    '经营建议会读取订单、库存、客户、质量、审批等经营数据生成建议。',
    '建议触发不依赖用户频繁点击刷新，而应由系统按规则或调度生成。',
    '智能建议启用后，建议内容会更偏业务分析和处理动作。',
    '页面只展示业务结论、影响范围和处理动作。'
  ],
  'ai::使用方式': [
    '先看风险等级，再看涉及部门和建议动作。',
    '建议不能替代负责人判断，特别是涉及客户承诺、赔付和资金事项。',
    '可把重要建议拆成责任人、截止时间和处理结果，形成管理闭环。',
    '高维建议仅对高权限管理人员展示。'
  ],
  'ai::持续优化': [
    '建议处理结果会沉淀为样本，后续用于优化建议质量。',
    '如果建议不准确，应反馈真实原因和处理结果。',
    '后续可接入更多经营指标，例如员工效率、客户复购、售后率和库存周转。',
    '经营建议质量依赖高质量业务数据，数据越规范建议越可靠。'
  ],

  'import-export::导入前检查': [
    '导入前先下载字段说明，确认哪些字段必填、哪些字段可选。',
    '日期建议统一为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss，数字字段不要带中文单位。',
    '第三方系统导出的库存、员工或客户数据，建议先另存为 xlsx。',
    '导入前保留原始文件副本，避免处理失败后无法恢复原始数据。'
  ],
  'import-export::导入失败处理': [
    '系统会尽量提示具体错误原因，例如字段缺失、数值不合法、重复条码、日期格式错误。',
    '根据错误提示修正对应行，不要盲目删除数据或覆盖线上数据。',
    '如果错误较多，先导入少量样例验证格式，再批量导入。',
    '重要历史数据导入前建议先在独立环境验证一次。'
  ],
  'import-export::导出数据': [
    '当前页导出适合快速查看；全部页导出适合归档和对账。',
    '全部页导出会经过后端生成标准 Excel，避免浏览器伪造表格导致文件打不开。',
    '如果数据量很大，先按日期、状态、客户、型号等条件筛选。',
    '导出的文件包含业务数据，请按公司数据安全要求保存。'
  ],

  'mini::登录与加入': [
    '微信一键登录授权手机号后，后端会查找或创建用户信息。',
    '没有加入组织的用户只能看到加入组织入口，不能使用业务功能。',
    '加入组织时填写真实姓名，管理员审核或分配权限后才能看到对应菜单。',
    '账号密码登录与网页端账号体系对齐，不再要求用户输入额外组织信息。'
  ],
  'mini::现场操作': [
    '考勤打卡适合员工每天使用，订单扫码适合生产和发货现场使用。',
    '库存扫码出入库适合仓库人员，质量登记适合质检或售后人员。',
    '蓝牙打印用于库存标签和订单流转码，网页端只负责生成任务和模板。',
    '现场操作失败时先看提示，再确认网络、权限、扫码内容和打印机连接。'
  ],
  'mini::双端同步': [
    '网页端创建或修改的订单、库存、模板、权限会同步影响小程序展示。',
    '小程序产生的打卡、扫码、打印和质量记录会同步回网页端。',
    '如果双端显示不一致，优先重新登录或下拉刷新小程序页面。',
    '上线前应确认小程序正式版接口指向公网地址。'
  ],

  'hidden-time::开启方式': [
    '打开对应新增或编辑抽屉，按 Ctrl + Alt + T，页面提示已开启业务时间修正。',
    '开启后会出现业务时间输入框，选择需要修正的业务日期或时间。',
    '再次按 Ctrl + Alt + T 会关闭模式；关闭后隐藏时间修正区域。',
    '出库单打印页面如果只修改时间，可点击保存时间。'
  ],
  'hidden-time::适用场景': [
    '历史订单补录，需要把业务时间调整为真实发生时间。',
    '库存历史入库补录，需要修正入库日期。',
    '出库单打印前发现业务日期不准确，需要修正打印业务日期。',
    '质量或售后记录补录，需要记录真实发生时间。'
  ],
  'hidden-time::注意事项': [
    '业务时间修正不是常规编辑功能，不建议普通员工使用。',
    '修正前应确认合同、出入库凭证、客户单据或线下记录。',
    '不要用该功能规避审批、隐藏错误或随意修改业务时间。',
    '如果需要大批量修正历史数据，应先规划导入方案，不要逐条手工改。'
  ]
}

const faqs = [
  {
    question: '为什么我看不到某个菜单？',
    answer: '菜单和接口都受角色权限控制。请联系管理员确认员工已加入组织、状态为在职，并绑定了对应角色。'
  },
  {
    question: '小程序显示已加入但仍提示无权限怎么办？',
    answer: '加入组织只代表账号已进入企业，不代表拥有业务权限。需要管理员在网页端员工管理中分配普通员工或对应岗位角色。'
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
    answer: '请确认文件为标准 Excel 格式，不要把网页表格伪装成 xls。建议先下载字段说明，再用 Excel 另存为 xlsx 后导入。'
  },
  {
    question: '微信一键登录为什么不需要短信验证码？',
    answer: '微信一键登录使用小程序手机号授权能力，微信返回一次性授权 code，后端换取手机号后匹配系统用户，不走短信验证码。'
  },
  {
    question: '库存趋势或大盘数据为空怎么办？',
    answer: '先确认是否已有真实出入库、订单或考勤数据；再确认定时任务和接口是否正常。没有业务数据时，大盘会展示空状态。'
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

function scrollToSection(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goRoute(path) {
  router.push(path)
}

function stepDetailList(sectionId, title) {
  return detailedSteps[`${sectionId}::${title}`] || []
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
  letter-spacing: -0.08em;
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
}
</style>
