package com.unlimited.sports.globox.model.order.dto;

import lombok.Data;

import javax.validation.constraints.Min;

@Data
public class GetOrderPageDto {

    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于0")
    private Integer pageSize = 10;
}