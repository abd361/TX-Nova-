package com.metrixstudio.txnova;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class BrowserActivity extends Activity {
    private static final int BG = Color.rgb(15, 12, 25);
    private static final int PANEL = Color.rgb(31, 25, 45);
    private static final int PURPLE = Color.rgb(153, 103, 255);
    private LinearLayout root, toolbar, controls;
    private EditText address;
    private ProgressBar progress;
    private FrameLayout webContainer;
    private final ArrayList<Tab> tabs = new ArrayList<>();
    private int active = 0;
    private boolean privateMode = false, desktopMode = false;
    private SharedPreferences prefs;

    private static class Tab {
        WebView web;
        String title = "New tab";
        Tab(WebView w) { web = w; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("tx_nova", MODE_PRIVATE);
        buildLayout();
        addTab(getIntent().getStringExtra("url"));
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size);
        t.setGravity(Gravity.CENTER); return t;
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE);
        b.setTextSize(12); b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        return b;
    }
    private void buildLayout() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        toolbar = new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL); toolbar.setPadding(dp(8),dp(8),dp(8),dp(5));
        address = new EditText(this); address.setSingleLine(true); address.setTextSize(14); address.setTextColor(Color.WHITE);
        address.setHintTextColor(0xffa59bb7); address.setHint("Search or enter website"); address.setPadding(dp(14),0,dp(10),0);
        address.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        toolbar.addView(address, new LinearLayout.LayoutParams(0,dp(46),1));
        Button go = button("Go"); toolbar.addView(go,new LinearLayout.LayoutParams(dp(55),dp(46)));
        go.setOnClickListener(v -> navigateFromAddress());
        address.setOnEditorActionListener((v, action, event) -> { navigateFromAddress(); return true; });
        root.addView(toolbar);
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        webContainer = new FrameLayout(this); root.addView(webContainer,new LinearLayout.LayoutParams(-1,0,1));
        controls = new LinearLayout(this); controls.setPadding(dp(3),dp(5),dp(3),dp(7)); controls.setGravity(Gravity.CENTER);
        String[] labels = {"‹","›","⌂","＋","Tabs","☆","↓","⋮"};
        for (String label: labels) {
            Button b = button(label); controls.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
            b.setOnClickListener(v -> handleControl(label));
        }
        root.addView(controls);
        setContentView(root);
    }

    private void handleControl(String action) {
        WebView w = current();
        switch(action) {
            case "‹": if(w.canGoBack()) w.goBack(); else Toast.makeText(this,"No previous page",Toast.LENGTH_SHORT).show(); break;
            case "›": if(w.canGoForward()) w.goForward(); break;
            case "⌂": w.loadDataWithBaseURL("https://txnova.local/", homeHtml(), "text/html", "UTF-8", null); break;
            case "＋": addTab(null); break;
            case "Tabs": showTabs(); break;
            case "☆": addBookmark(); break;
            case "↓": showDownloadsInfo(); break;
            case "⋮": showMenu(); break;
        }
    }

    private WebView current() { return tabs.get(active).web; }

    private void addTab(String url) {
        WebView web = new WebView(this);
        configureWebView(web);
        Tab tab = new Tab(web); tabs.add(tab); active = tabs.size()-1;
        webContainer.removeAllViews();
        webContainer.addView(web,new FrameLayout.LayoutParams(-1,-1));
        if (url == null || url.trim().isEmpty()) web.loadDataWithBaseURL("https://txnova.local/",homeHtml(),"text/html","UTF-8",null);
        else web.loadUrl(normalize(url));
        updateTabCount();
    }

    private void configureWebView(WebView web) {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(!privateMode);
        s.setDatabaseEnabled(!privateMode);
        s.setSupportZoom(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(desktopMode); s.setUseWideViewPort(desktopMode);
        s.setJavaScriptCanOpenWindowsAutomatically(false); s.setSupportMultipleWindows(false);
        s.setSaveFormData(false);
        if(privateMode) { s.setCacheMode(WebSettings.LOAD_NO_CACHE); web.clearCache(true); }
        else s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if(desktopMode) s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        else s.setUserAgentString(null);
        web.setBackgroundColor(Color.WHITE);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW,uri)); } catch(Exception ignored) {}
                return true;
            }
            @Override public void onPageStarted(WebView view,String url,Bitmap favicon) {
                address.setText(url); progress.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view,String url) {
                address.setText(url);
                tabs.get(active).title = view.getTitle() == null ? url : view.getTitle();
                progress.setVisibility(View.GONE);
                if(!privateMode && url.startsWith("http")) saveHistory(url,view.getTitle());
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error) {
                handler.cancel();
                Toast.makeText(BrowserActivity.this,"Secure connection error. Page blocked.",Toast.LENGTH_LONG).show();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view,int value) {
                progress.setProgress(value); progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public void onReceivedTitle(WebView view,String title) {
                if(tabs.size()>active) tabs.get(active).title = title;
            }
        });
        web.setDownloadListener((url,ua,disposition,mime,length) -> download(url,ua,disposition,mime));
    }

    private void navigateFromAddress() {
        String raw = address.getText().toString().trim(); if(raw.isEmpty()) return;
        current().loadUrl(normalize(raw));
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);
    }
    private String normalize(String raw) {
        if(raw.matches("(?i)^https?://.*")) return raw;
        if(raw.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}([/:?#].*)?$")) return "https://"+raw;
        try { return "https://duckduckgo.com/?q="+URLEncoder.encode(raw,"UTF-8"); }
        catch(Exception e) { return "https://duckduckgo.com"; }
    }
    private String homeHtml() {
        return "<html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='margin:0;background:#100d1d;color:white;font-family:Arial;text-align:center;padding:18vh 24px'><div style='font-size:48px;color:#a77aff'>✦</div><h1>TX Nova</h1><p style='color:#b9aecb'>Search or enter a website in the address bar above.</p><p><a style='color:#b99bff' href='https://duckduckgo.com'>DuckDuckGo</a>　<a style='color:#b99bff' href='https://www.google.com'>Google</a></p></body></html>";
    }

    private void showTabs() {
        if(tabs.isEmpty()) return;
        String[] names = new String[tabs.size()+1];
        for(int i=0;i<tabs.size();i++) names[i] = (i==active?"● ":"")+ (tabs.get(i).title==null?"Tab":tabs.get(i).title);
        names[tabs.size()] = "+ New tab";
        new AlertDialog.Builder(this).setTitle("Your tabs ("+tabs.size()+")").setItems(names,(d,which)->{
            if(which==tabs.size()) addTab(null); else switchTab(which);
        }).setNeutralButton("Close current tab",(d,w)->closeTab(active)).setNegativeButton("Cancel",null).show();
    }
    private void switchTab(int index) {
        if(index<0 || index>=tabs.size()) return;
        active=index; webContainer.removeAllViews();
        webContainer.addView(current(),new FrameLayout.LayoutParams(-1,-1));
        address.setText(current().getUrl()); updateTabCount();
    }
    private void closeTab(int index) {
        if(tabs.size()<=1) { current().loadDataWithBaseURL("https://txnova.local/",homeHtml(),"text/html","UTF-8",null); return; }
        Tab removed=tabs.remove(index); removed.web.stopLoading(); removed.web.destroy();
        if(active>=tabs.size()) active=tabs.size()-1;
        switchTab(active);
    }
    private void updateTabCount() { setTitle("TX Nova · "+tabs.size()+" tabs"); }

    private void addBookmark() {
        if(privateMode) { Toast.makeText(this,"Bookmarks are disabled in private mode.",Toast.LENGTH_SHORT).show(); return; }
        String url=current().getUrl(); if(url==null || !url.startsWith("http")) return;
        java.util.HashSet<String> set=new java.util.HashSet<>(prefs.getStringSet("bookmarks",new java.util.HashSet<>()));
        set.add(url); prefs.edit().putStringSet("bookmarks",set).apply();
        Toast.makeText(this,"Bookmark saved",Toast.LENGTH_SHORT).show();
    }
    private void showBookmarks() {
        java.util.Set<String> set=prefs.getStringSet("bookmarks",new java.util.HashSet<>());
        if(set.isEmpty()) { new AlertDialog.Builder(this).setMessage("No bookmarks saved yet.").setPositiveButton("OK",null).show(); return; }
        String[] items=set.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Bookmarks").setItems(items,(d,w)->current().loadUrl(items[w])).setNegativeButton("Close",null).show();
    }
    private void saveHistory(String url,String title) {
        try {
            JSONArray arr=new JSONArray(prefs.getString("history","[]"));
            JSONObject item=new JSONObject(); item.put("url",url); item.put("title",title==null?url:title); item.put("time",System.currentTimeMillis());
            arr.put(item); while(arr.length()>100) arr.remove(0);
            prefs.edit().putString("history",arr.toString()).apply();
        } catch(Exception ignored) {}
    }
    private void showHistory() {
        try {
            JSONArray arr=new JSONArray(prefs.getString("history","[]"));
            if(arr.length()==0) { new AlertDialog.Builder(this).setMessage("No history yet.").setPositiveButton("OK",null).show(); return; }
            List<String> labels=new ArrayList<>(), urls=new ArrayList<>();
            for(int i=arr.length()-1;i>=0;i--) { JSONObject o=arr.getJSONObject(i); labels.add(o.optString("title")); urls.add(o.optString("url")); }
            String[] a=labels.toArray(new String[0]);
            new AlertDialog.Builder(this).setTitle("Recent history").setItems(a,(d,w)->current().loadUrl(urls.get(w))).setNeutralButton("Clear history",(d,w)->{prefs.edit().remove("history").apply(); Toast.makeText(this,"History cleared",Toast.LENGTH_SHORT).show();}).setNegativeButton("Close",null).show();
        } catch(Exception e) { prefs.edit().remove("history").apply(); }
    }
    private void download(String url,String ua,String disposition,String mime) {
        if(privateMode) { Toast.makeText(this,"Downloads are disabled in private mode.",Toast.LENGTH_LONG).show(); return; }
        try {
            DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,Uri.parse(url).getLastPathSegment()==null?"txnova-download":Uri.parse(url).getLastPathSegment());
            req.setMimeType(mime); req.addRequestHeader("User-Agent",ua);
            String cookie=CookieManager.getInstance().getCookie(url); if(cookie!=null) req.addRequestHeader("Cookie",cookie);
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
            Toast.makeText(this,"Download started. Check Android Downloads.",Toast.LENGTH_LONG).show();
        } catch(Exception e) { Toast.makeText(this,"Could not start download.",Toast.LENGTH_LONG).show(); }
    }
    private void showDownloadsInfo() {
        new AlertDialog.Builder(this).setTitle("Downloads").setMessage("Files downloaded by TX Nova are saved in your Android Downloads folder.").setPositiveButton("Open Downloads",(d,w)->{
            Intent i=new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS); try{startActivity(i);}catch(Exception ignored){}
        }).setNegativeButton("Close",null).show();
    }
    private void showMenu() {
        String[] items={"Bookmarks","History","Desktop site: "+(desktopMode?"ON":"OFF"),"Private browsing: "+(privateMode?"ON":"OFF"),"Clear browsing data","Share page","Settings"};
        new AlertDialog.Builder(this).setTitle("TX Nova").setItems(items,(d,w)->{
            switch(w) {
                case 0: showBookmarks(); break;
                case 1: showHistory(); break;
                case 2: toggleDesktop(); break;
                case 3: togglePrivate(); break;
                case 4: clearDataDialog(); break;
                case 5: sharePage(); break;
                case 6: showSettings(); break;
            }
        }).show();
    }
    private void toggleDesktop() {
        desktopMode=!desktopMode;
        for(Tab t:tabs) { WebSettings s=t.web.getSettings(); s.setUseWideViewPort(desktopMode); s.setLoadWithOverviewMode(desktopMode); s.setUserAgentString(desktopMode?"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36":null); }
        current().reload();
        Toast.makeText(this,"Desktop mode "+(desktopMode?"enabled":"disabled"),Toast.LENGTH_SHORT).show();
    }
    private void togglePrivate() {
        privateMode=!privateMode;
        if(privateMode) {
            prefs.edit().remove("history").apply();
            CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush();
        }
        for(Tab t:tabs) {
            WebSettings s=t.web.getSettings(); s.setDomStorageEnabled(!privateMode); s.setDatabaseEnabled(!privateMode);
            s.setCacheMode(privateMode?WebSettings.LOAD_NO_CACHE:WebSettings.LOAD_DEFAULT);
            if(privateMode) t.web.clearCache(true);
        }
        if(privateMode) addTab(null);
        Toast.makeText(this,privateMode?"Private mode on — history and downloads are not saved by TX Nova.":"Private mode off",Toast.LENGTH_LONG).show();
    }
    private void clearDataDialog() {
        new AlertDialog.Builder(this).setTitle("Clear browsing data?").setMessage("This clears TX Nova's saved history, cookies, cache and website storage. Bookmarks are kept.")
            .setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{
                prefs.edit().remove("history").apply();
                CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush();
                for(Tab t:tabs) { t.web.clearHistory(); t.web.clearCache(true); t.web.clearFormData(); t.web.clearSslPreferences(); }
                Toast.makeText(this,"Browsing data cleared",Toast.LENGTH_SHORT).show();
            }).show();
    }
    private void sharePage() {
        String url=current().getUrl(); if(url==null) return;
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,url);
        startActivity(Intent.createChooser(i,"Share page"));
    }
    private void showSettings() {
        new AlertDialog.Builder(this).setTitle("TX Nova settings")
            .setMessage("Search engine: DuckDuckGo\\nDesktop site: "+(desktopMode?"On":"Off")+"\\nPrivate browsing: "+(privateMode?"On":"Off")+"\\nDownloads: Android Downloads folder")
            .setPositiveButton("OK",null).show();
    }
    @Override public void onBackPressed() {
        if(current()!=null && current().canGoBack()) current().goBack();
        else if(tabs.size()>1) closeTab(active);
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        for(Tab t:tabs) { t.web.stopLoading(); t.web.destroy(); }
        tabs.clear(); super.onDestroy();
    }
}