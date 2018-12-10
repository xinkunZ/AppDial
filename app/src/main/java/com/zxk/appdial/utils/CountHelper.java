package com.zxk.appdial.utils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

  // properties
  private String countFileName = "appCountCache";

  // normal text
  private String unCountFileName = "shyApps";

  public CountHelper(Activity activity) {
    try {
      String[] fileList = activity.fileList();
      List<String> files = new ArrayList<>();
      files.addAll(Arrays.asList(fileList));
      if (!files.contains(countFileName)) {
        FileOutputStream outputStream = activity
            .openFileOutput(countFileName, Context.MODE_PRIVATE);
        Properties properties = new Properties();
        properties.store(outputStream, "");
        outputStream.flush();
        outputStream.close();
      } else if (!files.contains(unCountFileName)) {
        FileOutputStream outputStream = activity.openFileOutput(unCountFileName,
            Context.MODE_PRIVATE);
        outputStream.write("\n".getBytes());
        outputStream.flush();
        outputStream.close();
      }
    } catch (Exception e) {

    }
  }

  private String readFile(Activity activity, String fileName) throws FileNotFoundException,
      IOException {
    FileInputStream inputStream = null;
    inputStream = activity.openFileInput(fileName);
    BufferedInputStream stream = new BufferedInputStream(inputStream, 128);
    byte[] b = new byte[128];
    int flag = stream.read(b);
    StringBuilder sb = new StringBuilder();
    while (flag != -1) {
      sb.append(new String(b, 0, flag));
      flag = stream.read(b);
    }
    safeClose(stream);
    return sb.toString();
  }

  public void changeCountState(String packageName, Activity activity, boolean enableCount) {
    try {
      if (isUnCount(activity, packageName) != enableCount) {
        return;
      }
      try {
        String content = readFile(activity, unCountFileName);
        StringBuilder sb = new StringBuilder(content);
        if (content.isEmpty()) {
          if (enableCount) {

          } else {
            sb.append(packageName).append("\n");
          }
        } else {
          if (enableCount) {
            List<String> notCountApps = new ArrayList<>();
            notCountApps.addAll(Arrays.asList(sb.toString().split("\n")));
            notCountApps.remove(packageName);
            sb.setLength(0);
            for (String notCountApp : notCountApps) {
              sb.append(notCountApp).append("\n");
            }
          } else {
            sb.append(packageName).append("\n");
          }
        }

        FileOutputStream outputStream = activity.openFileOutput(unCountFileName,
            Context.MODE_PRIVATE);
        outputStream.write(sb.toString().getBytes());
      } catch (FileNotFoundException e) {

      } catch (IOException e) {

      }
    } catch (Exception e) {

    }
  }

  public boolean isUnCount(Activity activity, String appPackageName) {
    try {
      String stringBuilder = readFile(activity, unCountFileName);
      return !stringBuilder.isEmpty()
          && Arrays.asList(stringBuilder.split("\n")).contains(appPackageName);
    } catch (Exception e) {
      System.err.println(e);
      return false;
    }
  }

  public int getCount(Activity activity, String appPackageName) {
    FileInputStream inputStream = null;
    try {
      Properties properties = new Properties();
      inputStream = activity.openFileInput(countFileName);
      properties.load(inputStream);
      String property = properties.getProperty(appPackageName);
      if (isUnCount(activity, appPackageName)) {
        return -1;
      }
      if (property == null) {
        return 0;
      } else {
        return Integer.parseInt(property);
      }
    } catch (Exception e) {
      return 0;
    } finally {
      safeClose(inputStream, null);
    }
  }

  private void safeClose(Closeable... stream) {
    try {
      if (stream != null) {
        for (Closeable closeable : stream) {
          if (closeable != null) {
            closeable.close();
          }
        }
      }
    } catch (Exception e) {
      Log.e(CountHelper.class.getName(), "error", e);
    }
  }

  public List<LocalApp> getFrequentApps4ShortcutsApp(Activity activity) {
    FileInputStream inputStream = null;
    try {
      Properties properties = new Properties();
      inputStream = activity.openFileInput(countFileName);
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
            inputStream = activity.openFileInput(countFileName);
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
          outStream = activity.openFileOutput(countFileName, Context.MODE_PRIVATE);
          properties.store(outStream, "");
          outStream.flush();
          outStream.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          Log.e(CountHelper.class.getName(), "error", e);
        } finally {
          safeClose(inputStream, outStream);
        }
      }
    }).start();

  }

}
