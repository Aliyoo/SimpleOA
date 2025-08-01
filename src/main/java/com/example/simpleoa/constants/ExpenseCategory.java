package com.example.simpleoa.constants;

import java.util.Arrays;
import java.util.List;

/**
 * 费用类别常量类
 */
public class ExpenseCategory {
    
    // 费用类别常量
    public static final String LABOR_FEE = "劳务费";
    public static final String HOUSING_FEE = "房屋费";
    public static final String TRAVEL_EXPENSE = "差旅费";
    public static final String TRANSPORTATION_FEE = "交通费";
    public static final String OFFICE_EXPENSE = "办公费";
    public static final String COMMUNICATION_FEE = "通信费";
    public static final String VEHICLE_FEE = "车辆费";
    public static final String FREIGHT_FEE = "货运费";
    public static final String MATERIAL_CONSUMPTION = "物料消耗费";
    public static final String REVIEW_ACCEPTANCE_FEE = "评审验收费";
    public static final String OVERTIME_MEAL_FEE = "加班餐费";
    public static final String WARRANTY_MAINTENANCE_FEE = "质保维护费(不含人工)";
    public static final String BUSINESS_ENTERTAINMENT = "业务招待费";
    
    /**
     * 获取所有费用类别列表
     */
    public static List<String> getAllCategories() {
        return Arrays.asList(
            LABOR_FEE,
            HOUSING_FEE,
            TRAVEL_EXPENSE,
            TRANSPORTATION_FEE,
            OFFICE_EXPENSE,
            COMMUNICATION_FEE,
            VEHICLE_FEE,
            FREIGHT_FEE,
            MATERIAL_CONSUMPTION,
            REVIEW_ACCEPTANCE_FEE,
            OVERTIME_MEAL_FEE,
            WARRANTY_MAINTENANCE_FEE,
            BUSINESS_ENTERTAINMENT
        );
    }
    
    /**
     * 验证费用类别是否有效
     */
    public static boolean isValidCategory(String category) {
        return getAllCategories().contains(category);
    }
    
    /**
     * 费用类别描述
     */
    public static String getCategoryDescription(String category) {
        switch (category) {
            case LABOR_FEE:
                return "支付给个人或单位的劳务报酬";
            case HOUSING_FEE:
                return "房租、物业费等房屋相关费用";
            case TRAVEL_EXPENSE:
                return "出差期间的交通、住宿、补贴等费用";
            case TRANSPORTATION_FEE:
                return "日常交通费用，如打车、公交等";
            case OFFICE_EXPENSE:
                return "办公用品、文具等费用";
            case COMMUNICATION_FEE:
                return "电话费、网络费等通信费用";
            case VEHICLE_FEE:
                return "车辆使用、维护、保养等费用";
            case FREIGHT_FEE:
                return "物流、快递、货运等费用";
            case MATERIAL_CONSUMPTION:
                return "生产或项目所需的物料消耗";
            case REVIEW_ACCEPTANCE_FEE:
                return "项目评审、验收等相关费用";
            case OVERTIME_MEAL_FEE:
                return "加班期间的餐饮费用";
            case WARRANTY_MAINTENANCE_FEE:
                return "质保期内的维护费用，不包含人工成本";
            case BUSINESS_ENTERTAINMENT:
                return "业务招待、商务宴请等费用";
            default:
                return "";
        }
    }
}
