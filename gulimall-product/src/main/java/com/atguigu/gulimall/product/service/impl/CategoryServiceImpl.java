package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2、组装成父子的树形结构
        //2.1）、找到所有的一级分类
        return entities.stream()
                //所有的一级分类的父id为0
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .peek((menu) -> menu.setChildren(getChildrens(menu, entities)))
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用
        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     */
    @CacheEvict(value = {"category"},key = "'getLevel1Categorys'")//失效模式
//    @CachePut//双写模式
//    @Caching(evict = {
//            @CacheEvict(value = {"category"},key = "'getLevel1Categorys'")
//            @CacheEvict(value = {"category"},key = "'getCatalogJson'")
//    })
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 如果缓存命中，方法不执行
     * key默认自动生成
     * 缓存的value是使用jdk的序列化
     * 默认的过期时间是-1，永不过期
     */
//    @Cacheable(value = {"category"},key = "'level1Categorys'")
    @Cacheable(value = {"category"},key = "#root.method.name",sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    /**
     * 产生堆外内存溢出: OutOfDirectMemoryError
     * Spring Boot 2以后默认使用lettuce作为操作redis的客户端。它使用netty进行网络通信。
     * Lettuce的bug导致netty堆外内存溢出-Xmx300m; netty如果没有指定堆外内存，默认使用-Xmx300m
     * 可以通过-Dio.netty.maxDirectMemory进行设置
     * 解决方案:不能只使用-Dio.netty.maxDirectMemory只去调大堆外内存。
     * 1) 、升级lettuce客户端。
     * 2) 、切换使用jedis
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //1.空结果缓存用来解决缓存穿透问题
        //2.设置过期时间（需要加随机值）：解决缓存雪崩问题
        //3.加锁：解决缓存穿透
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isBlank(catalogJson)) {
            //缓存没有，需要查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
            //查询到记得放入缓存
            String s = JSON.toJSONString(catalogJsonFromDb);
            redisTemplate.opsForValue().set("catalogJson", s, 1, TimeUnit.DAYS);
            return catalogJsonFromDb;
        }
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
    }

    /**
     * 缓存里面的数据如何与数据库保持一致
     * 缓存数据一致性问题
     * 双写模式：由于卡顿等原因，导致写缓存2在最前，写缓存1在后面就出现了不一致。脏数据问题:这是暂时性的脏数据问题，但是在数据稳定，缓存过期以后，又能得到最新的正确数据
     * 失效模式：
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        //设置分布式锁，注意锁的名字 锁的粒度
        //锁的粒度：具体缓存的是某个数据。比如11号商品，product-11-lock
        RLock lock = redissonClient.getLock("CatalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        String token = UUID.randomUUID().toString();
        //设置分布式锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.SECONDS);
        if (lock) {
            Map<String, List<Catelog2Vo>> dataFromDb = null;
//            redisTemplate.expire("lock",30,TimeUnit.SECONDS);
            try {
                dataFromDb = getDataFromDb();
            } catch (Exception e) {
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList("lock"), token);
            }
//            redisTemplate.delete("lock");
//            String lock1 = redisTemplate.opsForValue().get("lock");
//            if(token.equals(lock1)){
//                redisTemplate.delete("lock");
//            }
            return dataFromDb;
        } else {
            //加锁失败要重试
            //休眠100毫秒再重试
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock();
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isBlank(catalogJson)) {
            return JSON.parseObject(catalogJson,
                    new TypeReference<Map<String, List<Catelog2Vo>>>() {
                    });
        }
        //将数据库的多次查询变为一次
        List<CategoryEntity> entitiesAll = baseMapper.selectList(null);
        List<CategoryEntity> level1Categorys = getParent_cid(entitiesAll, 0);
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(),
                v -> {
                    //拿到每一个一级分类，查询这个一级分类的所有二级分类
                    List<CategoryEntity> entities = getParent_cid(entitiesAll, v.getCatId());
                    List<Catelog2Vo> catelog2Vos = null;
                    if (entities != null && entities.size() > 0) {
                        catelog2Vos = entities.stream().map(item -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, item.getCatId().toString(), item.getName());
                            //找当前二级分类的三级菜单
                            List<CategoryEntity> level3Catlog = getParent_cid(entitiesAll, item.getCatId());
                            if (level3Catlog != null) {
                                List<Catelog2Vo.catalog3Vo> collect3 = level3Catlog
                                        .stream()
                                        .map(level3 -> new Catelog2Vo.catalog3Vo(item.getCatId().toString(), level3.getCatId().toString(), level3.getName())).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect3);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }));
        String s = JSON.toJSONString(parentCid);
        redisTemplate.opsForValue().set("catalogJson", s, 1, TimeUnit.DAYS);
        return parentCid;
    }

    /**
     * 从数据库查询并封装
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {
        //将数据库的多次查询变为一次
        List<CategoryEntity> entitiesAll = baseMapper.selectList(null);
        List<CategoryEntity> level1Categorys = getParent_cid(entitiesAll, 0);
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(),
                v -> {
                    //拿到每一个一级分类，查询这个一级分类的所有二级分类
                    List<CategoryEntity> entities = getParent_cid(entitiesAll, v.getCatId());
                    List<Catelog2Vo> catelog2Vos = null;
                    if (entities != null && entities.size() > 0) {
                        catelog2Vos = entities.stream().map(item -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, item.getCatId().toString(), item.getName());
                            //找当前二级分类的三级菜单
                            List<CategoryEntity> level3Catlog = getParent_cid(entitiesAll, item.getCatId());
                            if (level3Catlog != null) {
                                List<Catelog2Vo.catalog3Vo> collect3 = level3Catlog
                                        .stream()
                                        .map(level3 -> new Catelog2Vo.catalog3Vo(item.getCatId().toString(), level3.getCatId().toString(), level3.getName())).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect3);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }));
        return parentCid;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> categoryEntities, long parentCid) {
        return categoryEntities.stream().filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;

    }

    /**
     * 递归查找所有菜单的子菜单
     *
     * @param root 当前的菜单
     * @param all  所有的菜单
     * @return 当前菜单在所有菜单里面的子菜单
     */
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        return all.stream()
                //过滤。当前菜单的分类id等于遍历到的菜单的父id
                .filter(categoryEntity -> categoryEntity.getParentCid().equals(root.getCatId()))
                .peek(categoryEntity -> {
                    //1、找到子菜单
                    categoryEntity.setChildren(getChildrens(categoryEntity, all));
                    //2、菜单的排序
                }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());
//        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
//            return categoryEntity.getParentCid() == root.getCatId();
//        }).map(categoryEntity -> {
//            //1、找到子菜单
//            categoryEntity.setChildren(getChildrens(categoryEntity, all));
//            return categoryEntity;
//        }).sorted((menu1, menu2) -> {
//            //2、菜单的排序
//            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
//        }).collect(Collectors.toList());
//        return children;
    }
}