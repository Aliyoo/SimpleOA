import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ReimbursementDetail from '../views/ReimbursementDetail.vue'
import { ElDialog, ElButton, ElDescriptions, ElDescriptionsItem, ElTable, ElTableColumn, ElImage, ElIcon, ElLink, ElEmpty } from 'element-plus'

// Mock axios
vi.mock('../utils/axios.js', () => ({
  default: {
    get: vi.fn()
  }
}))

// Mock element-plus icons
vi.mock('@element-plus/icons-vue', () => ({
  Picture: { name: 'Picture' }
}))

// Mock format utils
vi.mock('../utils/format.js', () => ({
  formatMoney: vi.fn((amount) => `¥${amount}`),
  formatDate: vi.fn((date) => date),
  formatReimbursementStatus: vi.fn((status) => status),
  getReimbursementStatusTagType: vi.fn(() => 'primary')
}))

const mockReimbursementData = {
  id: 1,
  title: '测试报销申请',
  applicant: {
    id: 1,
    username: 'testuser',
    realName: '测试用户'
  },
  project: {
    id: 1,
    name: '测试项目'
  },
  status: 'APPROVED',
  createTime: '2024-01-01 10:00:00',
  totalAmount: 1000.00,
  items: [
    {
      id: 1,
      expenseDate: '2024-01-01',
      itemCategory: '交通费',
      description: '出差车费',
      amount: 500.00,
      budget: {
        name: '差旅预算'
      }
    },
    {
      id: 2,
      expenseDate: '2024-01-02',
      itemCategory: '餐费',
      description: '午餐费用',
      amount: 500.00,
      budgetItem: {
        name: '餐饮预算'
      }
    }
  ],
  attachments: [
    'http://example.com/receipt1.jpg',
    'http://example.com/receipt2.png',
    'http://example.com/invoice.pdf'
  ]
}

describe('ReimbursementDetail', () => {
  let wrapper

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该在传入 mock 数据时正确渲染所有字段', async () => {
    wrapper = mount(ReimbursementDetail, {
      props: {
        visible: true,
        reimbursement: mockReimbursementData
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElDescriptions,
          ElDescriptionsItem,
          ElTable,
          ElTableColumn,
          ElImage,
          ElIcon,
          ElLink,
          ElEmpty
        }
      }
    })

    await wrapper.vm.$nextTick()

    // 验证基本信息显示
    expect(wrapper.text()).toContain('测试报销申请')
    expect(wrapper.text()).toContain('测试用户')
    expect(wrapper.text()).toContain('测试项目')
    expect(wrapper.text()).toContain('¥1000')

    // 验证费用明细表格
    expect(wrapper.text()).toContain('交通费')
    expect(wrapper.text()).toContain('出差车费')
    expect(wrapper.text()).toContain('¥500')
    expect(wrapper.text()).toContain('差旅预算')
    
    expect(wrapper.text()).toContain('餐费')
    expect(wrapper.text()).toContain('午餐费用')
    expect(wrapper.text()).toContain('餐饮预算')

    // 验证附件部分
    expect(wrapper.text()).toContain('附件')
  })

  it('应该正确处理图片附件', () => {
    wrapper = mount(ReimbursementDetail, {
      props: {
        visible: true,
        reimbursement: mockReimbursementData
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElDescriptions,
          ElDescriptionsItem,
          ElTable,
          ElTableColumn,
          ElImage,
          ElIcon,
          ElLink,
          ElEmpty
        }
      }
    })

    // 测试 isImage 方法
    expect(wrapper.vm.isImage('test.jpg')).toBe(true)
    expect(wrapper.vm.isImage('test.png')).toBe(true)
    expect(wrapper.vm.isImage('test.pdf')).toBe(false)

    // 测试 getImageAttachments 方法
    const imageAttachments = wrapper.vm.getImageAttachments()
    expect(imageAttachments).toHaveLength(2)
    expect(imageAttachments).toContain('http://example.com/receipt1.jpg')
    expect(imageAttachments).toContain('http://example.com/receipt2.png')
  })

  it('应该正确处理关闭操作', async () => {
    wrapper = mount(ReimbursementDetail, {
      props: {
        visible: true,
        reimbursement: mockReimbursementData
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElDescriptions,
          ElDescriptionsItem,
          ElTable,
          ElTableColumn,
          ElImage,
          ElIcon,
          ElLink,
          ElEmpty
        }
      }
    })

    // 触发关闭操作
    wrapper.vm.handleClose()

    // 验证 close 事件被触发
    expect(wrapper.emitted().close).toBeTruthy()
    expect(wrapper.emitted().close).toHaveLength(1)
  })

  it('应该正确处理空数据情况', () => {
    wrapper = mount(ReimbursementDetail, {
      props: {
        visible: true,
        reimbursement: null
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElDescriptions,
          ElDescriptionsItem,
          ElTable,
          ElTableColumn,
          ElImage,
          ElIcon,
          ElLink,
          ElEmpty
        }
      }
    })

    // 验证显示无数据状态
    expect(wrapper.text()).toContain('无数据')
  })

  it('应该正确处理无附件情况', () => {
    const dataWithoutAttachments = {
      ...mockReimbursementData,
      attachments: []
    }

    wrapper = mount(ReimbursementDetail, {
      props: {
        visible: true,
        reimbursement: dataWithoutAttachments
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElDescriptions,
          ElDescriptionsItem,
          ElTable,
          ElTableColumn,
          ElImage,
          ElIcon,
          ElLink,
          ElEmpty
        }
      }
    })

    // 验证显示暂无附件
    expect(wrapper.text()).toContain('暂无附件')
  })
})
