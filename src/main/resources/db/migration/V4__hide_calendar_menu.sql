-- 隐藏日历管理菜单
UPDATE menu SET is_hidden = TRUE WHERE id = 16 AND name = '日历管理';
