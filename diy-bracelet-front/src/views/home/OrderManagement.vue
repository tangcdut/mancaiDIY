<template>
  <div class="order-management">
    <div class="header">
      <h2>订单管理</h2>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm" class="demo-form-inline">
        <el-form-item label="订单号">
          <el-input
            v-model="filterForm.orderNo"
            placeholder="输入订单号支持模糊匹配"
            clearable
            @keyup.enter.native="fetchOrders" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="filterForm.status" placeholder="请选择订单状态" clearable @change="fetchOrders">
            <el-option label="全部" :value="null"></el-option>
            <el-option label="待支付" :value="0"></el-option>
            <el-option label="已支付" :value="1"></el-option>
            <el-option label="已发货" :value="2"></el-option>
            <el-option label="已完成" :value="3"></el-option>
            <el-option label="申请退款" :value="4"></el-option>
            <el-option label="已退款" :value="5"></el-option>
            <el-option label="已取消" :value="6"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="filterForm.isOverseas" placeholder="筛选国内/海外订单" clearable @change="fetchOrders">
            <el-option label="全部" :value="null"></el-option>
            <el-option label="国内版订单" :value="0"></el-option>
            <el-option label="海外版订单" :value="1"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="filterForm.showCancelled" @change="fetchOrders">隐藏已取消订单</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchOrders">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table :data="filteredOrders" style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="80"></el-table-column>
        <el-table-column prop="orderNo" label="订单号"></el-table-column>
        <el-table-column prop="amount" label="订单金额" width="100">
          <template slot-scope="scope">
            {{ scope.row.isOverseas === 1 ? '$' : '¥' }}{{ scope.row.amount }}
          </template>
        </el-table-column>
        <el-table-column prop="isOverseas" label="订单类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isOverseas === 1 ? 'warning' : 'info'">
              {{ scope.row.isOverseas === 1 ? '海外版' : '国内版' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运单号" min-width="180">
          <template slot-scope="scope">
            {{ scope.row.trackingNumber || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="receiverName" label="收货人" width="120"></el-table-column>
        <el-table-column prop="receiverPhone" label="手机号" width="140"></el-table-column>
        <el-table-column prop="receiverAddress" label="收货地址" min-width="200"></el-table-column>
        <el-table-column label="创建时间" width="180">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="支付时间" width="180">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.payTime) || '未支付' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="text" @click="viewOrder(scope.row)">查看</el-button>
            <el-button
              v-if="scope.row.status <= 3"
              size="mini"
              type="text"
              @click="showUpdateStatusDialog(scope.row)">
              修改状态
            </el-button>
            <el-button
              v-if="scope.row.status === 4"
              size="mini"
              type="text"
              @click="showRefundDialog(scope.row)">
              审核退款
            </el-button>
            <el-button
              v-if="scope.row.status === 1 || scope.row.status === 2 || (scope.row.status === 3 && allowCompletedRefund)"
              size="mini"
              type="text"
              style="color: #f56c6c;"
              @click="showRefundDialog(scope.row)">
              主动退款
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

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

    <!-- 订单详情对话框 -->
    <el-dialog title="订单详情" :visible.sync="detailDialogVisible" width="800px" class="order-detail-dialog">
      <el-card v-if="currentOrder" class="order-info-card">
        <div slot="header" class="card-header">
          <span class="header-title">订单信息</span>
          <el-tag :type="getStatusType(currentOrder.status)" size="medium">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </div>
        <div class="order-detail-top">
          <div class="order-detail-left" v-if="currentOrder.productImage">
            <div class="order-image-wrapper">
              <el-image
                class="order-image"
                :src="getImageUrl(currentOrder.productImage)"
                fit="contain"
                :preview-src-list="[getImageUrl(currentOrder.productImage)]">
              </el-image>
            </div>
          </div>
          <div class="order-detail-right">
            <el-descriptions :column="2" size="medium" border>
              <el-descriptions-item label="订单号">{{ currentOrder.orderNo || '—' }}</el-descriptions-item>
              <el-descriptions-item label="订单金额"><span class="value price">{{ currentOrder.isOverseas === 1 ? '$' : '¥' }}{{ currentOrder.amount }}</span></el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrder.createTime) || '—' }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ formatDateTime(currentOrder.payTime) || '未支付' }}</el-descriptions-item>
              <el-descriptions-item label="收货人">{{ currentOrder.receiverName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone || '—' }}</el-descriptions-item>
              <el-descriptions-item label="运单号" :span="2">{{ currentOrder.trackingNumber || '—' }}</el-descriptions-item>
              <el-descriptions-item label="收货地址" :span="2">{{ formatAddress(currentOrder) || '—' }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">
                <div class="order-long-text" v-if="currentOrder.remark">{{ currentOrder.remark }}</div>
                <span v-else>—</span>
              </el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">
                <div class="order-long-text" v-if="currentOrder.description">{{ currentOrder.description }}</div>
                <span v-else>—</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-card>

      <el-card v-if="currentOrder && currentOrder.items && currentOrder.items.length > 0" class="order-items-card">
        <div slot="header" class="card-header">
          <span class="header-title">商品信息</span>
          <span class="item-count">共 {{ currentOrder.items.length }} 件商品</span>
        </div>
        <el-table :data="currentOrder.items" style="width: 100%" :show-header="true">
          <el-table-column label="商品图片" width="100" align="center">
            <template slot-scope="scope">
              <el-image
                v-if="getItemImage(scope.row)"
                style="width: 70px; height: 70px; border-radius: 4px;"
                :src="getImageUrl(getItemImage(scope.row))"
                fit="cover"
                :preview-src-list="[getImageUrl(getItemImage(scope.row))]">
              </el-image>
              <div v-else class="no-image">
                <i class="el-icon-picture-outline"></i>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="商品名称" min-width="200">
            <template slot-scope="scope">
              <div class="product-title">{{ scope.row.title }}</div>
              <div class="product-desc" v-if="scope.row.description">{{ scope.row.description }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="120" align="right">
            <template slot-scope="scope">
              <span class="price-text">{{ currentOrder.isOverseas === 1 ? '$' : '¥' }}{{ scope.row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center"></el-table-column>
          <el-table-column label="小计" width="120" align="right">
            <template slot-scope="scope">
              <span class="subtotal-text">{{ currentOrder.isOverseas === 1 ? '$' : '¥' }}{{ (scope.row.price * scope.row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <div slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false" size="medium">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 更新订单状态对话框 -->
    <el-dialog title="更新订单状态" :visible.sync="updateStatusDialogVisible" width="500px">
      <el-form label-width="100px">
        <el-form-item label="当前状态">
          <span>{{ getStatusText(selectedOrder?.status) }}</span>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="newStatus" placeholder="请选择新状态" @change="handleStatusChange">
            <el-option
              v-for="status in getNextStatusOptions(selectedOrder?.status)"
              :key="status.value"
              :label="status.label"
              :value="status.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号" v-if="newStatus === 2">
          <el-input v-model="trackingNumber" placeholder="请输入物流单号"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="updateStatusDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="updateOrderStatus">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 审核/主动退款对话框 -->
    <el-dialog :title="refundOrder && refundOrder.status === 4 ? '审核退款' : '主动发起退款'" :visible.sync="refundDialogVisible" width="500px">
      <el-form label-width="100px">
        <el-form-item label="订单号">
          <span>{{ refundOrder?.orderNo || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(refundOrder?.status)">{{ getStatusText(refundOrder?.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="管理员手机号">
          <el-input v-model="refundAdminPhone" placeholder="请输入管理员手机号"></el-input>
        </el-form-item>
        <el-alert :title="refundOrder && refundOrder.isOverseas === 1 ? '提交后将实际发起 PayPal 退款' : '提交后将实际发起微信退款'" type="warning" show-icon :closable="false"></el-alert>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="refundDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="refundLoading" @click="adminApproveRefund">
          {{ refundOrder && refundOrder.status === 4 ? '确 定' : '确认发起退款' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getOrderList, getOrderDetail, updateOrderStatus, adminRefund, getRefundConfig } from '@/api/admin'

export default {
  name: 'OrderManagement',
  data () {
    return {
      loading: false,
      orders: [],
      allowCompletedRefund: false, // 是否允许已完成订单退款
      filterForm: {
        status: null,
        orderNo: '',
        showCancelled: false,
        isOverseas: null
      },
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      detailDialogVisible: false,
      updateStatusDialogVisible: false,
      currentOrder: null,
      selectedOrder: null,
      newStatus: null,
      trackingNumber: '',
      refundDialogVisible: false,
      refundAdminPhone: '',
      refundOrder: null,
      refundLoading: false
    }
  },
  created () {
    this.fetchRefundConfig()
    this.fetchOrders()
  },
  computed: {
    filteredOrders () {
      // 如果勾选了"隐藏已取消订单"，则过滤掉状态为6的订单
      if (this.filterForm.showCancelled) {
        return this.orders.filter(order => order.status !== 6)
      }
      return this.orders
    }
  },
  methods: {
    // 兼容后端返回的时间：
    // 1) 字符串如 "2025,12,17 20:05:00" 或 "2025,12,17"
    // 2) 数组如 [2025,12,17,20,5,0]
    // 3) 正常 ISO 字符串/时间戳
    formatDateTime (val) {
      if (!val && val !== 0) return ''

      const pad2 = n => (n < 10 ? '0' + n : '' + n)

      // 数组形式 [Y,M,D,h,m,s]
      if (Array.isArray(val)) {
        const [Y, M = 1, D = 1, h = 0, m = 0, s = 0] = val
        return `${Y}-${pad2(M)}-${pad2(D)} ${pad2(h)}:${pad2(m)}:${pad2(s)}`
      }

      if (typeof val === 'string') {
        // 逗号分隔的日期部分
        // e.g. "2025,12,17 20:05:00" 或 "2025,12,17"
        if (val.includes(',')) {
          const [datePart, timePart] = val.split(' ')
          const [Y, M = '1', D = '1'] = datePart.split(',')
          const dateStr = `${Y}-${pad2(+M)}-${pad2(+D)}`
          if (timePart) {
            return `${dateStr} ${timePart}`
          }
          return dateStr
        }

        // 其他字符串，尝试直接 new Date
        const d = new Date(val)
        if (!isNaN(d.getTime())) {
          const Y = d.getFullYear()
          const M = pad2(d.getMonth() + 1)
          const D = pad2(d.getDate())
          const h = pad2(d.getHours())
          const m = pad2(d.getMinutes())
          const s = pad2(d.getSeconds())
          return `${Y}-${M}-${D} ${h}:${m}:${s}`
        }
        return val
      }

      // 数字类型（时间戳）
      if (typeof val === 'number') {
        const d = new Date(val)
        if (!isNaN(d.getTime())) {
          const Y = d.getFullYear()
          const M = pad2(d.getMonth() + 1)
          const D = pad2(d.getDate())
          const h = pad2(d.getHours())
          const m = pad2(d.getMinutes())
          const s = pad2(d.getSeconds())
          return `${Y}-${M}-${D} ${h}:${m}:${s}`
        }
        return ''
      }

      return ''
    },
    async fetchRefundConfig () {
      try {
        const res = await getRefundConfig()
        this.allowCompletedRefund = res.data === true
      } catch (error) {
        console.error('获取退款配置失败:', error)
      }
    },
    async fetchOrders () {
      this.loading = true
      try {
        const params = {
          orderNo: this.filterForm.orderNo,
          page: this.pagination.currentPage,
          size: this.pagination.pageSize
        }
        // 仅在选择具体状态时才传给后端
        if (this.filterForm.status !== null && this.filterForm.status !== undefined && this.filterForm.status !== '') {
          params.status = this.filterForm.status
        }
        // 订单类型筛选
        if (this.filterForm.isOverseas !== null && this.filterForm.isOverseas !== undefined && this.filterForm.isOverseas !== '') {
          params.isOverseas = this.filterForm.isOverseas
        }
        // 移除空值参数
        Object.keys(params).forEach(key => {
          const value = params[key]
          if (value === null || value === undefined || value === '') {
            delete params[key]
          }
        })

        const res = await getOrderList(params)
        this.orders = res.data.orders || []

        // 设置分页信息
        this.pagination.total = res.data.total || 0
        this.pagination.currentPage = res.data.page || 1
        this.pagination.pageSize = res.data.size || this.pagination.pageSize
      } catch (error) {
        console.error('获取订单列表失败:', error)
        this.$message.error('获取订单列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSizeChange (val) {
      this.pagination.pageSize = val
      this.pagination.currentPage = 1
      this.fetchOrders()
    },
    handleCurrentChange (val) {
      this.pagination.currentPage = val
      this.fetchOrders()
    },
    getStatusText (status) {
      const statusMap = {
        0: '待支付',
        1: '已支付',
        2: '已发货',
        3: '已完成',
        4: '申请退款',
        5: '已退款',
        6: '已取消'
      }
      return statusMap[status] || '未知状态'
    },
    getStatusType (status) {
      const typeMap = {
        0: 'warning',
        1: 'primary',
        2: 'success',
        3: 'info',
        4: 'warning',
        5: 'danger',
        6: 'info'
      }
      return typeMap[status] || 'info'
    },
    getNextStatusOptions (currentStatus) {
      // 说明：
      // - 4(申请退款) / 5(已退款) 不允许通过“修改状态”设置
      // - 2(已发货) 与 3(已完成) 允许互相切换
      const statusOptions = [
        { value: 0, label: '待支付' },
        { value: 1, label: '已支付' },
        { value: 2, label: '已发货' },
        { value: 3, label: '已完成' }
      ]

      if (currentStatus === null || currentStatus === undefined) {
        return []
      }

      // 已发货(2) 与 已完成(3) 可相互修改
      if (currentStatus === 2) {
        return statusOptions.filter(option => option.value === 3)
      }
      if (currentStatus === 3) {
        return statusOptions.filter(option => option.value === 2)
      }

      // 只返回比当前状态大的状态选项
      return statusOptions.filter(option => option.value > currentStatus)
    },
    formatAddress (order) {
      if (!order) return ''
      const full = order.receiverAddress
      if (full) return full
      const parts = [order.receiverProvince, order.receiverCity, order.receiverDistrict, order.receiverDetail]
      return parts.filter(Boolean).join(' ')
    },
    async viewOrder (order) {
      try {
        const res = await getOrderDetail(order.orderId)
        this.currentOrder = res.data.order
        this.detailDialogVisible = true
      } catch (error) {
        console.error('获取订单详情失败:', error)
        this.$message.error('获取订单详情失败')
      }
    },
    showUpdateStatusDialog (order) {
      this.selectedOrder = order
      const options = this.getNextStatusOptions(order.status)
      this.newStatus = options.length > 0 ? options[0].value : null
      this.trackingNumber = order.trackingNumber || ''
      this.handleStatusChange(this.newStatus)
      this.updateStatusDialogVisible = true
    },
    showRefundDialog (order) {
      this.refundOrder = order
      this.refundAdminPhone = ''
      this.refundDialogVisible = true
    },
    async adminApproveRefund () {
      if (!this.refundOrder) return
      if (this.refundLoading) return
      if (!this.refundAdminPhone) {
        this.$message.warning('请填写管理员手机号')
        return
      }
      const isReview = this.refundOrder.status === 4
      this.refundLoading = true
      try {
        await adminRefund(this.refundOrder.orderId, this.refundAdminPhone)
        this.$message.success(isReview ? '退款审核成功，已提交退款' : '主动发起退款成功，已提交退款')
        this.refundDialogVisible = false
        this.refundOrder = null
        this.refundAdminPhone = ''
        this.fetchOrders()
      } catch (error) {
        console.error(isReview ? '审核退款失败:' : '主动发起退款失败:', error)
        this.$message.error(isReview ? '审核退款失败' : '主动发起退款失败')
      } finally {
        this.refundLoading = false
      }
    },
    async updateOrderStatus () {
      try {
        if (this.newStatus === 2 && !this.trackingNumber) {
          this.$message.warning('请填写物流单号')
          return
        }

        const payload = {
          orderId: this.selectedOrder.orderId,
          status: this.newStatus
        }

        // 发货需要携带物流单号
        if (this.newStatus === 2) {
          payload.trackingNumber = this.trackingNumber
        }

        await updateOrderStatus(payload)

        this.$message.success('状态更新成功')
        this.updateStatusDialogVisible = false
        this.trackingNumber = ''
        this.fetchOrders()
      } catch (error) {
        console.error('更新订单状态失败:', error)
        this.$message.error('更新订单状态失败')
      }
    },
    handleStatusChange (value) {
      if (value !== 2) {
        this.trackingNumber = ''
      }
    },
    getItemImage (item) {
      if (!item || typeof item !== 'object') return ''
      // 优先级顺序：常见的图片字段名
      const keys = ['imageUrl', 'productImage', 'product_image', 'image', 'coverImage', 'pic', 'thumbnail', 'thumb', 'url', 'img', 'photo', 'picture']
      for (const k of keys) {
        const v = item[k]
        if (typeof v === 'string' && v) return v
      }
      // 如果上面都找不到，尝试找任何以image或pic结尾的字段
      for (const k of Object.keys(item)) {
        if ((k.toLowerCase().includes('image') || k.toLowerCase().includes('pic')) && typeof item[k] === 'string' && item[k]) {
          return item[k]
        }
      }
      return ''
    },
    getImageUrl (relativePath) {
      if (!relativePath) return ''
      if (relativePath.startsWith('http://') || relativePath.startsWith('https://')) return relativePath
      const base = (process.env.VUE_APP_API_TARGET || process.env.VUE_APP_BASE_URL || window.location.origin || '').replace(/\/$/, '')
      if (relativePath.startsWith('/')) {
        return `${base}${relativePath}`
      }
      return `${base}/${relativePath}`
    }
  }
}
</script>

<style scoped>
.order-management {
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

.filter-bar {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.table-wrapper {
  flex: 1;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 20px;
}

.table-wrapper::-webkit-scrollbar {
  height: 12px;
  width: 12px;
}

.table-wrapper::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 6px;
}

.table-wrapper::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

.table-wrapper::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 6px;
}

::v-deep .el-table {
  margin-bottom: 0;
}

::v-deep .el-table__fixed-right {
  height: 100% !important;
}

.order-info-item .value.price {
  color: #f56c6c;
  font-weight: 600;
  font-size: 16px;
}

.order-detail-top {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.order-detail-left {
  width: 220px;
  flex: 0 0 220px;
}

.order-detail-right {
  flex: 1;
  min-width: 0;
}

.order-image {
  width: 200px;
  height: 200px;
  border-radius: 8px;
}

.order-image-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.order-long-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #606266;
}

::v-deep .order-detail-dialog .el-descriptions__label {
  color: #606266;
  font-weight: 600;
}

::v-deep .order-detail-dialog .el-descriptions__content {
  color: #303133;
}

.order-info-card,
.order-items-card {
  margin-bottom: 20px;
}

.order-items-card {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .header-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.card-header .item-count {
  color: #909399;
  font-size: 13px;
}

.no-image {
  width: 70px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
}

.no-image i {
  font-size: 28px;
  color: #c0c4cc;
}

.product-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  font-weight: 500;
}

.product-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.price-text {
  color: #606266;
  font-size: 14px;
}

.subtotal-text {
  color: #303133;
  font-weight: 600;
  font-size: 14px;
}

::v-deep .order-detail-dialog .el-dialog__body {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

::v-deep .order-detail-dialog .el-card__header {
  padding: 15px 20px;
  background: #fafafa;
}

::v-deep .order-detail-dialog .el-card__body {
  padding: 20px;
}

.el-table {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #ebeef5;
}

.el-pagination {
  margin-top: 20px;
  text-align: right;
  flex-shrink: 0;
}
</style>
