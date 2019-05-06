package com.zxk.appdial.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 伟大的多线程管理君
 * <p>
 *
 * @author zhangxinkun
 */
public class ThreadHelper<T> {

  private List<T> list;

  private int coutPerThread;
  private CountDownLatch countDownLatch;

  private ThreadHeplerUser<T> user;

  public ThreadHelper(List<T> list, ThreadHeplerUser<T> user, int coutPerThread) {
    this.list = list;
    this.user = user;
    this.coutPerThread = coutPerThread;
  }

  public void exe() {
    int size = list.size();
    if (size > 50) {

      int count = size / coutPerThread;
      boolean haveTail = false;
      if (count * coutPerThread != size) {
        haveTail = true;
      }
      countDownLatch = new CountDownLatch(count + (haveTail ? 1 : 0));
      for (int i = 0; i < count; i++) {
        System.out.println(String.format("从%s ~ %s ", i * coutPerThread, i * coutPerThread
            + coutPerThread));
        List<T> l = list.subList(i * coutPerThread, i * coutPerThread + coutPerThread);
        new Worker(l).start();
      }
      if (haveTail) {
        System.out.println(String.format("尾巴 %s ~ %s ", count * coutPerThread - 1, size - 1));
        List<T> latest = list.subList(count * coutPerThread - 1, size - 1);
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
