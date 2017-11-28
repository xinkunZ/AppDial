package com.zxk.appdial.cmp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.Button;
import com.zxk.appdial.R;

/**
 * @author zhangxinkun
 */
public class MyButton extends Button {

  private String chars;
  private String num;

  @Override
  public CharSequence getText() {
    return num;
  }

  @Override
  public void setText(CharSequence text, BufferType type) {
    num = text.toString();
    super.setText("", type);
  }

  public MyButton(Context context) {
    super(context);
  }

  public MyButton(Context context, AttributeSet attrs) {
    super(context, attrs);
    //获取自定义的属性
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.buttonText);
    chars = ta.getString(R.styleable.buttonText_t9Chars);
    num = ta.getString(R.styleable.buttonText_num);
    ta.recycle();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    Paint paint = new Paint();
    float line1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 30, getResources().getDisplayMetrics());
    float line2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 19, getResources().getDisplayMetrics());

    paint.setTextSize(line1);
    paint.setColor(Color.WHITE);
    float tagWidth = paint.measureText(num + "");

    int x = (int) (this.getWidth() - tagWidth) / 3;
    int y = this.getHeight() / 3;
    canvas.drawText(num + "", x, 2 * y, paint);

    paint.setTextSize(line2);
    paint.setColor(Color.GRAY);

    canvas.drawText(chars + "", x + tagWidth + 10f, 2 * y, paint);
  }
}
