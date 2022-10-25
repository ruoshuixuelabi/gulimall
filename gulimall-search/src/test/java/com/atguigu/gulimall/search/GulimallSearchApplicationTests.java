package com.atguigu.gulimall.search;

import com.alibaba.fastjson2.JSON;
import com.atguigu.gulimall.search.cong.GulimallElasticsearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {
    @Autowired
    private RestHighLevelClient client;

    @Test
    void contextLoads() {
        System.out.println(client);
    }

    /**
     * 测试存储数据到es
     */
    @Test
    void indexData() throws IOException {
        IndexRequest request = new IndexRequest("users");
        request.id("1");
//        request.source("username","zhangsan","age",18,"gender","男");
        User user = new User();
        user.setUsername("张三");
        user.setAge(18);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        request.source(jsonString, XContentType.JSON);
        IndexResponse indexResponse = client.index(request, GulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println(indexResponse);
    }

    @Test
    void searchData() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        //要从哪个索引进行检索
        searchRequest.indices("bank");
        //检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        searchSourceBuilder.aggregation(AggregationBuilders.terms("ageAgg").field("age").size(10));
        //计算平均薪资
        searchSourceBuilder.aggregation(AggregationBuilders.avg("balanceAvg").field("balance"));
        searchRequest.source(searchSourceBuilder);
        //执行检索操作
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println(searchResponse);
        //获取所有查询到的记录
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
            String index = searchHit.getIndex();
            System.out.println(index);
        }
        Aggregations aggregations = searchResponse.getAggregations();
        aggregations.asList().forEach(aggregation -> System.out.println("创建的聚合的名字=" + aggregation.getName()));
        Terms ageAgg = aggregations.get("ageAgg");
        ageAgg.getBuckets().forEach(e -> System.out.println("年龄是" + e.getKeyAsString()));
        Avg balanceAvg = aggregations.get("balanceAvg");
        System.out.println("平均薪资" + balanceAvg.getValue());
    }

//    @Test
//    void searchData1() throws IOException {
//        elasticsearchTemplate.
//    }
}

@Data
class User {
    private String username;
    private String gender;
    private Integer age;
}