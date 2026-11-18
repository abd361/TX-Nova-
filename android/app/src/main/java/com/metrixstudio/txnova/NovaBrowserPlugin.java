package com.metrixstudio.txnova;

import android.content.Intent;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NovaBrowser")
public class NovaBrowserPlugin extends Plugin {
    @PluginMethod
    public void open(PluginCall call) {
        String url = call.getString("url", "https://duckduckgo.com");
        Intent intent = new Intent(getContext(), BrowserActivity.class);
        intent.putExtra("url", url);
        getContext().startActivity(intent);
        call.resolve();
    }
}