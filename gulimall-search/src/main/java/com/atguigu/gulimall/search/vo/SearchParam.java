package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * @author admin
 */
@Data
public class SearchParam {
    /**
     * 页面传递过来的全文检索关键字
     */
    private String keyword;
    /**
     * 页面传递过来的三级分类id
     */
    private Long catalog3Id;
    /**
     * 排序条件
     * sort=saleCount_asc/desc
     * hotScore_asc/desc
     * skuPrice_asc/desc
     */
    private String sort;
    /**
     * 过滤条件
     * hasStock(是否有货)、skuPrice区间、brandId、catalog3Id、attrs
     * hasStock=0/1
     * skuPrice1_500/_500/500_
     * <p>
     * 0代表无库存，1代表有库存
     */
    private Integer hasStock = 1;
    /**
     * 价格区间查询
     */
    private String skuPrice;
    /**
     * 按照品牌进行查询，可以有多个
     */
    private List<Long> brandId;
    /**
     * 按照属性进行筛选
     */
    private List<String> attrs;
    /**
     * 页码
     */
    private Integer pageNum = 1;
    /**
     * 页面上的查询条件
     */
    private String _queryString;
}
