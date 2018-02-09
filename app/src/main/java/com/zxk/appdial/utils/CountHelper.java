package com.zxk.appdial.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.zxk.appdial.model.LocalApp;

/**
 * 伟大的计数君
 *
 * @author zhangxinkun
 */
public class CountHelper {

  private String fileName = "appCountCache";

  public int getCount(Activity activity, String appName) {
    FileInputStream inputStream = null;
    try {
      Properties properties = new Properties();
      inputStream = activity.openFileInput(fileName);
      properties.load(inputStream);
      String property = properties.getProperty(appName);
      if (property == null) {
        return 0;
      } else {
        return Integer.parseInt(property);
      }
    } catch (Exception e) {
      return 0;
    } finally {
      saleClose(inputStream, null);
    }
  }

  private void saleClose(FileInputStream inputStream, FileOutputStream outputStream) {
    try {
      if (inputStream != null) {
        inputStream.close();
      }
      if (outputStream != null) {
        outputStream.close();
      }
    } catch (Exception e) {
      Log.e(CountHelper.class.getName(), "error", e);
    }
  }

  public List<LocalApp> getFirstFour4ShortcutsApp(Activity activity) {
    FileInputStream inputStream = null;
    try {
      Properties properties = new Properties();
      inputStream = activity.openFileInput(fileName);
      properties.load(inputStream);
      List<LocalApp> apps = new ArrayList<>();
      for (Object key : properties.keySet()) {
        try {
          LocalApp app = new LocalApp();
          app.setPackageName((String) key);
          app.setCount(Integer.parseInt(properties.get(key).toString()));
          apps.add(app);
        } catch (Exception e) {
          Log.e(CountHelper.class.getName(), "error", e);
        }
      }
      Collections.sort(apps, (o1, o2) -> o1.getCount() - o2.getCount());
      Collections.reverse(apps);
      return apps.size() >= 4 ? apps.subList(0, 4) : apps;
    } catch (Exception e) {
      return null;
    }
  }

  public void recordAppCount(final String packageName, final int newValue, final Activity activity) {
    new Thread(new Runnable() {

      @Override
      public void run() {
        FileInputStream inputStream = null;
        FileOutputStream outStream = null;
        try {
          Properties properties = new Properties();
          try {
            inputStream = activity.openFileInput(fileName);
            properties.load(inputStream);
          } catch (FileNotFoundException e) {

          } catch (IOException e) {
            Log.e(this.getClass().getName(), "error", e);
          } finally {
            if (inputStream != null) {
              inputStream.close();
            }
          }
          properties.setProperty(packageName, newValue + "");
          outStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
          properties.store(outStream, "");
          outStream.flush();
          outStream.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          Log.e(CountHelper.class.getName(), "error", e);
        } finally {
          saleClose(inputStream, outStream);
        }
      }
    }).start();

  }
}
