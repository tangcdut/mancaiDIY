<template>
  <div class="main-layout">
    <div class="sidebar">
      <div class="user-info">
        <el-avatar :size="40" :src="userInfo.avatar"></el-avatar>
        <span class="username">{{ userInfo.displayName }}</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router>
        <el-menu-item index="/home/welcome">
          <i class="el-icon-s-home"></i>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/home/banner">
          <i class="el-icon-picture-outline"></i>
          <span>轮播图管理</span>
        </el-menu-item>
        <el-menu-item index="/home/category">
          <i class="el-icon-menu"></i>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/home/product">
          <i class="el-icon-goods"></i>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/home/diy-material">
          <i class="el-icon-bangzhu"></i>
          <span>DIY材料管理</span>
        </el-menu-item>
        <el-menu-item index="/home/order">
          <i class="el-icon-s-order"></i>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/home/customer-service">
          <i class="el-icon-service"></i>
          <span>客服二维码</span>
        </el-menu-item>
        <el-menu-item @click="handleLogout">
          <i class="el-icon-switch-button"></i>
          <span>退出登录</span>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="main-content">
      <router-view></router-view>
      <div class="footer">
        <a href="https://beian.miit.gov.cn/" target="_blank" class="beian-link">
          XX
        </a>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MainLayout',
  data () {
    return {
      userInfo: {
        displayName: localStorage.getItem('username') || '管理员',
        avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
      },
      activeMenu: '/home/welcome'
    }
  },
  methods: {
    handleLogout () {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 200px;
  background-color: #304156;
  color: #fff;
}

.user-info {
  padding: 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #1f2d3d;
}

.username {
  margin-left: 10px;
  font-size: 14px;
}

.menu {
  border-right: none;
}

.main-content {
  flex: 1;
  padding: 20px;
  background-color: #f0f2f5;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.footer {
  margin-top: auto;
  padding: 20px;
  text-align: center;
  color: #909399;
  font-size: 12px;
  background-color: #f0f2f5;
}

.beian-link {
  color: #909399;
  text-decoration: none;
}

.beian-link:hover {
  color: #409EFF;
  text-decoration: underline;
}
</style>
