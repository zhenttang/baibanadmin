package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.*;
import com.yunke.backend.user.domain.entity.UserPoint;

import com.yunke.backend.user.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserPointService {
    
    private final UserPointRepository userPointRepository;
    
    // 1. 获取或创建用户积分记录
    @Transactional
    public UserPointDTO getUserPoints(Long userId) {
        UserPoint point = userPointRepository.findByUserId(String.valueOf(userId))
            .orElseGet(() -> {
                UserPoint newPoint = new UserPoint();
                newPoint.setUserId(String.valueOf(userId));
                newPoint.setTotalPoints(0);
                newPoint.setLevel(0);
                newPoint.setPostCount(0);
                newPoint.setReplyCount(0);
                newPoint.setReputation(0);
                newPoint.setContinuousSignInDays(0);
                return userPointRepository.save(newPoint);
            });
        return toDTO(point);
    }
    
    // 2. 每日签到
    @Transactional
    public UserPointDTO signIn(Long userId) {
        UserPoint point = userPointRepository.findByUserId(String.valueOf(userId))
            .orElseThrow(() -> new IllegalArgumentException("用户积分记录不存在"));
        
        LocalDate today = LocalDate.now();
        
        // 检查今天是否已签到
        if (point.getLastSignInDate() != null && point.getLastSignInDate().toLocalDate().equals(today)) {
            throw new IllegalStateException("今日已签到");
        }
        
        // 计算连续签到天数
        LocalDate yesterday = today.minusDays(1);
        if (point.getLastSignInDate() != null && point.getLastSignInDate().toLocalDate().equals(yesterday)) {
            // 连续签到
            point.setContinuousSignInDays(point.getContinuousSignInDays() + 1);
        } else {
            // 断签，重新开始
            point.setContinuousSignInDays(1);
        }
        
        // 签到奖励5分
        point.setTotalPoints(point.getTotalPoints() + 5);
        point.setLevel(calculateLevel(point.getTotalPoints()));
        point.setLastSignInDate(LocalDateTime.now());
        
        UserPoint saved = userPointRepository.save(point);
        return toDTO(saved);
    }
    
    // 3. 增加积分
    @Transactional
    public UserPointDTO addPoints(PointOperationRequest request) {
        UserPoint point = userPointRepository.findByUserId(String.valueOf(request.getUserId()))
            .orElseThrow(() -> new IllegalArgumentException("用户积分记录不存在"));
        
        point.setTotalPoints(point.getTotalPoints() + request.getPoints());
        point.setLevel(calculateLevel(point.getTotalPoints()));
        
        UserPoint saved = userPointRepository.save(point);
        return toDTO(saved);
    }
    
    // 4. 扣减积分
    @Transactional
    public UserPointDTO deductPoints(PointOperationRequest request) {
        UserPoint point = userPointRepository.findByUserId(String.valueOf(request.getUserId()))
            .orElseThrow(() -> new IllegalArgumentException("用户积分记录不存在"));
        
        int newPoints = point.getTotalPoints() - request.getPoints();
        point.setTotalPoints(Math.max(0, newPoints)); // 防止负数
        point.setLevel(calculateLevel(point.getTotalPoints()));
        
        UserPoint saved = userPointRepository.save(point);
        return toDTO(saved);
    }
    
    // 5. 更新发帖数
    @Transactional
    public void updatePostCount(Long userId, int increment) {
        userPointRepository.findByUserId(String.valueOf(userId)).ifPresent(point -> {
            point.setPostCount(point.getPostCount() + increment);
            userPointRepository.save(point);
        });
    }
    
    // 6. 更新回复数
    @Transactional
    public void updateReplyCount(Long userId, int increment) {
        userPointRepository.findByUserId(String.valueOf(userId)).ifPresent(point -> {
            point.setReplyCount(point.getReplyCount() + increment);
            userPointRepository.save(point);
        });
    }
    
    // 7. 计算等级
    private int calculateLevel(int totalPoints) {
        return totalPoints / 100;
    }
    
    // 8. 转DTO
    private UserPointDTO toDTO(UserPoint entity) {
        UserPointDTO dto = new UserPointDTO();
        dto.setId(entity.getId());
        try {
            dto.setUserId(entity.getUserId() == null ? null : Long.parseLong(entity.getUserId()));
        } catch (NumberFormatException e) {
            dto.setUserId(null);
        }
        dto.setTotalPoints(entity.getTotalPoints());
        dto.setLevel(entity.getLevel());
        dto.setPostCount(entity.getPostCount());
        dto.setReplyCount(entity.getReplyCount());
        dto.setReputation(entity.getReputation());
        dto.setLastSignInDate(entity.getLastSignInDate() == null ? null : entity.getLastSignInDate().toLocalDate());
        dto.setContinuousSignInDays(entity.getContinuousSignInDays());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
