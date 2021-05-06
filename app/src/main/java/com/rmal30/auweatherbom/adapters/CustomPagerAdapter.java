package com.rmal30.auweatherbom.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import com.rmal30.auweatherbom.R;

//Pager adapter to switch between radar and weather
public class CustomPagerAdapter extends PagerAdapter {

    public CustomPagerAdapter(Context context) {

    }

    public Object instantiateItem(ViewGroup collection, int position) {
        int resId = 0;
        switch (position) {
            case 0:
                resId = R.id.weather;
                break;
            case 1:
                resId = R.id.radar;
                break;
        }
        return collection.findViewById(resId);
    }

    @Override
    public void destroyItem(ViewGroup arg0, int arg1, Object arg2) {
        arg0.removeView((View) arg2);
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public int getCount() {
        return 2;
    }
}

