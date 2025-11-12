package com.yunke.backend.common.entity.base;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 实体基类，提供通用功能
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
public abstract class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
} 