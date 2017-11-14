package com.zxk.appdial.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.zxk.appdial.model.LocalApps;

/**
 * @author zhangxinkun
 */
public class AppTools {

  private volatile List<LocalApps> apps = null;

  private int threadSize = 20;
  private CountDownLatch countDownLatch;

  private PackageManager packageManager;

  public AppTools(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  public List<LocalApps> scanLocalInstallAppList() {
    if (apps != null) {
      return apps;
    }
    apps = new ArrayList<>();
    try {
      List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);

      int size = packageInfos.size();
      if (size > 50) {
        int count = size / threadSize;
        boolean haveTail = false;
        if (count * threadSize != size) {
          haveTail = true;
        }
        countDownLatch = new CountDownLatch(count + (haveTail ? 1 : 0));
        for (int i = 0; i < count; i++) {
          Log.d(AppTools.class.getName(), String.format("从%s 到 %s", i * threadSize, i * threadSize + threadSize));
          List<PackageInfo> l = packageInfos.subList(i * threadSize, i * threadSize + threadSize);
          new FilterWorker(l).start();
        }
        if (haveTail) {
          Log.d(AppTools.class.getName(), String.format("从%s 到 %s", count * threadSize, size));
          List<PackageInfo> latest = packageInfos.subList(count * threadSize, size);
          new FilterWorker(latest).start();
        }
      } else {
        countDownLatch = new CountDownLatch(1);
        new FilterWorker(packageInfos).start();
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
    }

    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return apps;
  }

  private class FilterWorker extends Thread {

    private List<PackageInfo> list;

    public FilterWorker(List<PackageInfo> l) {
      this.list = l;
    }

    @Override
    public void run() {
      super.run();
      Log.d(AppTools.class.getName(), "遍历内容： " + list.toString());
      for (int i = 0; i < list.size(); i++) {
        PackageInfo packageInfo = list.get(i);
        //过滤不能打开的app
        Intent intent = packageManager.getLaunchIntentForPackage(packageInfo.packageName);
        if (intent == null) {
          continue;
        }
        LocalApps myAppInfo = new LocalApps();
        myAppInfo.setPackageName(packageInfo.packageName);
        myAppInfo.setAppName(packageInfo.applicationInfo.loadLabel(packageManager).toString());
        myAppInfo.setClassName(packageInfo.applicationInfo.className);

        if (packageInfo.applicationInfo.loadIcon(packageManager) == null) {
          continue;
        }
        myAppInfo.setIcon(packageInfo.applicationInfo.loadIcon(packageManager));
        apps.add(myAppInfo);
      }
      countDownLatch.countDown();
    }
  }

}


