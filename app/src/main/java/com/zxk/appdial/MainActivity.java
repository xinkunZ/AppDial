package com.zxk.appdial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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

  private static Set<LocalApps> filter = Collections.synchronizedSet(new HashSet<LocalApps>());;

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
      if (!isFunc(view)) {
        CharSequence text = ((Button) view).getText();
        if (getResources().getString(R.string.button_delete).equals(text)) {
          String s = string.toString();
          if (s.length() > 0) {
            string.setLength(0);
            string.append(s.substring(0, s.length() - 1));
          }
        } else {
          string.append(text);
        }
        mHandler.post(new Runnable() {
          @Override
          public void run() {
            t9Filter();
            numberTextView.setText(string.toString());
          }
        });
      }
    }
  }

  private void t9Filter() {
    if (string.toString().isEmpty()) {
      listViewAdapter.setData(appInfos);
    } else {
      filter.clear();
      int size = appInfos.size();
      if (size > 50) {
        int count = size / 10;
        for (int i = 0; i < count; i++) {
          List<LocalApps> list1 = appInfos.subList(i * count, i * count + 10);
          new Worker(list1).start();
        }
        List<LocalApps> last = appInfos.subList(count * 10 - 1, size - 1);
        new Worker(last).start();
      } else {
        new Worker(appInfos).start();
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
      filter.addAll(newList);
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          ArrayList<LocalApps> data = new ArrayList<>();
          data.addAll(filter);
          listViewAdapter.setData(data);
        }
      });
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
