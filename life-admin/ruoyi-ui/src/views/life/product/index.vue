<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="商品名称/店铺名" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-cascader v-model="categoryId" :options="categoryOptions" :props="categoryProps" placeholder="全部分类" clearable style="width: 200px" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['life:product:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['life:product:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['life:product:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['life:product:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="封面" align="center" width="80">
        <template slot-scope="scope">
          <el-image style="width: 48px; height: 48px" :src="lifeImgUrl(scope.row.coverImage)" :preview-src-list="[lifeImgUrl(scope.row.coverImage)]" fit="cover">
            <div slot="error" class="image-slot"><i class="el-icon-picture-outline"></i></div>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column label="商品名称" prop="name" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="所属分类" prop="categoryName" align="center" width="110" />
      <el-table-column label="店铺" prop="shopName" :show-overflow-tooltip="true" width="110" />
      <el-table-column label="现金价(元)" align="center" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.price">{{ fenToYuan(scope.row.price) }}</span>
          <el-tag v-else type="info" size="mini">不可现金购</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="积分价" align="center" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.pointsPrice">{{ scope.row.pointsPrice }}</span>
          <el-tag v-else type="info" size="mini">不可兑换</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="库存" align="center" width="80">
        <template slot-scope="scope">
          <span>{{ scope.row.stock === -1 ? '不限' : scope.row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column label="销量" prop="sales" align="center" width="70" />
      <el-table-column label="上架" align="center" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" v-hasPermi="['life:product:edit']"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" align="center" width="70" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="130" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['life:product:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['life:product:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改商品对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-cascader v-model="form.categoryId" :options="categoryOptions" :props="categoryProps" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="店铺名称" prop="shopName">
              <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发货地" prop="location">
              <el-input v-model="form.location" placeholder="请输入发货地" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="现金价格(元)" prop="priceYuan">
              <el-input-number v-model="form.priceYuan" :min="0" :precision="2" :step="1" style="width: 100%" />
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
              <div class="el-form-item-msg">0表示不可积分兑换</div>
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
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="-1" :step="1" style="width: 100%" />
              <div class="el-form-item-msg">-1表示不限量</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低等级" prop="minLevel">
              <el-input-number v-model="form.minLevel" :min="0" :step="1" style="width: 100%" />
              <div class="el-form-item-msg">0表示不限</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="逗号分隔, 如: 新品,热卖" />
        </el-form-item>
        <el-form-item label="封面图" prop="coverImage">
          <ImageUpload v-model="form.coverImage" :limit="1" action="/life/common/upload" />
        </el-form-item>
        <el-form-item label="详情图" prop="imageList">
          <ImageUpload v-model="form.imageList" :limit="9" action="/life/common/upload" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
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
import { listProduct, getProduct, addProduct, updateProduct, changeProductStatus, delProduct } from "@/api/life/product"
import { listProductCategory } from "@/api/life/productCategory"
import { fenToYuan, yuanToFen, lifeImgUrl } from "@/utils/life"

export default {
  name: "Product",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      productList: [],
      title: "",
      open: false,
      categoryId: [],
      categoryOptions: [],
      categoryProps: { checkStrictly: true, emitPath: false, value: "id", label: "name", children: "children" },
      queryParams: { pageNum: 1, pageSize: 10, keyword: null, categoryId: null, status: null },
      form: {},
      rules: {
        name: [{ required: true, message: "商品名称不能为空", trigger: "blur" }],
        categoryId: [{ required: true, message: "商品分类不能为空", trigger: "change" }]
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
    /** 分类两极树(管理端含禁用) */
    getCategoryTree() {
      listProductCategory().then(res => {
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
      listProduct(this.queryParams).then(res => {
        this.productList = res.rows
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
        tags: null, shopName: null, location: null, priceYuan: null, originalPriceYuan: null,
        pointsPrice: 0, perUserLimit: 0, minLevel: 0, stock: 0, status: 0, sort: 0
      }
      this.resetForm("form")
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增商品"
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids[0]
      getProduct(id).then(res => {
        const data = res.data
        this.form = {
          ...data,
          priceYuan: data.price ? data.price / 100 : null,
          originalPriceYuan: data.originalPrice ? data.originalPrice / 100 : null,
          imageList: this.imagesToJson(data.images)
        }
        this.open = true
        this.title = "修改商品"
      })
    },
    /** 详情图: 库中JSON数组串 <-> 上传组件逗号串 */
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
      this.$modal.confirm('确认将商品"' + row.name + '"执行' + text + '操作?').then(() => {
        return changeProductStatus(row.id, row.status)
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
          price: yuanToFen(this.form.priceYuan),
          originalPrice: yuanToFen(this.form.originalPriceYuan),
          images: this.jsonToImages(this.form.imageList)
        }
        delete data.priceYuan
        delete data.originalPriceYuan
        delete data.imageList
        if (this.form.id != null) {
          updateProduct(data).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.open = false
            this.getList()
          })
        } else {
          addProduct(data).then(() => {
            this.$modal.msgSuccess("新增成功")
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除所选商品(软删除)?').then(() => {
        return delProduct(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download("life/product/export", { ...this.queryParams }, `product_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
