package com.zxk.appdial.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;

import com.zxk.appdial.model.LocalApp;

/**
 * 伟大的计数君
 *
 * @author zhangxinkun
 */
public class CountHelper {

  // properties
  private String FILE_COUNT = "appCountCache";

  // text with \n
  private String FILE_UNCOUNT = "shyApps";

  // json
  private String File_APP_NAME = "appname";

  // text with \n
  private String File_NO_MAINACTIVITY_APP = "noMainActivity";

  private Activity activity;
  private JSONObject cachedPackageNameMap = new JSONObject();

  private File cacheDir;
  private List<String> uncountApps = new ArrayList<>();
  private List<String> noMainActivityApps = new ArrayList<>();
  private List<OutputStream> cachedAppIcon = new ArrayList<>();

  public File getCacheDir() {
    return cacheDir;
  }

  public List<OutputStream> getCachedAppIcon() {
    return cachedAppIcon;
  }

  public JSONObject getCachedPackageNameMap() {
    return cachedPackageNameMap;
  }

  public List<String> getNoMainActivityApps() {
    return noMainActivityApps;
  }

  public CountHelper(Activity activity) {
    try {
      this.activity = activity;
      cacheDir = new File(activity.getExternalCacheDir().getParentFile(), "count");
      if (!cacheDir.exists()) {
        cacheDir.mkdirs();
      }
      File countFile = new File(cacheDir, FILE_COUNT);
      if (!countFile.exists()) {
        countFile.createNewFile();
        recoverOldData();
      }

      File unCountFile = new File(cacheDir, FILE_UNCOUNT);
      if (!unCountFile.exists()) {
        unCountFile.createNewFile();
        recoverOldData();
      }
      File pakcageNameMapFile = new File(cacheDir, File_APP_NAME);
      if (!pakcageNameMapFile.exists()) {
        pakcageNameMapFile.createNewFile();
      } else {
        cachedPackageNameMap = new JSONObject(FileUtils.readFileToString(pakcageNameMapFile,
            StandardCharsets.UTF_8));
      }

      loadUnCountApps();
      loadNoMainActivityApps();
    } catch (Exception e) {
      Log.e("CountHelper", e.getMessage(), e);
    }
  }

  private void loadNoMainActivityApps() throws IOException {
    File file = new File(cacheDir, File_NO_MAINACTIVITY_APP);
    if (!file.exists()) {
      file.createNewFile();
    }
    boolean isSameDay = DateUtils.isSameDay(new Date(), new Date(file.lastModified()));
    if (isSameDay) {
      String noMainActivity = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      noMainActivityApps = StringUtils.isBlank(noMainActivity) ? new ArrayList<>() : Arrays
          .asList(noMainActivity.split("\n"));
    } else {
      noMainActivityApps = new ArrayList<>();
    }
  }

  private void loadUnCountApps() throws Exception {
    File file = new File(cacheDir, FILE_UNCOUNT);
    String uncount = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    uncountApps = StringUtils.isBlank(uncount) ? new ArrayList<>() : Arrays.asList(uncount
        .split("\n"));
  }

  private void recoverOldData() throws IOException {
    for (String s : activity.fileList()) {
      FileUtils.copyFile(activity.getFileStreamPath(s), new File(cacheDir, s));
    }
  }

  private FileOutputStream getFileOutStream(String name) throws FileNotFoundException {
    File file = new File(cacheDir, name);
    return new FileOutputStream(file);
  }

  private FileInputStream getFileInStream(String name) throws FileNotFoundException {
    File file = new File(cacheDir, name);
    return new FileInputStream(file);
  }

  public void changeCountState(String packageName, Activity activity, boolean enableCount) {
    try {
      if (isUnCount(packageName) != enableCount) {
        return;
      }
      StringBuilder sb = new StringBuilder(StringUtils.join(uncountApps, "\n"));
      if (uncountApps.isEmpty()) {
        if (enableCount) {

        } else {
          sb.append(packageName).append("\n");
        }
      } else {
        if (enableCount) {
          List<String> notCountApps = new ArrayList<>(Arrays.asList(sb.toString().split("\n")));
          notCountApps.remove(packageName);
          sb.setLength(0);
          for (String notCountApp : notCountApps) {
            sb.append(notCountApp).append("\n");
          }
        } else {
          sb.append(packageName).append("\n");
        }
      }
      File file = new File(cacheDir, FILE_UNCOUNT);
      FileUtils.write(file, sb.toString(), StandardCharsets.UTF_8);
    } catch (Exception e) {

    }
  }

  public boolean isUnCount(String appPackageName) {
    try {
      return uncountApps.contains(appPackageName);
    } catch (Exception e) {
      Log.e("isUnCount", e.getMessage());
      return false;
    }
  }

  public int getCount(String appPackageName) {
    FileInputStream inputStream = null;
    try {
      Properties properties = new Properties();
      inputStream = getFileInStream(FILE_COUNT);
      properties.load(inputStream);
      String property = properties.getProperty(appPackageName);
      if (isUnCount(appPackageName)) {
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
      inputStream = getFileInStream(FILE_COUNT);
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
            inputStream = getFileInStream(FILE_COUNT);
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
          outStream = getFileOutStream(FILE_COUNT);
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

  public void savePackageNameMap() {
    try {
      FileUtils.writeStringToFile(new File(cacheDir, File_APP_NAME),
          cachedPackageNameMap.toString(), StandardCharsets.UTF_8);
    } catch (Exception e) {

    }
  }

  public void saveNoMainActivityApps() {
    try {
      FileUtils.writeStringToFile(new File(cacheDir, File_NO_MAINACTIVITY_APP),
          StringUtils.join(noMainActivityApps, "\n"), StandardCharsets.UTF_8);
    } catch (Exception e) {

    }
  }

}
