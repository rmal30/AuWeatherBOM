package com.rmal30.auweatherbom.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rmal30.auweatherbom.R;
import com.rmal30.auweatherbom.models.Forecast;

import java.util.ArrayList;

//Adapter that loads the forecast view
public class ForecastAdapter extends BaseAdapter {
    private final Context ctx;
    private final ArrayList<Forecast> forecasts;
    private boolean invertedDisplay;

    public ForecastAdapter(Context ctx, ArrayList<Forecast> forecasts){
        this.ctx = ctx;
        this.forecasts = forecasts;
        this.invertedDisplay = false;
    }

    public void setInvertedDisplay(boolean invertedDisplay) {
        this.invertedDisplay = invertedDisplay;
    }

    public View getView(int i, View v, ViewGroup parent){
        if(v == null){
            final LayoutInflater li = LayoutInflater.from(this.ctx);
            v = li.inflate(R.layout.forecast_card, parent, false);

        }
        if(i >= forecasts.size()){
            return v;
        }
        Forecast forecast = forecasts.get(i);
        int weatherIcon;

        //Forecast weather icons
        switch(forecast.forecastIcon){
            case 1: weatherIcon = R.drawable.ic_sunny; break;
            case 2: weatherIcon = R.drawable.ic_clear; break;
            case 3: weatherIcon = R.drawable.ic_partly_cloudy; break;
            case 4: weatherIcon = R.drawable.ic_cloudy; break;
            case 6: case 10: weatherIcon = R.drawable.ic_foggy; break;
            case 8: case 11: case 12: case 17:  case 18: weatherIcon = R.drawable.ic_rainy; break;
            case 9: case 13: case 19: weatherIcon = R.drawable.ic_windy; break;
            case 14: weatherIcon = R.drawable.ic_frost; break;
            case 15: weatherIcon = R.drawable.ic_snowy; break;
            case 16: weatherIcon = R.drawable.ic_stormy; break;
            default: weatherIcon = R.drawable.ic_partly_cloudy; break;
        }

        final ImageView imageView = (ImageView) v.findViewById(R.id.imageView);

        if(Build.VERSION.SDK_INT < 23) {
            imageView.setImageDrawable(ctx.getResources().getDrawable(weatherIcon));
        }else{
            imageView.setImageDrawable(ctx.getResources().getDrawable(weatherIcon, ctx.getTheme()));
        }
        final TextView day = (TextView) v.findViewById(R.id.day);
        final TextView min = (TextView) v.findViewById(R.id.min);
        final TextView max = (TextView) v.findViewById(R.id.max);
        final TextView prob = (TextView) v.findViewById(R.id.rainprob);
        final ImageView humidityView = ((ImageView) v.findViewById(R.id.raindrop));

        TextView[] textElements = {day, min, max, prob};

        if (this.invertedDisplay) {
            v.setBackgroundResource(R.drawable.card_black);
            int blackColor = Color.argb(200, 0, 0, 0);
            for (TextView element: textElements) {
                element.setTextColor(blackColor);
            }
            imageView.setColorFilter(blackColor);
            humidityView.setColorFilter(blackColor);
        } else {
            v.setBackgroundResource(R.drawable.card);
            int whiteColor = Color.argb(200, 255,255,255);
            for (TextView element: textElements) {
                element.setTextColor(whiteColor);
            }
            imageView.setColorFilter(whiteColor);
            humidityView.setColorFilter(whiteColor);
        }

        final LinearLayout probLayout = (LinearLayout) v.findViewById(R.id.rainlayout);
        day.setText(forecast.day);
        if (forecast.min==null) {
            min.setVisibility(View.GONE);
        } else {
            min.setText(forecast.min + "°");
            min.setVisibility(View.VISIBLE);
        }

        if(forecast.max==null){
            max.setVisibility(View.INVISIBLE);
        }else{
            max.setText(forecast.max + "°");
            max.setVisibility(View.VISIBLE);
        }
        if(!forecast.rainProbability.isEmpty()) {
            prob.setText(forecast.rainProbability);
            probLayout.setVisibility(View.VISIBLE);
        }else{
            probLayout.setVisibility(View.INVISIBLE);
        }
        return v;
    }
    public int getCount(){
        return this.forecasts.size();
    }
    public long getItemId(int position){
        return position;
    }
    public View getItem(int position){
        return null;
    }
}