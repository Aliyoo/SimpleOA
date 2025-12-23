package com.example.simpleoa.repository;

import com.example.simpleoa.model.LeaveSystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 假期系统配置数据访问接口
 */
@Repository
public interface LeaveSystemConfigRepository extends JpaRepository<LeaveSystemConfig, Long> {

    /**
     * 根据配置键查找配置
     * @param configKey 配置键
     * @return 配置
     */
    Optional<LeaveSystemConfig> findByConfigKey(String configKey);

    /**
     * 根据配置类型查找配置列表
     * @param configType 配置类型
     * @return 配置列表
     */
    List<LeaveSystemConfig> findByConfigType(String configType);

    /**
     * 查找所有系统配置
     * @return 系统配置列表
     */
    List<LeaveSystemConfig> findByIsSystemTrue();

    /**
     * 查找所有非系统配置
     * @return 非系统配置列表
     */
    List<LeaveSystemConfig> findByIsSystemFalse();

    /**
     * 根据配置键模糊匹配查找配置
     * @param configKeyPattern 配置键模式
     * @return 配置列表
     */
    @Query("SELECT c FROM LeaveSystemConfig c WHERE c.configKey LIKE %:pattern%")
    List<LeaveSystemConfig> findByConfigKeyContaining(@Param("pattern") String configKeyPattern);

    /**
     * 检查配置键是否存在
     * @param configKey 配置键
     * @return 是否存在
     */
    boolean existsByConfigKey(String configKey);

    /**
     * 统计系统配置数量
     * @return 系统配置数量
     */
    @Query("SELECT COUNT(c) FROM LeaveSystemConfig c WHERE c.isSystem = true")
    long countSystemConfigs();

    /**
     * 统计非系统配置数量
     * @return 非系统配置数量
     */
    @Query("SELECT COUNT(c) FROM LeaveSystemConfig c WHERE c.isSystem = false")
    long countCustomConfigs();

    /**
     * 根据创建人查找配置
     * @param createdBy 创建人ID
     * @return 配置列表
     */
    List<LeaveSystemConfig> findByCreatedBy(Long createdBy);

    /**
     * 根据更新人查找配置
     * @param updatedBy 更新人ID
     * @return 配置列表
     */
    List<LeaveSystemConfig> findByUpdatedBy(Long updatedBy);

    /**
     * 查找指定描述模糊匹配的配置
     * @param description 描述关键词
     * @return 配置列表
     */
    List<LeaveSystemConfig> findByDescriptionContainingIgnoreCase(String description);
}