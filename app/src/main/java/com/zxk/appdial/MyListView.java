package com.zxk.appdial;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * @author zhangxinkun
 */
public class MyListView extends ListView implements AbsListView.OnScrollListener {
  public MyListView(Context context) {
    super(context);
    setOnScrollListener(this);
  }

  public MyListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOnScrollListener(this);

  }

  public MyListView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOnScrollListener(this);

  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public MyListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    setOnScrollListener(this);
  }

  private OnScrollDirectionListener mListener;
  private float startY = 0;//按下时y值
  private int mTouchSlop;//系统值

  public void setOnScrollDirectionListener(OnScrollDirectionListener listener) {
    this.mListener = listener;
  }

  public interface OnScrollDirectionListener {
    //向上滑动
    void onScrollUp();

    //向下滑动
    void onScrollDown();
  }

  private int state = -1;

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    state = scrollState;
  }

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (state != SCROLL_STATE_TOUCH_SCROLL) {
      return super.onTouchEvent(ev);
    }
    int action = ev.getAction();
    switch (action) {
    case MotionEvent.ACTION_DOWN:
      startY = ev.getY();
      break;
    case MotionEvent.ACTION_MOVE:
      if (Math.abs(ev.getY() - startY) > mTouchSlop) {
        if (ev.getY() - startY >= 0) {
          mListener.onScrollDown();
        } else {
          mListener.onScrollUp();
        }
      }
      startY = ev.getY();
      break;
    }
    return super.onTouchEvent(ev);
  }
}
