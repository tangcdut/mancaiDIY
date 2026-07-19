<template>
  <div class="category-management">
    <div class="header">
      <h2>商品分类管理</h2>
      <el-button type="primary" icon="el-icon-plus" @click="addCategory">添加分类</el-button>
    </div>

    <el-table :data="categories" style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="80"></el-table-column>
      <el-table-column prop="name" label="分类名称"></el-table-column>
      <el-table-column prop="sort" label="排序" width="80"></el-table-column>
      <el-table-column label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="editCategory(scope.row)">编辑</el-button>
          <el-button size="mini" type="text" @click="deleteCategory(scope.row.id)">删除</el-button>
          <el-button size="mini" type="text" @click="toggleStatus(scope.row)">
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页控件 -->
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="pagination.currentPage"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pagination.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      style="margin-top: 20px; text-align: right;">
    </el-pagination>

    <!-- 添加/编辑分类对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="categoryForm" :rules="rules" ref="categoryForm" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="categoryForm.sort" :min="1" label="排序"></el-input-number>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="categoryForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用">
          </el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveCategory">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '@/api/admin'

export default {
  name: 'CategoryManagement',
  data () {
    return {
      loading: false,
      categories: [],
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      dialogVisible: false,
      dialogTitle: '添加分类',
      categoryForm: {
        id: null,
        name: '',
        sort: 1,
        status: 1
      },
      rules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' }
        ]
      }
    }
  },
  created () {
    this.fetchCategories()
  },
  methods: {
    async fetchCategories () {
      this.loading = true
      try {
        const params = {
          page: this.pagination.currentPage,
          pageSize: this.pagination.pageSize
        }
        const res = await getCategoryList(params)
        this.categories = res.data.records || []

        // 设置分页信息
        this.pagination.total = res.data.total || 0
      } catch (error) {
        console.error('获取分类列表失败:', error)
        this.$message.error('获取分类列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSizeChange (val) {
      this.pagination.pageSize = val
      this.pagination.currentPage = 1
      this.fetchCategories()
    },
    handleCurrentChange (val) {
      this.pagination.currentPage = val
      this.fetchCategories()
    },
    addCategory () {
      this.dialogTitle = '添加分类'
      this.categoryForm = {
        id: null,
        name: '',
        sort: 1,
        status: 1
      }
      this.dialogVisible = true
    },
    editCategory (category) {
      this.dialogTitle = '编辑分类'
      this.categoryForm = { ...category }
      this.dialogVisible = true
    },
    async saveCategory () {
      this.$refs.categoryForm.validate(async (valid) => {
        if (valid) {
          try {
            if (this.categoryForm.id) {
              // 更新分类
              await updateCategory(this.categoryForm)
              this.$message.success('更新成功')
            } else {
              // 添加分类
              await addCategory(this.categoryForm)
              this.$message.success('添加成功')
            }
            this.dialogVisible = false
            this.fetchCategories()
          } catch (error) {
            console.error('保存分类失败:', error)
            // 显示后端返回的错误信息
            const errorMsg = error.response?.data?.msg || error.message || '保存分类失败'
            this.$message.error(errorMsg)
          }
        }
      })
    },
    async deleteCategory (id) {
      this.$confirm('确定要删除这个分类吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteCategory(id)
          this.$message.success('删除成功')
          this.fetchCategories()
        } catch (error) {
          console.error('删除分类失败:', error)
          // 显示后端返回的错误信息
          const errorMsg = error.response?.data?.msg || error.message || '删除分类失败'
          this.$message.error(errorMsg)
        }
      }).catch(() => {
        // 用户取消删除操作
      })
    },
    async toggleStatus (category) {
      try {
        const updatedCategory = {
          ...category,
          status: category.status === 1 ? 0 : 1
        }
        await updateCategory(updatedCategory)
        this.$message.success('状态更新成功')
        this.fetchCategories()
      } catch (error) {
        console.error('更新状态失败:', error)
        this.$message.error('更新状态失败')
      }
    }
  }
}
</script>

<style scoped>
.category-management {
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

.el-table {
  flex: 1;
}
</style>
