package com.zxk.appdial.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.zxk.appdial.MainActivity;
import com.zxk.appdial.model.LocalApp;

/**
 * 伟大的app管理君
 *
 * @author zhangxinkun
 */
public class AppHelper implements ThreadHelper.ThreadHeplerUser<PackageInfo> {

  private ConcurrentHashMap<LocalApp, Object> apps = null;

  private PackageManager packageManager;
  private CountHelper countHelper;

  public AppHelper(PackageManager packageManager, CountHelper countHelper) {
    this.packageManager = packageManager;
    this.countHelper = countHelper;
  }

  public List<LocalApp> scanLocalInstallAppList(boolean reload) {
    if (reload) {
      apps = null;
    }
    if (apps != null) {
      return new ArrayList<>(apps.keySet());
    }
    apps = new ConcurrentHashMap<>();
    try {
      List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
      new ThreadHelper<>(packageInfos, this, MainActivity.coutPerThread).exe();
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
    List<LocalApp> result = new ArrayList<>(apps.keySet());
    Collections.sort(result);
    Collections.reverse(result);
    return result;
  }

  @Override
  public void run(List<PackageInfo> list) {
    // Log.d(AppHelper.class.getName(), "遍历内容： " + list.toString());

    for (int i = 0; i < list.size(); i++) {
      PackageInfo packageInfo = list.get(i);
      if (countHelper.getNoMainActivityApps().contains(packageInfo.packageName)) {
        continue;
      }
      // else {
      // Intent intent =
      // packageManager.getLaunchIntentForPackage(packageInfo.packageName);
      // if (intent == null) {
      // countHelper.getNoMainActivityApps().add(packageInfo.packageName);
      // continue;
      // }
      // }
      LocalApp myAppInfo = new LocalApp();
      myAppInfo.setPackageName(packageInfo.packageName);
      myAppInfo.setAppName(packageInfo.packageName.replace(".", ""));
      String name = countHelper.getCachedPackageNameMap().optString(packageInfo.packageName, null);
      if (name == null) {
        name = packageInfo.applicationInfo.loadLabel(packageManager).toString();
        try {
          countHelper.getCachedPackageNameMap().put(packageInfo.packageName, name);
        } catch (Exception e) {
        }
      }
      myAppInfo.setAppName(name);
      myAppInfo.setClassName(packageInfo.applicationInfo.className);
      myAppInfo.setPinyin(getPinyin(myAppInfo.getAppName(), myAppInfo.getPackageName()));
      myAppInfo.setCount(countHelper.getCount(myAppInfo.getPackageName()));
      myAppInfo.setInCount(!countHelper.isUnCount(myAppInfo.getPackageName()));
      int hashCode = myAppInfo.getPackageName().hashCode();
      File iconcFile = new File(countHelper.getCacheDir(), "/icons/" + hashCode);
      if (iconcFile.exists()) {
        myAppInfo.setIcon(Drawable.createFromPath(iconcFile.getAbsolutePath()));
      } else {
        Drawable appIcon = packageInfo.applicationInfo.loadIcon(packageManager);
        if (appIcon == null) {
          continue;
        }
        myAppInfo.setIcon(appIcon);
        try {
          iconcFile.getParentFile().mkdirs();
          iconcFile.createNewFile();
          FileOutputStream outputStream = new FileOutputStream(iconcFile);
          getBitmapFromDrawable(appIcon).compress(CompressFormat.PNG, 100, outputStream);
          outputStream.close();
        } catch (Exception e) {

        }
      }
      apps.put(myAppInfo, new Object());
    }
    Log.d(AppHelper.class.getName(), Thread.currentThread().getName() + "结束");
  }

  private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
    final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bmp);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bmp;
  }

  private static String getPinyin(String appName, String defaultName) {
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
    new Thread(() -> {
      countHelper.savePackageNameMap();
      countHelper.saveNoMainActivityApps();
    }).start();
  }

}
