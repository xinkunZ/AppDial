package com.zxk.appdial.model;

import java.io.Serializable;

import android.graphics.drawable.Drawable;

/**
 * @author zhangxinkun
 */
public class LocalApps implements Serializable {

  private static final long serialVersionUID = 1L;
  private String appName;
  private String packageName;
  private String className;
  private Drawable icon;
  private String simpleChn;
  private String pinyin;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    LocalApps localApps = (LocalApps) o;

    if (appName != null ? !appName.equals(localApps.appName) : localApps.appName != null)
      return false;
    return packageName != null ? packageName.equals(localApps.packageName) : localApps.packageName == null;
  }

  @Override
  public int hashCode() {
    int result = appName != null ? appName.hashCode() : 0;
    result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
    return result;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String name) {
    this.appName = name;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public Drawable getIcon() {
    return icon;
  }

  public void setIcon(Drawable icon) {
    this.icon = icon;
  }

  public String getSimpleChn() {
    return simpleChn;
  }

  public void setSimpleChn(String simpleChn) {
    this.simpleChn = simpleChn;
  }

  public String getPinyin() {
    return pinyin;
  }

  public void setPinyin(String pinyin) {
    this.pinyin = pinyin;
  }
}
