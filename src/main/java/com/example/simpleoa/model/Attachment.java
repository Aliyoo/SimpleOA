package com.example.simpleoa.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附件信息嵌入类
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    
    private String url;           // 文件访问路径
    private String originalName;  // 原始文件名
}
