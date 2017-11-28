package com.zxk.appdial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.t9search.model.PinyinSearchUnit;
import com.t9search.util.PinyinUtil;
import com.t9search.util.T9Util;
import com.zxk.appdial.cmp.MyButton;
import com.zxk.appdial.model.LocalApps;
import com.zxk.appdial.utils.AppTools;
import com.zxk.appdial.utils.CountHelper;
import com.zxk.appdial.utils.ThreadHelper;

public class MainActivity extends Activity implements ThreadHelper.ThreadHeplerUser<LocalApps> {

  private ListView apppsListView;
  private ListViewAdapter listViewAdapter;

  private TextView numberTextView;
  private AppTools appTools;
  private CountHelper countHelper;

  private FirebaseAnalytics firebaseAnalytics;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    createEventHandlers();
    initAppList();
  }

  private void createEventHandlers() {
    firebaseAnalytics = FirebaseAnalytics.getInstance(this);
    apppsListView = (ListView) findViewById(R.id.appList);
    numberTextView = (TextView) findViewById(R.id.numberTextView);
    appTools = new AppTools(getPackageManager(), this);
    countHelper = new CountHelper();
    listViewAdapter = new ListViewAdapter();
    fuckOPPO();
    apppsListView.setAdapter(listViewAdapter);
    apppsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LocalApps item = (LocalApps) listViewAdapter.getItem(position);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, Build.MODEL);
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.getAppName());
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        item.setCount(item.getCount() + 1);
        countHelper.recordAppCount(item.getAppName(), item.getCount(), MainActivity.this);

        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(item.getPackageName());
        startActivity(intent);
      }

    });
  }

  private void fuckOPPO() {
    //辣鸡oppo需要手动指定id创建事件
    LinearLayout view = (LinearLayout) findViewById(R.id.lineOne);
    for (int i = 0; i < view.getChildCount(); i++) {
      View childAt = view.getChildAt(i);
      if (childAt instanceof MyButton) {
        childAt.setOnClickListener(this::clickButton);
      }
    }
    view = (LinearLayout) findViewById(R.id.lineTwo);
    for (int i = 0; i < view.getChildCount(); i++) {
      View childAt = view.getChildAt(i);
      if (childAt instanceof MyButton) {
        childAt.setOnClickListener(this::clickButton);
      }
    }
    view = (LinearLayout) findViewById(R.id.lineThree);
    for (int i = 0; i < view.getChildCount(); i++) {
      View childAt = view.getChildAt(i);
      if (childAt instanceof MyButton) {
        childAt.setOnClickListener(this::clickButton);
      }
    }
    MyButton buttonV = (MyButton) findViewById(R.id.buttonv);
    buttonV.setOnClickListener(this::miniDial);

    MyButton button0 = (MyButton) findViewById(R.id.button0);
    button0.setOnClickListener(this::clickButton);

    MyButton buttonC = (MyButton) findViewById(R.id.buttonc);
    buttonC.setOnClickListener(this::clickButton);
    //垃圾oppo结束
  }

  @Override
  protected void onResume() {
    if (appInfos != null) {
      Collections.sort(appInfos);
      Collections.reverse(appInfos);
    }
    if (lastApps != null) {
      Collections.sort(lastApps);
      Collections.reverse(lastApps);
    }
    super.onResume();
  }

  public Handler mHandler = new Handler();
  private static List<LocalApps> appInfos = null;

  private static List<LocalApps> lastApps = new ArrayList<>();

  private volatile static List<LocalApps> filter = Collections.synchronizedList(new ArrayList<LocalApps>());

  private void initAppList() {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        //扫描得到APP列表
        if (appInfos == null) {
          appInfos = appTools.scanLocalInstallAppList();
        }
        mHandler.post(new Runnable() {
          @Override
          public void run() {
            listViewAdapter.setData(appInfos);
            lastApps.addAll(appInfos);
          }
        });
      }
    });
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
      }
    });
  }

  private void t9Filter(boolean delete) {
    filter.clear();
    if (string.toString().isEmpty()) {
      //为空则展示全部
      filter = new ArrayList<>();
      filter.addAll(appInfos);
      afterRun();
    } else {
      if (delete) {
        //退格 重查 后续考虑加入上上笔搜索结果直接获取
        lastApps = new ArrayList<>();
        lastApps.addAll(appInfos);
      }
      Log.d("搜索 - ", String.format("从%s中 T9搜索 %s", lastApps, string));
      new ThreadHelper<>(lastApps, this).exe();
    }
  }

  @Override
  public void run(List<LocalApps> list) {
    List<LocalApps> newList = new ArrayList<>();
    for (LocalApps localApps : list) {
      PinyinSearchUnit unit = new PinyinSearchUnit();
      unit.setBaseData(localApps.getAppName());
      PinyinUtil.parse(unit);
      if (T9Util.match(unit, string.toString())) {
        newList.add(localApps);
      }
    }
    syncAdd(newList);
  }

  @Override
  public void afterRun() {
    Log.d("搜索", "结果 - " + filter);
    listViewAdapter.setData(filter);
    lastApps = new ArrayList<>();
    lastApps.addAll(filter);
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
