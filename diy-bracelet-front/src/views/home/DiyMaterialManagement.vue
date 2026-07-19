<template>
  <div class="diy-material-management">
    <div class="header">
      <h2>DIY材料管理</h2>
      <el-button type="primary" icon="el-icon-plus" @click="addMaterial">添加材料</el-button>
    </div>

    <!-- 筛选条件 -->
    <div class="filters">
      <el-select v-model="filterCategory" placeholder="筛选分类" clearable @change="fetchMaterials">
        <el-option label="全部分类" value=""></el-option>
        <el-option v-for="cat in categories" :key="cat.key" :label="cat.name" :value="cat.key"></el-option>
      </el-select>
      <el-select v-model="filterColorSeries" placeholder="筛选色系" clearable @change="fetchMaterials" style="margin-left: 10px;">
        <el-option label="全部色系" value=""></el-option>
        <el-option v-for="color in colorSeries" :key="color.key" :label="color.name" :value="color.key"></el-option>
      </el-select>
    </div>

    <div class="table-container">
      <el-table :data="materials" style="width: 100%;" v-loading="loading" height="100%">
      <el-table-column type="index" label="序号" width="60"></el-table-column>
      <el-table-column prop="imageUrl" label="图片" width="80">
        <template slot-scope="scope">
          <div v-if="scope.row.imageUrl" class="material-image">
            <img :src="getImageUrl(scope.row.imageUrl)" alt="材料图片" />
          </div>
          <div v-else class="material-color-preview" :style="getColorStyle(scope.row.color)"></div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="材料名称" width="120"></el-table-column>
      <el-table-column prop="categoryKey" label="分类" width="100">
        <template slot-scope="scope">
          {{ getCategoryName(scope.row.categoryKey) }}
        </template>
      </el-table-column>
      <el-table-column prop="colorSeriesKey" label="色系" width="100">
        <template slot-scope="scope">
          {{ getColorSeriesName(scope.row.colorSeriesKey) }}
        </template>
      </el-table-column>
      <el-table-column prop="size" label="尺寸(mm)" width="90"></el-table-column>
      <el-table-column prop="price" label="单价(元)" width="100">
        <template slot-scope="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column prop="priceOverseas" label="单价(美元)" width="100">
        <template slot-scope="scope">
          ${{ scope.row.priceOverseas }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80"></el-table-column>
      <el-table-column prop="weight" label="重量(g)" width="90"></el-table-column>
      <el-table-column prop="sort" label="排序" width="70"></el-table-column>
      <el-table-column label="状态" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="editMaterial(scope.row)">编辑</el-button>
          <el-button size="mini" type="text" @click="deleteMaterial(scope.row.id)">删除</el-button>
          <el-button size="mini" type="text" @click="toggleStatus(scope.row)">
            {{ scope.row.status === 1 ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <!-- 添加/编辑材料对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="700px">
      <el-form :model="materialForm" :rules="rules" ref="materialForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="材料名称" prop="title">
              <el-input v-model="materialForm.title" placeholder="请输入材料名称"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryKey">
              <el-select v-model="materialForm.categoryKey" placeholder="请选择分类" style="width: 100%;">
                <el-option v-for="cat in categories" :key="cat.key" :label="cat.name" :value="cat.key"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="色系" prop="colorSeriesKey">
              <el-select v-model="materialForm.colorSeriesKey" placeholder="请选择色系" style="width: 100%;">
                <el-option v-for="color in colorSeries" :key="color.key" :label="color.name" :value="color.key"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="尺寸(mm)" prop="size">
              <el-input-number v-model="materialForm.size" :min="0" :precision="2" :step="0.5" style="width: 100%;"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单价(元)" prop="price">
              <el-input-number v-model="materialForm.price" :min="0" :precision="2" :step="0.1" style="width: 100%;"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价(美元)" prop="priceOverseas">
              <el-input-number v-model="materialForm.priceOverseas" :min="0" :precision="2" :step="0.1" style="width: 100%;"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="materialForm.stock" :min="0" style="width: 100%;"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重量(g)" prop="weight">
              <el-input-number v-model="materialForm.weight" :min="0" :precision="2" :step="0.01" style="width: 100%;"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="materialForm.sort" :min="0" style="width: 100%;"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="颜色值" prop="color">
          <el-input v-model="materialForm.color" placeholder="CSS颜色或渐变，如 #ffffff 或 radial-gradient(...)"></el-input>
        </el-form-item>
        <el-form-item label="材料图片">
          <el-upload
            class="avatar-uploader"
            :action="uploadAction"
            :data="{module: 'diy-material'}"
            :show-file-list="false"
            :on-success="handleImageUploadSuccess"
            :before-upload="beforeImageUpload"
            :headers="{authentication: $store?.state?.token || localStorage.getItem('token')}"
            name="file">
            <img v-if="materialForm.imageUrl" :src="getImageUrl(materialForm.imageUrl)" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
          <div class="upload-tip">建议上传尺寸：200x200px</div>
        </el-form-item>
        <el-form-item label="实物图一">
          <el-upload
            class="avatar-uploader"
            :action="uploadAction"
            :data="{module: 'diy-material'}"
            :show-file-list="false"
            :on-success="handleRealImage1UploadSuccess"
            :before-upload="beforeImageUpload"
            :headers="{authentication: $store?.state?.token || localStorage.getItem('token')}"
            name="file">
            <img v-if="materialForm.realImageUrl1" :src="getImageUrl(materialForm.realImageUrl1)" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
          <div class="upload-tip">用于小程序端细节展示实拍一</div>
        </el-form-item>
        <el-form-item label="实物图二">
          <el-upload
            class="avatar-uploader"
            :action="uploadAction"
            :data="{module: 'diy-material'}"
            :show-file-list="false"
            :on-success="handleRealImage2UploadSuccess"
            :before-upload="beforeImageUpload"
            :headers="{authentication: $store?.state?.token || localStorage.getItem('token')}"
            name="file">
            <img v-if="materialForm.realImageUrl2" :src="getImageUrl(materialForm.realImageUrl2)" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
          <div class="upload-tip">用于小程序端细节展示实拍二</div>
        </el-form-item>
        <el-form-item label="材料描述">
          <el-input type="textarea" v-model="materialForm.description" :rows="3" placeholder="请输入材料描述"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="materialForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架">
          </el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveMaterial">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getDiyMaterialList, addDiyMaterial, updateDiyMaterial, deleteDiyMaterial, getColorSeriesList } from '@/api/admin'

export default {
  name: 'DiyMaterialManagement',
  data () {
    return {
      loading: false,
      materials: [],
      filterCategory: '',
      filterColorSeries: '',
      dialogVisible: false,
      dialogTitle: '添加材料',
      materialForm: {
        id: null,
        categoryKey: '',
        colorSeriesKey: '',
        title: '',
        description: '',
        size: null,
        color: '',
        price: null,
        priceOverseas: null,
        stock: 0,
        weight: null,
        imageUrl: '',
        realImageUrl1: '',
        realImageUrl2: '',
        sort: 0,
        status: 1
      },
      rules: {
        title: [
          { required: true, message: '请输入材料名称', trigger: 'blur' }
        ],
        categoryKey: [
          { required: true, message: '请选择分类', trigger: 'change' }
        ],
        price: [
          { required: true, message: '请输入单价(元)', trigger: 'blur' }
        ],
        priceOverseas: [
          { required: true, message: '请输入美元单价', trigger: 'blur' }
        ]
      },
      categories: [
        { key: 'main_bead', name: '主珠' },
        { key: 'accessory', name: '配饰' },
        { key: 'spacer', name: '间珠' },
        { key: 'pendant', name: '吊坠' },
        { key: 'rope', name: '绳子' }
      ],
      colorSeries: []
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
    this.fetchColorSeries()
    this.fetchMaterials()
  },
  methods: {
    async fetchColorSeries () {
      try {
        const res = await getColorSeriesList()
        if (res.data && Array.isArray(res.data)) {
          this.colorSeries = res.data.map(item => ({
            key: item.keyCode,
            name: item.name
          }))
        }
      } catch (error) {
        console.error('获取色系列表失败:', error)
        this.$message.error('获取色系列表失败')
      }
    },
    async fetchMaterials () {
      this.loading = true
      try {
        const params = {}
        if (this.filterCategory) {
          params.categoryKey = this.filterCategory
        }
        if (this.filterColorSeries) {
          params.colorSeriesKey = this.filterColorSeries
        }
        const res = await getDiyMaterialList(params)
        this.materials = res.data || []
      } catch (error) {
        console.error('获取材料列表失败:', error)
        this.$message.error('获取材料列表失败')
      } finally {
        this.loading = false
      }
    },
    addMaterial () {
      this.dialogTitle = '添加材料'
      this.materialForm = {
        id: null,
        categoryKey: '',
        colorSeriesKey: '',
        title: '',
        description: '',
        size: null,
        color: '',
        price: null,
        priceOverseas: null,
        stock: 0,
        weight: null,
        imageUrl: '',
        realImageUrl1: '',
        realImageUrl2: '',
        sort: 0,
        status: 1
      }
      this.dialogVisible = true
    },
    editMaterial (material) {
      this.dialogTitle = '编辑材料'
      this.materialForm = { ...material }
      this.dialogVisible = true
    },
    async saveMaterial () {
      this.$refs.materialForm.validate(async (valid) => {
        if (valid) {
          try {
            if (this.materialForm.id) {
              // 更新材料
              await updateDiyMaterial(this.materialForm)
              this.$message.success('更新成功')
            } else {
              // 添加材料
              await addDiyMaterial(this.materialForm)
              this.$message.success('添加成功')
            }
            this.dialogVisible = false
            this.fetchMaterials()
          } catch (error) {
            console.error('保存材料失败:', error)
            const errorMsg = error.response?.data?.msg || error.message || '保存材料失败'
            this.$message.error(errorMsg)
          }
        }
      })
    },
    async deleteMaterial (id) {
      this.$confirm('确定要删除这个材料吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteDiyMaterial(id)
          this.$message.success('删除成功')
          this.fetchMaterials()
        } catch (error) {
          console.error('删除材料失败:', error)
          const errorMsg = error.response?.data?.msg || error.message || '删除材料失败'
          this.$message.error(errorMsg)
        }
      }).catch(() => {
        // 用户取消删除操作
      })
    },
    async toggleStatus (material) {
      try {
        const updatedMaterial = {
          ...material,
          status: material.status === 1 ? 0 : 1
        }
        await updateDiyMaterial(updatedMaterial)
        this.$message.success('状态更新成功')
        this.fetchMaterials()
      } catch (error) {
        console.error('更新状态失败:', error)
        this.$message.error('更新状态失败')
      }
    },
    getCategoryName (key) {
      const category = this.categories.find(c => c.key === key)
      return category ? category.name : key
    },
    getColorSeriesName (key) {
      const colorSeries = this.colorSeries.find(c => c.key === key)
      return colorSeries ? colorSeries.name : key
    },
    getColorStyle (color) {
      if (!color) return 'background: #e0e5eb;'
      return `background: ${color};`
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
        this.materialForm.imageUrl = response.data
        this.$message.success('图片上传成功')
      } else {
        this.$message.error('图片上传失败: ' + (response.msg || '未知错误'))
      }
    },
    handleRealImage1UploadSuccess (response, file, fileList) {
      if (response.code === 1) {
        this.materialForm.realImageUrl1 = response.data
        this.$message.success('实物图一上传成功')
      } else {
        this.$message.error('实物图一上传失败: ' + (response.msg || '未知错误'))
      }
    },
    handleRealImage2UploadSuccess (response, file, fileList) {
      if (response.code === 1) {
        this.materialForm.realImageUrl2 = response.data
        this.$message.success('实物图二上传成功')
      } else {
        this.$message.error('实物图二上传失败: ' + (response.msg || '未知错误'))
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
.diy-material-management {
  background: #fff;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.filters {
  margin-bottom: 10px;
  flex-shrink: 0;
}

.table-container {
  flex: 1;
  overflow: hidden;
  margin-top: 10px;
}

.material-image {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 4px;
}

.material-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
}

.material-color-preview {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}
</style>
