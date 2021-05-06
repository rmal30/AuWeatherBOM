package com.rmal30.auweatherbom.activities;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import com.rmal30.auweatherbom.FTPReader;
import com.rmal30.auweatherbom.R;
import com.rmal30.auweatherbom.Utils;
import com.rmal30.auweatherbom.XMLParser;
import com.rmal30.auweatherbom.adapters.CustomPagerAdapter;
import com.rmal30.auweatherbom.adapters.ForecastAdapter;
import com.rmal30.auweatherbom.layouts.USwipeRefreshLayout;
import com.rmal30.auweatherbom.models.Forecast;
import com.rmal30.auweatherbom.models.Observation;
import com.rmal30.auweatherbom.models.Tree;

public class MainActivity extends AppCompatActivity {

    String root = "ftp://ftp.bom.gov.au/anon/gen/"; //Bureau of Meteorology FTP site
    final String fwoRoot = root + "fwo/"; //Weather observations and forecasts
    final String radarRoot = root + "radar/"; //Radar
    final String[] states = {"NSW", "VIC", "QLD", "WA", "SA", "TAS", "NT"}; //States of Australia
    final String[] forecastIDs = {"11060", "10753", "11295", "14199", "10044", "16710", "10207"};
    final String[] letters = {"N", "V", "Q", "W", "S", "T", "D"}; //Used by BOM
    boolean loaded = true;
    boolean radarLoaded = false;
    Tree[] observations = {null, null, null, null, null, null, null};
    Tree[] forecastData = {null, null, null, null, null, null, null};
    String[] lastUpdated = new String[7];
    String[] lastUpdated2 = new String[7];
    int zoomLevel = 2;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    String[] places;
    Toast toast;
    ArrayList<String> favorites;
    ArrayList<Forecast> forecasts = new ArrayList<>();
    Stack<String> history = new Stack<>();
    Map<String, String> townList, stationList, forecastList, radarList;
    String currentPlace, currentTemp;
    ArrayAdapter<String> listAdapter;

    FTPReader ftpReader = new FTPReader(root);

    //Startup
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        CustomPagerAdapter mCustomPagerAdapter = new CustomPagerAdapter(this);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCustomPagerAdapter);
        mViewPager.setCurrentItem(0);

        //Get all indexed text files that contain location information
        stationList = getList("stations.txt", 4);
        forecastList = getList("forecastList.txt", 4);
        townList = getList("townList.txt", 5);
        radarList = getList("radarList.txt", 5);
        places = townList.keySet().toArray(new String[0]);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String place = sp.getString("location", null); //Get back previous location visited
        String bookmarks = sp.getString("Bookmarks", ""); //Get weather bookmarks

        //Read favorites in
        if (bookmarks.equals("")) {
            favorites = new ArrayList<>();
        } else {
            favorites = new ArrayList<>(Arrays.asList(bookmarks.split("\\n")));
        }

        if (townList != null && townList.containsKey(place)) {
            if (currentPlace != null && !currentPlace.equals(place)) {
                history.push(currentPlace);
            }
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getData(place); //Load stored place
        } else {
            //Bad data, so reset all stored data
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.remove("Bookmarks");
            editor.remove("location");
            editor.apply();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final LayoutInflater li = LayoutInflater.from(this);
        View v = li.inflate(R.layout.drawer_main, null); //Drawer with bookmarks
        setupDrawer();
        ((RelativeLayout)findViewById(R.id.navigation_drawer)).addView(v);
        ListView mDrawerList = (ListView) v.findViewById(R.id.left_drawer);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_selectable_list_item, favorites);
        mDrawerList.setAdapter(listAdapter);
        registerForContextMenu(mDrawerList);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String place = favorites.get(position);
                if (currentPlace != null && !currentPlace.equals(place)) {
                    history.push(currentPlace);
                }
                if (loaded) {
                    getData(place); //Load weather information from location stored in bookmark
                    mDrawerLayout.closeDrawer(findViewById(R.id.navigation_drawer));
                }
            }
        });

        final USwipeRefreshLayout swipeRefreshLayout = (USwipeRefreshLayout) findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(new USwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (loaded) {
                    getData(currentPlace);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        //Forecast view
        GridView forecastGrid = (GridView) findViewById(R.id.forecasts);
        ForecastAdapter forecastAdapter = new ForecastAdapter(this, forecasts);
        forecastGrid.setAdapter(forecastAdapter);
        forecastGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Forecast forecast = forecasts.get(position);
                if (toast != null) {
                    toast.cancel();
                }
                String summary = forecast.day + ": " + forecast.description + " " + forecast.rainRange;
                toast = Toast.makeText(MainActivity.this, summary, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    //Load radar
    public void loadRadar(final String place, final boolean invalidate, final int oldZoomLevel) {
        radarLoaded = false;
        String radarPlace = Utils.findNearestLocation(townList.get(place), radarList);
        final int[] zoomRange = {512, 256, 128, 64};
        ftpReader.setup();
        if (!ftpReader.isOpen()) {
            return;
        }
        if (radarPlace != null) {
            String radarID = radarPlace.split(", ")[2];
            String radarFileName = "IDR" + radarID + String.valueOf(zoomLevel) +".gif";
            byte[] byteArray = ftpReader.readFile(radarRoot, radarFileName);
            while (invalidate && byteArray == null && radarList.size() > 0) {
                radarList.remove(radarPlace);
                radarPlace = Utils.findNearestLocation(townList.get(place), radarList);
                if (radarPlace != null) {
                    radarID = radarPlace.split(", ")[2];
                    if (!ftpReader.isOpen()) {
                        return;
                    }
                    radarFileName = "IDR" + radarID + String.valueOf(zoomLevel) + ".gif";
                    byteArray = ftpReader.readFile(radarRoot, radarFileName);
                } else {
                    byteArray = null;
                }
            }
            final byte[] byteArray2 = byteArray;
            final String radarPlace2 = radarPlace;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (byteArray2 != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray2, 0, byteArray2.length);
                        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        String[] stationData, radarData;
                        int height = bitmap.getHeight();
                        int width = bitmap.getWidth();
                        int stateID = 0;
                        String stationName;
                        radarData = radarList.get(radarPlace2).split(",");
                        HashMap<String, Float> colorTemp = new HashMap<>();
                        String stationPlace;
                        Float temp;

                        for(int i = 0; i < states.length; i++) {
                            stationPlace = Utils.findNearestLocation(townList.get(place), stationList);
                            if (stationPlace != null && states[i].equals(stationPlace.split(", ")[1])) {
                                stateID = i;
                            }
                        }

                        if (observations[stateID] != null) {
                            for (Tree obsStation : observations[stateID].children) {
                                stationName = obsStation.properties.get("description");
                                List<Tree> info = obsStation.children.get(0).children.get(0).children;

                                for (Tree m : info) {
                                    if (m.properties.get("type").equals("air_temperature")) {
                                        colorTemp.put(stationName.split(",")[0], Float.valueOf(m.value));
                                    }
                                }
                            }
                        }
                        for(String station: stationList.keySet()) {
                            stationData = stationList.get(station).split(",");
                            int[] stationCoordinates = Utils.getStationImageCoordinatesFromLocation(
                                    width, height, zoomRange[zoomLevel - 1], radarData, stationData);
                            if (colorTemp.containsKey(station.split(", ")[0])) {
                                temp = colorTemp.get(station.split(", ")[0]);
                                Utils.drawTempIcon(temp, bitmap, stationCoordinates[0], stationCoordinates[1]);
                            }
                        }
                        TextView radarText = (TextView) findViewById(R.id.radarTitle);
                        String radarDescription = radarPlace2.split(",")[0] + " radar, " + String.valueOf(zoomRange[zoomLevel-1]) + "km:";
                        radarText.setText(radarDescription);
                        ImageView imageView = (ImageView) findViewById(R.id.radarImage);
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        findViewById(R.id.map_controls).setVisibility(View.VISIBLE);
                    } else if (invalidate) {
                        TextView radarText = (TextView) findViewById(R.id.radarTitle);
                        radarText.setText("Radar not available");
                        ImageView imageView = (ImageView) findViewById(R.id.radarImage);
                        imageView.setVisibility(View.GONE);
                        findViewById(R.id.map_controls).setVisibility(View.GONE);
                    } else {
                        zoomLevel = oldZoomLevel;
                    }
                    radarLoaded = true;
                }
            });

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView radarText = (TextView) findViewById(R.id.radarTitle);
                    radarText.setText("No radars nearby");
                    ImageView imageView = (ImageView) findViewById(R.id.radarImage);
                    imageView.setVisibility(View.GONE);
                    findViewById(R.id.map_controls).setVisibility(View.GONE);
                    radarLoaded = true;
                }});
        }
    }

    //Zoom in or out of radar
    public void zoom(View v) {
        if (!radarLoaded) {
            return;
        }
        final int oldZoomLevel = zoomLevel;
        int clickedId = v.getId();
        if (clickedId == R.id.zoom_in) {
            if (zoomLevel < 4) {
                zoomLevel++;
            }
        } else if (clickedId == R.id.zoom_out) {
            if (zoomLevel > 1) {
                zoomLevel--;
            }
        }
        if (zoomLevel == oldZoomLevel) {
            return;
        }
        findViewById(R.id.mapProgress).setVisibility(View.VISIBLE);
        Thread t = new Thread() {
            @Override
            public void run() {
                loadRadar(currentPlace, false, oldZoomLevel);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.mapProgress).setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        t.start();
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name) {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    //Read index file and store information in a dictionary for easy access
    public Map<String, String> getList(String filename, int columnCount) {
        byte[] buffer = new byte[4096];
        try {
            BufferedInputStream inputStream = new BufferedInputStream(this.getAssets().open(filename));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int bytes;
            while ((bytes = inputStream.read(buffer, 0, buffer.length)) > 0) {
                outputStream.write(buffer, 0, bytes);
            }
            String s = outputStream.toString();
            Map<String, String> list = new HashMap<>();
            String[] lines = s.split("\\n");
            String[] parts;
            for (String line:lines) {
                parts = line.split(",");
                if (columnCount == 4) {
                    list.put(parts[0] + ", " + parts[1], parts[2] + "," + parts[3]);
                } else if (columnCount == 5) {
                    list.put(parts[1] + ", " + parts[2] + ", " + parts[0], parts[3] + "," + parts[4]);
                }
            }
            outputStream.close();
            inputStream.close();
            return list;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //Get weather data for a location
    public void getData(final String place) {
        if (place == null) {
            return;
        }

        //Read local index if not read yet
        if (townList == null || townList.size() == 0) {
            townList = getList("townList.txt", 5);
            stationList = getList("stations.txt", 4);
            forecastList = getList("forecastList.txt", 4);
            radarList = getList("radarList.txt", 5);
            places = townList.keySet().toArray(new String[0]);
        }

        //Invalid place chosen - Not in the town list index
        if (!townList.containsKey(place)) {
            if (toast != null) {
                toast.cancel();
            }
            toast = Toast.makeText(this, R.string.invalid_place, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        currentPlace = place;

        this.invalidateOptionsMenu();
        ((USwipeRefreshLayout) findViewById(R.id.refresh)).setRefreshing(true);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("location", currentPlace);
        editor.apply();


        //Find nearest BOM observation station and forecast place
        final String obsStation = Utils.findNearestLocation(townList.get(place), stationList);
        final String forecastPlace = Utils.findNearestLocation(townList.get(place), forecastList);
        int obsStateID = -1, fStateID = -1;


        //Find the state of the place
        for (int i = 0; i < states.length; i++) {
            if (obsStation != null && states[i].equals(obsStation.split(", ")[1])) {
                obsStateID = i;
            }
            if (forecastPlace != null && states[i].equals(forecastPlace.split(", ")[1])) {
                fStateID = i;
            }
        }

        final int obsStateID2 = obsStateID;
        final int fStateID2 = fStateID;
        loaded = false;
        final TextView textView = (TextView) findViewById(R.id.textView);

        //Download weather in background
        Thread t = new Thread() {
            public void run () {
                String s;
                int obsDist2 = 0, fDist2;
                String issueTime = null;
                Observation observation = new Observation();
                String title2 = "";
                StringBuilder sb = new StringBuilder();
                String lastModified;
                if (obsStateID2 != -1) {
                    final String filename = "ID" + letters[obsStateID2] + "60920.xml"; //Weather observations
                    obsDist2 = Utils.distance(townList.get(place), stationList.get(obsStation));
                    lastModified = ftpReader.getDateModified(fwoRoot, filename);
                    if (lastModified == null) {
                        lastModified = lastUpdated[obsStateID2];
                    }
                    if ((observations[obsStateID2] == null || lastUpdated[obsStateID2] == null
                            || !lastUpdated[obsStateID2].equals(lastModified))) {
                        s = ftpReader.readText(fwoRoot, filename);
                        if (s == null || s.isEmpty()) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    String errorMessage = "Cannot get weather data, please check if internet is enabled";
                                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    ((USwipeRefreshLayout) findViewById(R.id.refresh)).setRefreshing(false);
                                    findViewById(R.id.relative).setVisibility(View.VISIBLE);
                                }
                            });
                            loaded = true;
                            return;
                        }
                        observations[obsStateID2] = XMLParser.parseXML(s).children.get(1);
                        lastUpdated[obsStateID2] = ftpReader.getDateModified(fwoRoot, filename);
                    }
                    zoomLevel = 2;
                    loadRadar(currentPlace, true, 2);
                    String place2 = obsStation.split(", ")[0];
                    for (Tree station : observations[obsStateID2].children) {
                        String stationName = station.properties.get("description");
                        if (stationName.equals(place2)) {
                            //Place found in weather data
                            sb.delete(0, sb.length());
                            title2 = stationName.split(",")[0];
                            Tree data = station.children.get(0);
                            String[] date = data.properties.get("time-local").split("T");
                            String time = date[1].substring(0, 5);
                            String[] day = date[0].split("-");
                            issueTime = time + " " + day[2] + "/" + day[1] + "/" + day[0];
                            observation = new Observation(data.children.get(0).children);
                            sb.append(observation.humidity);
                            sb.append(observation.wind);
                            sb.append("\n");
                            sb.append(observation.rainfall);
                        }
                    }
                    if (issueTime != null) {
                        sb.append("Last update: ").append(issueTime);
                    }
                } else {
                    title2 = "No observations nearby";
                }
                StringBuilder sb2 = new StringBuilder();
                forecasts.clear();
            //Forecasts
            if (fStateID2 != -1) {
                final String filename2 = "ID" + letters[fStateID2] + forecastIDs[fStateID2] + ".xml";
                lastModified = ftpReader.getDateModified(fwoRoot, filename2);
                if (lastModified == null) {
                    lastModified = lastUpdated[fStateID2];
                }
                if (forecastData[fStateID2] == null || lastUpdated2[fStateID2] == null || lastUpdated2[fStateID2].equals(lastModified)) {
                    s = ftpReader.readText(fwoRoot, filename2);
                    if (s == null || s.isEmpty()) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "Cannot get weather data, please check if internet is enabled", Toast.LENGTH_SHORT).show();
                                findViewById(R.id.progressBar).setVisibility(View.GONE);
                                ((USwipeRefreshLayout) findViewById(R.id.refresh)).setRefreshing(false);
                                findViewById(R.id.relative).setVisibility(View.VISIBLE);
                            }
                        });
                        loaded = true;
                        return;
                    }
                    forecastData[fStateID2] = XMLParser.parseXML(s);
                    lastUpdated2[fStateID2] = ftpReader.getDateModified(fwoRoot, filename2);
                }
                fDist2 = Utils.distance(townList.get(place), forecastList.get(forecastPlace));
                String[] daysArray = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                Tree forecastInfo = forecastData[fStateID2].children.get(1);
                String time = forecastData[fStateID2].children.get(0).children.get(3).value;
                Date date;
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", Locale.ENGLISH).parse(time);
                } catch (Exception e) {
                    e.printStackTrace();
                    date = new Date();
                }
                Calendar c = Calendar.getInstance();
                c.setTime(date);

                int j;

                for (Tree area : forecastInfo.children) {
                    String areaName = area.properties.get("description");
                    if (areaName.equals(forecastPlace.split(", ")[0])) {
                        if (area.hasChildren) {
                            forecasts.clear();
                            sb2.delete(0, sb2.length());
                            String strDist = "";
                            if (fDist2 > 5 && !areaName.equals(place.split(", ")[0])) {
                                strDist = " (" + fDist2 + "km away)";
                            }
                            sb2.append(areaName).append(" forecast").append(strDist).append(":");
                            j = 0;
                            for (Tree m : area.children) {
                                Forecast forecast = new Forecast();
                                Calendar init = Calendar.getInstance();
                                if (issueTime != null) {
                                    try {
                                        init.setTime(new SimpleDateFormat("hh:mm dd/MM/yyyy", Locale.ENGLISH).parse(issueTime));
                                    }catch(ParseException e) {issueTime = null;}
                                }
                                init.add(Calendar.DATE, -j);
                                boolean diffDates = c.get(Calendar.DAY_OF_MONTH) != init.get(Calendar.DAY_OF_MONTH);
                                if (diffDates) {
                                    if (c.before(init)) {
                                        j++;
                                        continue;
                                    }
                                    forecast.day = daysArray[(c.get(Calendar.DAY_OF_WEEK) + j - 1) % 7];
                                } else {
                                    forecast.day = "Today";
                                }
                                forecast.rainRange = "";
                                forecast.rainProbability = "";
                                int hour = 0;
                                if (issueTime != null) {
                                    hour = Integer.parseInt(issueTime.split(":")[0]);
                                }
                                //Access properties
                                for (Tree k : m.children) {
                                    switch (k.properties.get("type")) {
                                        case "precis":
                                            forecast.description = k.value;
                                            break;
                                        case "precipitation_range":
                                            if (!k.value.equals("0 mm")) {
                                                forecast.rainRange = k.value + " rain";
                                            }
                                            break;
                                        case "probability_of_precipitation":
                                            if (!k.value.equals("0%")) {
                                                forecast.rainProbability = k.value;
                                            }
                                            break;
                                        case "air_temperature_minimum":
                                            if ((observation.currentTemp.isEmpty() || !observation.minTemp.equals("")) && diffDates) {
                                                forecast.min = k.value;
                                            } else if (issueTime != null && hour < 7) {
                                                observation.minTemp = k.value;
                                                forecast.min = k.value;
                                            }
                                            break;
                                        case "air_temperature_maximum":
                                            if ((observation.currentTemp.isEmpty() || !observation.maxTemp.equals("")) && diffDates) {
                                                forecast.max = k.value;
                                            } else if (
                                                    (
                                                        observation.currentTemp.isEmpty() ||
                                                        Double.parseDouble(observation.currentTemp) < Double.parseDouble(k.value) ||
                                                        hour < 6
                                                    ) && issueTime != null && hour < 15
                                            ) {
                                                observation.maxTemp = k.value;
                                                forecast.max = k.value;
                                            }
                                            break;
                                        case "forecast_icon_code":
                                            if (!k.value.equals("##")) {
                                                forecast.forecastIcon = Integer.parseInt(k.value);
                                            }
                                    }
                                }
                                forecasts.add(forecast);
                                j++;
                            }
                        }
                    }
                }
                sb2.append(String.format(Locale.getDefault(), "\nLast updated at %1$tH:%1$tM %1$te/%1$tm/%1$tY", date));
            } else {
                sb2.append("No forecasts nearby");
            }
                final String
                    temp = observation.currentTemp,
                    maxTemp = observation.maxTemp,
                    appTemp = observation.apparentTemp,
                    minTemp=observation.minTemp,
                    title = title2;
                final String s2 = sb.toString();
                final String s3 = sb2.toString();
                final int obsDist = obsDist2;
                //All weather information ready - Add it to GUI
                runOnUiThread(new Runnable() {
                    public void run() {
                        findViewById(R.id.relative).setVisibility(View.VISIBLE);
                        ((USwipeRefreshLayout) findViewById(R.id.refresh)).setRefreshing(false);
                        Drawable[] gd = new Drawable[2];
                        TransitionDrawable td = null;
                        GridView grid = ((GridView)findViewById(R.id.forecasts));
                        if (!title.isEmpty()) {
                            findViewById(R.id.obs).setVisibility(View.VISIBLE);
                            ((TextView) findViewById(R.id.title)).setText(title);
                            if (obsDist > 5 && !title.equals(place.split(", ")[0])) {
                                String distanceStr = obsDist + "km away from " + place.split(",")[0];
                                ((TextView) findViewById(R.id.dist)).setText(distanceStr);
                                findViewById(R.id.dist).setVisibility(View.VISIBLE);
                            } else {
                                findViewById(R.id.dist).setVisibility(View.GONE);
                            }
                            textView.setText(s2);
                            if (!temp.isEmpty()) {
                                findViewById(R.id.tempLayout).setVisibility(View.VISIBLE);
                                ((TextView) findViewById(R.id.temp)).setText(String.format("%s°C", temp));
                                ((TextView) findViewById(R.id.minTemp)).setText(String.format("Min: %s°C", minTemp));
                                ((TextView) findViewById(R.id.maxTemp)).setText(String.format("Max: %s°C", maxTemp));
                                gd[0] = new ColorDrawable(Color.TRANSPARENT);
                                gd[1] = new GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    new int[]{
                                        Utils.tempColor(Float.parseFloat(temp) + 1f),
                                        Utils.tempColor(Float.parseFloat(temp)),
                                        Utils.tempColor(Float.parseFloat(temp) - 1f)
                                    }
                                );
                                if (currentTemp != null) {
                                    gd[0] = new GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM,
                                        new int[]{
                                            Utils.tempColor(Float.parseFloat(currentTemp) + 1f),
                                            Utils.tempColor(Float.parseFloat(currentTemp)),
                                            Utils.tempColor(Float.parseFloat(currentTemp) - 1f)
                                        }
                                    );
                                }
                                td = new TransitionDrawable(gd);
                                td.startTransition(300);
                                currentTemp = temp;

                                ((ForecastAdapter) grid.getAdapter()).setInvertedDisplay(Float.parseFloat(currentTemp) < 0);

                                int[] viewIds = {
                                    R.id.temp,
                                    R.id.title,
                                    R.id.minTemp,
                                    R.id.maxTemp,
                                    R.id.dist,
                                    R.id.appTemp,
                                    R.id.radarTitle,
                                    R.id.textView,
                                    R.id.forecast
                                };
                                int whiteColor = Color.argb(200, 255, 255, 255);
                                ((ImageView)findViewById(R.id.zoom_in)).setColorFilter(whiteColor);
                                ((ImageView)findViewById(R.id.zoom_out)).setColorFilter(whiteColor);
                                for(int viewId:viewIds) {
                                    ((TextView) findViewById(viewId)).setTextColor(whiteColor);
                                }
                                if (Float.parseFloat(temp) < 0) {
                                    int blackColor = Color.argb(200, 0, 0, 0);
                                    ((ImageView)findViewById(R.id.zoom_in)).setColorFilter(blackColor);
                                    ((ImageView)findViewById(R.id.zoom_out)).setColorFilter(blackColor);
                                    for(int viewId:viewIds) {
                                        ((TextView) findViewById(viewId)).setTextColor(Color.BLACK);
                                    }
                                }
                            } else {
                                findViewById(R.id.tempLayout).setVisibility(View.GONE);
                            }

                            if (appTemp != null && !appTemp.isEmpty()) {
                                findViewById(R.id.appTemp).setVisibility(View.VISIBLE);
                                ((TextView) findViewById(R.id.appTemp)).setText(String.format("%s", appTemp));
                            } else {
                                findViewById(R.id.appTemp).setVisibility(View.GONE);
                            }
                        } else {
                            findViewById(R.id.obs).setVisibility(View.GONE);
                        }

                        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
                        if (Build.VERSION.SDK_INT >= 18) {
                            viewPager.setBackground(td);
                        } else if (Build.VERSION.SDK_INT >= 16) {
                            viewPager.setBackground(gd[1]);
                        } else {
                            viewPager.setBackgroundDrawable(gd[1]);
                        }

                        ((ForecastAdapter) grid.getAdapter()).notifyDataSetChanged();
                        ViewGroup.LayoutParams params = grid.getLayoutParams();
                        final float scale = MainActivity.this.getResources().getDisplayMetrics().density;
                        params.width = (int) Math.ceil(forecasts.size() * 90 * scale);
                        grid.setNumColumns(forecasts.size());
                        grid.setLayoutParams(params);
                        ((TextView) findViewById(R.id.forecast)).setText(s3);
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        loaded = true;
                    }
                });
            }
        };
        t.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        //Add or remove from bookmarks
        if (id == R.id.star) {
            if (toast != null) {
                toast.cancel();
            }

            if (!favorites.contains(currentPlace)) {
                favorites.add(currentPlace);
                toast = Toast.makeText(MainActivity.this, "Added to favorites", Toast.LENGTH_SHORT);
                item.setIcon(R.drawable.ic_star_on);
            } else {
                favorites.remove(currentPlace);
                toast = Toast.makeText(MainActivity.this, "Removed from favorites", Toast.LENGTH_SHORT);
                item.setIcon(R.drawable.ic_star_off);
            }
            
            toast.show();
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("Bookmarks", Utils.joinArrayList(favorites, "\n"));
            editor.apply();
            listAdapter.notifyDataSetChanged();
        } else if (id == R.id.search) {
            showSearch();
        } else if (id == R.id.about) {
            Utils.showInfoDialog(this);
        } else if (id == R.id.radarMenu) {
            showRadar(null);
        }
        return super.onOptionsItemSelected(item);
    }

    //Show radar
    public void showRadar(View v) {
        ((ViewPager)findViewById(R.id.pager)).setCurrentItem(1);
    }

    //User pressed back button, so close bookmarks, go to previous place or close the app
    @Override
    public void onBackPressed() {
        View v = findViewById(R.id.navigation_drawer);
        if (mDrawerLayout.isDrawerOpen(v)) {
            mDrawerLayout.closeDrawer(v); //Bookmarks open, so close that
            return;
        }
        if (!history.isEmpty()) {
            if (loaded) {
                getData(history.pop()); //Go back if user pressed back when the weather was loaded
                return;
            }
        }

        // Otherwise defer to system default behavior.
        super.onBackPressed(); // Close application if user pressed back before the weather loads
    }

    //Show the search field that allows the user to enter a location
    public void showSearch() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        if (townList == null) {
            townList = new HashMap<>();
            places = new String[0];
        }
        final AutoCompleteTextView location = new AutoCompleteTextView(this);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, places);
        location.setAdapter(adapter);
        location.setHint("Enter a location");
        location.setInputType(InputType.TYPE_CLASS_TEXT);
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                location.setText("");
            }
        });

        alertDialogBuilder.setView(location);
        final AlertDialog alertDialog = alertDialogBuilder.create();
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.TOP;
            wlp.y = 100;
            window.setAttributes(wlp);
        }
        alertDialog.show();
        location.setOnEditorActionListener( new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    //User pressed enter
                    if (adapter.getCount() > 0) {
                        getData(adapter.getItem(0));
                        alertDialog.dismiss();
                    } else {
                        if (toast != null) {
                            toast.cancel();
                        }
                        toast = Toast.makeText(MainActivity.this, "Place not found", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                return false;
            }
        });

        //Place selected
        location.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View view2 = getCurrentFocus();
                if (view2 != null) {
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputManager != null) {
                        inputManager.hideSoftInputFromWindow(view2.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
                String place = location.getText().toString();
                if (currentPlace != null && !currentPlace.equals(place)) {
                    history.push(currentPlace);
                }
                getData(place); //Load weather data for place
                alertDialog.dismiss();
            }
        });
    }

    //User long pressed on one of the bookmarks. Context menu allows user to delete bookmarks
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) info;
        String place = listAdapter.getItem(menuInfo.position);
        menu.add(Menu.NONE, 0, 0, "Delete " + place);
        menu.getItem(0).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 0:
                        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                        favorites.remove(info.position);
                        MainActivity.this.invalidateOptionsMenu();
                        listAdapter.notifyDataSetChanged();
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                        editor.putString("Bookmarks", Utils.joinArrayList(favorites, "\n"));
                        editor.apply();
                        if (toast != null) {
                            toast.cancel();
                        }
                        toast = Toast.makeText(MainActivity.this,"Place removed from bookmarks", Toast.LENGTH_SHORT);
                        toast.show();
                        return true;
                    default:
                        return false;
                }
            }
        });
        super.onCreateContextMenu(menu, v, info);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mDrawerToggle.onConfigurationChanged(config);
    }

    //Show whether a place is bookmarked or not
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int starId;
        if (currentPlace != null) {
            if (favorites.contains(currentPlace)) {
                starId = R.drawable.ic_star_on;
            } else {
                starId = R.drawable.ic_star_off;
            }
            menu.findItem(R.id.star).setVisible(true);

            Drawable starIcon;
            if (Build.VERSION.SDK_INT < 22) {
                starIcon = this.getResources().getDrawable(starId);
            } else {
                starIcon = this.getResources().getDrawable(starId, this.getTheme());
            }
            menu.findItem(R.id.star).setIcon(starIcon);
        }
        return super.onPrepareOptionsMenu(menu);
    }
}
