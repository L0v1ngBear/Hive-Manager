import { createRouter, createWebHistory } from 'vue-router'
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
        meta: { title: '标签打印' }
      },
      {
        path: 'receipt',
        name: 'Receipt',
        component: () => import('@/views/function/receipt.vue'),
        meta: { title: '出库单打印' }
      },
      {
        path: 'price',
        name: 'Price',
        component: () => import('@/views/function/price/price.vue'),
        meta: { title: '价格管理' }
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/function/employee/employee.vue'),
        meta: { title: '员工管理' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/function/role/role.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'customer',
        name: 'Customer',
        component: () => import('@/views/function/customer/customer.vue'),
        meta: { title: '客户管理' }
      },
      {
        path: 'document',
        name: 'Document',
        component: () => import('@/views/function/document/document.vue'),
        meta: { title: '文档管理' }
      },
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/function/approval/approvalCenter.vue'),
        meta: { title: '审批中心' }
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

  return true
})

export default router