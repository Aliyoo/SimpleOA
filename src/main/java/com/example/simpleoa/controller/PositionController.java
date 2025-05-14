package com.example.simpleoa.controller;

import com.example.simpleoa.model.Position;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/positions")
public class PositionController {
    private final PositionService positionService;

    @Autowired
    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    // 岗位基本管理
    @PostMapping
    public Position createPosition(@RequestBody Position position) {
        return positionService.createPosition(position);
    }

    @PutMapping("/{id}")
    public Position updatePosition(@PathVariable Long id, @RequestBody Position position) {
        position.setId(id);
        return positionService.updatePosition(position);
    }

    @DeleteMapping("/{id}")
    public void deletePosition(@PathVariable Long id) {
        positionService.deletePosition(id);
    }

    @GetMapping("/{id}")
    public Position getPositionById(@PathVariable Long id) {
        return positionService.getPositionById(id);
    }

    @GetMapping
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/department/{department}")
    public List<Position> getPositionsByDepartment(@PathVariable String department) {
        return positionService.getPositionsByDepartment(department);
    }

    @GetMapping("/level/{level}")
    public List<Position> getPositionsByLevel(@PathVariable String level) {
        return positionService.getPositionsByLevel(level);
    }

    @GetMapping("/active")
    public List<Position> getActivePositions() {
        return positionService.getActivePositions();
    }

    @GetMapping("/search")
    public List<Position> searchPositionsByName(@RequestParam String name) {
        return positionService.searchPositionsByName(name);
    }

    @GetMapping("/{id}/user-count")
    public Long countUsersByPosition(@PathVariable Long id) {
        return positionService.countUsersByPosition(id);
    }

    // 用户岗位管理
    @PostMapping("/users/{userId}/assign/{positionId}")
    public User assignPositionToUser(@PathVariable Long userId, @PathVariable Long positionId) {
        return positionService.assignPositionToUser(userId, positionId);
    }

    @GetMapping("/{id}/users")
    public List<User> getUsersByPosition(@PathVariable Long id) {
        return positionService.getUsersByPosition(id);
    }

    // 岗位统计分析
    @GetMapping("/stats/department")
    public Map<String, Object> getPositionStatsByDepartment() {
        return positionService.getPositionStatsByDepartment();
    }

    @GetMapping("/stats/distribution")
    public Map<String, Object> getPositionDistribution() {
        return positionService.getPositionDistribution();
    }
}
