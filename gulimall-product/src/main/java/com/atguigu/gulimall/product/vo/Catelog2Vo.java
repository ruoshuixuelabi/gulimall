package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author admin
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catelog2Vo {
    private  String catalog1Id;
    private List catalog3List;
    private  String id;
    private  String name;



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static  class catalog3Vo{
        private  String catalog2Id;
        private  String id;
        private  String name;
    }
}
