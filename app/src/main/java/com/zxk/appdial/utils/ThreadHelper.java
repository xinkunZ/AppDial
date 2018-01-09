package com.zxk.appdial.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 完全看不出来这个类是我自己写的呢
 *
 * @author zhangxinkun
 */
public class ThreadHelper<T> {

  private List<T> list;
  private int threadSize = 20;
  private CountDownLatch countDownLatch;

  private ThreadHeplerUser<T> user;

  public ThreadHelper(List<T> list, ThreadHeplerUser<T> user) {
    this(list, user, 20);
  }

  public ThreadHelper(List<T> list, ThreadHeplerUser<T> user, int threadSize) {
    this.list = list;
    this.user = user;
    this.threadSize = threadSize;
  }

  public void exe() {
    int size = list.size();
    if (size > 50) {

      int count = size / threadSize;
      boolean haveTail = false;
      if (count * threadSize != size) {
        haveTail = true;
      }
      countDownLatch = new CountDownLatch(count + (haveTail ? 1 : 0));
      for (int i = 0; i < count; i++) {
        System.out.println(String.format("从%s ~ %s ", i * threadSize, i * threadSize + threadSize));
        List<T> l = list.subList(i * threadSize, i * threadSize + threadSize);
        new Worker(l).start();
      }
      if (haveTail) {
        System.out.println(String.format("尾巴 %s ~ %s ", count * threadSize - 1, size - 1));
        List<T> latest = list.subList(count * threadSize - 1, size - 1);
        new Worker(latest).start();
      }
    } else {
      countDownLatch = new CountDownLatch(1);
      new Worker(list).start();
    }
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    user.afterRun();
  }

  private class Worker extends Thread {

    private List<T> apps = new ArrayList<>();

    public Worker(List<T> apps) {
      this.apps = apps;
    }

    @Override
    public void run() {
      user.run(apps);
      countDownLatch.countDown();
    }
  }

  public interface ThreadHeplerUser<T> {

    void run(List<T> list);

    void afterRun();
  }

}
