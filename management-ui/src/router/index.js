import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '@/layout/index.vue'
import { useUserStore } from '@/stores/user'

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
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
        meta: { title: '总览大盘', icon: 'dashboard' }
      },
      {
        path: 'dashboard/ai-advices',
        name: 'DashboardAiAdvice',
        component: () => import('@/views/dashboard/aiAdvice.vue'),
        meta: { title: 'AI 经营建议' }
      }
    ]
  },
  {
    path: '/function',
    name: 'Function',
    component: Layout,
    children: [
      {
        path: 'label',
        name: 'Label',
        component: () => import('@/views/function/label.vue'),
        meta: { title: '标签打印', permissions: ['label:template:list'] }
      },
      {
        path: 'receipt',
        name: 'Receipt',
        component: () => import('@/views/function/receipt.vue'),
        meta: { title: '出库单打印', permissions: ['receipt:print:list'] }
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('@/views/function/inventory/inventory.vue'),
        meta: { title: '库存管理', permissions: ['inventory:warning:list', 'inventory:record:recent', 'inventory:cloth:in', 'inventory:cloth:out'] }
      },
      {
        path: 'price',
        name: 'Price',
        component: () => import('@/views/function/price/price.vue'),
        meta: { title: '价格管理', permissions: ['price:list'] }
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/function/employee/employee.vue'),
        meta: { title: '员工管理', permissions: ['employee:list'] }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/function/role/role.vue'),
        meta: { title: '角色管理', permissions: ['role:list'] }
      },
      {
        path: 'customer',
        name: 'Customer',
        component: () => import('@/views/function/customer/customer.vue'),
        meta: { title: '客户管理', permissions: ['customer:page'] }
      },
      {
        path: 'document',
        name: 'Document',
        component: () => import('@/views/function/document/document.vue'),
        meta: { title: '文档管理', permissions: ['document:list'] }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/function/order/order.vue'),
        meta: { title: '订单管理', permissions: ['sales:order:list', 'production:order:list'] }
      },
      {
        path: 'bad-product',
        name: 'BadProduct',
        component: () => import('@/views/function/badProduct/badProduct.vue'),
        meta: { title: '次品管理' }
      },
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/function/approval/approvalCenter.vue'),
        meta: { title: '审批中心', permissions: ['approval:leave', 'approval:finance', 'approval:leave:submit', 'approval:finance:submit'] }
      }
    ]
  },
  {
    path: '/platform',
    name: 'Platform',
    component: Layout,
    children: [
      {
        path: 'tenant',
        name: 'PlatformTenant',
        component: () => import('@/views/platform/tenant/index.vue'),
        meta: { title: '租户管理', permissions: ['platform:tenant:view'], developerOnly: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  const hasToken = Boolean(userStore.token)

  if (to.meta?.public) {
    if (hasToken && to.path === '/login') {
      return to.query.redirect ? String(to.query.redirect) : '/dashboard'
    }
    return true
  }

  if (!hasToken) {
    return {
      path: '/login',
      query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : {}
    }
  }

  if (Array.isArray(to.meta?.permissions) && to.meta.permissions.length && !userStore.hasAnyPermission(to.meta.permissions)) {
    ElMessage.warning('您暂无权限访问当前页面，如需开通请联系管理员')
    return '/dashboard'
  }

  if (to.meta?.developerOnly && !userStore.isDeveloper) {
    ElMessage.warning('当前页面仅平台超管可见')
    return '/dashboard'
  }

  return true
})

export default router
