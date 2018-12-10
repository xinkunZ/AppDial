package com.zxk.appdial.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.zxk.appdial.model.LocalApp;

/**
 * 伟大的app管理君
 *
 * @author zhangxinkun
 */
public class AppHelper implements ThreadHelper.ThreadHeplerUser<PackageInfo> {

  private volatile List<LocalApp> apps = null;

  private PackageManager packageManager;
  private CountHelper countHelper;
  private Activity mainActivity;

  public AppHelper(PackageManager packageManager, Activity mainActivity) {
    this.packageManager = packageManager;
    this.countHelper = new CountHelper(mainActivity);
    this.mainActivity = mainActivity;
  }

  public List<LocalApp> scanLocalInstallAppList(boolean reload) {
    if (reload) {
      apps = null;
    }
    if (apps != null) {
      return apps;
    }
    apps = new ArrayList<>();
    try {
      List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
      new ThreadHelper<>(packageInfos, this, 16).exe();// 多开点线程
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
    Iterator<LocalApp> iterator = apps.iterator();
    while (iterator.hasNext()) {
      if (iterator.next() == null) {
        iterator.remove();
      }
    }
    Collections.sort(apps);
    Collections.reverse(apps);
    return apps;
  }

  @Override
  public void run(List<PackageInfo> list) {
    Log.d(AppHelper.class.getName(), "遍历内容： " + list.toString());
    for (int i = 0; i < list.size(); i++) {
      PackageInfo packageInfo = list.get(i);
      // 过滤不能打开的app
      Intent intent = packageManager.getLaunchIntentForPackage(packageInfo.packageName);
      if (intent == null) {
        continue;
      }
      LocalApp myAppInfo = new LocalApp();
      myAppInfo.setPackageName(packageInfo.packageName);
      myAppInfo.setAppName(packageInfo.applicationInfo.loadLabel(packageManager).toString());
      myAppInfo.setClassName(packageInfo.applicationInfo.className);
      myAppInfo.setPinyin(getPinyin(myAppInfo.getAppName(), myAppInfo.getPackageName()));
      myAppInfo.setCount(countHelper.getCount(mainActivity, myAppInfo.getPackageName()));
      myAppInfo.setInCount(!countHelper.isUnCount(mainActivity, myAppInfo.getPackageName()));
      if (packageInfo.applicationInfo.loadIcon(packageManager) == null) {
        continue;
      }
      myAppInfo.setIcon(packageInfo.applicationInfo.loadIcon(packageManager));
      apps.add(myAppInfo);
    }
  }

  private String getPinyin(String appName, String defaultName) {
    try {
      HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
      format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
      format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
      StringBuilder sb = new StringBuilder();
      char[] chars = appName.toCharArray();
      for (char aChar : chars) {
        String[] strings = PinyinHelper.toHanyuPinyinStringArray(aChar, format);
        if (strings != null && strings.length > 0) {
          sb.append(strings[0]);
        } else {
          sb.append(aChar);
        }
      }
      return sb.toString();
    } catch (Exception e) {
      return defaultName;
    }
  }

  @Override
  public void afterRun() {

  }

}
