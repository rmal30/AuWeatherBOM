package com.rmal30.auweatherbom;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;

import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class MainActivity extends AppCompatActivity {

    //Swipe refresh layout for reloading weather. Only triggers when the user slides down
    public static class USwipeRefreshLayout extends SwipeRefreshLayout {
        private int touchSlop;
        private float prevX;
        private boolean declined;

        public USwipeRefreshLayout( Context context, AttributeSet attrs ) {
            super( context, attrs );
            touchSlop = ViewConfiguration.get( context ).getScaledTouchSlop();
        }

        @Override
        public boolean onInterceptTouchEvent( MotionEvent event ) {
            switch( event.getAction() ){
                case MotionEvent.ACTION_DOWN:
                    prevX = MotionEvent.obtain( event ).getX();
                    declined = false; // New action
                    break;

                case MotionEvent.ACTION_MOVE:
                    final float eventX = event.getX();
                    float xDiff = Math.abs( eventX - prevX );
                    if( declined || xDiff > touchSlop ){
                        declined = true; // Memorize
                        return false;
                    }
                    break;
            }
            return super.onInterceptTouchEvent( event );
        }
    }

    //Object used to store the weather data
    private class Tree{
        String type, value;
        ArrayList<Tree> children;
        boolean hasChildren;
        HashMap<String, String> properties;
        private Tree(String type, String value, HashMap<String,String> properties, boolean hasChildren){
            this.type = type;
            this.properties = properties;
            this.hasChildren = hasChildren;
            if(this.hasChildren) {
                this.children = new ArrayList<>();
            }else{
                this.value = value;
            }
        }
        private Tree(){

        }
        /*
        public ArrayList<Tree> getChildrenOfType(String type){
            ArrayList<Tree> childrenOfType =  new ArrayList<>();
            for(Tree child:children){
                if(child.type.equals(type)){
                    childrenOfType.add(child);
                }
            }
            return childrenOfType;
        }
        public ArrayList<Tree> getChildrenWithProperty(String property, String value){
            ArrayList<Tree> childrenWithProperty =  new ArrayList<>();
            for(Tree child:children){
                if(child.properties.get(property).equals(value)){
                    childrenWithProperty.add(child);
                }
            }
            return childrenWithProperty;
        }
        */
    }

    //Forecast object
    public class Forecast{
        String day, max,min,rainProbability, description, rainRange;
        int forecastIcon;
        public Forecast(){

        }
    }

    //Adapter that loads the forecast view
    public class ForecastAdapter extends BaseAdapter{
        private Context ctx;
        private ArrayList<Forecast> forecasts;

        public ForecastAdapter(Context ctx, ArrayList<Forecast> forecasts){
            this.ctx = ctx;
            this.forecasts = forecasts;
        }
        public View getView(int i, View v, ViewGroup parent){
            if(v==null){
                final LayoutInflater li = LayoutInflater.from(this.ctx);
                v = li.inflate(R.layout.forecast_card, null);
            }
            if(i>=forecasts.size()){
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
            if(Build.VERSION.SDK_INT<23) {
                //noinspection deprecation
                imageView.setImageDrawable(ctx.getResources().getDrawable(weatherIcon));
            }else{
                imageView.setImageDrawable(ctx.getResources().getDrawable(weatherIcon, ctx.getTheme()));
            }
            final TextView day = (TextView) v.findViewById(R.id.day);
            final TextView min = (TextView) v.findViewById(R.id.min);
            final TextView max = (TextView) v.findViewById(R.id.max);
            final TextView prob = (TextView) v.findViewById(R.id.rainprob);
            final LinearLayout probLayout = (LinearLayout) v.findViewById(R.id.rainlayout);
            day.setText(forecast.day);
            if(forecast.min==null){
                min.setVisibility(View.GONE);
            }else{
                min.setText(forecast.min+"°");
                min.setVisibility(View.VISIBLE);
            }

            if(forecast.max==null){
                max.setVisibility(View.INVISIBLE);
            }else{
                max.setText(forecast.max+"°");
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
            return 0;
        }
        public View getItem(int position){
            return null;
        }
    }

    //Pager adapter to switch between radar and weather
    public class CustomPagerAdapter extends PagerAdapter{
        private Context mContext;

        public CustomPagerAdapter(Context context) {
            mContext = context;
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
            return findViewById(resId);
        }

        @Override
        public void destroyItem(ViewGroup arg0, int arg1, Object arg2) {
            arg0.removeView((View) arg2);
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    //Old and slower way of parsing XML
    public ArrayList<Tree> parseXMLOld(String xmlData, int start, int end){
        char c, c2;
        if(start == 0){
            do{start++;}while(xmlData.charAt(start)!='>');
        }
        ArrayList<Tree> XMLNodes = new ArrayList<>();
        Tree XMLNode;
        HashMap<String, String> properties;
        String type;
        int begin, depth, j, endTag, center;
        boolean open = false, hasChildren;
        while(start<end){
            do{start++;}while(start<end && xmlData.charAt(start)!='<');
            if(start==end){
                return XMLNodes;
            }
            begin = start+1;
            do {
                c = xmlData.charAt(start);
                start++;
            }while(c!=' ' && c!='>');
            type = xmlData.substring(begin, start-1);
            if(c == ' '){
                properties = new HashMap<>();
                begin = start;
                center = start;
                while(c!='>') {
                    c = xmlData.charAt(start);
                    if (c == '"') {
                        open = !open;
                    }else if(c == '='){
                        center = start;
                    }else if (!open && (c == ' ' || c == '>')) {
                        properties.put(xmlData.substring(begin, center), xmlData.substring(center+2, start-1));
                        begin = start + 1;
                    }
                    start++;
                }
            }else{
                properties = null;
            }
            if(xmlData.charAt(start-2)!='/'){
                depth = 1; j = start + 1;
                c2 = xmlData.charAt(start);
                hasChildren = false;
                endTag = start;
                while (j < end && (depth > 0 || open)) {
                    c = c2;
                    c2 = xmlData.charAt(j);
                    if(c2 == '>'){
                        open = false;
                    }else if (c == '<') {
                        open = true;
                        if (c2 == '/') {
                            depth--;
                            endTag = j - 1;
                        } else {
                            depth++;
                            hasChildren = true;
                        }
                    }
                    j++;
                }
                if (hasChildren) {
                    XMLNode = new Tree(type, null, properties, true);
                    XMLNode.children = parseXMLOld(xmlData, start, endTag);
                } else {
                    XMLNode = new Tree(type, xmlData.substring(start, endTag), properties, false);
                }
                start = j + 1;
            }else{
                XMLNode = new Tree(type, null, properties, false);
            }
           XMLNodes.add(XMLNode);
        }
        return XMLNodes;
    }

    //XML parser
    public Tree parseXML(String xmlData){
        if(xmlData==null){
            return null;
        }
        Tree node = new Tree();
        node.hasChildren = false;
        Stack<Tree> stack = new Stack<>();
        stack.add(node);
        char c;
        int i=0;
        do{i++;}while(xmlData.charAt(i)!='>');
        do{i++;}while(xmlData.charAt(i)!='<');
        while(!stack.isEmpty()) {
            node = stack.peek();
            if(node.type != null) {
                if(xmlData.charAt(i + 1) == '/') {
                    do {
                        i++;
                    } while (xmlData.charAt(i) != '>');
                    stack.pop();
                    if(!stack.isEmpty()) {
                        do {
                            i++;
                        } while (xmlData.charAt(i) != '<');
                    }
                }else {
                    node.hasChildren = true;
                    Tree child = new Tree();
                    child.hasChildren = false;
                    if(node.children == null) {
                        node.children = new ArrayList<Tree>();
                    }
                    node.children.add(child);
                    stack.push(child);
                }
            } else {
                int begin = i, center;
                boolean open;
                do {
                    c = xmlData.charAt(i);
                    i++;
                } while (c != ' ' && c != '>');
                node.type = xmlData.substring(begin, i - 1);
                if (c == ' ') {
                    node.properties = new HashMap<>();
                    begin = i;
                    center = i;
                    open = false;
                    while (c != '>') {
                        c = xmlData.charAt(i);
                        if(c == '"') {
                            open = !open;
                        } else if (c == '=') {
                            center = i;
                        } else if (!open && (c == ' ' || c == '>')) {
                            node.properties.put(xmlData.substring(begin, center), xmlData.substring(center + 2, i - 1));
                            begin = i + 1;
                        }
                        i++;
                    }
                } else {
                    node.properties = null;
                }
                if (xmlData.charAt(i - 2) == '/') {
                    stack.pop();
                    do{i++;}while(xmlData.charAt(i)!='<');
                }
                begin=i;
                while (xmlData.charAt(i)!='<') {
                    i++;
                }
                if(xmlData.charAt(i+1)=='/'){
                    node.value = xmlData.substring(begin, i);
                }

            }
        }
        return node;
    }

    //Print out the object data as xml, used to verify that xml is read properly
    public String printXML(ArrayList<Tree> tree){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(Tree t:tree){
            sb.append("Type: ");
            sb.append(t.type);
            sb.append("\n");
            sb.append("Properties:");
            if(t.properties!=null) {
                for (String key : t.properties.keySet()) {
                    sb.append(key);
                    sb.append(" ");
                    sb.append(t.properties.get(key));
                    sb.append("\n");
                }
            }
            sb.append("Value: {");
            if(t.hasChildren){
                sb.append(printXML(t.children));
            }else{
                sb.append(t.value);
            }
            sb.append("}");
            sb.append("\n\n");
        }
        sb.append("]");
        return sb.toString();
    }

    String root = "ftp://ftp.bom.gov.au/anon/gen/"; //Bureau of Meteorology FTP site
    final String fwoRoot = root+"fwo/"; //Weather observations and forecasts
    final String radarRoot = root+"radar/"; //Radar
    final String[] states = {"NSW", "VIC", "QLD", "WA", "SA", "TAS", "NT"}; //States of Australia
    final String[] forecastIDs = {"11060", "10753", "11295", "14199", "10044", "16710", "10207"};
    final String[] letters = {"N", "V", "Q", "W", "S", "T", "D"}; //Used by BOM
    boolean loaded = true;
    boolean radarLoaded = false;
    Tree[] observations = {null, null, null, null, null, null, null};
    Tree[] forecastData = {null, null, null, null, null, null, null};
    long now = System.currentTimeMillis();
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
    HashMap<String, String> townList, stationList, forecastList, radarList;
    String currentPlace, currentTemp;
    ArrayAdapter<String> listAdapter;
    FTPClient ftp;

    //Used for testing purposes
    public void saveStationData(){
        ArrayList<String> places = new ArrayList<>();
        String file = "";
        String place;
        for(int i=0; i<states.length; i++){
            //Tree observations = parseXML(new String(readFile(fwoRoot, "ID"+letters[i]+"60920.xml"))).children.get(1);
            Tree stateForecasts = parseXML(readText(fwoRoot, "ID"+letters[i]+forecastIDs[i]+".xml")).children.get(1);
            /*
            for(Tree station:observations.children){
                if(station.hasChildren) {
                    place = station.properties.get("description");
                    if(!places.contains(place)){
                        places.add(place);
                        file+=place+","+states[i]+","+station.properties.get("lat")+","+station.properties.get("lon")+"\n";
                    }
                }
            }
            */

            for(Tree area:stateForecasts.children){
                if(area.hasChildren) {
                    place =  area.properties.get("description");
                    if(!places.contains(place)){
                        places.add(place);
                        file+=place+","+states[i]+"\n";
                    }
                }
            }
        }
        //((EditText) findViewById(R.id.file)).setText(file);
    }
    public void mergeData(){
        byte[] buffer = new byte[4096];
        try {
            BufferedInputStream bis = new BufferedInputStream(this.getAssets().open("listAll.txt"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytes;
            while ((bytes = bis.read(buffer, 0, buffer.length)) > 0) {
                baos.write(buffer, 0, bytes);
            }
            String s = baos.toString();
            HashMap<String, String> stationData = new HashMap<>();
            String[] lines = s.split("\\n");
            String[] parts;
            for (int i = 0; i < lines.length; i++) {
                parts = lines[i].split(",");
                stationData.put(parts[0]+","+parts[1],parts[2]+","+parts[3]);
            }
            bis = new BufferedInputStream(this.getAssets().open("forecastList.txt"));
            baos = new ByteArrayOutputStream();
            while ((bytes = bis.read(buffer, 0, buffer.length)) > 0) {
                baos.write(buffer, 0, bytes);
            }
            s = baos.toString();
            lines = s.split("\\n");
            String file="";
            for (int i = 0; i < lines.length; i++) {
                file+=lines[i]+","+stationData.get(lines[i])+"\n";
            }
            baos.close();
            bis.close();
            //((EditText) findViewById(R.id.file)).setText(file);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //Startup
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        CustomPagerAdapter mCustomPagerAdapter = new CustomPagerAdapter(this);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCustomPagerAdapter);
        mViewPager.setCurrentItem(0);


        //Get all indexed text files that contain location information
        stationList = getList("stations.txt", 4);
        forecastList = getList("forecastList.txt", 4);
        townList = getList("townList.txt", 5);
        radarList = getList("radarList.txt", 5);
        places = townList.keySet().toArray(new String[townList.size()]);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String place = sp.getString("location", null); //Get back previous location visited
        String bookmarks = sp.getString("Bookmarks", ""); //Get weather bookmarks

        //Read favorites in
        if(bookmarks.equals("")){
            favorites = new ArrayList<>();
        }else{
            favorites = new ArrayList<>(Arrays.asList(bookmarks.split("\\n")));
        }


        if(townList!=null && townList.containsKey(place)) {
            if(currentPlace!=null && !currentPlace.equals(place)){
                history.push(currentPlace);
            }
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            getData(place); //Load stored place
        }else{
            //Bad data, so reset all stored data
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.remove("Bookmarks");
            editor.remove("location");
            editor.apply();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final LayoutInflater li = LayoutInflater.from(this);
        View v = li.inflate(R.layout.drawer_main, null); //Drawer with bookmarks
        setupDrawer();
        ((RelativeLayout)findViewById(R.id.navigation_drawer)).addView(v);
        ListView mDrawerList = (ListView) v.findViewById(R.id.left_drawer);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_selectable_list_item, favorites);
        mDrawerList.setAdapter(listAdapter);
        registerForContextMenu(mDrawerList);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String place = favorites.get(position);
                if(currentPlace!=null && !currentPlace.equals(place)){
                    history.push(currentPlace);
                }
                if(loaded) {
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
        GridView grid = (GridView) findViewById(R.id.forecasts);
        ForecastAdapter forecastAdapter = new ForecastAdapter(this, forecasts);
        grid.setAdapter(forecastAdapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Forecast forecast = forecasts.get(position);
                if(toast!=null){toast.cancel();}
                toast = Toast.makeText(MainActivity.this, forecast.day+": "+forecast.description+" "+forecast.rainRange, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    //Load radar
    public void loadRadar(String place, final boolean invalidate, final int oldZoomLevel){
        radarLoaded = false;
        String radarPlace = findNearestLocation(townList.get(place), radarList);
        final int[] zoomRange = {512, 256, 128, 64};
        setupFTP();
        try {
            if (ftp == null || !ftp.sendNoOp()) {
                return;
            }
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
        if(radarPlace!=null) {
            String radarID = radarPlace.split(", ")[2];
            byte[] byteArray = readFile(radarRoot, "IDR" + radarID + String.valueOf(zoomLevel) +".gif");
            while(invalidate && byteArray==null && radarList.size()>0){
                radarList.remove(radarPlace);
                radarPlace = findNearestLocation(townList.get(place), radarList);
                if(radarPlace!=null) {
                    radarID = radarPlace.split(", ")[2];
                    try {
                        if (ftp == null || !ftp.sendNoOp()) {
                            return;
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        return;
                    }
                    byteArray = readFile(radarRoot, "IDR" + radarID + String.valueOf(zoomLevel) + ".gif");
                }else{
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
                        TextView radarText = (TextView) findViewById(R.id.radarTitle);
                        radarText.setText(radarPlace2.split(",")[0] + " radar, "+String.valueOf(zoomRange[zoomLevel-1])+"km:");
                        ImageView imageView = (ImageView) findViewById(R.id.radarImage);
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        findViewById(R.id.map_controls).setVisibility(View.VISIBLE);
                    } else if(invalidate){
                        TextView radarText = (TextView) findViewById(R.id.radarTitle);
                        radarText.setText("Radar not available");
                        ImageView imageView = (ImageView) findViewById(R.id.radarImage);
                        imageView.setVisibility(View.GONE);
                        findViewById(R.id.map_controls).setVisibility(View.GONE);
                    }else{
                        zoomLevel = oldZoomLevel;
                    }
                    radarLoaded = true;
                }
            });

        }else{
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
    public void zoom(View v){
        if(!radarLoaded){
            return;
        }
        final int oldZoomLevel = zoomLevel;
        //findViewById(R.id.radarImage).setVisibility(View.INVISIBLE);

        switch(v.getId()){
            case R.id.zoom_in:
                if(zoomLevel<4){
                    zoomLevel++;
                }
                break;
            case R.id.zoom_out:
                if(zoomLevel>1){
                    zoomLevel--;
                }
                break;
        }
        if(zoomLevel == oldZoomLevel){
            return;
        }
        findViewById(R.id.mapProgress).setVisibility(View.VISIBLE);
        Thread t = new Thread() {
            public void run() {
                loadRadar(currentPlace, false, oldZoomLevel);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //findViewById(R.id.radarImage).setVisibility(View.VISIBLE);
                        findViewById(R.id.mapProgress).setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        t.start();
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.app_name, R.string.app_name) {
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
    public HashMap<String, String> getList(String filename, int columnCount){
        byte[] buffer = new byte[4096];
        try {
            BufferedInputStream bis = new BufferedInputStream(this.getAssets().open(filename));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytes;
            while ((bytes = bis.read(buffer, 0, buffer.length)) > 0) {
                baos.write(buffer, 0, bytes);
            }
            String s = baos.toString();
            HashMap<String, String> list = new HashMap<>();
            String[] lines = s.split("\\n");
            String[] parts;
            for (int i = 0; i < lines.length; i++) {
                parts = lines[i].split(",");
                if(columnCount==4) {
                    list.put(parts[0] + ", " + parts[1], parts[2] + "," + parts[3]);
                }else if(columnCount==5){
                    list.put(parts[1]+", "+parts[2]+", "+parts[0], parts[3]+","+parts[4]);
                }
            }
            baos.close();
            bis.close();
            return list;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //Start accessing BOM ftp
    public void setupFTP(){
        try {
            if(ftp==null || !ftp.sendNoOp()) {
                ftp = new FTPClient();
                ftp.setBufferSize(65536);
                URL url = new URL(root);
                ftp.connect(url.getHost(), 21);
                ftp.login("anonymous", String.valueOf(Math.floor(Math.random() * 10000) + "@b.c"));
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();
            }
        } catch (Exception e) {
            ftp = null;
        }
    }

    //Download weather files from BOM
    public ByteArrayOutputStream readFileStream(String path, String filename){
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        try {
            URL url = new URL(path + filename);
            setupFTP();
            ftp.retrieveFile(url.getPath(), baos);
            if(baos.size()==0){
                return null;
            }
           return baos;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //Download file and save it as a byte array
    public byte[] readFile(String path, String filename){
        final ByteArrayOutputStream baos = readFileStream(path, filename);
        try {
            byte[] bytes = baos.toByteArray();//toString("UTF-8");
            baos.close();
            return bytes;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //Find out when BOM data files were last updated
    public String getDateModified(String path, String filename){
        setupFTP();
        try {
            return ftp.getModificationTime(new URL(fwoRoot + filename).getPath());
        }catch(Exception e){
            return null;
        }
    }

    //Download file and return the string contents of it
    public String readText(String path, String filename){
        final ByteArrayOutputStream baos = readFileStream(path, filename);
        try {
            String s = baos.toString("UTF-8");
            baos.close();
            return s;
        }catch(Exception e){
            return null;
        }
    }

    //Compute approximate distance between two coordinates. Use Pythogoras theorem as distances are short enough
    public int distance(String pos1, String pos2){
        String[] coords1 = pos1.split(",");
        String[] coords2 = pos2.split(",");
        float lat1 = Float.valueOf(coords1[0]);
        float lon1 = Float.valueOf(coords1[1]);
        float lat2 = Float.valueOf(coords2[0]);
        float lon2 = Float.valueOf(coords2[1]);
        return (int) Math.round((10000/90)*Math.sqrt((lat2-lat1)*(lat2-lat1)+(lon2-lon1)*(lon2-lon1)));
    }

    //Find the nearest location from a given position
    public String findNearestLocation(String position, HashMap<String, String> list){
        int minDist = 10000;
        int curDist;
        String nearestPlace=null;
        for(String key:list.keySet()){
            curDist = distance(position, list.get(key));
            if(curDist<minDist){
                minDist = curDist;
                nearestPlace = key;
            }
        }
        if(minDist>600){
            return null;
        }
        return nearestPlace;
    }

    //Get weather data for a location
    public void getData(final String place){
        if(place==null){
            return;
        }

        //Read local index if not read yet
        if(townList==null || townList.size()==0){
            townList = getList("townList.txt", 5);
            stationList = getList("stations.txt", 4);
            forecastList = getList("forecastList.txt", 4);
            radarList = getList("radarList.txt", 5);
            places = townList.keySet().toArray(new String[townList.size()]);
        }

        //Invalid place chosen - Not in the town list index
        if(!townList.containsKey(place)){
            if(toast!=null){toast.cancel();}
            toast = Toast.makeText(this, "Place chosen is invalid, please select another place", Toast.LENGTH_SHORT);
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
        final String obsStation = findNearestLocation(townList.get(place), stationList);
        final String forecastPlace = findNearestLocation(townList.get(place), forecastList);
        int obsStateID=-1, fStateID=-1;


        //Find the state of the place
        for(int i=0; i<states.length; i++){
            if(obsStation!=null && states[i].equals(obsStation.split(", ")[1])){
                obsStateID = i;
            }
            if(forecastPlace!=null && states[i].equals(forecastPlace.split(", ")[1])){
                fStateID = i;
            }
        }

        final int obsStateID2 = obsStateID;
        final int fStateID2 = fStateID;
        loaded = false;
        final TextView textView = (TextView) findViewById(R.id.textView);

        //Download weather in background
        Thread t = new Thread() {
            public void run (){
                String s;
                int obsDist2=0, fDist2=0;
                long now = System.currentTimeMillis();
                String issueTime = null;
                String appTemp2 = "", temp2 = "", maxTemp2 = "", minTemp2 = "";
                String title2 = "";
                StringBuilder sb = new StringBuilder();
                String lastModified;
                if(obsStateID2!=-1){
                    final String filename = "ID" + letters[obsStateID2] + "60920.xml"; //Weather observations
                    obsDist2 = distance(townList.get(place), stationList.get(obsStation));
                    lastModified = getDateModified(fwoRoot, filename);
                    if(lastModified==null){
                        lastModified = lastUpdated[obsStateID2];
                    }
                if ((observations[obsStateID2] == null || lastUpdated[obsStateID2] == null
                        || !lastUpdated[obsStateID2].equals(lastModified))) {
                    s = readText(fwoRoot, filename);
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
                    observations[obsStateID2] = parseXML(s).children.get(1);
                    lastUpdated[obsStateID2] = getDateModified(fwoRoot, filename);
                }
                zoomLevel = 2;
                loadRadar(currentPlace, true, 2);
                String units;
                String place2 = obsStation.split(", ")[0];
                for (Tree station : observations[obsStateID2].children) {
                    String stationName = station.properties.get("description");
                    //boolean wordExists2 = stationName2.toUpperCase().contains(place2.split(" ")[0]);
                    if (stationName.equals(place2)) {
                        //Place found in weather data
                        sb.delete(0, sb.length());
                        title2 = stationName.split(",")[0];
                        Tree data = station.children.get(0);
                        String[] date = data.properties.get("time-local").split("T");
                        String time = date[1].substring(0, 5);
                        String[] day = date[0].split("-");
                        issueTime = time + " " + day[2] + "/" + day[1] + "/" + day[0];
                        ArrayList<Tree> info = data.children.get(0).children;
                        appTemp2 = "";
                        temp2 = "";
                        maxTemp2 = "";
                        minTemp2 = "";
                        for (Tree m : info) {
                            units = m.properties.get("units");
                            if (units == null) {
                                units = "";
                            }
                            String line = "";
                            switch (m.properties.get("type")) {
                                case "apparent_temp":
                                    appTemp2 = "Feels like " + m.value + "°C";
                                    break;
                                case "air_temperature":
                                    temp2 = m.value;
                                    break;
                                case "rel-humidity":
                                    line = m.value + units + " humidity \n";
                                    break;
                                case "wind_dir":
                                    line = "Wind: " + m.value + " " + units;
                                    break;
                                case "wind_spd_kmh":
                                    if (!m.value.equals("0")) {
                                        line = m.value + " " + units;
                                    }
                                    break;
                                case "rainfall":
                                    if (!m.value.equals("0.0")) {
                                        line = "\n" + m.value + " " + units + " rain";
                                    }
                                    break;
                                case "maximum_air_temperature":
                                    maxTemp2 = m.value;
                                    break;
                                case "minimum_air_temperature":
                                    minTemp2 = m.value;
                                    break;
                            }
                            if (minTemp2.equals("")) {
                                minTemp2 = "--";
                            }
                            if (maxTemp2.equals("")) {
                                maxTemp2 = "--";
                            }
                            if (!line.equals("")) {
                                sb.append(line);
                            }
                        }
                    }
                }
                if (issueTime != null) {
                    sb.append("\nLast update: " + issueTime);
                }
            }else{
                title2 = "No observations nearby";
            }
                StringBuilder sb2 = new StringBuilder();
                forecasts.clear();
            //Forecasts
            if(fStateID2!=-1) {
                final String filename2 = "ID" + letters[fStateID2] + forecastIDs[fStateID2] + ".xml";
                lastModified = getDateModified(fwoRoot, filename2);
                if(lastModified==null){
                    lastModified = lastUpdated[fStateID2];
                }
                if (forecastData[fStateID2] == null || lastUpdated2[fStateID2] == null || lastUpdated2[fStateID2].equals(lastModified)) {
                    s = readText(fwoRoot, filename2);
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
                    forecastData[fStateID2] = parseXML(s);
                    lastUpdated2[fStateID2] = getDateModified(fwoRoot, filename2);
                }
                fDist2 = distance(townList.get(place), forecastList.get(forecastPlace));
                String daysArray[] = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
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
                //s+=printXML(forecasts.children);
                int j;

                for (Tree area : forecastInfo.children) {
                    String areaName = area.properties.get("description");
                    if (areaName.equals(forecastPlace.split(", ")[0])) {
                        if (area.hasChildren) {
                            forecasts.clear();
                            sb2.delete(0, sb2.length());
                            String strDist = "";
                            if(fDist2>5 && !areaName.equals(place.split(", ")[0])){
                                strDist = " ("+fDist2+"km away)";
                            }
                            sb2.append(areaName + " forecast"+strDist+":");
                            j = 0;
                            for (Tree m : area.children) {
                                Forecast forecast = new Forecast();
                                Calendar init = Calendar.getInstance();
                                if(issueTime!=null){
                                    try {
                                        init.setTime(new SimpleDateFormat("hh:mm dd/MM/yyyy", Locale.ENGLISH).parse(issueTime));
                                    }catch(ParseException e){issueTime = null;}
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
                                if(issueTime != null) {
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
                                            if ((temp2.isEmpty() || !minTemp2.equals("")) && diffDates) {
                                                forecast.min = k.value;
                                            } else if (issueTime != null && hour <7) {
                                                minTemp2 = k.value;
                                                forecast.min = k.value;
                                            }

                                        case "air_temperature_maximum":
                                            if ((temp2.isEmpty() || !maxTemp2.equals("")) && diffDates) {
                                                forecast.max = k.value;
                                            } else if ( (temp2.isEmpty() || Double.parseDouble(temp2)<Double.parseDouble(k.value) || hour < 6)&& issueTime != null && hour < 15) {
                                                maxTemp2 = k.value;
                                                forecast.max = k.value;
                                            }
                                            break;
                                        case "forecast_icon_code":
                                            if(!k.value.equals("##")) {
                                                forecast.forecastIcon = Integer.parseInt(k.value);
                                            }
                                    }
                                }
                                forecasts.add(forecast);
                                //s+=printXML(m.children);
                                j++;
                            }
                        }
                    }
                }
                sb2.append(String.format(Locale.getDefault(), "\nLast updated at %1$tH:%1$tM %1$te/%1$tm/%1$tY", date));
            }else{
                sb2.append("No forecasts nearby");
            }
                final String temp=temp2, maxTemp=maxTemp2, appTemp = appTemp2, minTemp=minTemp2, title = title2;
                final String s2 = sb.toString();
                final String s3 = sb2.toString();
                final int obsDist=obsDist2;
                //All weather information ready - Add it to GUI
                runOnUiThread(new Runnable(){
                    public void run(){
                        findViewById(R.id.relative).setVisibility(View.VISIBLE);
                        ((USwipeRefreshLayout) findViewById(R.id.refresh)).setRefreshing(false);
                        Drawable[] gd= new Drawable[2];
                        TransitionDrawable td = null;
                        if(!title.isEmpty()) {
                            findViewById(R.id.obs).setVisibility(View.VISIBLE);
                            ((TextView) findViewById(R.id.title)).setText(title);
                            if (obsDist > 5 && !title.equals(place.split(", ")[0])) {
                                ((TextView) findViewById(R.id.dist)).setText(obsDist + "km away from " + place.split(",")[0]);
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
                                gd[1] = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                                        new int[]{tempColor(Float.valueOf(temp) + 1.5f), tempColor(Float.valueOf(temp)), tempColor(Float.valueOf(temp) - 1.5f)});
                                if (currentTemp != null) {
                                    gd[0] = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                                            new int[]{tempColor(Float.valueOf(currentTemp) + 1.5f), tempColor(Float.valueOf(currentTemp)), tempColor(Float.valueOf(currentTemp) - 1.5f)});
                                }
                                td = new TransitionDrawable(gd);
                                td.startTransition(300);
                                currentTemp = temp;
                            } else {
                                findViewById(R.id.tempLayout).setVisibility(View.GONE);
                            }

                            if (!appTemp.isEmpty()) {
                                findViewById(R.id.appTemp).setVisibility(View.VISIBLE);
                                ((TextView) findViewById(R.id.appTemp)).setText(String.format("%s", appTemp));
                            }else {
                                findViewById(R.id.appTemp).setVisibility(View.GONE);
                            }
                        }else{
                            findViewById(R.id.obs).setVisibility(View.GONE);
                        }

                        if(Build.VERSION.SDK_INT > 19) {
                            if (Build.VERSION.SDK_INT < 16) {
                                findViewById(R.id.pager).setBackgroundDrawable(td);
                            } else {
                                findViewById(R.id.pager).setBackground(td);
                            }
                        }else{
                            if (Build.VERSION.SDK_INT < 16) {
                                findViewById(R.id.pager).setBackgroundDrawable(gd[1]);
                            } else {
                                findViewById(R.id.pager).setBackground(gd[1]);
                            }
                        }
                        GridView grid = ((GridView)findViewById(R.id.forecasts));
                        ((ForecastAdapter) grid.getAdapter()).notifyDataSetChanged();
                        ViewGroup.LayoutParams params = grid.getLayoutParams();
                        final float scale = MainActivity.this.getResources().getDisplayMetrics().density;
                        params.width = (int) Math.ceil(forecasts.size()*90*scale);
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

    //Longest common substring, no longer used
    public double lcs(String str1, String str2){
        int i=0;
        str1 = str1.replaceAll("\\(", "").replaceAll("\\)","").replaceAll("-","");
        str2 = str2.replaceAll("\\(", "").replaceAll("\\)","").replaceAll("-","");
        while(i<Math.min(str1.length(), str2.length()) && str1.charAt(i)==str2.charAt(i)){i++;}
        return ((double)i)/(str1.length()+str2.length());
    }

    //Color code for temperature.
    int tempColor(float temp) {
        int color;
        int silver = Color.rgb(150, 150,150);
        int grey = Color.rgb(100, 100, 100);
        int dgrey = Color.rgb(50, 50, 50);
        int purple = Color.rgb(100, 0, 100);
        int blue = Color.rgb(0, 50, 125);
        int teal = Color.rgb(0, 125, 125);
        int green = Color.rgb(50, 125, 50);
        int yellow = Color.rgb(150, 150, 0);
        int orange = Color.rgb(200, 125, 0);
        int red = Color.rgb(250, 0, 0);
        int brown = Color.rgb(75, 25, 25);
        int black = Color.rgb(0, 0, 0);
        int[] colorArray = {silver, grey, dgrey, purple, blue, teal, green, yellow, orange, red, brown, black};
        if(temp<-10){
            color = silver;
        }else if(temp<45) {
            int i = (int) Math.floor((temp+10)/5);
            color = interpolate(colorArray[i], colorArray[i+1], temp%5, 5);
        }else{
            color = black;
        }
        return color;
    }

    //Used to find colors that blend between two colors
    int interpolate(int color1, int color2, float control, int limit){
        float red = Color.red(color1)*(1-control/limit)+Color.red(color2)*control/limit;
        float blue = Color.blue(color1)*(1-control/limit)+Color.blue(color2)*control/limit;
        float green = Color.green(color1)*(1-control/limit)+Color.green(color2)*control/limit;
        return Color.rgb(Math.round(red), Math.round(green), Math.round(blue));
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
            if(toast!=null){toast.cancel();}
            if(!favorites.contains(currentPlace)) {
                favorites.add(currentPlace);
                toast = Toast.makeText(MainActivity.this, "Added to favorites", Toast.LENGTH_SHORT);
                item.setIcon(R.drawable.ic_star_on);
            }else{
                favorites.remove(currentPlace);
                toast = Toast.makeText(MainActivity.this, "Removed from favorites", Toast.LENGTH_SHORT);
                item.setIcon(R.drawable.ic_star_off);
            }
            toast.show();
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("Bookmarks", joinArrayList(favorites, "\n"));
            editor.apply();
            listAdapter.notifyDataSetChanged();
        }else if(id==R.id.search){
            showSearch();
        }else if(id==R.id.about){
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            String message = "";
            alertDialogBuilder.setTitle("About AU Weather:");
            message+="This app uses weather information from the Bureau of Meteorology (BoM): http://www.bom.gov.au/\n\n";
            message+="This app is not sponsored or endorsed in any way by the Bureau of Meteorology.\n\n";
            message+="The developers do not accept responsibility for any loss or damage occasioned by use of the information in this app.\n\n";
            message+="This app also uses Meteocons weather icons: http://www.alessioatzeni.com/meteocons/\n";
            alertDialogBuilder.setMessage(message);
            alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            final AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }else if(id==R.id.radarMenu){
            showRadar(null);
        }
        return super.onOptionsItemSelected(item);
    }

    //Show radar
    public void showRadar(View v){
        ((ViewPager)findViewById(R.id.pager)).setCurrentItem(1);
    }


    //User pressed back button, so close bookmarks, go to previous place or close the app
    @Override
    public void onBackPressed() {
        View v = findViewById(R.id.navigation_drawer);
        if(mDrawerLayout.isDrawerOpen(v)){
            mDrawerLayout.closeDrawer(v); //Bookmarks open, so close that
            return;
        }
        if (history.size()>0) {
            if(loaded) {
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
            //window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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
                    if(adapter.getCount()>0) {
                        getData(adapter.getItem(0));
                        alertDialog.dismiss();
                    }else{
                        if(toast!=null){toast.cancel();}
                        toast = Toast.makeText(MainActivity.this, "Place not found", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    //((TextView)findViewById(R.id.textView)).setText("Feature not implemented yet");
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
                    inputManager.hideSoftInputFromWindow(view2.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                String place = location.getText().toString();
                if(currentPlace!=null && !currentPlace.equals(place)){
                    history.push(currentPlace);
                }
                getData(place); //Load weather data for place
                alertDialog.dismiss();
            }
        });
    }

    //No longer used
    public void showBookmarks(){
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        ListView listView = new ListView(this);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String bookmarks = sp.getString("Bookmarks", "");
        favorites.clear();
        if(!bookmarks.equals("")){
            favorites.addAll(Arrays.asList(bookmarks.split("\\n")));
        }
        listAdapter.notifyDataSetChanged();
        listView.setAdapter(listAdapter);
        alertDialogBuilder.setTitle("Bookmarks:");
        if(favorites.size()==0){
            alertDialogBuilder.setMessage("No places");
        }else{
            alertDialogBuilder.setView(listView);
        }
        if(currentPlace!=null && !favorites.contains(currentPlace)) {
            alertDialogBuilder.setPositiveButton("Add current place", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    favorites.add(currentPlace);
                    listAdapter.notifyDataSetChanged();
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                    editor.putString("Bookmarks", joinArrayList(favorites, "\n"));
                    editor.apply();
                    if(toast!=null){toast.cancel();}
                    toast = Toast.makeText(MainActivity.this, "Place added to bookmarks", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
        alertDialogBuilder.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = alertDialogBuilder.show();
        listView.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String place = favorites.get(position);
                if(currentPlace!=null && !currentPlace.equals(place)){
                    history.push(currentPlace);
                }
                getData(place);
                alertDialog.dismiss();
            }
        });
        registerForContextMenu(listView);
    }

    //User long pressed on one of the bookmarks. Context menu allows user to delete bookmarks
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) info;
        String place = listAdapter.getItem(acmi.position);
        menu.add(Menu.NONE, 0, 0, "Delete "+place);
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
                        editor.putString("Bookmarks", joinArrayList(favorites, "\n"));
                        editor.apply();
                        if(toast!=null){toast.cancel();}
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

    //Join elements of an array list into a string
    public String joinArrayList(ArrayList<String> arrList, String separator){
    String prefix = "";
    StringBuilder sb = new StringBuilder();
    for(String s:arrList){
        sb.append(prefix);
        sb.append(s);
        prefix = separator;
    }
    return sb.toString();
}

    @Override
    public void onConfigurationChanged(Configuration config){
        super.onConfigurationChanged(config);
        mDrawerToggle.onConfigurationChanged(config);
    }

    //Show whether a place is bookmarked or not
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int starId;
        if (currentPlace!=null) {
            if (favorites.contains(currentPlace)) {
                starId = R.drawable.ic_star_on;
            } else {
                starId = R.drawable.ic_star_off;
            }
            menu.findItem(R.id.star).setVisible(true);
            if (Build.VERSION.SDK_INT < 22) {
                menu.findItem(R.id.star).setIcon(this.getResources().getDrawable(starId));
            } else {
                menu.findItem(R.id.star).setIcon(this.getResources().getDrawable(starId, this.getTheme()));
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }
}