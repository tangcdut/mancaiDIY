<template>
  <div class="customer-service-management">
    <div class="header">
      <h2>客服二维码管理</h2>
    </div>

    <div class="qr-container" v-loading="loading">
      <div class="qr-card">
        <h3>当前客服二维码</h3>
        <div class="qr-preview" v-if="currentQRCode">
          <el-image
            style="width: 300px; height: 300px"
            :src="getImageUrl(currentQRCode)"
            fit="contain"
            :preview-src-list="[getImageUrl(currentQRCode)]">
          </el-image>
          <div class="qr-actions">
            <el-button type="danger" size="small" @click="deleteQRCode">删除二维码</el-button>
          </div>
        </div>
        <div class="qr-empty" v-else>
          <i class="el-icon-picture-outline"></i>
          <p>暂未上传客服二维码</p>
        </div>

        <div class="upload-section">
          <h4>上传新二维码</h4>
          <el-upload
            class="upload-demo"
            drag
            :action="uploadUrl"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :show-file-list="false"
            accept="image/*">
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">只能上传jpg/png/gif文件，且不超过5MB</div>
          </el-upload>
        </div>

        <div class="qr-tips">
          <h4>使用说明：</h4>
          <ul>
            <li>1. 上传客服微信二维码，用户可在小程序中扫码联系客服</li>
            <li>2. 支持图片格式：jpg、png、gif、bmp、webp</li>
            <li>3. 文件大小限制：5MB以内</li>
            <li>4. 上传新二维码会自动替换旧的二维码</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCustomerServiceQR, deleteCustomerServiceQR } from '@/api/admin'

export default {
  name: 'CustomerServiceManagement',
  data () {
    return {
      loading: false,
      currentQRCode: '',
      baseUrl: process.env.VUE_APP_BASE_URL || 'http://localhost:8080' // 后端服务器地址（用于图片显示）
    }
  },
  computed: {
    uploadUrl () {
      if (process.env.NODE_ENV === 'development') {
        return '/admin/common/upload/customer-service-qr'
      }
      // 生产优先使用 VUE_APP_API_TARGET（与 vue.config.js 中的 proxy 目标一致），其次使用 VUE_APP_BASE_URL，再退回到 this.baseUrl
      const target = (process.env.VUE_APP_API_TARGET || process.env.VUE_APP_BASE_URL || this.baseUrl).replace(/\/$/, '')
      return `${target}/admin/common/upload/customer-service-qr`
    },

    uploadHeaders () {
      return {
        authentication: localStorage.getItem('token') || ''
      }
    }
  },
  created () {
    this.fetchQRCode()
  },
  methods: {
    async fetchQRCode () {
      this.loading = true
      try {
        const res = await getCustomerServiceQR()
        if (res.code === 1 && res.data) {
          this.currentQRCode = res.data
        } else {
          this.currentQRCode = ''
        }
      } catch (error) {
        console.error('获取客服二维码失败:', error)
        this.currentQRCode = ''
      } finally {
        this.loading = false
      }
    },

    getImageUrl (path) {
      if (!path) return ''
      // 已经是完整 URL，直接返回
      if (path.startsWith('http://') || path.startsWith('https://')) {
        return path
      }
      // 计算后端基址：优先 VUE_APP_API_TARGET -> VUE_APP_BASE_URL -> this.baseUrl
      const target = (process.env.VUE_APP_API_TARGET || process.env.VUE_APP_BASE_URL || this.baseUrl || '').replace(/\/$/, '')
      // 以 / 开头的相对路径，直接拼接到后端域名上
      if (path.startsWith('/')) {
        return `${target}${path}`
      }
      // 其它（例如返回文件名或无前缀路径）也拼接
      return `${target}/${path}`
    },

    beforeUpload (file) {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5

      if (!isImage) {
        this.$message.error('只能上传图片文件!')
        return false
      }
      if (!isLt5M) {
        this.$message.error('上传图片大小不能超过 5MB!')
        return false
      }
      return true
    },

    handleUploadSuccess (response, file, fileList) {
      if (response.code === 1) {
        this.$message.success('上传成功')
        this.fetchQRCode()
      } else {
        this.$message.error(response.msg || '上传失败')
      }
    },

    handleUploadError (err, file, fileList) {
      console.error('上传失败:', err)
      this.$message.error('上传失败')
    },

    async deleteQRCode () {
      try {
        await this.$confirm('确定要删除当前客服二维码吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        this.loading = true
        const res = await deleteCustomerServiceQR()
        if (res.code === 1) {
          this.$message.success('删除成功')
          this.currentQRCode = ''
        } else {
          this.$message.error(res.msg || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除客服二维码失败:', error)
          this.$message.error('删除失败')
        }
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.customer-service-management {
  background: #fff;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.qr-container {
  flex: 1;
  overflow-y: auto;
}

.qr-card {
  max-width: 800px;
  margin: 0 auto;
}

.qr-card h3 {
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
}

.qr-card h4 {
  margin: 20px 0 10px;
  font-size: 16px;
  color: #666;
}

.qr-preview {
  text-align: center;
  padding: 20px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  margin-bottom: 30px;
}

.qr-actions {
  margin-top: 15px;
}

.qr-empty {
  text-align: center;
  padding: 60px 20px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  margin-bottom: 30px;
  color: #999;
}

.qr-empty i {
  font-size: 80px;
  color: #ddd;
}

.qr-empty p {
  margin-top: 15px;
  font-size: 14px;
}

.upload-section {
  margin-bottom: 30px;
}

.qr-tips {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.qr-tips ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.qr-tips li {
  padding: 8px 0;
  color: #666;
  font-size: 14px;
}

.upload-demo {
  margin-top: 10px;
}
</style>
