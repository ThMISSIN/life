<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="按用户ID筛选" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 140px">
          <el-option v-for="(label, value) in typeMap" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['life:balanceRecord:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="流水ID" prop="id" align="center" width="80" />
      <el-table-column label="用户" align="center" width="130">
        <template slot-scope="scope">
          <span>{{ scope.row.nickname || (scope.row.userId ? '用户' + scope.row.userId : '-') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变动金额(元)" align="center" width="130">
        <template slot-scope="scope">
          <span :style="{ color: scope.row.amount >= 0 ? '#67C23A' : '#F56C6C' }">
            {{ scope.row.amount >= 0 ? '+' : '' }}{{ fenToYuan(scope.row.amount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.amount >= 0 ? 'success' : 'danger'">{{ typeMap[scope.row.type] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联订单号" prop="orderNo" :show-overflow-tooltip="true" width="190" />
      <el-table-column label="备注" prop="remark" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="时间" align="center" prop="createTime" width="170">
        <template slot-scope="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listBalanceRecord } from "@/api/life/balanceRecord"
import { fenToYuan } from "@/utils/life"

export default {
  name: "BalanceRecord",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      recordList: [],
      dateRange: [],
      typeMap: { 1: '充值', 2: '消费', 3: '退款', 4: '管理调整' },
      queryParams: { pageNum: 1, pageSize: 10, userId: null, type: null, startTime: null, endTime: null }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    fenToYuan,
    getList() {
      this.loading = true
      this.queryParams.startTime = this.dateRange && this.dateRange.length ? this.dateRange[0] : null
      this.queryParams.endTime = this.dateRange && this.dateRange.length ? this.dateRange[1] : null
      listBalanceRecord(this.queryParams).then(res => {
        this.recordList = res.rows
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
    handleExport() {
      this.download("life/balanceRecord/export", { ...this.queryParams }, `balanceRecord_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
