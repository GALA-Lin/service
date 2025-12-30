package com.unlimited.sports.globox.model.demo.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * TODO
 *
 * @author dk
 * @since 2025/12/20 09:04
 */
@Data
public class DeductDTO implements Serializable {
    private Long userId;
    private BigDecimal money;
}
