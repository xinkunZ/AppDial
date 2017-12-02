package com.zxk.appdial.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

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
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          Log.e(CountHelper.class.getName(), "error", e);
        }
      }
    }
  }

  public void recordAppCount(final String appName, final int newValue, final Activity activity) {
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
          properties.setProperty(appName, newValue + "");
          outStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
          properties.store(outStream, "");
          outStream.flush();
          outStream.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          try {
            if (outStream != null) {
              outStream.close();
            }
          } catch (IOException e) {

          }
        }
      }
    }).start();

  }
}
