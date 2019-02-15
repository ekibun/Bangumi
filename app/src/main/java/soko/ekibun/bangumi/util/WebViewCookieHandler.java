package soko.ekibun.bangumi.util;

import android.content.Context;
import android.support.annotation.Keep;
import android.webkit.CookieManager;
import android.webkit.WebView;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import soko.ekibun.bangumi.ui.web.WebActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebViewCookieHandler implements CookieJar {
    private CookieManager mCookieManager = CookieManager.getInstance();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) { }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String urlString = url.toString();
        String cookiesString = mCookieManager.getCookie(urlString);

        if (cookiesString != null && !cookiesString.isEmpty()) {
            String[] cookieHeaders = cookiesString.split(";");
            List<Cookie> cookies = new ArrayList<>(cookieHeaders.length);

            for (String header : cookieHeaders) {
                cookies.add(Cookie.parse(url, header));
            }

            return cookies;
        }

        return Collections.emptyList();
    }

    @Keep
    public static String getCookie(String url){
        return  CookieManager.getInstance().getCookie(url);
    }

    @Keep
    public static String getUserAgent(Context context){
        return (new WebView(context)).getSettings().getUserAgentString();
    }

    @Keep
    public static void setCookie(String url, String cookie){
        CookieManager.getInstance().setCookie(url, cookie);
    }
}