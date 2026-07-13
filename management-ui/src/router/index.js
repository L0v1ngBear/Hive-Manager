import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '@/layout/index.vue'
import { useUserStore } from '@/stores/user'
import { buildLoginQuery, normalizeLoginRedirect } from '@/utils/redirect'
import { routeAccessDenied } from '@/utils/access'

const ANNOUNCEMENT_PERMISSIONS = [
  'notification:announcement:list',
  'notification:announcement:publish',
  'dashboard:*'
]

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/join-organization',
    name: 'JoinOrganization',
    component: () => import('@/views/JoinOrganization.vue'),
    meta: { public: true, title: '加入组织' }
  },
  {
    path: '/force-password-change',
    name: 'ForcePasswordChange',
    component: () => import('@/views/ForcePasswordChange.vue'),
    meta: { title: '修改初始密码' }
  },
  {
    path: '/privacy',
    name: 'Privacy',
    component: () => import('@/views/legal/LegalPage.vue'),
    meta: { public: true, title: '隐私政策' }
  },
  {
    path: '/terms',
    name: 'Terms',
    component: () => import('@/views/legal/LegalPage.vue'),
    meta: { public: true, title: '服务条款' }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/',
    name: 'Home',
    component: Layout,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '总览大盘', icon: 'dashboard', features: ['module.dashboard'] }
      },
      {
        path: 'manual',
        name: 'UserManual',
        component: () => import('@/views/manual/UserManual.vue'),
        meta: { title: '使用手册', features: ['module.manual'] }
      },
      {
        path: 'no-permission',
        name: 'NoPermission',
        component: () => import('@/views/NoPermission.vue'),
        meta: { title: '暂无权限', allowDenied: true }
      }
    ]
  },
  {
    path: '/function',
    name: 'Function',
    component: Layout,
    children: [
      {
        path: 'announcement',
        name: 'Announcement',
        component: () => import('@/views/function/announcement/announcement.vue'),
        meta: {
          title: '公告查看',
          permissions: ANNOUNCEMENT_PERMISSIONS,
          features: ['module.dashboard']
        }
      },
      {
        path: 'announcement/publish',
        name: 'AnnouncementPublish',
        component: () => import('@/views/function/announcement/publish.vue'),
        meta: {
          title: '发布公告',
          permissions: ['notification:announcement:publish', 'dashboard:*'],
          features: ['module.dashboard']
        }
      },
      {
        path: 'label',
        name: 'Label',
        component: () => import('@/views/function/label.vue'),
        meta: { title: '标签打印', permissions: ['label:template:list'], features: ['module.label'] }
      },
      {
        path: 'receipt',
        name: 'Receipt',
        component: () => import('@/views/function/receipt.vue'),
        meta: { title: '出库单打印', permissions: ['receipt:print:list'], features: ['module.receipt'] }
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('@/views/function/inventory/inventory.vue'),
        meta: {
          title: '库存管理',
          permissions: ['inventory:warning:list', 'inventory:record:recent', 'inventory:cloth:in', 'inventory:cloth:out'],
          features: ['module.inventory']
        }
      },
      {
        path: 'inventory/model-detail',
        name: 'InventoryModelDetail',
        component: () => import('@/views/function/inventory/InventoryModelDetail.vue'),
        meta: {
          title: '单匹布明细',
          permissions: ['inventory:warning:list', 'inventory:record:recent', 'inventory:cloth:out'],
          features: ['module.inventory']
        }
      },
      {
        path: 'price',
        name: 'Price',
        component: () => import('@/views/function/price/price.vue'),
        meta: { title: '价格管理', permissions: ['price:list'], features: ['module.price'] }
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/function/employee/employee.vue'),
        meta: { title: '员工管理', permissions: ['employee:list'], features: ['module.employee'] }
      },
      {
        path: 'organization',
        name: 'Organization',
        component: () => import('@/views/function/organization/organization.vue'),
        meta: { title: '部门管理', permissions: ['employee:list'], features: ['module.employee'] }
      },
      {
        path: 'attendance',
        name: 'Attendance',
        component: () => import('@/views/function/attendance/attendanceManagement.vue'),
        meta: { title: '考勤管理', permissions: ['attendance:record:list', 'attendance:*'], features: ['module.attendance'] }
      },
      {
        path: 'equipment',
        name: 'Equipment',
        component: () => import('@/views/function/equipment/equipment.vue'),
        meta: {
          title: '设备巡检',
          permissions: ['equipment:list', 'equipment:detail', 'equipment:inspection:list'],
          features: ['module.equipment']
        }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/function/role/role.vue'),
        meta: { title: '角色管理', permissions: ['role:list'], features: ['module.role'] }
      },
      {
        path: 'tenant',
        name: 'TenantManage',
        component: () => import('@/views/function/tenant/tenant.vue'),
        meta: { title: '企业授权', developerOnly: true }
      },
      {
        path: 'customer',
        name: 'Customer',
        component: () => import('@/views/function/customer/customer.vue'),
        meta: { title: '客户管理', permissions: ['customer:page'], features: ['module.customer'] }
      },
      {
        path: 'document',
        name: 'Document',
        component: () => import('@/views/function/document/document.vue'),
        meta: { title: '文档管理', permissions: ['document:list'], features: ['module.document'] }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/function/order/order.vue'),
        meta: { title: '订单列表', permissions: ['order:list'], features: ['module.order'] }
      },
      {
        path: 'installation-task',
        name: 'InstallationTask',
        component: () => import('@/views/function/installationTask/installationTask.vue'),
        meta: { title: '安装任务', permissions: ['order:list'], features: ['module.order'] }
      },
      {
        path: 'bad-product',
        name: 'BadProduct',
        component: () => import('@/views/function/badProduct/badProduct.vue'),
        meta: {
          title: '质量管理',
          permissions: ['badproduct:list', 'badproduct:save', 'badproduct:process'],
          features: ['module.badProduct']
        }
      },
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/function/approval/approvalCenter.vue'),
        meta: {
          title: '审批中心',
          permissions: ['approval:leave', 'approval:finance', 'approval:resignation', 'approval:leave:submit', 'approval:finance:submit', 'approval:resignation:submit', 'order:list', 'badproduct:process'],
          features: ['module.approval']
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

const FORCE_PASSWORD_CHANGE_PATH = '/force-password-change'
const PLATFORM_TENANT_PATH = '/function/tenant'
const NO_PERMISSION_PATH = '/no-permission'

router.beforeEach((to) => {
  const userStore = useUserStore()
  const hasToken = Boolean(userStore.token)

  if (to.meta?.public) {
    if (hasToken && to.path === '/login') {
      if (userStore.mustChangePassword) {
        return FORCE_PASSWORD_CHANGE_PATH
      }
      return normalizeLoginRedirect(to.query.redirect)
    }
    return true
  }

  if (!hasToken) {
    return {
      path: '/login',
      query: buildLoginQuery(to.fullPath)
    }
  }

  if (userStore.mustChangePassword && to.path !== FORCE_PASSWORD_CHANGE_PATH) {
    return {
      path: FORCE_PASSWORD_CHANGE_PATH,
      query: { redirect: to.fullPath }
    }
  }

  if (!userStore.mustChangePassword && to.path === FORCE_PASSWORD_CHANGE_PATH) {
    return '/dashboard'
  }

  if (to.path === FORCE_PASSWORD_CHANGE_PATH) {
    return true
  }

  if (to.path === NO_PERMISSION_PATH || to.meta?.allowDenied) {
    return true
  }

  if (userStore.isPlatformTenant && to.path !== PLATFORM_TENANT_PATH) {
    return PLATFORM_TENANT_PATH
  }

  const deniedReason = routeAccessDenied(userStore, to.meta || {})
  if (deniedReason) {
    ElMessage.warning(`${deniedReason}，页面内容已隐藏`)
    return {
      path: NO_PERMISSION_PATH,
      query: { from: to.fullPath }
    }
  }

  return true
})

export default router
