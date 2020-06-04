package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author admin
 */
@Controller
public class IndexController {
    @Autowired
    CategoryService categoryService;
    @Autowired
    RedissonClient redissonClient;

    @GetMapping({"index.html", "/"})
    public String indexPage(Model model) {
        //TODO 查询出所有的1级分类
        List<CategoryEntity> entities = categoryService.getLevel1Categorys();
        model.addAttribute("categorys", entities);
        return "index";
    }

    @GetMapping("index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatalogJson(Model model) {
        return categoryService.getCatalogJson();
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //获取一把锁，只要锁的名字一样，就是一把锁
        RLock mylock = redissonClient.getLock("mylock");
        //这个锁是阻塞式的
//        mylock.lock();
        //10秒钟后自动解锁，但是一定注意，自动解锁的时间一定要大于业务的执行时间
        //一旦调用了这个方法，锁不会自动续期
        //问题: lock. lock(10, TimeUnit. SECONDS);在锁时间到了以后，不会自动续期。
        //1、如果我们传递了锁的超时时间，就发送给redis执行脚本，进行占锁，默认超时就是我们指定的时间
        //2.如果我们未指定锁的超时时间，就使用30 * 1000 [lockWatchdogTimeout看门狗的默认时间] ;
        //只要占锁成功，就会启动一个定时任务[重新给锁设置过期时间，新的过期时间就是看[门狗的默认时间]
        //internallockLeaseTime [看门狗时间] / 3, 10s
        mylock.lock(10, TimeUnit.SECONDS);
        try {
            System.out.println("加锁成功，开始执行业务" + Thread.currentThread().getName());
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("释放锁" + Thread.currentThread().getName());
            mylock.unlock();
        }
        return "hello";
    }

    /**
     * 保证一定能读到最新数据，修改期间写锁是一个排它锁(互斥锁)，读锁是一个共享锁
     * 写锁没释放，读锁一定要等待
     * 读+读：相当于无锁
     * 读+写：等待写锁释放
     * 写+读：等待写锁释放
     * 写+写：阻塞方式
     */
    @ResponseBody
    @GetMapping("/write")
    public String writeValue() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.writeLock();
        String s = "";
        try {
            rLock.lock();
            System.out.println("写锁加锁成功" + Thread.currentThread().getName());
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("写锁释放成功" + Thread.currentThread().getName());
            rLock.unlock();
        }
        return s;
    }

    @ResponseBody
    @GetMapping("/read")
    public String readValue() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.readLock();
        String s = "";
        try {
            rLock.lock();
            System.out.println("读锁加锁成功" + Thread.currentThread().getName());
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("读锁加锁成功" + Thread.currentThread().getName());
            rLock.unlock();
        }
        return s;
    }

    /**
     * 放假锁门
     * 所有班级的人走了才能锁门
     */
    @ResponseBody
    @GetMapping("/lockDoor")
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();
        return "放假了";
    }

    @ResponseBody
    @GetMapping("/gogogo/{id}")
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();
        return id + "班的人走了";
    }

    /**
     * 模拟车库停车
     * 3个车位
     */
    @ResponseBody
    @GetMapping("/park")
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        //获取一个信号，获取一个值，在这里相当于拿一个车位
        park.acquire();
        return "停车ok";
    }

    /**
     * 车开走的方法
     */
    @ResponseBody
    @GetMapping("/go")
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        //释放车位
        park.release();
        return "释放ok";
    }
}