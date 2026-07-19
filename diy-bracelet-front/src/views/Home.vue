<template>
  <div class="home">
    <MainLayout />
  </div>
</template>

<script>
import MainLayout from './layout/MainLayout.vue'

export default {
  name: 'Home',
  components: {
    MainLayout
  },
  data () {
    return {
    }
  }
}
</script>

<style scoped>
.home {
  height: 100vh;
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s;
}

.logo:hover {
  background-color: #263445;
}

.logo img {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}

.el-menu {
  border-right: none;
}

.el-header {
  background-color: #fff;
  color: #333;
  line-height: 60px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}

.header-right {
  float: right;
  margin-right: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.greeting {
  color: #606266;
  font-size: 14px;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}

.el-menu-item {
  height: 50px;
  line-height: 50px;
}

.el-menu-item i {
  margin-right: 10px;
}

.user-detail {
  padding: 10px;
  min-width: 200px;
}

.user-detail p {
  margin: 5px 0;
  color: #606266;
  font-size: 14px;
}

.user-detail i {
  margin-right: 8px;
  color: #909399;
}
</style>
