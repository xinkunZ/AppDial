package com.zxk.appdial;

import java.util.ArrayList;
import java.util.List;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.zxk.appdial.model.LocalApps;
import com.zxk.appdial.utils.AppTools;

public class MainActivity extends Activity {

  private View root;
  private ListView apppsListView;
  private View dial;

  private ListViewAdapter listViewAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    apppsListView = (ListView) findViewById(R.id.appList);
    dial = findViewById(R.id.dial);
    root = findViewById(R.id.rlRoot);

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

  private void initAppList() {
    new Thread() {
      @Override
      public void run() {
        super.run();
        //扫描得到APP列表
        final List<LocalApps> appInfos = AppTools.scanLocalInstallAppList(MainActivity.this.getPackageManager());
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
    View viewById = findViewById(R.id.dial);
    if (viewById.getVisibility() == View.VISIBLE) {
      viewById.setVisibility(View.GONE);
    } else {
      viewById.setVisibility(View.VISIBLE);
    }
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
