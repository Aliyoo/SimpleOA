package com.example.simpleoa.model;

public enum LeaveType {
    ANNUAL_LEAVE("年假", 15),           // 年假，默认15天
    SICK_LEAVE("病假", 10),             // 病假，默认10天
    PERSONAL_LEAVE("事假", 5),          // 事假，默认5天
    MARRIAGE_LEAVE("婚假", 3),          // 婚假，默认3天
    MATERNITY_LEAVE("产假", 98),        // 产假，默认98天
    PATERNITY_LEAVE("陪产假", 15),      // 陪产假，默认15天
    BEREAVEMENT_LEAVE("丧假", 3),       // 丧假，默认3天
    OTHER("其他", 0);                   // 其他
    
    private final String displayName;
    private final int defaultDays;
    
    LeaveType(String displayName, int defaultDays) {
        this.displayName = displayName;
        this.defaultDays = defaultDays;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDefaultDays() {
        return defaultDays;
    }
}
