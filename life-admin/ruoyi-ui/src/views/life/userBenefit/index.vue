<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="按用户ID筛选" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="权益ID" prop="benefitId">
        <el-input v-model="queryParams.benefitId" placeholder="按权益ID筛选" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="订单号精确查" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="券码" prop="code">
        <el-input v-model="queryParams.code" placeholder="券码精确查" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="权益名称" prop="benefitName">
        <el-input v-model="queryParams.benefitName" placeholder="权益名称关键词" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="用户" prop="userKeyword">
        <el-input v-model="queryParams.userKeyword" placeholder="昵称/用户名" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="获得时间">
        <el-date-picker v-model="dateRange" type="daterange" value-format="yyyy-MM-dd" range-separator="-"
                        start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-finished" size="mini" @click="handleVerifyCode" v-hasPermi="['life:userBenefit:verify']">券码核销</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['life:userBenefit:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userBenefitList">
      <el-table-column label="封面" align="center" width="80">
        <template slot-scope="scope">
          <el-image style="width: 48px; height: 48px" :src="lifeImgUrl(scope.row.coverImage)" fit="cover">
            <div slot="error" class="image-slot"><i class="el-icon-picture-outline"></i></div>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column label="权益名称" prop="benefitName" :show-overflow-tooltip="true" min-width="130" />
      <el-table-column label="用户" align="center" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.nickname || (scope.row.userId ? '用户' + scope.row.userId : '-') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="券码" prop="code" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="关联订单号" prop="orderNo" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="statusTagType(scope.row.status)">{{ statusMap[scope.row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效时间" align="center" prop="effectiveTime" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.effectiveTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="过期时间" align="center" prop="expireTime" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.expireTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="核销时间" align="center" prop="verifyTime" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.verifyTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="获得时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-if="scope.row.status === 1" size="mini" type="text" icon="el-icon-finished" @click="handleVerifyRow(scope.row)" v-hasPermi="['life:userBenefit:verify']">核销</el-button>
          <el-button v-if="scope.row.status === 0 || scope.row.status === 1" size="mini" type="text" icon="el-icon-close" @click="handleVoid(scope.row)" v-hasPermi="['life:userBenefit:void']">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listUserBenefit, voidUserBenefit, verifyUserBenefitByCode } from "@/api/life/userBenefit"
import { lifeImgUrl } from "@/utils/life"

export default {
  name: "UserBenefit",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      dateRange: [],
      userBenefitList: [],
      statusMap: { 0: '待生效', 1: '未使用', 2: '已使用', 3: '已过期', 4: '已作废' },
      queryParams: { pageNum: 1, pageSize: 10, userId: null, benefitId: null, orderNo: null, code: null, status: null, benefitName: null, userKeyword: null, beginTime: null, endTime: null }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    lifeImgUrl,
    statusTagType(status) {
      const map = { 0: 'info', 1: 'success', 2: 'primary', 3: 'warning', 4: 'danger' }
      return map[status] || 'info'
    },
    getList() {
      this.loading = true
      this.queryParams.beginTime = this.dateRange && this.dateRange.length === 2 ? this.dateRange[0] : null
      this.queryParams.endTime = this.dateRange && this.dateRange.length === 2 ? this.dateRange[1] : null
      listUserBenefit(this.queryParams).then(res => {
        this.userBenefitList = res.rows
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
    /** 工具栏: 商家手输/扫码枪录入券码核销 */
    handleVerifyCode() {
      this.$modal.prompt('请输入或扫描顾客出示的券码').then(({ value }) => {
        const code = (value || '').trim()
        if (!code) {
          this.$modal.msgError('券码不能为空')
          return
        }
        return this.doVerify(code, null)
      }).catch(() => {})
    },
    /** 行内: 按列表行券码核销(先弹信息二次确认, 防误扫) */
    handleVerifyRow(row) {
      this.doVerify(row.code, row)
    },
    doVerify(code, row) {
      const tip = row
        ? '确认为用户"' + (row.nickname || row.userId) + '"核销权益"' + row.benefitName + '"?'
        : '确认核销券码 "' + code + '"?'
      this.$modal.confirm(tip).then(() => {
        return verifyUserBenefitByCode(code)
      }).then(res => {
        const b = res.data || {}
        this.$modal.msgSuccess('核销成功: ' + (b.benefitName || '') + ' / 用户' + (b.nickname || b.userId || ''))
        this.getList()
      }).catch(() => {})
    },
    handleVoid(row) {
      this.$modal.confirm('确认作废用户"' + (row.nickname || row.userId) + '"的权益"' + row.benefitName + '"? 作废后将按权益当前积分价退还积分, 并回补库存。').then(() => {
        return voidUserBenefit(row.id)
      }).then(() => {
        this.$modal.msgSuccess("作废成功")
        this.getList()
      }).catch(() => {})
    },
    handleExport() {
      this.download("life/userBenefit/export", { ...this.queryParams }, `userBenefit_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
