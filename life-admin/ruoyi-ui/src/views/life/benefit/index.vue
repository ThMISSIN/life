<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="权益名称" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-cascader v-model="categoryId" :options="categoryOptions" :props="categoryProps" placeholder="全部分类" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="权益类型" prop="benefitType">
        <el-select v-model="queryParams.benefitType" placeholder="全部类型" clearable style="width: 140px">
          <el-option label="券码类" :value="1" />
          <el-option label="直充类" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['life:benefit:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['life:benefit:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['life:benefit:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['life:benefit:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="benefitList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="封面" align="center" width="80">
        <template slot-scope="scope">
          <el-image style="width: 48px; height: 48px" :src="lifeImgUrl(scope.row.coverImage)" :preview-src-list="[lifeImgUrl(scope.row.coverImage)]" fit="cover">
            <div slot="error" class="image-slot"><i class="el-icon-picture-outline"></i></div>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column label="权益名称" prop="name" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="所属分类" prop="categoryName" align="center" width="110" />
      <el-table-column label="权益类型" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.benefitType === 1 ? 'primary' : 'success'">{{ scope.row.benefitType === 1 ? '券码类' : '直充类' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="积分价" prop="pointsPrice" align="center" width="80" />
      <el-table-column label="原价(元)" align="center" width="90">
        <template slot-scope="scope">{{ fenToYuan(scope.row.originalPrice) }}</template>
      </el-table-column>
      <el-table-column label="有效期" align="center" width="110">
        <template slot-scope="scope">
          <span v-if="scope.row.validType === 1">{{ scope.row.validDays }} 天</span>
          <span v-else-if="scope.row.validType === 2">{{ parseTime(scope.row.fixedEndTime, '{y}-{m}-{d}') }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" align="center" width="70" />
      <el-table-column label="销量" prop="sales" align="center" width="70" />
      <el-table-column label="上架" align="center" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" v-hasPermi="['life:benefit:edit']"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="130" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['life:benefit:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['life:benefit:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改权益对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="权益名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入权益名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权益分类" prop="categoryId">
              <el-cascader v-model="form.categoryId" :options="categoryOptions" :props="categoryProps" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="权益类型" prop="benefitType">
              <el-radio-group v-model="form.benefitType">
                <el-radio :label="1">券码类</el-radio>
                <el-radio :label="2">直充类</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价(元)" prop="originalPriceYuan">
              <el-input-number v-model="form.originalPriceYuan" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="积分价格" prop="pointsPrice">
              <el-input-number v-model="form.pointsPrice" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每人限兑" prop="perUserLimit">
              <el-input-number v-model="form.perUserLimit" :min="0" :step="1" style="width: 100%" />
              <div class="el-form-item-msg">0表示不限</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="有效期类型" prop="validType">
              <el-radio-group v-model="form.validType">
                <el-radio :label="1">相对天数</el-radio>
                <el-radio :label="2">固定截止日</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.validType === 1" label="有效天数" prop="validDays">
              <el-input-number v-model="form.validDays" :min="1" :step="1" style="width: 100%" />
            </el-form-item>
            <el-form-item v-if="form.validType === 2" label="截止时间" prop="fixedEndTime">
              <el-date-picker v-model="form.fixedEndTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择固定截止时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低等级" prop="minLevel">
              <el-input-number v-model="form.minLevel" :min="0" :step="1" style="width: 100%" />
              <div class="el-form-item-msg">0表示不限</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图" prop="coverImage">
          <ImageUpload v-model="form.coverImage" :limit="1" action="/life/common/upload" />
        </el-form-item>
        <el-form-item label="详情图" prop="imageList">
          <ImageUpload v-model="form.imageList" :limit="9" action="/life/common/upload" />
        </el-form-item>
        <el-form-item label="权益描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入权益描述" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">上架</el-radio>
                <el-radio :label="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sort">
              <el-input-number v-model="form.sort" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listBenefit, getBenefit, addBenefit, updateBenefit, changeBenefitStatus, delBenefit } from "@/api/life/benefit"
import { listBenefitCategory } from "@/api/life/benefitCategory"
import { fenToYuan, yuanToFen, lifeImgUrl } from "@/utils/life"

export default {
  name: "Benefit",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      benefitList: [],
      title: "",
      open: false,
      categoryId: [],
      categoryOptions: [],
      categoryProps: { checkStrictly: true, emitPath: false, value: "id", label: "name", children: "children" },
      queryParams: { pageNum: 1, pageSize: 10, keyword: null, categoryId: null, status: null, benefitType: null },
      form: {},
      rules: {
        name: [{ required: true, message: "权益名称不能为空", trigger: "blur" }],
        categoryId: [{ required: true, message: "权益分类不能为空", trigger: "change" }],
        benefitType: [{ required: true, message: "权益类型不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getCategoryTree()
    this.getList()
  },
  methods: {
    lifeImgUrl,
    fenToYuan,
    getCategoryTree() {
      listBenefitCategory().then(res => {
        const list = res.data || []
        const roots = list.filter(c => c.parentId === 0)
        this.categoryOptions = roots.map(root => {
          const children = list.filter(c => c.parentId === root.id)
          return children.length ? { id: root.id, name: root.name, children } : { id: root.id, name: root.name }
        })
      })
    },
    getList() {
      this.loading = true
      this.queryParams.categoryId = this.categoryId && this.categoryId.length ? this.categoryId : null
      listBenefit(this.queryParams).then(res => {
        this.benefitList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.categoryId = []
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    reset() {
      this.form = {
        id: null, name: null, categoryId: null, description: null, coverImage: null, imageList: null,
        benefitType: 1, pointsPrice: 0, originalPriceYuan: null, validType: 1, validDays: 30,
        fixedEndTime: null, perUserLimit: 0, minLevel: 0, stock: 0, sales: 0, status: 0, sort: 0
      }
      this.resetForm("form")
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增权益"
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids[0]
      getBenefit(id).then(res => {
        const data = res.data
        this.form = {
          ...data,
          originalPriceYuan: data.originalPrice ? data.originalPrice / 100 : null,
          imageList: this.imagesToJson(data.images)
        }
        this.open = true
        this.title = "修改权益"
      })
    },
    imagesToJson(images) {
      if (!images) return ""
      try {
        const arr = JSON.parse(images)
        return Array.isArray(arr) ? arr.join(",") : images
      } catch (e) {
        return images
      }
    },
    jsonToImages(imageList) {
      if (!imageList) return null
      const arr = String(imageList).split(",").filter(Boolean)
      return arr.length ? JSON.stringify(arr) : null
    },
    handleStatusChange(row) {
      const text = row.status === 1 ? "上架" : "下架"
      this.$modal.confirm('确认将权益"' + row.name + '"执行' + text + '操作?').then(() => {
        return changeBenefitStatus(row.id, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(() => {
        row.status = row.status === 1 ? 0 : 1
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        const data = {
          ...this.form,
          originalPrice: yuanToFen(this.form.originalPriceYuan),
          images: this.jsonToImages(this.form.imageList)
        }
        delete data.originalPriceYuan
        delete data.imageList
        if (this.form.id != null) {
          updateBenefit(data).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.open = false
            this.getList()
          })
        } else {
          addBenefit(data).then(() => {
            this.$modal.msgSuccess("新增成功")
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除所选权益(存在待生效/未使用发放记录时禁止)?').then(() => {
        return delBenefit(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download("life/benefit/export", { ...this.queryParams }, `benefit_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
