package com.zxk.appdial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.t9search.model.PinyinSearchUnit;
import com.t9search.util.PinyinUtil;
import com.t9search.util.T9Util;
import com.zxk.appdial.model.LocalApps;
import com.zxk.appdial.utils.AppTools;

public class MainActivity extends Activity {

  private ListView apppsListView;
  private ListViewAdapter listViewAdapter;

  private TextView numberTextView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    apppsListView = (ListView) findViewById(R.id.appList);
    numberTextView = (TextView) findViewById(R.id.numberTextView);

    listViewAdapter = new ListViewAdapter();
    apppsListView.setAdapter(listViewAdapter);
    apppsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LocalApps item = (LocalApps) listViewAdapter.getItem(position);
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(item.getPackageName());
        startActivity(intent);
      }

    });

    initAppList();
  }

  public Handler mHandler = new Handler();
  private static List<LocalApps> appInfos = null;

  private static List<LocalApps> lastApps = new ArrayList<>();

  private volatile static List<LocalApps> filter = Collections.synchronizedList(new ArrayList<LocalApps>());

  private void initAppList() {
    new Thread() {
      @Override
      public void run() {
        super.run();
        //扫描得到APP列表
        if (appInfos == null) {
          appInfos = AppTools.scanLocalInstallAppList(MainActivity.this.getPackageManager());
        }
        mHandler.post(new Runnable() {
          @Override
          public void run() {
            listViewAdapter.setData(appInfos);
            lastApps.addAll(appInfos);
          }
        });
      }
    }.start();
  }

  public void miniDial(View view) {
    if (view instanceof Button) {
      CharSequence text = ((Button) view).getText();
      if ("v".equals(text)) {
        View one = findViewById(R.id.lineOne);
        View two = findViewById(R.id.lineTwo);
        View three = findViewById(R.id.lineThree);
        one.setVisibility(View.GONE);
        two.setVisibility(View.GONE);
        three.setVisibility(View.GONE);
        ((Button) view).setText("^");
      } else {
        View one = findViewById(R.id.lineOne);
        View two = findViewById(R.id.lineTwo);
        View three = findViewById(R.id.lineThree);
        one.setVisibility(View.VISIBLE);
        two.setVisibility(View.VISIBLE);
        three.setVisibility(View.VISIBLE);
        ((Button) view).setText("v");
      }
    }

  }

  private StringBuilder string = new StringBuilder();

  public void clickButton(View view) {
    if (view instanceof Button) {
      boolean delete = false;

      if (!isFunc(view)) {
        CharSequence text = ((Button) view).getText();
        if (getResources().getString(R.string.button_delete).equals(text)) {
          String s = string.toString();
          if (s.length() > 0) {
            delete = true;
            string.setLength(0);
            string.append(s.substring(0, s.length() - 1));
          }
        } else {
          string.append(text);
        }
        final boolean finalDelete = delete;
        searchAndLoadToUI(finalDelete);
      }
    }
  }

  private void searchAndLoadToUI(final boolean isDelete) {
    mHandler.post(new Runnable() {

      @Override
      public void run() {
        numberTextView.setText(string.toString());
        t9Filter(isDelete);
        new LoadDataToUI().start();
      }
    });
  }

  private class LoadDataToUI extends Thread {

    @Override
    public void run() {
      if (countDownLatch != null) {
        try {
          //等待所有的搜索线程跑完
          countDownLatch.await();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          listViewAdapter.setData(filter);
          lastApps = new ArrayList<>();
          lastApps.addAll(filter);
        }
      });
    }
  }

  private CountDownLatch countDownLatch = null;

  private final int threadSize = 20;

  private void t9Filter(boolean delete) {
    filter.clear();
    if (string.toString().isEmpty()) {
      filter = new ArrayList<>();
      filter.addAll(appInfos);
      listViewAdapter.setData(appInfos);
    } else {
      if (delete) {
        //退格 重查 后续考虑加入上上笔搜索结果直接获取
        lastApps = new ArrayList<>();
        lastApps.addAll(appInfos);
      }

      int size = lastApps.size();
      if (size > 50) {

        int count = size / threadSize;
        boolean haveTail = false;
        if (count * threadSize != size) {
          haveTail = true;
        }
        countDownLatch = new CountDownLatch(count + (haveTail ? 1 : 0));
        for (int i = 0; i < count; i++) {
          System.out.println(String.format("从%s ~ %s ", i * threadSize, i * threadSize + threadSize));
          List<LocalApps> l = lastApps.subList(i * threadSize, i * threadSize + threadSize);
          new Worker(l).start();
        }
        if (haveTail) {
          System.out.println(String.format("尾巴 %s ~ %s ", count * threadSize - 1, size - 1));
          List<LocalApps> latest = lastApps.subList(count * threadSize - 1, size - 1);
          new Worker(latest).start();
        }
      } else {
        countDownLatch = new CountDownLatch(1);
        new Worker(lastApps).start();
      }
    }
  }

  private class Worker extends Thread {

    private List<LocalApps> apps = new ArrayList<>();

    public Worker(List<LocalApps> apps) {
      this.apps = apps;
    }

    @Override
    public void run() {
      List<LocalApps> newList = new ArrayList<>();
      for (LocalApps localApps : apps) {
        PinyinSearchUnit unit = new PinyinSearchUnit();
        unit.setBaseData(localApps.getAppName());
        PinyinUtil.parse(unit);
        if (T9Util.match(unit, string.toString())) {
          newList.add(localApps);
        }
      }
      syncAdd(newList);
      countDownLatch.countDown();
    }
  }

  private synchronized void syncAdd(List<LocalApps> newList) {
    for (LocalApps localApps : newList) {
      if (!filter.contains(localApps)) {
        filter.add(localApps);
      }
    }
  }

  private boolean isFunc(View view) {
    String miniSize = getResources().getString(R.string.button_miniSize);
    String maxSize = getResources().getString(R.string.button_maxSize);
    CharSequence text = ((Button) view).getText();
    return miniSize.equals(text) || maxSize.equals(text);
  }

  private class ListViewAdapter extends BaseAdapter {

    private List<LocalApps> data = new ArrayList<LocalApps>();

    @Override

    public int getCount() {
      return data == null ? 0 : data.size();
    }

    @Override
    public Object getItem(int position) {
      return data == null ? null : data.get(position);
    }

    @Override
    public long getItemId(int position) {
      return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      LocalApps item = (LocalApps) getItem(position);
      ViewHolder mViewHolder;
      View view;
      if (convertView == null) {
        mViewHolder = new ViewHolder();
        view = LayoutInflater.from(getBaseContext()).inflate(R.layout.activity_list_item, parent, false);
        mViewHolder.iv_app_icon = (ImageView) view.findViewById(R.id.appIcon);
        mViewHolder.tx_app_name = (TextView) view.findViewById(R.id.appName);
        view.setTag(mViewHolder);
      } else {
        view = convertView;
        mViewHolder = (ViewHolder) view.getTag();
      }
      mViewHolder.iv_app_icon.setImageDrawable(item.getIcon());
      mViewHolder.tx_app_name.setText(item.getAppName());
      return view;
    }

    public void setData(List<LocalApps> data) {
      this.data = data;
      notifyDataSetChanged();
    }
  }

  static class ViewHolder {
    ImageView iv_app_icon;
    TextView tx_app_name;
  }
}
