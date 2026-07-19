import Vue from 'vue'
import VueRouter from 'vue-router'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import Welcome from '../views/home/Welcome.vue'

// 动态导入页面组件
const BannerManagement = () => import('../views/home/BannerManagement.vue')
const CategoryManagement = () => import('../views/home/CategoryManagement.vue')
const ProductManagement = () => import('../views/home/ProductManagement.vue')
const OrderManagement = () => import('../views/home/OrderManagement.vue')
const CustomerServiceManagement = () => import('../views/home/CustomerServiceManagement.vue')
const DiyMaterialManagement = () => import('../views/home/DiyMaterialManagement.vue')

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    redirect: '/home/welcome',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'welcome',
        name: 'Welcome',
        component: Welcome,
        meta: { requiresAuth: true }
      },
      {
        path: 'banner',
        name: 'BannerManagement',
        component: BannerManagement,
        meta: { requiresAuth: true }
      },
      {
        path: 'category',
        name: 'CategoryManagement',
        component: CategoryManagement,
        meta: { requiresAuth: true }
      },
      {
        path: 'product',
        name: 'ProductManagement',
        component: ProductManagement,
        meta: { requiresAuth: true }
      },
      {
        path: 'order',
        name: 'OrderManagement',
        component: OrderManagement,
        meta: { requiresAuth: true }
      },
      {
        path: 'customer-service',
        name: 'CustomerServiceManagement',
        component: CustomerServiceManagement,
        meta: { requiresAuth: true }
      },
      {
        path: 'diy-material',
        name: 'DiyMaterialManagement',
        component: DiyMaterialManagement,
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = new VueRouter({
  mode: 'history',
  base: '/manage/',
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
