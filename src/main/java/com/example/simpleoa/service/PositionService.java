package com.example.simpleoa.service;

import com.example.simpleoa.model.Position;
import com.example.simpleoa.model.User;

import java.util.List;
import java.util.Map;

public interface PositionService {
    // 岗位基本管理
    Position createPosition(Position position);
    Position updatePosition(Position position);
    void deletePosition(Long id);
    Position getPositionById(Long id);
    List<Position> getAllPositions();
    List<Position> getPositionsByDepartment(String department);
    List<Position> getPositionsByLevel(String level);
    List<Position> getActivePositions();
    List<Position> searchPositionsByName(String name);
    Long countUsersByPosition(Long positionId);
    
    // 用户岗位管理
    User assignPositionToUser(Long userId, Long positionId);
    List<User> getUsersByPosition(Long positionId);
    
    // 岗位统计分析
    Map<String, Object> getPositionStatsByDepartment();
    Map<String, Object> getPositionDistribution();
}
