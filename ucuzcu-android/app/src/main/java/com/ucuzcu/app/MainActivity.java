package com.ucuzcu.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int GREEN = Color.rgb(16, 97, 62);
    private static final int TEXT = Color.rgb(35, 43, 39);
    private static final int MUTED = Color.rgb(99, 110, 104);
    private static final int BG = Color.rgb(247, 249, 248);

    private EditText searchInput;
    private Button searchButton;
    private TextView statusText;
    private LinearLayout resultsContainer;
    private WebView webView;
    private ProgressBar progressBar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int searchId = 0;
    private int stage = 0;
    private String currentQuery = "";
    private String sourceSearchUrl = "";
    private String selectedProductUrl = "";

    private static class Offer {
        double price;
        String priceText;
        String seller;
        String detail;
        String url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(GREEN);
        buildUi();
        prepareWebView();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(36));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("UCUZCU");
        title.setTextSize(36);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(GREEN);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, fullWidth(dp(58)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Ürünü yaz. Ucuzcu fiyatları tarayıp en ucuz seçenekleri sıralasın.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(74, 88, 80));
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subtitleParams = fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.setMargins(0, dp(4), 0, dp(20));
        root.addView(subtitle, subtitleParams);

        searchInput = new EditText(this);
        searchInput.setHint("Örn: Samsung S26 256 GB siyah");
        searchInput.setTextSize(17);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams inputParams = fullWidth(dp(60));
        inputParams.setMargins(0, 0, 0, dp(12));
        root.addView(searchInput, inputParams);

        searchButton = new Button(this);
        searchButton.setText("EN UCUZ 10'U BUL");
        searchButton.setTextSize(16);
        searchButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        searchButton.setTextColor(Color.WHITE);
        searchButton.setBackgroundColor(GREEN);
        searchButton.setOnClickListener(v -> startSearch());
        root.addView(searchButton, fullWidth(dp(58)));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusRowParams = fullWidth(dp(54));
        statusRowParams.setMargins(0, dp(12), 0, dp(4));
        root.addView(statusRow, statusRowParams);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        statusRow.addView(progressBar, new LinearLayout.LayoutParams(dp(34), dp(34)));

        statusText = new TextView(this);
        statusText.setText("Hazır. Bir ürün yazıp aramayı başlat.");
        statusText.setTextSize(13);
        statusText.setTextColor(MUTED);
        statusText.setPadding(dp(8), 0, 0, 0);
        statusRow.addView(statusText, new LinearLayout.LayoutParams(0, dp(54), 1f));

        TextView info = new TextView(this);
        info.setText("V2 Beta • İlk otomatik fiyat motoru Akakçe üzerindeki güncel satıcı tekliflerini okumayı dener. Sayfa yapısı değişirse sonuç alınamayabilir.");
        info.setTextSize(12);
        info.setTextColor(Color.rgb(117, 126, 121));
        LinearLayout.LayoutParams infoParams = fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        infoParams.setMargins(0, 0, 0, dp(12));
        root.addView(info, infoParams);

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsContainer, fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        webView.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(dp(1), dp(1));
        root.addView(webView, webParams);

        showWelcome();
        setContentView(scrollView);
    }

    private void prepareWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(false);
        settings.setBlockNetworkImage(true);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0 Mobile Safari/537.36 Ucuzcu/0.2");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                final int id = searchId;
                handler.postDelayed(() -> {
                    if (id != searchId) return;
                    if (stage == 1) {
                        findProductPage();
                    } else if (stage == 2) {
                        extractOffers();
                    }
                }, 900);
            }
        });
    }

    private void showWelcome() {
        resultsContainer.removeAllViews();
        TextView ready = new TextView(this);
        ready.setText("🔎 Ürün adını mümkün olduğunca net yaz.\nÖrnek: Samsung Galaxy S26 256 GB Siyah");
        ready.setTextSize(15);
        ready.setTextColor(Color.rgb(62, 72, 67));
        ready.setGravity(Gravity.CENTER_HORIZONTAL);
        ready.setPadding(0, dp(24), 0, dp(8));
        resultsContainer.addView(ready, fullWidth(dp(90)));
    }

    private void startSearch() {
        String query = searchInput.getText().toString().trim();
        if (query.length() < 3) {
            searchInput.setError("Aramak istediğin ürünü biraz daha açık yaz.");
            searchInput.requestFocus();
            return;
        }

        currentQuery = query;
        sourceSearchUrl = "https://www.akakce.com/arama/?q=" + Uri.encode(query);
        selectedProductUrl = "";
        stage = 1;
        searchId++;
        final int id = searchId;

        resultsContainer.removeAllViews();
        setSearching(true, "Ürün bulunuyor…");
        webView.loadUrl(sourceSearchUrl);

        handler.postDelayed(() -> {
            if (id == searchId && stage != 0) {
                showFailure("Tarama zaman aşımına uğradı.");
            }
        }, 25000);
    }

    private void findProductPage() {
        if (stage != 1) return;

        String safeQuery = JSONObject.quote(currentQuery);
        String script = "(function(){" +
                "function n(s){return (s||'').toLocaleLowerCase('tr-TR').replace(/\\s+/g,' ').trim();}" +
                "var q=n(" + safeQuery + ");" +
                "var tokens=q.split(' ').filter(function(x){return x.length>1;});" +
                "var best=null,bestScore=-1;" +
                "Array.prototype.slice.call(document.querySelectorAll('a[href]')).forEach(function(a){" +
                "var href=a.href||''; if(href.indexOf('akakce.com')<0) return;" +
                "if(href.indexOf('fiyati')<0 && href.indexOf('en-ucuz')<0) return;" +
                "var t=n((a.innerText||'')+' '+((a.parentElement&&a.parentElement.innerText)||''));" +
                "if(t.length<3) return; var score=0; tokens.forEach(function(tok){if(t.indexOf(tok)>=0) score+=2;});" +
                "if(t.indexOf(q)>=0) score+=8; if(href.indexOf('arama')>=0) score-=10;" +
                "if(score>bestScore){bestScore=score;best=href;}" +
                "});" +
                "return bestScore>1?best:null;" +
                "})()";

        final int id = searchId;
        webView.evaluateJavascript(script, value -> {
            if (id != searchId || stage != 1) return;
            String url = decodeJsString(value);
            if (url == null || url.length() < 8) {
                extractSearchPageResults();
                return;
            }
            selectedProductUrl = url;
            stage = 2;
            setSearching(true, "Satıcı fiyatları taranıyor…");
            webView.loadUrl(url);
        });
    }

    private void extractSearchPageResults() {
        if (stage != 1) return;

        String script = "(function(){" +
                "function c(s){return (s||'').replace(/\\s+/g,' ').trim();}" +
                "function p(s){var m=s.match(/(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)\\s*TL/i);if(!m)return null;" +
                "return {txt:m[1]+' TL',num:parseFloat(m[1].replace(/\\./g,'').replace(',','.'))};}" +
                "var out=[],seen={};" +
                "Array.prototype.slice.call(document.querySelectorAll('a[href]')).forEach(function(a){" +
                "var card=a.closest('li,article,section,div'); if(!card)return; var t=c(card.innerText);" +
                "if(t.length<10||t.length>450||t.indexOf('TL')<0)return; var pr=p(t);if(!pr||pr.num<10)return;" +
                "var href=a.href||''; if(href.indexOf('akakce.com')<0)return;" +
                "var key=pr.txt+'|'+href;if(seen[key])return;seen[key]=1;" +
                "out.push({price:pr.num,priceText:pr.txt,seller:'Ürün sonucu',detail:t.substring(0,170),url:href});" +
                "});" +
                "out.sort(function(a,b){return a.price-b.price;});return JSON.stringify(out.slice(0,10));" +
                "})()";

        final int id = searchId;
        webView.evaluateJavascript(script, value -> {
            if (id != searchId || stage != 1) return;
            List<Offer> offers = parseOffers(decodeJsString(value));
            if (offers.isEmpty()) {
                showFailure("Ürün sayfası otomatik seçilemedi.");
            } else {
                stage = 0;
                showOffers(offers, "Arama sonuçları");
            }
        });
    }

    private void extractOffers() {
        if (stage != 2) return;

        String script = "(function(){" +
                "function c(s){return (s||'').replace(/\\s+/g,' ').trim();}" +
                "function price(s){var ms=s.match(/(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)\\s*TL/ig);if(!ms||!ms.length)return null;" +
                "for(var i=0;i<ms.length;i++){var raw=ms[i].replace(/\\s*TL/i,'');var num=parseFloat(raw.replace(/\\./g,'').replace(',','.'));" +
                "if(num>=10&&num<=10000000)return {txt:raw+' TL',num:num};}return null;}" +
                "var title=c((document.querySelector('h1')||{}).innerText)||'Ürün';var out=[],seen={};" +
                "Array.prototype.slice.call(document.querySelectorAll('li,article,section,div')).forEach(function(card){" +
                "var t=c(card.innerText);if(t.length<20||t.length>520||t.indexOf('TL')<0)return;" +
                "var count=(t.match(/Satıcıya Git/gi)||[]).length;if(count!==1)return;var pr=price(t);if(!pr)return;" +
                "var links=Array.prototype.slice.call(card.querySelectorAll('a[href]'));" +
                "var go=links.find(function(a){return /Satıcıya Git/i.test(c(a.innerText));})||links.find(function(a){return (a.href||'').indexOf('http')===0;});" +
                "var href=go?go.href:location.href;var seller='Satıcı';" +
                "var sm=t.match(/Satıcı:\\s*([^|]{2,70})/i);if(sm)seller=c(sm[1]).substring(0,70);" +
                "if(seller==='Satıcı'){var alts=Array.prototype.slice.call(card.querySelectorAll('img[alt]')).map(function(i){return c(i.alt);}).filter(function(x){return x.length>1&&x.length<60;});if(alts.length)seller=alts[0];}" +
                "if(seller==='Satıcı'){var slash=t.lastIndexOf('/');if(slash>=0&&t.length-slash<75)seller=c(t.substring(slash+1));}" +
                "var key=pr.txt+'|'+seller+'|'+href;if(seen[key])return;seen[key]=1;" +
                "out.push({price:pr.num,priceText:pr.txt,seller:seller,detail:t.substring(0,220),url:href});" +
                "});" +
                "out.sort(function(a,b){return a.price-b.price;});return JSON.stringify({title:title,offers:out.slice(0,10)});" +
                "})()";

        final int id = searchId;
        webView.evaluateJavascript(script, value -> {
            if (id != searchId || stage != 2) return;
            String decoded = decodeJsString(value);
            try {
                JSONObject obj = new JSONObject(decoded == null ? "{}" : decoded);
                String title = obj.optString("title", currentQuery);
                List<Offer> offers = parseOffers(obj.optJSONArray("offers"));
                if (offers.isEmpty()) {
                    showFailure("Satıcı fiyatları sayfadan okunamadı.");
                    return;
                }
                stage = 0;
                showOffers(offers, title);
            } catch (Exception e) {
                showFailure("Fiyat verisi işlenemedi.");
            }
        });
    }

    private List<Offer> parseOffers(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            return parseOffers(new JSONArray(json));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Offer> parseOffers(JSONArray array) {
        List<Offer> list = new ArrayList<>();
        if (array == null) return list;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            double price = o.optDouble("price", -1);
            String priceText = o.optString("priceText", "").trim();
            String seller = o.optString("seller", "Satıcı").trim();
            String detail = o.optString("detail", "").trim();
            String url = o.optString("url", selectedProductUrl).trim();
            if (price <= 0 || priceText.isEmpty()) continue;
            String key = String.format(Locale.US, "%.2f|%s|%s", price, seller, url);
            if (!seen.add(key)) continue;
            Offer offer = new Offer();
            offer.price = price;
            offer.priceText = priceText;
            offer.seller = seller.isEmpty() ? "Satıcı" : seller;
            offer.detail = detail;
            offer.url = url.isEmpty() ? selectedProductUrl : url;
            list.add(offer);
        }
        Collections.sort(list, Comparator.comparingDouble(a -> a.price));
        if (list.size() > 10) return new ArrayList<>(list.subList(0, 10));
        return list;
    }

    private void showOffers(List<Offer> offers, String productTitle) {
        setSearching(false, offers.size() + " fiyat bulundu • En ucuzdan pahalıya sıralandı");
        resultsContainer.removeAllViews();

        TextView product = new TextView(this);
        product.setText(productTitle);
        product.setTextSize(19);
        product.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        product.setTextColor(TEXT);
        LinearLayout.LayoutParams productParams = fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        productParams.setMargins(0, dp(8), 0, dp(4));
        resultsContainer.addView(product, productParams);

        TextView source = new TextView(this);
        source.setText("Kaynak: Akakçe • Fiyatlar sayfada görünen tekliflerden alınır");
        source.setTextSize(12);
        source.setTextColor(MUTED);
        LinearLayout.LayoutParams sourceParams = fullWidth(dp(34));
        sourceParams.setMargins(0, 0, 0, dp(5));
        resultsContainer.addView(source, sourceParams);

        for (int i = 0; i < offers.size(); i++) {
            addOfferCard(i, offers.get(i));
        }
    }

    private void addOfferCard(int index, Offer offer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackgroundColor(Color.WHITE);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top, fullWidth(dp(46)));

        TextView rank = new TextView(this);
        rank.setText(index == 0 ? "🥇" : (index + 1) + ".");
        rank.setTextSize(index == 0 ? 22 : 18);
        rank.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        rank.setTextColor(TEXT);
        top.addView(rank, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView seller = new TextView(this);
        seller.setText(offer.seller);
        seller.setTextSize(15);
        seller.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        seller.setTextColor(TEXT);
        top.addView(seller, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView price = new TextView(this);
        price.setText(offer.priceText);
        price.setTextSize(18);
        price.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        price.setTextColor(GREEN);
        price.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(price, new LinearLayout.LayoutParams(dp(142), dp(46)));

        if (!offer.detail.isEmpty()) {
            TextView detail = new TextView(this);
            detail.setText(shortenDetail(offer.detail));
            detail.setTextSize(12);
            detail.setTextColor(MUTED);
            detail.setMaxLines(2);
            LinearLayout.LayoutParams detailParams = fullWidth(dp(44));
            detailParams.setMargins(0, dp(2), 0, dp(7));
            card.addView(detail, detailParams);
        }

        Button go = new Button(this);
        go.setText("ÜRÜNE GİT");
        go.setTextColor(Color.WHITE);
        go.setTextSize(13);
        go.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        go.setBackgroundColor(GREEN);
        go.setOnClickListener(v -> openUrl(offer.url));
        card.addView(go, fullWidth(dp(48)));

        LinearLayout.LayoutParams cardParams = fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(7), 0, 0);
        resultsContainer.addView(card, cardParams);
    }

    private String shortenDetail(String detail) {
        String d = detail.replaceAll("\\s+", " ").trim();
        if (d.length() > 180) d = d.substring(0, 180) + "…";
        return d;
    }

    private void showFailure(String reason) {
        stage = 0;
        setSearching(false, reason);
        resultsContainer.removeAllViews();

        TextView message = new TextView(this);
        message.setText("Bu aramada otomatik fiyat listesi alınamadı. Bu genelde kaynak sayfanın yapısı değiştiğinde veya bağlantı taramayı engellediğinde olur.");
        message.setTextSize(14);
        message.setTextColor(TEXT);
        message.setPadding(0, dp(12), 0, dp(10));
        resultsContainer.addView(message, fullWidth(dp(92)));

        Button openSearch = new Button(this);
        openSearch.setText("AKAKÇE ARAMASINI AÇ");
        openSearch.setTextColor(Color.WHITE);
        openSearch.setBackgroundColor(GREEN);
        openSearch.setOnClickListener(v -> openUrl(sourceSearchUrl));
        resultsContainer.addView(openSearch, fullWidth(dp(52)));
    }

    private void setSearching(boolean searching, String text) {
        progressBar.setVisibility(searching ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!searching);
        searchButton.setAlpha(searching ? 0.65f : 1f);
        statusText.setText(text);
    }

    private String decodeJsString(String value) {
        if (value == null || "null".equals(value) || "undefined".equals(value)) return null;
        try {
            JSONArray wrapper = new JSONArray("[" + value + "]");
            if (wrapper.isNull(0)) return null;
            return wrapper.optString(0, null);
        } catch (Exception e) {
            return null;
        }
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Bu bağlantıyı açacak tarayıcı bulunamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout.LayoutParams fullWidth(int height) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        searchId++;
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
