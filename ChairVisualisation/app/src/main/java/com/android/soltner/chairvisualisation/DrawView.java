package com.android.soltner.chairvisualisation;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by Angelo on 22.07.2019.
 */

public class DrawView extends View{
    Paint paint=new Paint();

    private void init(){
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(30);
    }

    public DrawView(Context context){
        super(context);
        init();
    }

    public DrawView(Context context, AttributeSet attrs){
        super(context,attrs);
        init();
    }
    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    public void onDraw(Canvas canvas){
        DisplayMetrics met=new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(met);
        int halfheight=((View) (this.getParent())).getHeight()/2;
        int halfwidth= ((View) (this.getParent())).getWidth()/2;

        float[] lines={halfwidth,halfheight-300,halfwidth,halfheight,
                halfwidth,halfheight,halfwidth+300,halfheight};

        //canvas.drawCircle(halfwidth,halfheight,100,paint);

        canvas.drawLines(lines,paint);
    }
}
