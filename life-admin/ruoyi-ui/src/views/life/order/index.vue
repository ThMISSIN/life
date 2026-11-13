<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="订单类型" prop="orderType">
        <el-select v-model="queryParams.orderType" placeholder="全部类型" clearable style="width: 140px">
          <el-option label="商品订单" :value="1" />
          <el-option label="权益订单" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="按用户ID筛选" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="下单时间">
        <el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['life:order:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList">
      <el-table-column type="expand" width="40">
        <template slot-scope="scope">
          <el-descriptions :column="1" border size="small" class="order-expand">
            <el-descriptions-item v-for="d in (scope.row.details || [])" :key="d.id" :label="d.itemType === 2 ? '权益' : '商品'">
              <span style="display: inline-flex; align-items: center">
                <el-image v-if="d.itemImage" style="width: 40px; height: 40px; margin-right: 8px" :src="lifeImgUrl(d.itemImage)" fit="cover" />
                {{ d.itemName }} × {{ d.quantity }}，小计 {{ fenToYuan(d.totalAmount) }} 元<template v-if="d.pointsPrice">，{{ d.pointsPrice }} 积分</template>
              </span>
            </el-descriptions-item>
            <el-descriptions-item v-if="!(scope.row.details && scope.row.details.length)" label="明细">无明细数据</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-table-column>
      <el-table-column label="订单号" prop="orderNo" :show-overflow-tooltip="true" width="200" />
      <el-table-column label="下单人" prop="nickname" align="center" width="110">
        <template slot-scope="scope">
          <span>{{ scope.row.nickname || (scope.row.userId ? '用户' + scope.row.userId : '-') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.orderType === 1 ? 'primary' : 'success'">{{ scope.row.orderType === 1 ? '商品订单' : '权益订单' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="statusTagType(scope.row.status)">{{ statusMap[scope.row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="总金额(元)" align="center" width="100">
        <template slot-scope="scope">{{ fenToYuan(scope.row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="实付(元)" align="center" width="100">
        <template slot-scope="scope">{{ fenToYuan(scope.row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="抵扣积分" prop="pointsUsed" align="center" width="90" />
      <el-table-column label="支付时间" align="center" prop="payTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.payTime) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['life:order:query']">详情</el-button>
          <el-button v-if="scope.row.status === 1" size="mini" type="text" icon="el-icon-truck" @click="handleDeliver(scope.row)" v-hasPermi="['life:order:deliver']">发货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 订单详情对话框 -->
    <el-dialog title="订单详情" :visible.sync="open" width="720px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusMap[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="下单人">{{ detail.nickname || detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ detail.orderType === 1 ? '商品订单' : '权益订单' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ fenToYuan(detail.totalAmount) }} 元</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ fenToYuan(detail.payAmount) }} 元</el-descriptions-item>
        <el-descriptions-item label="抵扣积分">{{ detail.pointsUsed || 0 }}</el-descriptions-item>
        <el-descriptions-item label="支付渠道">{{ detail.payChannel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ parseTime(detail.payTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">
          <span v-if="address">{{ address.name }} {{ address.phone }} {{ address.address }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="订单备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">订单明细</el-divider>
      <el-table :data="detail.details || []" size="small" border>
        <el-table-column label="图片" width="70" align="center">
          <template slot-scope="scope">
            <el-image v-if="scope.row.itemImage" style="width: 40px; height: 40px" :src="lifeImgUrl(scope.row.itemImage)" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column label="名称" prop="itemName" :show-overflow-tooltip="true" />
        <el-table-column label="类型" width="90" align="center">
          <template slot-scope="scope">{{ scope.row.itemType === 2 ? '虚拟权益' : '实物商品' }}</template>
        </el-table-column>
        <el-table-column label="单价(元)" width="90" align="center">
          <template slot-scope="scope">{{ fenToYuan(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" prop="quantity" width="70" align="center" />
        <el-table-column label="小计(元)" width="90" align="center">
          <template slot-scope="scope">{{ fenToYuan(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="积分价" prop="pointsPrice" width="80" align="center" />
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="open = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listOrder, getOrder, deliverOrder } from "@/api/life/order"
import { fenToYuan, lifeImgUrl } from "@/utils/life"

export default {
  name: "Order",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      orderList: [],
      open: false,
      detail: {},
      dateRange: [],
      statusMap: { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已退款' },
      queryParams: { pageNum: 1, pageSize: 10, orderType: null, status: null, userId: null, startTime: null, endTime: null }
    }
  },
  computed: {
    address() {
      if (!this.detail.deliveryAddress) return null
      try {
        return JSON.parse(this.detail.deliveryAddress)
      } catch (e) {
        return null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    lifeImgUrl,
    fenToYuan,
    statusTagType(status) {
      const map = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'danger' }
      return map[status] || 'info'
    },
    getList() {
      this.loading = true
      this.queryParams.startTime = this.dateRange && this.dateRange.length ? this.dateRange[0] : null
      this.queryParams.endTime = this.dateRange && this.dateRange.length ? this.dateRange[1] : null
      listOrder(this.queryParams).then(res => {
        this.orderList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleDetail(row) {
      getOrder(row.id).then(res => {
        this.detail = res.data || {}
        this.open = true
      })
    },
    handleDeliver(row) {
      this.$modal.confirm('确认对订单"' + row.orderNo + '"执行发货操作?').then(() => {
        return deliverOrder(row.id)
      }).then(() => {
        this.$modal.msgSuccess("发货成功")
        this.getList()
      }).catch(() => {})
    },
    handleExport() {
      this.download("life/order/export", { ...this.queryParams }, `order_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.order-expand {
  margin: 8px 40px;
  width: auto;
}
</style>
