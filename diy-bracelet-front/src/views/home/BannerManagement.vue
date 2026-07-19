<template>
  <div class="banner-management">
    <div class="header">
      <h2>轮播图管理</h2>
      <el-button type="primary" icon="el-icon-plus" @click="addBanner">添加轮播图</el-button>
    </div>

    <el-table :data="banners" style="width: 100%" v-loading="loading" row-class-name="banner-table-row" :header-cell-style="{background: '#f5f7fa', fontWeight: 'bold'}">
      <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
      <el-table-column label="图片" width="140" align="center">
        <template slot-scope="scope">
          <el-image
            style="width: 120px; height: 60px"
            :src="getImageUrl(scope.row.imageUrl)"
            fit="cover"
            :preview-src-list="[getImageUrl(scope.row.imageUrl)]">
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center"></el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template slot-scope="scope">
          <el-button size="small" type="primary" plain @click="editBanner(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="deleteBanner(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑轮播图对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" @close="resetForm">
      <el-form :model="bannerForm" :rules="rules" ref="bannerForm" label-width="90px" label-position="right">
        <el-form-item label="轮播图" prop="imageUrl">
          <el-upload
            class="banner-uploader"
            :action="uploadAction"
            :data="{module: 'banner'}"
            :show-file-list="false"
            :on-success="handleImageUploadSuccess"
            :before-upload="beforeImageUpload"
            :headers="{authentication: $store?.state?.token || localStorage.getItem('token')}"
            name="file">
            <img v-if="bannerForm.imageUrl" :src="getImageUrl(bannerForm.imageUrl)" class="banner-image">
            <i v-else class="el-icon-plus banner-uploader-icon"></i>
          </el-upload>
          <div class="upload-tip">建议尺寸：750x300px</div>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="bannerForm.sort" :min="1" label="排序"></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveBanner">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getBannerList, addBanner, updateBanner, deleteBanner } from '@/api/admin'

export default {
  name: 'BannerManagement',
  data () {
    return {
      loading: false,
      banners: [],
      dialogVisible: false,
      dialogTitle: '添加轮播图',
      bannerForm: {
        id: null,
        imageUrl: '',
        sort: 1
      },
      rules: {
        imageUrl: [
          { required: true, message: '请上传轮播图', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    uploadAction () {
      if (process.env.NODE_ENV === 'development') {
        return '/admin/common/upload'
      }
      const target = (process.env.VUE_APP_API_TARGET || process.env.VUE_APP_BASE_URL || window.location.origin || '').replace(/\/$/, '')
      return `${target}/admin/common/upload`
    }
  },
  created () {
    this.fetchBanners()
  },
  methods: {
    resetForm () {
      this.$refs.bannerForm?.clearValidate()
    },
    async fetchBanners () {
      this.loading = true
      try {
        const res = await getBannerList()
        this.banners = res.data.banners || []
      } catch (error) {
        console.error('获取轮播图列表失败:', error)
        this.$message.error('获取轮播图列表失败')
      } finally {
        this.loading = false
      }
    },
    addBanner () {
      this.dialogTitle = '添加轮播图'
      this.bannerForm = {
        id: null,
        imageUrl: '',
        sort: 1
      }
      this.dialogVisible = true
    },
    editBanner (banner) {
      this.dialogTitle = '编辑轮播图'
      this.bannerForm = { ...banner }
      this.dialogVisible = true
    },
    async saveBanner () {
      this.$refs.bannerForm.validate(async (valid) => {
        if (valid) {
          try {
            if (this.bannerForm.id) {
              // 更新轮播图
              await updateBanner(this.bannerForm)
              this.$message.success('更新成功')
            } else {
              // 添加轮播图
              await addBanner(this.bannerForm)
              this.$message.success('添加成功')
            }
            this.dialogVisible = false
            this.fetchBanners()
          } catch (error) {
            console.error('保存轮播图失败:', error)
            this.$message.error('保存轮播图失败')
          }
        }
      })
    },
    async deleteBanner (id) {
      try {
        await this.$confirm('确定要删除这个轮播图吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteBanner(id)
        this.$message.success('删除成功')
        this.fetchBanners()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除轮播图失败:', error)
          this.$message.error('删除轮播图失败')
        }
      }
    },
    getImageUrl (relativePath) {
      if (!relativePath) return ''
      if (relativePath.startsWith('http://') || relativePath.startsWith('https://')) return relativePath
      const base = (process.env.VUE_APP_API_TARGET || process.env.VUE_APP_BASE_URL || window.location.origin || '').replace(/\/$/, '')
      if (relativePath.startsWith('/')) {
        return `${base}${relativePath}`
      }
      return `${base}/${relativePath}`
    },
    handleImageUploadSuccess (response, file, fileList) {
      if (response.code === 1) {
        this.bannerForm.imageUrl = response.data
        this.$refs.bannerForm.validateField('imageUrl')
        this.$message.success('图片上传成功')
      } else {
        this.$message.error('图片上传失败: ' + (response.msg || '未知错误'))
      }
    },
    beforeImageUpload (file) {
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
    }
  }
}
</script>

<style scoped>
.banner-management {
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
  margin-bottom: 25px;
}

.header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.el-table {
  flex: 1;
}

.banner-table-row {
  height: 100px !important;
}

/deep/ .el-table__row {
  height: 100px;
}

.banner-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.banner-uploader .el-upload:hover {
  border-color: #409EFF;
}

.banner-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 200px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.banner-image {
  width: 200px;
  height: 100px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.5;
}

/deep/ .el-dialog {
  border-radius: 4px;
}

/deep/ .el-form-item {
  margin-bottom: 22px;
}

/deep/ .dialog-footer {
  text-align: right;
  padding: 0;
  border-top: 1px solid #ebeef5;
  margin-top: 20px;
  padding-top: 20px;
}
</style>
