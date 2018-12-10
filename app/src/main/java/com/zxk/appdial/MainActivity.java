package com.zxk.appdial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.zxk.appdial.model.LocalApp;
import com.zxk.appdial.utils.AppHelper;
import com.zxk.appdial.utils.CountHelper;
import com.zxk.appdial.utils.ThreadHelper;

public class MainActivity extends Activity implements ThreadHelper.ThreadHeplerUser<LocalApp> {

  private ListView apppsListView;
  private ListViewAdapter listViewAdapter;

  private TextView numberTextView;
  private AppHelper appHelper;
  private CountHelper countHelper;

  private FirebaseAnalytics firebaseAnalytics;
  private ShortcutManager shortcutManager = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    createEventHandlers();
    initAppList(false);
  }

  private void createEventHandlers() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      shortcutManager = getSystemService(ShortcutManager.class);
    }

    firebaseAnalytics = FirebaseAnalytics.getInstance(this);
    apppsListView = findViewById(R.id.appList);
    numberTextView = findViewById(R.id.numberTextView);
    appHelper = new AppHelper(getPackageManager(), this);
    countHelper = new CountHelper(this);
    listViewAdapter = new ListViewAdapter();
    fuckOPPO();
    apppsListView.setAdapter(listViewAdapter);
    apppsListView.setOnItemClickListener((parent, view, position, id) -> {
      LocalApp item = (LocalApp) listViewAdapter.getItem(position);
      Bundle bundle = new Bundle();
      bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, Build.MODEL);
      bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.getAppName());
      firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
      if (item.isInCount()) {
        item.setCount(item.getCount() + 1);
        countHelper.recordAppCount(item.getPackageName(), item.getCount(), MainActivity.this);
      }
      updateShortcuts();
      PackageManager packageManager = getPackageManager();
      Intent intent = packageManager.getLaunchIntentForPackage(item.getPackageName());
      startActivity(intent);
    });

    registerForContextMenu(apppsListView);
  }

  private void updateShortcuts() {
    if (shortcutManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      List<LocalApp> apps = countHelper.getFrequentApps4ShortcutsApp(this);
      List<ShortcutInfo> shortcutInfos = new ArrayList<>();
      if (apps != null) {
        int max = shortcutManager.getMaxShortcutCountPerActivity();
        max = max > apps.size() ? apps.size() : max;
        for (int i = 0; i < max; i++) {
          try {
            LocalApp app = apps.get(i);
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(app.getPackageName(), 0);
            Intent intent = packageManager.getLaunchIntentForPackage(packageInfo.packageName);
            if (intent != null) {
              CharSequence appName = packageInfo.applicationInfo.loadLabel(packageManager);
              ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, packageInfo.packageName)
                  //
                  .setIntent(intent)
                  .setIcon(
                      Icon.createWithBitmap(drawableToBitmap(packageInfo.applicationInfo
                          .loadIcon(packageManager)))).setShortLabel(appName).setLongLabel(appName)
                  .build();
              shortcutInfos.add(shortcutInfo);
            }
          } catch (Exception e) {

          }
        }
        shortcutManager.setDynamicShortcuts(shortcutInfos);
      }
    }
  }

  public static Bitmap drawableToBitmap(Drawable drawable) {
    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
        drawable.getIntrinsicHeight(),
        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
            : Bitmap.Config.RGB_565);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    drawable.draw(canvas);
    return bitmap;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    menu.setHeaderTitle(R.string.operation);
    int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
    menu.add(0, position, Menu.NONE, R.string.appInfo);
    menu.add(0, position, Menu.NONE, R.string.uninstall);
    LocalApp item = (LocalApp) apppsListView.getItemAtPosition(position);
    if (item.isInCount()) {
      menu.add(0, position, Menu.NONE, R.string.notCalac);
    } else {
      menu.add(0, position, Menu.NONE, R.string.calac);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    LocalApp selectedApp = (LocalApp) apppsListView.getItemAtPosition(itemId);
    if (getString(R.string.appInfo).equals(item.getTitle())) {
      Intent intent = new Intent();
      intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(Uri.fromParts("package", selectedApp.getPackageName(), null));
      startActivity(intent);
    } else if (getString(R.string.notCalac).equals(item.getTitle())) {
      new Thread(() -> {
        countHelper.changeCountState(selectedApp.getPackageName(), this, false);
        initAppList(true);
        searchAndLoadToUI(false);
      }).start();
    } else if (getString(R.string.calac).equals(item.getTitle())) {
      new Thread(() -> {
        countHelper.changeCountState(selectedApp.getPackageName(), this, true);
        initAppList(true);
        searchAndLoadToUI(false);
      }).start();
    } else {
      Uri uri = Uri.parse("package:" + selectedApp.getPackageName());
      Intent intent = new Intent(Intent.ACTION_DELETE, uri);
      startActivity(intent);
    }
    return true;
  }

  private void fuckOPPO() {
    // 辣鸡oppo需要手动指定id创建事件
    LinearLayout view = findViewById(R.id.lineOne);
    for (int i = 0; i < view.getChildCount(); i++) {
      View childAt = view.getChildAt(i);
      if (childAt instanceof MyButton) {
        childAt.setOnClickListener(this::clickButton);
      }
    }
    view = findViewById(R.id.lineTwo);
    for (int i = 0; i < view.getChildCount(); i++) {
      View childAt = view.getChildAt(i);
      if (childAt instanceof MyButton) {
        childAt.setOnClickListener(this::clickButton);
      }
    }
    view = findViewById(R.id.lineThree);
    for (int i = 0; i < view.getChildCount(); i++) {
      View childAt = view.getChildAt(i);
      if (childAt instanceof MyButton) {
        childAt.setOnClickListener(this::clickButton);
      }
    }
    MyButton buttonV = findViewById(R.id.buttonv);
    buttonV.setOnClickListener(this::miniDial);

    MyButton button0 = findViewById(R.id.button0);
    button0.setOnClickListener(this::clickButton);

    MyButton buttonC = findViewById(R.id.buttonc);
    buttonC.setOnClickListener(this::clickButton);
    // 垃圾oppo结束

    buttonC.setOnLongClickListener(this::clearText);
  }

  private boolean clearText(View view) {
    searchText.setLength(0);
    searchAndLoadToUI(false);
    return true;
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
  private static List<LocalApp> appInfos = null;

  private static List<LocalApp> lastApps = new ArrayList<>();

  private volatile static List<LocalApp> filter = Collections
      .synchronizedList(new ArrayList<LocalApp>());

  private void initAppList(boolean reload) {
    // 扫描得到APP列表
    if (reload) {
      appInfos = null;
    }

    if (appInfos == null) {
      appInfos = appHelper.scanLocalInstallAppList(reload);
    }
    mHandler.post(() -> {
      listViewAdapter.setData(appInfos);
      lastApps.addAll(appInfos);
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

  private StringBuilder searchText = new StringBuilder();

  public void clickButton(View view) {
    if (view instanceof Button) {
      boolean delete = false;

      if (!isFunc(view)) {
        CharSequence text = ((Button) view).getText();
        if (getResources().getString(R.string.button_delete).equals(text)) {
          String s = searchText.toString();
          if (s.length() > 0) {
            delete = true;
            searchText.setLength(0);
            searchText.append(s.substring(0, s.length() - 1));
          }
        } else {
          searchText.append(text);
        }
        final boolean finalDelete = delete;
        searchAndLoadToUI(finalDelete);
      }
    }
  }

  private void searchAndLoadToUI(final boolean isDelete) {
    mHandler.post(() -> {
      numberTextView.setText(searchText.toString());
      t9Filter(isDelete);
    });
  }

  private void t9Filter(boolean delete) {
    filter.clear();
    if (searchText.toString().isEmpty()) {
      // 为空则展示全部
      filter = new ArrayList<>();
      filter.addAll(appInfos);
      afterRun();
    } else {
      if (delete) {
        // 退格 重查 后续考虑加入上上笔搜索结果直接获取
        lastApps = new ArrayList<>();
        lastApps.addAll(appInfos);
      }
      new ThreadHelper<>(lastApps, this).exe();
    }
  }

  @Override
  public void run(List<LocalApp> list) {
    List<LocalApp> newList = new ArrayList<>();
    for (LocalApp localApp : list) {
      PinyinSearchUnit unit = new PinyinSearchUnit();
      unit.setBaseData(localApp.getAppName());
      PinyinUtil.parse(unit);
      if (T9Util.match(unit, searchText.toString())) {
        newList.add(localApp);
      }
    }
    syncAdd(newList);
  }

  @Override
  public void afterRun() {
    listViewAdapter.setData(filter);
    lastApps = new ArrayList<>();
    lastApps.addAll(filter);
  }

  private synchronized void syncAdd(List<LocalApp> newList) {
    for (LocalApp localApp : newList) {
      if (!filter.contains(localApp)) {
        filter.add(localApp);
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

    private List<LocalApp> data = new ArrayList<>();

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
      LocalApp item = (LocalApp) getItem(position);
      ViewHolder mViewHolder;
      View view;
      if (convertView == null) {
        mViewHolder = new ViewHolder();
        view = LayoutInflater.from(getBaseContext()).inflate(R.layout.activity_list_item, parent,
            false);
        mViewHolder.iv_app_icon = view.findViewById(R.id.appIcon);
        mViewHolder.tx_app_name = view.findViewById(R.id.appName);
        view.setTag(mViewHolder);
      } else {
        view = convertView;
        mViewHolder = (ViewHolder) view.getTag();
      }
      mViewHolder.iv_app_icon.setImageDrawable(item.getIcon());
      mViewHolder.tx_app_name.setText(item.getAppName());
      return view;
    }

    public void setData(List<LocalApp> data) {
      this.data = data;
      notifyDataSetChanged();
    }
  }

  static class ViewHolder {
    ImageView iv_app_icon;
    TextView tx_app_name;
  }
}
