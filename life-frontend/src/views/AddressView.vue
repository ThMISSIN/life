<template>
  <div class="container-shell">
    <AppSidebar />

    <main class="addr-main">
      <!-- 吸顶标题栏 -->
      <div class="addr-header">
        <button class="addr-header__back" @click="goBack">
          <span class="ms-icon">arrow_back</span>
        </button>
        <div class="addr-header__title-wrap">
          <h2 class="addr-header__title">收货地址</h2>
          <p class="addr-header__sub">下单时将以此地址快照发货，最多可维护 20 条</p>
        </div>
        <el-button type="primary" round class="addr-header__add" @click="openEdit(null)">
          <span class="ms-icon addr-header__add-icon">add</span>
          新增地址
        </el-button>
      </div>

      <div v-loading="loading" class="addr-body">
        <template v-if="list.length">
          <div v-for="a in list" :key="a.id" class="addr-card">
            <div class="addr-card__head">
              <div class="addr-card__who">
                <span class="addr-card__name">{{ a.receiverName }}</span>
                <span class="addr-card__phone">{{ a.receiverPhone }}</span>
                <span v-if="a.isDefault === 1" class="addr-card__default">默认</span>
              </div>
              <div class="addr-card__ops">
                <el-button text size="small" @click="openEdit(a)">编辑</el-button>
                <el-button
                  v-if="a.isDefault !== 1"
                  text
                  size="small"
                  @click="onSetDefault(a)"
                >
                  设为默认
                </el-button>
                <el-button text size="small" type="danger" @click="onDelete(a)">删除</el-button>
              </div>
            </div>
            <p class="addr-card__detail">
              <span class="ms-icon addr-card__loc">location_on</span>
              {{ a.province }}{{ a.city }}{{ a.district }} {{ a.address }}
            </p>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="还没有收货地址">
          <el-button type="primary" @click="openEdit(null)">新增地址</el-button>
        </el-empty>
      </div>
    </main>

    <!-- 新增/编辑地址弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editForm.id ? '编辑地址' : '新增地址'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="80px">
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="editForm.receiverName" maxlength="20" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="editForm.receiverPhone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="editForm.province" maxlength="20" placeholder="如：广东省" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="editForm.city" maxlength="20" placeholder="如：深圳市" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="editForm.district" maxlength="20" placeholder="如：南山区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input
            v-model="editForm.address"
            type="textarea"
            :rows="2"
            maxlength="100"
            show-word-limit
            placeholder="街道、门牌号等"
          />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="editForm.isDefaultBool" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 收货地址管理页（后端 UserAddressController /user/address/**）
 * - 新增 POST / 编辑 PUT（编辑时 id 必传）
 * - 设默认 PUT /{id}/default；删除 DELETE /{id}（逻辑删）
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import {
  getAddressList,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress,
} from '@/api/address'

const router = useRouter()

const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try {
    list.value = (await getAddressList()) || []
  } catch (e) {
    ElMessage.error(e.message || '地址加载失败')
  } finally {
    loading.value = false
  }
}

// —— 新增/编辑 ——
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)

const editForm = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  address: '',
  isDefaultBool: false,
})

const rules = {
  receiverName: [{ required: true, message: '请填写收件人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请填写手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  province: [{ required: true, message: '请填写省份', trigger: 'blur' }],
  city: [{ required: true, message: '请填写城市', trigger: 'blur' }],
  district: [{ required: true, message: '请填写区县', trigger: 'blur' }],
  address: [{ required: true, message: '请填写详细地址', trigger: 'blur' }],
}

function openEdit(a) {
  if (a) {
    Object.assign(editForm, {
      id: a.id,
      receiverName: a.receiverName,
      receiverPhone: a.receiverPhone,
      province: a.province,
      city: a.city,
      district: a.district,
      address: a.address,
      isDefaultBool: a.isDefault === 1,
    })
  } else {
    Object.assign(editForm, {
      id: null,
      receiverName: '',
      receiverPhone: '',
      province: '',
      city: '',
      district: '',
      address: '',
      isDefaultBool: list.value.length === 0,
    })
  }
  editVisible.value = true
}

async function onSave() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      receiverName: editForm.receiverName.trim(),
      receiverPhone: editForm.receiverPhone.trim(),
      province: editForm.province.trim(),
      city: editForm.city.trim(),
      district: editForm.district.trim(),
      address: editForm.address.trim(),
      isDefault: editForm.isDefaultBool ? 1 : 0,
    }
    if (editForm.id) {
      await updateAddress({ id: editForm.id, ...payload })
    } else {
      await addAddress(payload)
    }
    ElMessage.success('地址已保存')
    editVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onSetDefault(a) {
  try {
    await setDefaultAddress(a.id)
    ElMessage.success('已设为默认地址')
    load()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function onDelete(a) {
  try {
    await ElMessageBox.confirm(`删除「${a.receiverName}」的这条地址？`, '删除地址', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteAddress(a.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

function goBack() {
  router.back()
}

onMounted(load)
</script>

<style scoped>
.addr-main {
  width: 100%;
  max-width: 760px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  min-height: 100vh;
}

.addr-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  background: rgba(250, 248, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.addr-header__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text);
  cursor: pointer;
  transition: background-color 0.2s;
}

.addr-header__back:hover {
  background: var(--color-surface-hover);
}

.addr-header__title-wrap {
  flex: 1;
}

.addr-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.addr-header__sub {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.addr-header__add-icon {
  font-size: 16px;
  margin-right: 2px;
}

.addr-body {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.addr-card {
  padding: 14px 16px;
  border: 1px solid var(--color-border-strong);
  border-radius: 14px;
  background: var(--color-surface);
}

.addr-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.addr-card__who {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.addr-card__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.addr-card__phone {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.addr-card__default {
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-orange);
  border: 1px solid var(--color-orange);
  border-radius: 4px;
}

.addr-card__detail {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.addr-card__loc {
  font-size: 16px;
  color: var(--color-primary);
  flex-shrink: 0;
}
</style>
