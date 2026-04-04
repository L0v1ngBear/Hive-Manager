import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 导入公共布局父类
import Layout from '@/layout/index.vue'

// 定义静态路由字典
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/Home/dashboard', // 访问根目录时，自动重定向到大盘页面
  },
  {
    path: '/',
    name: 'Home',
    component: Layout, // 将 Layout 设置为这里的核心父组件
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        // 使用懒加载引入刚才写好的业务页面
        component: () => import('@/views/dashboard/index.vue'),
        meta: {
          title: '总览大盘', // 这个 title 会被 Navbar 顶部导航栏读取
          icon: 'dashboard'
        }
      }
    ]
  },
  // 后续你可以继续在这里往下加路由，比如：
  // {
  //   path: '/inventory',
  //   component: Layout,
  //   children: [
  //     {
  //       path: 'list',
  //       component: () => import('@/views/inventory/list.vue'),
  //       meta: { title: '现有库存列表' }
  //     }
  //   ]
  // }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  // 切换路由时，页面滚动条自动回到顶部
  scrollBehavior: () => ({ top: 0 })
})

export default router
