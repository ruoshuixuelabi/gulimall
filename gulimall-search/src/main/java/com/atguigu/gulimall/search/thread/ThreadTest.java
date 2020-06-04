package com.atguigu.gulimall.search.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author admin
 */
public class ThreadTest {
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main方法开始");
//        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程是" + Thread.currentThread().getName());
//            int i = 10 / 2;
//            System.out.println("运行结果是" + i);
//        }, executorService);
//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程是" + Thread.currentThread().getName());
//            int i = 10 / 0;
//            System.out.println("运行结果是" + i);
//            return i;
//        }, executorService)
//                //whenComplete虽然可以得到异常信息，但是没办法修改返回数据
//                .whenComplete((res, ex) -> System.out.println("异步任务完成了，结果是" + res + "；异常是" + ex))
//                //exceptionally可以感知到异常，并且返回一个默认值
//                .exceptionally(throwable -> 10);

//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程是" + Thread.currentThread().getName());
//            int i = 10 / 0;
//            System.out.println("运行结果是" + i);
//            return i;
//        }, executorService).handle((result, throwable) -> {
//            if (result != null) {
//                return result * 2;
//            }
//            if (throwable != null) {
//                return 1;
//            }
//            return 0;
//        });
//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程是" + Thread.currentThread().getName());
//            int i = 10 / 2;
//            System.out.println("运行结果是" + i);
//            return i;
//        }, executorService)
//                //thenRun 不能获取到上一步的执行结果
////                .thenRunAsync(() -> System.out.println("任务2启动了"), executorService);
//                //thenAccept 能接收到上一步的执行结果，但是没有返回值
////                .thenAcceptAsync(result -> System.out.println("任务2启动了，上步执行结果result="+result), executorService);
//                //thenApplyAsync 能接收上一步的执行结果，并且有返回值
//                .thenApplyAsync(result -> {
//                    System.out.println("任务2启动了，上步执行结果result=" + result);
//                    return "hello " + result;
//                }, executorService);


        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前任务1线程是" + Thread.currentThread().getName());
            int i = 10 / 2;
            System.out.println("任务1运行结果是" + i);
            return i;
        }, executorService);
        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前任务2线程是" + Thread.currentThread().getName());

            try {
                Thread.sleep(1000);
                System.out.println("任务2运行结果是");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "hello";
        }, executorService);
        //runAfterBothAsync是不能感受到前两个的运行结果的
//        future1.runAfterBothAsync(future2,()-> System.out.println("任务3开始"),executorService);

//        future1.thenAcceptBothAsync(future2,(integer, s) -> System.out.println("任务3开始，之前的结果f1="+integer+"；f2="+s),executorService);
//        CompletableFuture<String> future3 = future1.thenCombineAsync(future2, (integer, s) -> integer + s, executorService);
//        System.out.println(future3.get());
        //两个任务只要有1个完成
//        future1.runAfterEitherAsync(future2,() ->System.out.println("任务3开始"),executorService);
//        future1.acceptEitherAsync(future2,integer -> System.out.println("任务3开始，之前的结果是"+integer),executorService);
//        CompletableFuture<String> future3 = future1.applyToEitherAsync(future2, o -> o.toString() + "haha", executorService);

        CompletableFuture<Void> allOf = CompletableFuture.allOf(future1, future2);
        //等待结果完成
        allOf.get();
        System.out.println("main方法结束");
    }
}