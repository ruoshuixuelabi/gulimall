package com.atguigu.gulimall.product.entity;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}