package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * @author admin
 */
@Data
public class SkuItemVo {
    //SKU基本信息获取 pms_sku_info
    SkuInfoEntity info;
    //SKU的图片信息pms_sku_images
    List<SkuImagesEntity> images;
    //获取spu的销售属性组合
    //获取spu的介绍
    SpuInfoDescEntity desp;
    //获取spu 的规格参数信息
}
