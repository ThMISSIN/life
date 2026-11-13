<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标题" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="公告标题" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 140px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已撤回" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['life:announcement:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="announcementList">
      <el-table-column label="ID" prop="id" align="center" width="70" />
      <el-table-column label="公告标题" prop="title" :show-overflow-tooltip="true" min-width="180" />
      <el-table-column label="类型" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini">{{ typeText(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" align="center" width="70">
        <template slot-scope="scope">
          <el-tag size="mini" type="danger" v-if="scope.row.top === 1">置顶</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" align="center" prop="publishTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.publishTime) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-if="scope.row.status !== 1" v-hasPermi="['life:announcement:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-upload2" @click="handlePublish(scope.row)" v-if="scope.row.status !== 1" v-hasPermi="['life:announcement:publish']">发布</el-button>
          <el-button size="mini" type="text" icon="el-icon-download" @click="handleRevoke(scope.row)" v-if="scope.row.status === 1" v-hasPermi="['life:announcement:publish']">下架</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['life:announcement:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改公告对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="公告标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入公告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公告类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
                <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否置顶" prop="top">
              <el-radio-group v-model="form.top">
                <el-radio :label="0">否</el-radio>
                <el-radio :label="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.id == null">
            <el-form-item label="保存方式" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="0">存为草稿</el-radio>
                <el-radio :label="1">直接发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
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
import { listAnnouncement, getAnnouncement, addAnnouncement, updateAnnouncement, publishAnnouncement, revokeAnnouncement, delAnnouncement } from "@/api/life/announcement"

export default {
  name: "Announcement",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      announcementList: [],
      title: "",
      open: false,
      typeOptions: [
        { value: 1, label: "系统公告" },
        { value: 2, label: "活动公告" },
        { value: 3, label: "权益公告" }
      ],
      queryParams: { pageNum: 1, pageSize: 10, keyword: null, type: null, status: null },
      form: {},
      rules: {
        title: [{ required: true, message: "公告标题不能为空", trigger: "blur" }],
        type: [{ required: true, message: "公告类型不能为空", trigger: "change" }],
        content: [{ required: true, message: "公告内容不能为空", trigger: "blur" }]
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
    statusText(status) {
      return status === 1 ? "已发布" : (status === 2 ? "已撤回" : "草稿")
    },
    statusTagType(status) {
      return status === 1 ? "success" : (status === 2 ? "info" : "warning")
    },
    getList() {
      this.loading = true
      listAnnouncement(this.queryParams).then(res => {
        this.announcementList = res.rows
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
    reset() {
      this.form = {
        id: null, title: null, content: null, type: null, top: 0, status: 0
      }
      this.resetForm("form")
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增公告"
    },
    handleUpdate(row) {
      this.reset()
      getAnnouncement(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = "修改公告"
      })
    },
    handlePublish(row) {
      this.$modal.confirm('发布公告后将向全体用户收件箱投递该公告并实时推送, 确认发布"' + row.title + '"?').then(() => {
        return publishAnnouncement(row.id)
      }).then(() => {
        this.$modal.msgSuccess("发布成功")
        this.getList()
      }).catch(() => {})
    },
    handleRevoke(row) {
      this.$modal.confirm('确认下架(撤回)公告"' + row.title + '"? 用户收件箱中的已读状态不受影响').then(() => {
        return revokeAnnouncement(row.id)
      }).then(() => {
        this.$modal.msgSuccess("已下架")
        this.getList()
      }).catch(() => {})
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateAnnouncement(this.form).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.open = false
            this.getList()
          })
        } else {
          addAnnouncement(this.form).then(() => {
            this.$modal.msgSuccess(this.form.status === 1 ? "新增并发布成功" : "已存为草稿")
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除公告"' + row.title + '"(软删除)?').then(() => {
        return delAnnouncement(row.id)
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
