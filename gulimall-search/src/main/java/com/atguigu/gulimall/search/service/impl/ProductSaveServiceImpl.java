package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.cong.GulimallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * @author admin
 */
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    private RestHighLevelClient client;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        skuEsModels.forEach((e) -> {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            String s = JSON.toJSONString(e);
            indexRequest.id(e.getSkuId().toString());
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        });
        BulkResponse bulk = client.bulk(bulkRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
        //TODO 如果出现错误我们还可以进行其他的处理bulk可以得到很多的信息
        return bulk.hasFailures();
    }
}