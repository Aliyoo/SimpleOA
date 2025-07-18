// 全局配置文件
export const APP_CONFIG = {
  // 默认日期范围配置
  DEFAULT_DATE_RANGE: {
    // 默认开始日期：上月25号
    start: () => {
      const now = new Date();
      const lastMonth = now.getMonth() === 0 ? 11 : now.getMonth() - 1;
      const lastYear = now.getMonth() === 0 ? now.getFullYear() - 1 : now.getFullYear();
      return new Date(lastYear, lastMonth, 25);
    },
    // 默认结束日期：本月24号
    end: () => {
      const now = new Date();
      return new Date(now.getFullYear(), now.getMonth(), 24);
    },
    // 格式化日期为 YYYY-MM-DD
    formatDate: (date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    // 格式化月份为 YYYY-MM
    formatMonth: (date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      return `${year}-${month}`;
    },
    // 获取默认日期范围（字符串格式 YYYY-MM-DD）
    getRange: () => {
      const start = APP_CONFIG.DEFAULT_DATE_RANGE.start();
      const end = APP_CONFIG.DEFAULT_DATE_RANGE.end();
      return [APP_CONFIG.DEFAULT_DATE_RANGE.formatDate(start), APP_CONFIG.DEFAULT_DATE_RANGE.formatDate(end)];
    },
    // 获取默认月份范围（字符串格式 YYYY-MM）
    getMonthRange: () => {
      const start = APP_CONFIG.DEFAULT_DATE_RANGE.start();
      const end = APP_CONFIG.DEFAULT_DATE_RANGE.end();
      return [APP_CONFIG.DEFAULT_DATE_RANGE.formatMonth(start), APP_CONFIG.DEFAULT_DATE_RANGE.formatMonth(end)];
    },
    // 获取默认日期范围（Date 对象格式）
    getRangeDates: () => {
      return [APP_CONFIG.DEFAULT_DATE_RANGE.start(), APP_CONFIG.DEFAULT_DATE_RANGE.end()];
    }
  }
};