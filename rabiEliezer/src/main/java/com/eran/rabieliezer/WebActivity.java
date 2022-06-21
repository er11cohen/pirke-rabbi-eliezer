package com.eran.rabieliezer;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.FindListener;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

import com.eran.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WebActivity extends Activity {

    WebView wv;
    ProgressBar progressBar;
    WebSettings wvSetting;
    int scrollY = 0;
    MenuItem nightModeItem = null;
    MenuItem PreviousSearch = null;
    MenuItem NextSearch = null;
    SharedPreferences rabiEliezerPreferences;
    SharedPreferences defaultSharedPreferences;
    boolean fullScreen = false;
    AudioManager am;
    String phoneStatus;
    int startRingerMode = 2;//RINGER_MODE_NORMAL

    String perekName;
    int perekIndex;
    boolean pageReady = false;
    GestureDetector gs = null;
    ActionBar actionBar = null;
    int activeMatch = 0;
    int totalMatch = 0;
    String currentQuery = "";
    int noResultCount = 0;
    SearchView searchView;
    Boolean isFirstOnPageFinished = true;
    String appName = "/RabiEliezer";
    int lastPageIndex = 5;

    public enum Search {
        WITHOUT_SEARCH, PREVIOUS_SEARCH, NEXT_SEARCH
    }


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        fullScreen = defaultSharedPreferences.getBoolean("CBFullScreen", false);

        if (fullScreen && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_web);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (fullScreen) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                actionBar.hide();
            }
        }

        boolean keepScreenOn = defaultSharedPreferences.getBoolean("CBKeepScreenOn", false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        phoneStatus = defaultSharedPreferences.getString("phone_status", "-1");

        rabiEliezerPreferences = getSharedPreferences("rabiEliezerPreferences", MODE_PRIVATE);
        wv = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        wvSetting = wv.getSettings();


        Intent intent = getIntent();
        String requiredFileName = intent.getStringExtra("requiredFileName");
        if (requiredFileName != null) {
            ArrayList<Perek> locationList;
            Gson gson = new Gson();

            SharedPreferences preferences = getSharedPreferences("Locations", MODE_PRIVATE);
            String preferencesLocationsJson = preferences.getString("preferencesLocationsJson", null);
            if (preferencesLocationsJson != null) {
                locationList = gson.fromJson(preferencesLocationsJson, new TypeToken<ArrayList<Perek>>() {
                }.getType());
                Perek requiredLocation = null;
                if (requiredFileName.equals("-1")/*lastLocation*/) {
                    int lastLocation = locationList.size() - 1;
                    requiredLocation = locationList.get(lastLocation);
                } else//History
                {
                    for (int i = locationList.size() - 1; i >= 0; i--) {
                        requiredLocation = locationList.get(i);
                        if (requiredLocation.getTime().equals(requiredFileName)) {
                            break;
                        }
                    }
                }

                if (requiredLocation != null) {
                    perekName = requiredLocation.getPerekName();
                    perekIndex = requiredLocation.getPerekIndex();
                    scrollY = requiredLocation.getScrollY();
                } else {
                    finish();//not need to arrive to here
                }
            } else {
                finish();//not need to arrive to here
            }
        } else {
            Perek perek = (Perek) intent.getParcelableExtra("perek");
            perekName = perek.getPerekName();
            perekIndex = perek.getPerekIndex();
        }

        wvSetting.setCacheMode(WebSettings.LOAD_NO_CACHE);
        wvSetting.setJavaScriptEnabled(true);
        LoadWebView(/*Search.WITHOUT_SEARCH*/);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            WeakReference<Activity> WeakReferenceActivity = new WeakReference<Activity>(this);
            Utils.toggleFullScreen(WeakReferenceActivity, getApplicationContext(), R.id.webView, actionBar, fullScreen);
        }

        WeakReference<Activity> WeakReferenceActivity = new WeakReference<Activity>(this);
        Utils.firstDoubleClickInfo(defaultSharedPreferences, WeakReferenceActivity);
    }

    protected void onResume() {
        super.onResume();//Always call the superclass method first

        startRingerMode = am.getRingerMode();

        if (!phoneStatus.equals("-1") && startRingerMode != 0/*silent*/) {
            am.setRingerMode(Integer.parseInt(phoneStatus));
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.web, menu);
        nightModeItem = menu.findItem(R.id.nightMode);
        PreviousSearch = menu.findItem(R.id.previousSearch);
        NextSearch = menu.findItem(R.id.nextSearch);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {

            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            searchItem.setVisible(true);///////////////////////////
            searchView = (SearchView) searchItem.getActionView();
            searchView.setQueryHint("חיפוש בפרק הנוכחי");

            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setOnQueryTextListener(new OnQueryTextListener() {

                @Override
                public boolean onQueryTextChange(String query) {
                    //Toast.makeText(getApplicationContext(),"onQueryTextChange " +query ,Toast.LENGTH_LONG).show();

                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentQuery = query;
                    noResultCount = 0;
                    scrollY = wv.getScrollY();//save the location before the search
                    find(currentQuery, Search.NEXT_SEARCH);
                    // TODO Auto-generated method stub
                    return true;
                }


            });

            searchView.setOnCloseListener(new OnCloseListener() {
                @Override
                public boolean onClose() {
                    closeSearch(true);
                    return false;
                }

            });

            searchView.setOnSearchClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                }
            });
        }


        return true;
    }


    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.nextChapter:
                moveChapter(true);
                break;
            case R.id.previousChapter:
                moveChapter(false);
                break;
            case R.id.nightMode:
                Utils.NightMode(true, rabiEliezerPreferences, wv, nightModeItem);
                break;
            case R.id.zoomUp:
                Utils.changeSize(true, rabiEliezerPreferences, wvSetting);
                break;
            case R.id.zoomDown:
                Utils.changeSize(false, rabiEliezerPreferences, wvSetting);
                break;
            case R.id.previousSearch:
                previousSearch();
                break;
            case R.id.nextSearch:
                nextSearch();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NewApi")
    private void find(String query, Search searchKind) {
        final Search finalSearch[] = new Search[1];
        finalSearch[0] = searchKind;

        wv.findAllAsync(query);

        PreviousSearch.setVisible(true);
        NextSearch.setVisible(true);

        wv.setFindListener(new FindListener() {

            public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {

            }
        });
    }

    private void previousSearch() {
        wv.findNext(false);
    }

    private void nextSearch() {
        wv.findNext(true);
    }

    private void moveChapter(Boolean next) {
        if (next == true && perekIndex < UtilRabiEliezer.pereks.length - 1) {
            perekIndex++;
        } else if (next == false && perekIndex > 0) {
            perekIndex--;
        } else {
            return;
        }

        perekName = "פרק " + UtilRabiEliezer.pereks[perekIndex];
        scrollY = 0;
        LoadWebView();
    }

    private void LoadWebView(/*final Search searchKind*/) {
        setTitle(perekName);

        wv.loadUrl("file:///android_asset/html/" + (perekIndex + 1) + ".html");

        int size = Utils.readSize(rabiEliezerPreferences);
        wvSetting.setDefaultFontSize(size);

        wv.setWebViewClient(new WebViewClient() {


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                //if(searchKind == Search.WITHOUT_SEARCH)
                //{
                if (isFirstOnPageFinished) {
                    Utils.showWebView(wv, progressBar, true);
                }
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        ChangeWebViewBySettings();
                        if (scrollY != 0) {
                            isFirstOnPageFinished = false;
                            wv.scrollTo(0, scrollY);
                        } else if (isFirstOnPageFinished) {
                            isFirstOnPageFinished = false;
                        }

                        pageReady = true;
                        Utils.setOpacity(wv, 1);
                    }
                }, 500);
                //	}
//            	else//fromSearch
//            	{
//            		ChangeWebViewBySettings();
//            		find(currentQuery, searchKind);
//            		Utils.setOpacity(wv, 1);
//            	}
            }
        });


    }

    private void ChangeWebViewBySettings() {
        Utils.NightMode(false, rabiEliezerPreferences, wv, nightModeItem);
    }

    @Override
    protected void onPause() {
        super.onPause();  //Always call the superclass method first

        if (!phoneStatus.equals("-1") && startRingerMode != 0/*silent*/) {
            am.setRingerMode(startRingerMode);
        }

        File path = Utils.getFilePath(getApplicationContext());
        File folder = new File(path + appName);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }

        if (success && pageReady) {
            SharedPreferences preferences = getSharedPreferences("Locations", MODE_PRIVATE);
            String preferencesLocationsJson = preferences.getString("preferencesLocationsJson", null);

            if (preferencesLocationsJson == null)//for second install, remove the old files
            {
                if (folder.isDirectory()) {
                    String[] children = folder.list();
                    for (int i = 0; i < children.length; i++) {
                        new File(folder, children[i]).delete();
                    }
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String currentTime = sdf.format(new Date());

            View content = findViewById(R.id.layout);
            content.setDrawingCacheEnabled(true);
            Bitmap bitmap = content.getDrawingCache();
            File file = new File(path + appName + "/" + currentTime + ".png");
            ArrayList<Perek> locationList = new ArrayList<Perek>();
            Gson gson = new Gson();
            try {
                file.createNewFile();
                FileOutputStream ostream = new FileOutputStream(file);
                bitmap.compress(CompressFormat.PNG, 100, ostream);
                ostream.close();

                scrollY = wv.getScrollY();
                Perek location = new Perek(currentTime, scrollY, perekIndex, perekName);


                if (preferencesLocationsJson != null) {
                    locationList = gson.fromJson(preferencesLocationsJson, new TypeToken<ArrayList<Perek>>() {
                    }.getType());
                    if (locationList.size() >= 10) {
                        String idFirstLocation = locationList.get(0).getTime();
                        File imageToDelete = new File(path + appName + "/" + idFirstLocation + ".png");
                        if (imageToDelete.exists()) {
                            boolean deleted = imageToDelete.delete();
                        }

                        locationList.remove(0);
                    }
                }


                locationList.add(location);

                String json = gson.toJson(locationList);

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("preferencesLocationsJson", json);
                editor.commit();

            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

    }

    @SuppressLint("NewApi")
    @Override
    public void onBackPressed() {

        wv.clearFocus();//for close pop-up of copy, select etc.

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            if (!searchView.isIconified()) {
                closeSearch(false);
                return;
            }
        }

        super.onBackPressed();
    }

    //for voice search
    @SuppressLint("NewApi")
    @Override
    protected void onNewIntent(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                searchView.setQuery(query, true);

                //close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void closeSearch(Boolean fromListener) {
        wv.clearMatches();//clear the finds
        PreviousSearch.setVisible(false);
        NextSearch.setVisible(false);

        if (!fromListener) {// the listener do this by himself
            searchView.setIconified(true);// clear the searchView
            searchView.onActionViewCollapsed();//close the searchView
        }
    }

}


