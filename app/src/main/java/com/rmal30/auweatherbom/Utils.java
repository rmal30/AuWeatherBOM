package com.rmal30.auweatherbom;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    public static boolean drawNumber(int i, int j, int number){
        short[] digits = {32319, 17394, 24253, 32437, 31879, 30391, 30399, 31777, 32447, 32439};
        if(j > 2 || j < -2 || i > 3 || i < -3 || number <= -10) {
            return false;
        }else if(number < 0){
            if(i < 0){
                return 1 == ((4228 >> ((i+3)*5 + (j+2))) & 1);
            }else if(i > 0){
                return 1 == ((digits[-number] >> ((i-1)*5 + (j+2))) & 1);
            }else{
                return false;
            }
        }else if(number < 10){
            return 1 == ((digits[number] >> ((i+1)*5 + (j+2))) & 1);
        }else if(i < 0){
            return 1 == ((digits[number/10] >> ((i+3)*5 + (j+2))) & 1);
        }else if(i > 0){
            return 1 == ((digits[number%10] >> ((i-1)*5 + (j+2))) & 1);
        }else{
            return false;
        }
    }
    //Compute approximate distance between two coordinates. Use Pythagoras theorem as distances are short enough
    public static int distance(String pos1, String pos2){
        String[] coords1 = pos1.split(",");
        String[] coords2 = pos2.split(",");
        float lat1 = Float.parseFloat(coords1[0]);
        float lon1 = Float.parseFloat(coords1[1]);
        float lat2 = Float.parseFloat(coords2[0]);
        float lon2 = Float.parseFloat(coords2[1]);
        return (int) Math.round((10000/90)*Math.sqrt((lat2-lat1)*(lat2-lat1)+(lon2-lon1)*(lon2-lon1)));
    }

    //Join elements of an array list into a string
    public static String joinArrayList(ArrayList<String> arrList, String separator) {
        String prefix = "";
        StringBuilder sb = new StringBuilder();
        for(String s:arrList) {
            sb.append(prefix);
            sb.append(s);
            prefix = separator;
        }
        return sb.toString();
    }

    //Find the nearest location from a given position
    public static String findNearestLocation(String position, Map<String, String> list) {
        int minDist = 10000;
        int curDist;
        String nearestPlace=null;
        for(String key:list.keySet()) {
            curDist = Utils.distance(position, list.get(key));
            if(curDist<minDist) {
                minDist = curDist;
                nearestPlace = key;
            }
        }
        if(minDist>600) {
            return null;
        }
        return nearestPlace;
    }

    //Longest common substring, no longer used
    public static double lcs(String str1, String str2) {
        int i=0;
        str1 = str1.replaceAll("\\(", "").replaceAll("\\)","").replaceAll("-","");
        str2 = str2.replaceAll("\\(", "").replaceAll("\\)","").replaceAll("-","");
        while(i<Math.min(str1.length(), str2.length()) && str1.charAt(i)==str2.charAt(i)) {i++;}
        return ((double)i)/(str1.length()+str2.length());
    }

    //Used to find colors that blend between two colors
    public static int interpolate(int color1, int color2, float control, int limit) {
        float red = Color.red(color1)*(1-control/limit)+Color.red(color2)*control/limit;
        float blue = Color.blue(color1)*(1-control/limit)+Color.blue(color2)*control/limit;
        float green = Color.green(color1)*(1-control/limit)+Color.green(color2)*control/limit;
        return Color.rgb(Math.round(red), Math.round(green), Math.round(blue));
    }

    //Color code for temperature.
    public static int tempColor(float temp) {
        int color;
        int white = Color.rgb(255, 255, 255);
        int grey = Color.rgb(100, 100, 100);
        int purple = Color.rgb(125, 0, 125);
        int blue = Color.rgb(25, 75, 175);
        int teal = Color.rgb(0, 150, 150);
        int green = Color.rgb(50, 150, 50);
        int yellow = Color.rgb(175, 175, 0);
        int orange = Color.rgb(200, 125, 0);
        int red = Color.rgb(250, 0, 0);
        int brown = Color.rgb(75, 25, 25);
        int black = Color.rgb(0, 0, 0);
        int[] colorArray = {white, grey, purple, blue, teal, green, yellow, orange, red, brown, black};
        if (temp < -5) {
            color = white;
        } else if (temp < 45) {
            int i = (int) Math.floor((temp + 5) / 5);
            color = Utils.interpolate(colorArray[i], colorArray[i + 1], (temp + 100) % 5, 5);
        } else {
            color = black;
        }
        return color;
    }

    public static void drawTempIcon(Float temp, Bitmap bitmap, int x, int y){
        int color;
        if (Math.round(temp) < 0) {
            color = Color.BLACK;
        } else {
            color = Color.WHITE;
        }
        if (x > 10 && y > 15 && x < 500 && y < 490) {
            for (int i = -6; i <= 6; i++) {
                for (int j = -6; j <= 6; j++) {
                    if (i * i + j * j < 33) {

                        if (Utils.drawNumber(i, j, Math.round(temp))) {
                            bitmap.setPixel(x + i, y + j, color);
                        } else {
                            bitmap.setPixel(x + i, y + j, Utils.tempColor(temp));
                        }
                    } else if (i*i + j*j >= 33 && i*i + j*j <= 41) {
                        bitmap.setPixel(x + i, y + j, color);
                    }
                }
            }
        }
    }

    public static int[] getStationImageCoordinatesFromLocation(
            int width, int height, int zoomRange, String[] radarLocation, String[] stationLocation){
        float radarLatitude = Float.parseFloat(radarLocation[0]);
        float radarLongitude = Float.parseFloat(radarLocation[1]);
        float stationLatitude = Float.parseFloat(stationLocation[0]);
        float stationLongitude = Float.parseFloat(stationLocation[1]);
        int x = 261 + (int) Math.round(10000 * 0.9771 * 0.5 * Math.cos(radarLatitude * Math.PI / 180) * width * (stationLongitude - radarLongitude)/(90*zoomRange));
        int y = 261 - (int) Math.round(10000 * 0.9078 * 0.5 * height * (stationLatitude - radarLatitude)/(90*zoomRange));
        int[] position = {x, y};
        return position;
    }

    public static void showInfoDialog(Context context){
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        String message = "";
        alertDialogBuilder.setTitle("About AU Weather:");
        message += "This app uses weather information from the Bureau of Meteorology (BoM): http://www.bom.gov.au/\n\n";
        message += "This app is not sponsored or endorsed in any way by the Bureau of Meteorology.\n\n";
        message += "The developers do not accept responsibility for any loss or damage occasioned by use of the information in this app.\n\n";
        message += "This app also uses Meteocons weather icons: http://www.alessioatzeni.com/meteocons/\n";
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
