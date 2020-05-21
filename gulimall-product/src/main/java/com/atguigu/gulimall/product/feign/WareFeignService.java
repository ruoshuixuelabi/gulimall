package com.atguigu.gulimall.product.feign;

import com.atguigu.gulimall.product.entity.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author admin
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {
    @PostMapping("ware/waresku/hasstock")
    List<SkuHasStockVo> getSkuHasStock(@RequestBody List<Long> skuIds);
}