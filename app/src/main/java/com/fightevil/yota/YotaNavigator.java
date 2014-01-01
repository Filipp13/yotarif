package com.fightevil.yota;

import android.net.http.SslError;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class YotaNavigator {
    static final String tag = "yota.navigator";

    static String[] RateNames = null;

    static private String LoginUrl  = "https://my.yota.ru/login";
    static private String RateUrl   = "https://my.yota.ru/selfcare/devices";
    static private String DirectUrl = "https://my.yota.ru/selfcare/mydevices";

    static private String[] MoneyIsOverPeriods = {"0 дней", "0 days"}; // надо найти не зависящий от языка указатель на то, что деньги закончились

    static private String incScript       = "$('.increase a').click()";
    static private String decScript       = "$('.decrease a').click()";
    static private String commitScript    = "$('.tarriff-info a').click()";
    static private String setValueScript  = "$('.slider-container .slider-inner').yotaSlider('value', !val);";
    static private String manageRateScript =
            "var period= $('.tariff-choice-form input[name=\"period\"]')[0].getAttribute('value');" +
            "var rateNumber = $('.slider-container .slider-inner').yotaSlider('value');" +
            "window.injectedObject.ManageRate(rateNumber, period);";
    static private String setRateIfConnectedScript =
            "$.ajax({" +
                "type: 'POST'," +
                "url: '/devices/testDriveStatus'," +
                "data: {}," +
                "success: function(msg){" +
                    "window.injectedObject.SetRateOnTestConnectionSuccess();" +
                "}," +
                "error: function(XMLHttpRequest, textStatus, errorThrown) {" +
                    "window.injectedObject.OnTestConnectionError();" +
                "}" +
            "});";
    static private String getRateNamesThenManageRateScript =
            "var product = $('.tariff-choice-form input[name=\"product\"]')[0].getAttribute('value');" +
            "var steps = sliderData[product]['steps'];" +
            "window.injectedObject.SetRateCount(steps.length);" +
            "for (var i = 0; i < steps.length; ++i){" +
                "var rateName = steps[i]['speedNumber'] + ' ' + steps[i]['speedString'] + ' за ' + steps[i]['amountNumber'] + ' ' + steps[i]['amountString'];" +
                "window.injectedObject.SetRateName(i, rateName);" +
            "}" +
            "window.injectedObject.AfterRateDefined();";
    static private String loginScript =
            "$('input[id=\"username\"]')[0].setAttribute('value', '!lgn');" +
            "$('input[name=\"IDToken3\"]')[1].setAttribute('value', '!pas');" +
            "$('button[type=\"submit\"]')[0].click();";


    private WebView _webBrowser;
    private int _needRateNum = -1;
    private String _login;
    private String _password;
    private boolean _useDirectReference;
    private YotaNavigatorInterface _externalInterface;
    private boolean _isConnected = true;
    private boolean _wasError = false;
    private int freeAmount = 0; // сколько раз был выбран бесплатный тариф

    public YotaNavigator(WebView webBrowser, String login, String password, boolean useDirectReference, YotaNavigatorInterface externalInterface)
    {
        _login = login;
        _password = password;
        _useDirectReference = useDirectReference;
        _webBrowser = webBrowser;
        _externalInterface = externalInterface;
        WebSettings webSettings = _webBrowser.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadsImagesAutomatically(false);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setSupportZoom(false);
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        _webBrowser.setWebViewClient(webViewClient);

        class JsObject {
            @JavascriptInterface
            public void ManageRate(int curRateNumber, String period) {
                Log.d(tag, "rate and period determed" + " | " + RateNames[curRateNumber] + " |" + period);
                for (int i = 0; i < MoneyIsOverPeriods.length; ++i)
                    if (MoneyIsOverPeriods[i].equals(period)){
                        Log.d(tag, "period end");
                        _externalInterface.OnPeriodDefined(period);
                        Error(R.string.error_money_is_over, R.string.error_money_is_over_title);
                        return;
                    }
                _isConnected = true;
                // _needRateNym == 0 не будет никогда
                if (_needRateNum != 0)
                    freeAmount = 0;
                if ((_needRateNum > 0 && _needRateNum < RateNames.length && _needRateNum != curRateNumber) || (_needRateNum == 0 && freeAmount++ == 0)){
                    String script = setValueScript.replaceFirst("!val", String.valueOf(_needRateNum));
                    RunJavaScript(script);
                    RunJavaScript(commitScript);
                }
                else {
                    _externalInterface.OnPeriodDefined(period);
                    _externalInterface.OnRateDefined(curRateNumber);
                }
            }
            @JavascriptInterface
            public void SetRateOnTestConnectionSuccess(){
                Log.d(tag, "connection exist");
                RunJavaScript(manageRateScript);
            }
            @JavascriptInterface
            public void OnTestConnectionError(){
                Log.d(tag, "connection not exist");
                GoYota();
            }
            @JavascriptInterface
            public void SetRateCount(int count){
                Log.d(tag, "set rate count " + count);
                RateNames = new String[count];
            }
            @JavascriptInterface
            public void SetRateName(int num, String name){
                name = RateNamesProcessing(name);
                Log.d(tag, "set rate name " + num + "|" + name);
                RateNames[num] = name;
            }
            @JavascriptInterface
            public void AfterRateDefined(){
                _externalInterface.OnRateNamesDefined();
                Log.d(tag, (RateNames==null)?"null":RateNames.length+RateNames[0]);
                RunJavaScript(manageRateScript);
            }
        }
        _webBrowser.addJavascriptInterface(new JsObject(), "injectedObject");
    }

    private String RateNamesProcessing(String name){
        name = name.replaceAll("<div[^>]*>", "");
        name = name.replaceAll("</div[^>]*>", "");
        name = name.replaceAll(" \\(макс.\\)", "");
        return name;
    }

    public void GoYota(){
        _wasError = false;
        if (!_useDirectReference && (!TestLoginOrPassword(_login) || !TestLoginOrPassword(_password))){
            Error(R.string.error_incorrect_login, R.string.error_incorrect_login_title);
            return;
        }
        _isConnected = false;
        _externalInterface.OnStartConnection();
        if (_useDirectReference){
            Log.d(tag, "directUrl");
            _webBrowser.loadUrl(DirectUrl);
        }
        else{
            Log.d(tag, "rateUrl");
            _webBrowser.loadUrl(RateUrl);
        }
    }

    public void SetRate(int num){
        if (!_wasError)
            _externalInterface.OnRateSetupStart(num);
        _needRateNum = num;
        if (_wasError){
            Log.d(tag, "not direct set url");
            GoYota();
        }
        else if (_isConnected){
            Log.d(tag, "direct set url");
            RunJavaScript(setRateIfConnectedScript);
        }
    }

    private void RunJavaScript(String script){
        _webBrowser.loadUrl("javascript:" + script);
    }

    public Boolean TestLoginOrPassword(String word){
        if (word.equals(""))
            return false;
        if (word.contains(" "))
            return false;
        return true;
    }

    public void SetLogin(String login){
        _login = login;
    }

    public void SetPassword(String password){
        _password = password;
    }

    public void SetUseDirect(boolean useDirectReference){
        _useDirectReference = useDirectReference;
    }

    private void Error(String message, String title){
        _wasError = true;
        _externalInterface.OnError(message, title);
    }

    private void Error(int messageId, int titleId){
        _wasError = true;
        _externalInterface.OnError(messageId, titleId);
    }

    private WebViewClient webViewClient = new WebViewClient()
    {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (url.contains("loginError"))
            {
                Log.d(tag, "loginError");
                Error(R.string.error_incorrect_login, R.string.error_incorrect_login_title);
                return;
            }
            if (url.contains("error"))
            {
                Error("Превышено допустимое количество запросов с одного IP адреса", "");
                return;
            }
            if (url.contains("login"))
            {
                Log.d(tag, "login");
                if (!TestLoginOrPassword(_login) || !TestLoginOrPassword(_password)){
                    Error(R.string.error_incorrect_login, R.string.error_incorrect_login_title);
                    return;
                }
                try
                {
                    String loginBuf = _login.replaceAll("'", "\\'");
                    String passwordBuf = _password.replaceAll("'", "\\'");
                    String script = loginScript.replaceFirst("!lgn", loginBuf);
                    script = script.replaceFirst("!pas", passwordBuf);
                    RunJavaScript(script);
                }
                catch(Exception e)
                {
                    Error("Неизвестная ошибка доступа к личному кабинету Yota. Возможно, отсутствует соединение с сетью.", "");
                }
                return;
            }
            if (url.contains("profile"))
            {
                _webBrowser.loadUrl(RateUrl);
                return;
            }
            if (!url.contains("device")){
                Error("Неизвестная ошибка доступа к личному кабинету Yota. Возможно, отсутствует соединение с сетью.", "");
            }
            _externalInterface.OnPageFinished();
            if (RateNames == null)
                RunJavaScript(getRateNamesThenManageRateScript);
            else RunJavaScript(manageRateScript);
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.d(tag, "onReceiveError");
            _needRateNum = -1;
            Error(description, "");
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.d(tag, "onReceiveSSLError");
            _needRateNum = -1;
            Error("ssl error", "");
            super.onReceivedSslError(view, handler, error);
        }
    };

    public interface YotaNavigatorInterface {
        public void OnPeriodDefined(String period);
        // тариф считан или установлен новый
        public void OnRateDefined(int rateNumber);
        public void OnStartConnection();
        public void OnPageFinished();
        public void OnError(String message, String title);
        public void OnError(int messageId, int titleId);
        public void OnRateSetupStart(int rateNumber);
        public void OnRateNamesDefined();
    }
}