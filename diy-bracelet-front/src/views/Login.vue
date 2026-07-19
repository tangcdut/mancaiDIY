<template>
  <div class="login-container">
    <div class="login-box">
      <h2>DIY手链后台管理系统</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginForm" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="footer">
        <a href="https://beian.miit.gov.cn/" target="_blank" class="beian-link">
          XX
        </a>
      </div>
    </div>
  </div>
</template>

<script>
import { adminLogin } from '@/api/admin'

export default {
  name: 'Login',
  data () {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loading: false,
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    async handleLogin () {
      this.$refs.loginForm.validate(async valid => {
        if (valid) {
          this.loading = true
          try {
            const res = await adminLogin(this.loginForm)
            localStorage.setItem('token', res.data.token)
            localStorage.setItem('username', this.loginForm.username)
            this.$message.success('登录成功')
            this.$router.push('/home')
          } catch (error) {
            console.error('登录失败:', error)
            this.$message.error('登录失败：' + (error.response?.data?.msg || '未知错误'))
          } finally {
            this.loading = false
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('https://th.bing.com/th/id/R.dde8321f8d2c7aa0ddd046d3c2acf0e5?rik=7PqkkKREs%2foyGg&riu=http%3a%2f%2fi1.hdslb.com%2fbfs%2farchive%2f4f56ef5a820e5e34507c9d0e58258d373ce30399.jpg&ehk=uDlSNddpYQ%2fqfP6V5SNNC2ttaK0rvpjulh0ItqFHWTc%3d&risl=&pid=ImgRaw&r=0');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.login-box {
  width: 400px;
  padding: 30px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}

.footer {
  margin-top: auto;
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding-top: 20px;
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
