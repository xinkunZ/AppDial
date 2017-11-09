package com.zxk.appdial.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.zxk.appdial.model.LocalApps;

/**
 * @author zhangxinkun
 */
public class AppTools {

  private static AppTools instance = null;

  public static AppTools getTools() {
    if (instance == null) {
      instance = new AppTools();
    }
    return instance;
  }

  private static List<LocalApps> apps = null;

  public static List<LocalApps> scanLocalInstallAppList(PackageManager packageManager) {
    if (apps != null) {
      return apps;
    }
    apps = new ArrayList<>();
    try {
      List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
      for (int i = 0; i < packageInfos.size(); i++) {
        PackageInfo packageInfo = packageInfos.get(i);
        //过滤掉系统app
        if ((ApplicationInfo.FLAG_SYSTEM & packageInfo.applicationInfo.flags) > 0
            || packageInfo.applicationInfo.className == null) {
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
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
    return apps;
  }
}
