<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="name" placeholder="按分类名称过滤" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="正常" :value="1" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['life:productCategory:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-sort" size="mini" @click="toggleExpandAll">展开/折叠</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-if="refreshTable" v-loading="loading" :data="categoryTree" row-key="id" :default-expand-all="isExpandAll" :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
      <el-table-column label="分类名称" prop="name" min-width="180" />
      <el-table-column label="图标" align="center" width="80">
        <template slot-scope="scope">
          <el-image v-if="scope.row.icon" style="width: 36px; height: 36px" :src="lifeImgUrl(scope.row.icon)" fit="cover">
            <div slot="error" class="image-slot"><i class="el-icon-picture-outline"></i></div>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column label="层级" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.level === 1 ? 'primary' : 'info'">{{ scope.row.level === 1 ? '一级' : '二级' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序号" prop="sort" align="center" width="80" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" v-hasPermi="['life:productCategory:edit']"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-if="scope.row.level === 1" size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)" v-hasPermi="['life:productCategory:add']">新增</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['life:productCategory:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['life:productCategory:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改商品分类对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择上级分类" style="width: 100%">
            <el-option label="顶级分类" :value="0" />
            <el-option v-for="item in firstLevelCategories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <div class="el-form-item-msg">选择顶级分类为一级分类; 选择一级分类自动归为二级</div>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类图标" prop="icon">
          <ImageUpload v-model="form.icon" :limit="1" action="/life/common/upload" />
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { listProductCategory, getProductCategory, addProductCategory, updateProductCategory, delProductCategory } from "@/api/life/productCategory"
import { lifeImgUrl } from "@/utils/life"

export default {
  name: "ProductCategory",
  data() {
    return {
      loading: false,
      showSearch: true,
      isExpandAll: true,
      refreshTable: true,
      categoryTree: [],
      title: "",
      open: false,
      name: "",
      queryParams: { status: null },
      form: {},
      rules: {
        name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }],
        parentId: [{ required: true, message: "上级分类不能为空", trigger: "change" }]
      }
    }
  },
  computed: {
    firstLevelCategories() {
      return (this.categoryTree || []).filter(c => c.level === 1)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    lifeImgUrl,
    getList() {
      this.loading = true
      listProductCategory(this.queryParams).then(res => {
        const list = (res.data || []).filter(c => !this.name || c.name.indexOf(this.name) > -1)
        // 两级分类树: 直接按 parent_id 组装
        const roots = list.filter(c => c.parentId === 0).map(root => {
          const children = list.filter(c => c.parentId === root.id)
          return children.length ? { ...root, children } : { ...root }
        })
        this.categoryTree = roots
        this.loading = false
      })
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.name = ""
      this.resetForm("queryForm")
      this.handleQuery()
    },
    toggleExpandAll() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },
    reset() {
      this.form = { id: null, parentId: 0, name: null, icon: null, sort: 0, status: 1 }
      this.resetForm("form")
    },
    handleAdd(row) {
      this.reset()
      if (row && row.id) {
        this.form.parentId = row.id
      }
      this.open = true
      this.title = "新增商品分类"
    },
    handleUpdate(row) {
      this.reset()
      getProductCategory(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = "修改商品分类"
      })
    },
    handleStatusChange(row) {
      const text = row.status === 1 ? "启用" : "禁用"
      this.$modal.confirm('确认将分类"' + row.name + '"执行' + text + '操作?').then(() => {
        return updateProductCategory({ id: row.id, status: row.status })
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
          updateProductCategory(this.form).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.open = false
            this.getList()
          })
        } else {
          addProductCategory(this.form).then(() => {
            this.$modal.msgSuccess("新增成功")
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除分类"' + row.name + '"(存在子分类或挂载商品时禁止)?').then(() => {
        return delProductCategory(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
