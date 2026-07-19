<template>
  <div class="welcome-container">
    <div class="welcome-header">
      <h2>欢迎使用DIY手链后台管理系统</h2>
      <p class="welcome-subtitle">{{ greeting }}，{{ userInfo.displayName || '管理员' }}</p>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card">
          <div class="stats-item">
            <i class="el-icon-picture-outline"></i>
            <div class="stats-info">
              <div class="stats-title">轮播图</div>
              <div class="stats-value">{{ stats.banners || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card">
          <div class="stats-item">
            <i class="el-icon-menu"></i>
            <div class="stats-info">
              <div class="stats-title">商品分类</div>
              <div class="stats-value">{{ stats.categories || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card">
          <div class="stats-item">
            <i class="el-icon-goods"></i>
            <div class="stats-info">
              <div class="stats-title">商品数量</div>
              <div class="stats-value">{{ stats.products || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card">
          <div class="stats-item">
            <i class="el-icon-s-order"></i>
            <div class="stats-info">
              <div class="stats-title">待处理订单</div>
              <div class="stats-value">{{ stats.pendingOrders || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="quick-actions">
      <h3>快捷操作</h3>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" icon="el-icon-picture-outline" @click="$router.push('/home/banner')">轮播图管理</el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" icon="el-icon-menu" @click="$router.push('/home/category')">分类管理</el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" icon="el-icon-goods" @click="$router.push('/home/product')">商品管理</el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" icon="el-icon-s-order" @click="$router.push('/home/order')">订单管理</el-button>
        </el-col>
      </el-row>
    </div>

    <div class="tips-section">
      <h3>使用提示</h3>
      <el-card class="tips-card">
        <ul>
          <li>点击左侧菜单可以快速切换不同功能模块</li>
          <li>可以通过轮播图管理首页展示图片</li>
          <li>商品分类管理帮助您更好地组织商品</li>
          <li>订单管理可以查看和处理用户订单</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script>
import { getBannerList, getCategoryList, getProductList, getOrderList } from '@/api/admin'

export default {
  name: 'Welcome',
  data () {
    return {
      userInfo: {
        username: localStorage.getItem('username') || '',
        displayName: localStorage.getItem('username') || '管理员'
      },
      stats: {
        banners: 0,
        categories: 0,
        products: 0,
        pendingOrders: 0
      },
      greeting: ''
    }
  },
  created () {
    this.updateGreeting()
    this.fetchStats()
  },
  methods: {
    updateGreeting () {
      const hour = new Date().getHours()
      if (hour < 6) {
        this.greeting = '凌晨好'
      } else if (hour < 9) {
        this.greeting = '早上好'
      } else if (hour < 12) {
        this.greeting = '上午好'
      } else if (hour < 14) {
        this.greeting = '中午好'
      } else if (hour < 18) {
        this.greeting = '下午好'
      } else if (hour < 22) {
        this.greeting = '晚上好'
      } else {
        this.greeting = '深夜好'
      }
    },
    async fetchStats () {
      try {
        // 获取轮播图数量
        const bannerResponse = await getBannerList()
        this.stats.banners = bannerResponse.data.banners ? bannerResponse.data.banners.length : 0

        // 获取分类数量
        const categoryResponse = await getCategoryList()
        this.stats.categories = categoryResponse.data.categories ? categoryResponse.data.categories.length : 0

        // 获取商品数量
        const productResponse = await getProductList()
        this.stats.products = productResponse.data.products ? productResponse.data.products.length : 0

        // 获取待处理订单数量
        const orderResponse = await getOrderList({ status: 0, page: 1, size: 50 }) // 0表示待支付订单
        this.stats.pendingOrders = orderResponse.data.orders ? orderResponse.data.orders.length : 0
      } catch (error) {
        console.error('获取统计数据失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.welcome-container {
  padding: 20px;
}

.welcome-header {
  text-align: center;
  margin-bottom: 40px;
}

.welcome-header h2 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 10px;
}

.welcome-subtitle {
  font-size: 16px;
  color: #606266;
}

.stats-row {
  margin-bottom: 40px;
}

.stats-card {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stats-item i {
  font-size: 48px;
  color: #409EFF;
}

.stats-info {
  text-align: left;
}

.stats-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stats-value {
  font-size: 24px;
  color: #303133;
  font-weight: bold;
}

.quick-actions {
  margin-bottom: 40px;
}

.quick-actions h3 {
  margin-bottom: 20px;
  color: #303133;
}

.quick-actions .el-button {
  width: 100%;
  margin-bottom: 10px;
}

.tips-section h3 {
  margin-bottom: 20px;
  color: #303133;
}

.tips-card {
  background-color: #f5f7fa;
}

.tips-card ul {
  padding-left: 20px;
  margin: 0;
}

.tips-card li {
  line-height: 2;
  color: #606266;
}
</style>
