<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模板名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="模板名称" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 140px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['life:noticeTemplate:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['life:noticeTemplate:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" prop="id" align="center" width="70" />
      <el-table-column label="模板名称" prop="name" :show-overflow-tooltip="true" min-width="130" />
      <el-table-column label="模板编码" prop="code" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="类型" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.type === 2 ? 'warning' : (scope.row.type === 5 ? 'success' : '')">{{ typeText(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题模板" prop="titleTemplate" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="内容模板" prop="contentTemplate" :show-overflow-tooltip="true" min-width="220" />
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" v-hasPermi="['life:noticeTemplate:edit']"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="130" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['life:noticeTemplate:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['life:noticeTemplate:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改通知模板对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="name">
              <el-input v-model="form.name" placeholder="如: 订单发货通知" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="code">
              <el-input v-model="form.code" placeholder="唯一编码, 如 ORDER_SHIP_NOTIFY" :disabled="form.id != null" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="模板类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
                <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题模板" prop="titleTemplate">
          <el-input v-model="form.titleTemplate" placeholder="支持${变量}占位, 如: 订单已发货" />
        </el-form-item>
        <el-form-item label="内容模板" prop="contentTemplate">
          <el-input v-model="form.contentTemplate" type="textarea" :rows="4" placeholder="支持${变量}占位, 如: 您的订单${orderNo}已发货, 物流单号${logisticsNo}" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listNoticeTemplate, getNoticeTemplate, addNoticeTemplate, updateNoticeTemplate, changeNoticeTemplateStatus, delNoticeTemplate } from "@/api/life/noticeTemplate"

export default {
  name: "NoticeTemplate",
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      templateList: [],
      title: "",
      open: false,
      typeOptions: [
        { value: 1, label: "系统通知" },
        { value: 2, label: "订单通知" },
        { value: 3, label: "活动通知" },
        { value: 4, label: "权益通知" },
        { value: 5, label: "积分通知" }
      ],
      queryParams: { pageNum: 1, pageSize: 10, name: null, type: null, status: null },
      form: {},
      rules: {
        name: [{ required: true, message: "模板名称不能为空", trigger: "blur" }],
        code: [{ required: true, message: "模板编码不能为空", trigger: "blur" }],
        type: [{ required: true, message: "模板类型不能为空", trigger: "change" }],
        titleTemplate: [{ required: true, message: "标题模板不能为空", trigger: "blur" }],
        contentTemplate: [{ required: true, message: "内容模板不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    typeText(type) {
      const hit = this.typeOptions.find(item => item.value === type)
      return hit ? hit.label : type
    },
    getList() {
      this.loading = true
      listNoticeTemplate(this.queryParams).then(res => {
        this.templateList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    reset() {
      this.form = {
        id: null, name: null, code: null, type: null,
        titleTemplate: null, contentTemplate: null, status: 1
      }
      this.resetForm("form")
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增通知模板"
    },
    handleUpdate(row) {
      this.reset()
      getNoticeTemplate(row.id || this.ids[0]).then(res => {
        this.form = res.data
        this.open = true
        this.title = "修改通知模板"
      })
    },
    handleStatusChange(row) {
      const text = row.status === 1 ? "启用" : "禁用"
      this.$modal.confirm('确认' + text + '模板"' + row.name + '"?').then(() => {
        return changeNoticeTemplateStatus(row.id, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(() => {
        row.status = row.status === 1 ? 0 : 1
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateNoticeTemplate(this.form).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.open = false
            this.getList()
          })
        } else {
          addNoticeTemplate(this.form).then(() => {
            this.$modal.msgSuccess("新增成功")
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除所选通知模板(软删除)?').then(() => {
        return delNoticeTemplate(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    cancel() {
      this.open = false
      this.reset()
    }
  }
}
</script>
