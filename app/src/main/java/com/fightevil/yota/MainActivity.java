package com.fightevil.yota;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {
    static final int REQUEST_PREF_UPDATE = 1;
    static final String tag = "yota.main";

    YotaNavigator yota;
    MyArrayAdapter adapter;
    ListViewDataCalculator dataLoader;
    boolean useAnimation = true;
    MyAnimation animation;
    SharedPreferences prefs;
    TextView periodTextView;
    boolean needRestart = false;
    boolean needUpdate = false;
    ListView rateList;

    YotaNavigator.YotaNavigatorInterface onPageLoad = new YotaNavigator.YotaNavigatorInterface() {
        @Override
        public  void OnPeriodDefined(final String period){
            Log.d(tag, "period defined");
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    periodTextView.setText(getString(R.string.period_left) + " " + period);
                }
            });
        }

        @Override
        public void OnRateDefined(final int rateNumber) {
            Log.d(tag, "rate defined");
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (rateNumber >= 0 && rateNumber < YotaNavigator.RateNames.length){
                        getSupportActionBar().setTitle(YotaNavigator.RateNames[rateNumber]);
                        int pos = dataLoader.RateNumberToListViewPosition(rateNumber);
                        if (pos != -1)
                            adapter.SelectItem(pos);
                    }
                    else Toast.makeText(getApplicationContext(), getString(R.string.message_incorrect_rate), Toast.LENGTH_LONG).show();
                    if (useAnimation)
                        animation.Stop();
                }
            });
        }

        @Override
        public void OnStartConnection() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getSupportActionBar().setTitle(getString(R.string.message_connection_start));
                    if (useAnimation)
                        animation.Start();
                }
            });
        }

        @Override
        public void OnPageFinished() {
            //SetTitleOnUiThread(getString(R.string.message_connection_established));
        }

        @Override
        public void OnError(final String message, final String title) {
            Log.d(tag, "error");
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                if (!title.equals(""))
                    getSupportActionBar().setTitle(title);
                else
                    getSupportActionBar().setTitle(getString(R.string.message_connection_interrupted));
                if (!message.equals(""))
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                if (useAnimation)
                    animation.Error();
                adapter.DeselectAll();
                }
            });
        }

        @Override
        public void OnError(int messageId, int titleId) {
            OnError(getString(messageId), getString(titleId));
        }

        @Override
        public void OnRateSetupStart(final int rateNumber) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getSupportActionBar().setTitle(getString(R.string.message_transition_to) + " " + YotaNavigator.RateNames[rateNumber]);
                    if (useAnimation && !animation.IsRunning())
                        animation.Start();
                }
            });
        }

        @Override
        public void OnRateNamesDefined() {
            Log.d(tag, "update adapter time");
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UpdateAdapter();
                    SaveRateNames();
                    TextView rateErrorTextView = (TextView) findViewById(R.id.rateErrorTextView);
                    rateErrorTextView.setVisibility(View.GONE);
                }
            });
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("showPeriod")){
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdatePeriodPref();
                    }
                });
            }
            else if (key.startsWith("ratePreference")){
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdateAdapter();
                    }
                });
            }
            else if (key.equals("username")){
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookie();
                String username = prefs.getString("username", "");
                yota.SetLogin(username);
                needUpdate = true;
            }
            else if (key.equals("password")){
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookie();
                String password = prefs.getString("password", "");
                yota.SetPassword(password);
                needUpdate = true;
            }
            else if (key.equals("directReference")){
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookie();
                Boolean useDirectReference = prefs.getBoolean("directReference", getResources().getBoolean(R.bool.pref_default_direct_ref));
                yota.SetUseDirect(useDirectReference);
                needUpdate = true;
            }
            else if (key.equals("rateCount")){
                Log.d(tag, "needRestart");
                int rateCount = prefs.getInt(key, 0);
                Log.d(tag, "nr " + rateCount);
                if (rateCount == 0){
                    needRestart = true;
                    Log.d(tag, "nr" + needRestart);
                }
            }
        }
    };

    private void ReadRateNames(){
        int count = prefs.getInt("rateCount", 0);
        Log.d(tag, "rateCount onActivityStart " + count);
        if (count > 0){
            YotaNavigator.RateNames = new String[count];
            for (int i = 0; i < count; ++i)
                YotaNavigator.RateNames[i] = prefs.getString("rateNumber"+i, "");
        }
    }

    private void SaveRateNames(){
        if (YotaNavigator.RateNames != null){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("rateCount", YotaNavigator.RateNames.length);
            for (int i = 0; i < YotaNavigator.RateNames.length; ++i)
                editor.putString("rateNumber"+i, YotaNavigator.RateNames[i]);
            editor.commit();
        }
    }

    private void UpdateAdapter(){
        dataLoader = new ListViewDataCalculator();
        adapter = new MyArrayAdapter(getApplicationContext(), MyArrayAdapter.CreateModelList(dataLoader.GetListViewElems()));
        rateList.setAdapter(adapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // считываем сохраненные настройки
        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        ReadRateNames();
        prefs.registerOnSharedPreferenceChangeListener(spChanged);

        periodTextView = (TextView)findViewById(R.id.periodTextView);
        periodTextView.setText(getString(R.string.period_left) + " ...");
        UpdatePeriodPref();

        String username = prefs.getString("username", "");
        String password = prefs.getString("password", "");
        Boolean useDirectReference = prefs.getBoolean("directReference", getResources().getBoolean(R.bool.pref_default_direct_ref));

        WebView myWebView = (WebView) findViewById(R.id.webView);
        if (Keys.debug)
            myWebView.setVisibility(View.VISIBLE);
        yota = new YotaNavigator(myWebView, username, password, useDirectReference, onPageLoad); 
        rateList = (ListView)findViewById(R.id.listView);
        dataLoader = new ListViewDataCalculator();
        adapter = new MyArrayAdapter(getApplicationContext(), MyArrayAdapter.CreateModelList(dataLoader.GetListViewElems()));
        rateList.setAdapter(adapter);
        rateList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (!adapter.IsSelected(position) &&
                        !getSupportActionBar().getTitle().equals(getString(R.string.error_incorrect_login_title)) &&
                        !getSupportActionBar().getTitle().equals(getString(R.string.error_money_is_over_title))){
                    yota.SetRate(dataLoader.ListViewPositionToRateNumber(position));
                    adapter.SelectItem(position);
                }
            }
        });

        if (YotaNavigator.RateNames == null){
            TextView rateErrorTextView = (TextView)findViewById(R.id.rateErrorTextView);
            rateErrorTextView.setVisibility(View.VISIBLE);
        }

        if (useAnimation)
            animation = new MyAnimation();
        yota.GoYota();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update:
                yota.GoYota();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_PREF_UPDATE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(tag, "onActivityResult " + needRestart + "|" + requestCode + "|" + REQUEST_PREF_UPDATE);
        switch (requestCode) {
            case REQUEST_PREF_UPDATE:
                if (needRestart){
                    needRestart = false;
                    // очищаем cookie чтобы использовать новые логин/пароль
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeAllCookie();
                    // перезагружаем MainActivity
                    Intent intent = getIntent();
                    MainActivity.this.finish();
                    startActivity(intent);
                } else if (needUpdate){
                    needUpdate = false;
                    yota.GoYota();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void UpdatePeriodPref(){
        if (prefs.getBoolean("showPeriod", getResources().getBoolean(R.bool.pref_default_show_period)))
            periodTextView.setVisibility(View.VISIBLE);
        else
            periodTextView.setVisibility(View.GONE);
    }

    public class MyAnimation {
        private int interval = 400;
        private CountDownTimer timer;
        private int curImage = 0;
        private int imageCount = 8;
        private boolean isRunning = false;
        public MyAnimation(){
            timer = new CountDownTimer(Integer.MAX_VALUE, interval) {
                public void onTick(long millisUntilFinished) {
                    if (isRunning){
                        int id = MainActivity.this.getResources().getIdentifier("i" + (curImage+1), "drawable", MainActivity.this.getPackageName());
                        curImage = (curImage + 1) % imageCount;
                        SetIconOnUiThread(id);
                    }
                }
                public void onFinish() {
                }
            };
        }
        private void SetIconOnUiThread(final int id){
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                getSupportActionBar().setIcon(id);
                }
            });
        }
        public void Start(){
            isRunning = true;
            curImage = 0;
            timer.start();
        }
        public void Stop(){
            isRunning = false;
            timer.cancel();
            SetIconOnUiThread(R.drawable.ic_launcher);

        }
        public void Error(){
            isRunning = false;
            timer.cancel();
            SetIconOnUiThread(R.drawable.err);
        }
        public boolean IsRunning(){
            return isRunning;
        }
    }

    public static class Model {
        private String name;
        private boolean selected;

        public Model(String name) {
            this.name = name;
            selected = false;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    public static class MyArrayAdapter extends ArrayAdapter<Model>{
        private final Context context;
        private final List<Model> values;

        public MyArrayAdapter(Context context, List<Model> values) {
            super(context, R.layout.list_item, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_item, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.list_view_item_text);
            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.list_view_item_check);
            textView.setText(values.get(position).getName());
            checkBox.setChecked(values.get(position).isSelected());
            return rowView;
        }

        public void SelectItem(int position){
            SetValuesTofDefault();
            values.get(position).setSelected(true);
            notifyDataSetChanged();
        }

        public boolean IsSelected(int position){
            return values.get(position).isSelected();
        }

        public void DeselectAll(){
            SetValuesTofDefault();
            notifyDataSetChanged();
        }

        private void SetValuesTofDefault(){
            for (int i = 0; i < values.size(); ++i)
                values.get(i).setSelected(false);
        }

        private static List<Model> CreateModelList(String[] textValues){
            List<Model> values = new ArrayList<Model>();
            for (int i = 0; i < textValues.length; ++i)
                values.add(new Model(textValues[i]));
            return values;
        }
    }

    class ListViewDataCalculator{
        boolean[] rateVisibility = null;

        public ListViewDataCalculator(){
            if (YotaNavigator.RateNames != null){
                rateVisibility = new boolean[YotaNavigator.RateNames.length];
                for (int i = 0; i < YotaNavigator.RateNames.length; ++i){
                    rateVisibility[i] = prefs.getBoolean("ratePreference"+i, SettingsActivity.DefaultVisibilityListViewElem(i));
                }
            }
        }

        public String[] GetListViewElems(){
            List<String> res = new ArrayList<String>();
            if (rateVisibility != null)
                for (int i = 0; i < YotaNavigator.RateNames.length; ++i)
                    if (rateVisibility[i])
                        res.add(YotaNavigator.RateNames[i]);
            return res.toArray(new String[res.size()]);
        }

        public int RateNumberToListViewPosition(int rateNumber){
            if (!rateVisibility[rateNumber])
                return -1;
            int res = 0;
            for (int i = 0; i < rateNumber; ++i)
                if (rateVisibility[i])
                    res++;
            return res;
        }

        public int ListViewPositionToRateNumber(int position){
            int p = -1;
            for (int i = 0; i < YotaNavigator.RateNames.length; ++i){
                if (rateVisibility[i])
                    p++;
                if (p == position)
                    return i;
            }
            return -1;
        }
    }
}