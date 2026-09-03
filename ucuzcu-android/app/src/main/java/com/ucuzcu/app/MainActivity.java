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

    private final String[][] sources = new String[][]{
            {"Akakçe", "https://www.akakce.com/arama/?q="},
            {"Cimri", "https://www.cimri.com/arama?q="},
            {"Trendyol", "https://www.trendyol.com/sr?q="},
            {"Hepsiburada", "https://www.hepsiburada.com/ara?q="},
            {"N11", "https://www.n11.com/arama?q="},
            {"Amazon Türkiye", "https://www.amazon.com.tr/s?k="}
    };

    private EditText searchInput;
    private Button searchButton;
    private TextView statusText;
    private LinearLayout resultsContainer;
    private WebView webView;
    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<Offer> collected = new ArrayList<>();
    private int searchId = 0;
    private int sourceIndex = 0;
    private boolean scanning = false;
    private boolean extracting = false;
    private String currentQuery = "";

    private static class Offer {
        double price;
        String priceText;
        String source;
        String title;
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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("UCUZCU", 36, GREEN, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, fullWidth(dp(58)));

        TextView subtitle = text("Ne arıyorsan yaz. Ucuzcu farklı kaynakları tarayıp en ucuz seçenekleri bulsun.", 16, Color.rgb(74, 88, 80), false);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams sp = fullWidth(-2);
        sp.setMargins(0, dp(4), 0, dp(20));
        root.addView(subtitle, sp);

        searchInput = new EditText(this);
        searchInput.setHint("Örn: Bosch matkap, Nike Air Max 42, Samsung S26");
        searchInput.setTextSize(17);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams ip = fullWidth(dp(60));
        ip.setMargins(0, 0, 0, dp(12));
        root.addView(searchInput, ip);

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
        LinearLayout.LayoutParams srp = fullWidth(dp(58));
        srp.setMargins(0, dp(12), 0, dp(4));
        root.addView(statusRow, srp);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        statusRow.addView(progressBar, new LinearLayout.LayoutParams(dp(34), dp(34)));

        statusText = text("Hazır. Aradığın ürünü yaz.", 13, MUTED, false);
        statusText.setPadding(dp(8), 0, 0, 0);
        statusRow.addView(statusText, new LinearLayout.LayoutParams(0, dp(58), 1f));

        TextView info = text("V5 Beta • Gerçek ürün fiyatı doğrulanır; kupon, indirim tutarı, taksit, beden ve benzeri yan rakamlar fiyat kabul edilmez.", 12, Color.rgb(117, 126, 121), false);
        LinearLayout.LayoutParams infop = fullWidth(-2);
        infop.setMargins(0, 0, 0, dp(12));
        root.addView(info, infop);

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsContainer, fullWidth(-2));

        webView = new WebView(this);
        webView.setVisibility(View.INVISIBLE);
        root.addView(webView, new LinearLayout.LayoutParams(dp(1), dp(1)));

        showWelcome();
        setContentView(scroll);
    }

    private void prepareWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadsImagesAutomatically(false);
        s.setBlockNetworkImage(true);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0 Mobile Safari/537.36 Ucuzcu/0.5");

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!scanning || extracting || sourceIndex >= sources.length) return;
                final int id = searchId;
                final int idx = sourceIndex;
                extracting = true;
                handler.postDelayed(() -> {
                    if (id == searchId && scanning && idx == sourceIndex) extractCurrentSource();
                }, 1300);
            }
        });
    }

    private void showWelcome() {
        resultsContainer.removeAllViews();
        TextView t = text("🔎 İstediğin ürünü normal şekilde yaz.\nÖrnek: 205 55 R16 lastik • A4 kağıt 80 gr • Philips kahve makinesi", 15, Color.rgb(62, 72, 67), false);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(0, dp(24), 0, dp(8));
        resultsContainer.addView(t, fullWidth(dp(100)));
    }

    private void startSearch() {
        String q = searchInput.getText().toString().trim();
        if (q.length() < 2) {
            searchInput.setError("Aramak istediğin ürünü yaz.");
            searchInput.requestFocus();
            return;
        }

        currentQuery = q;
        collected.clear();
        sourceIndex = 0;
        searchId++;
        scanning = true;
        extracting = false;
        resultsContainer.removeAllViews();
        setSearching(true, "Kaynaklar hazırlanıyor…");
        loadCurrentSource();
    }

    private void loadCurrentSource() {
        if (!scanning) return;
        if (sourceIndex >= sources.length) {
            finishSearch();
            return;
        }

        extracting = false;
        String source = sources[sourceIndex][0];
        String url = sources[sourceIndex][1] + Uri.encode(currentQuery);
        setSearching(true, (sourceIndex + 1) + "/" + sources.length + " • " + source + " taranıyor…");
        final int id = searchId;
        final int idx = sourceIndex;
        webView.stopLoading();
        webView.loadUrl(url);

        handler.postDelayed(() -> {
            if (id == searchId && scanning && idx == sourceIndex) {
                sourceIndex++;
                extracting = false;
                loadCurrentSource();
            }
        }, 9000);
    }

    private void extractCurrentSource() {
        if (!scanning || sourceIndex >= sources.length) return;
        final int id = searchId;
        final int idx = sourceIndex;
        final String sourceName = sources[idx][0];
        String q = JSONObject.quote(currentQuery);
        String source = JSONObject.quote(sourceName);

        String script = "(function(){" +
                "function n(s){return (s||'').toLocaleLowerCase('tr-TR').replace(/[\\-_\\/]+/g,' ').replace(/[^a-z0-9çğıöşü ]/gi,' ').replace(/\\s+/g,' ').trim();}" +
                "function clean(s){return (s||'').replace(/\\s+/g,' ').trim();}" +
                "function number(raw){if(raw==null)return null;var s=(''+raw).replace(/\\s/g,'').replace(/TL|₺/gi,'');if(!s)return null;var v;if(s.indexOf(',')>=0)v=parseFloat(s.replace(/\\./g,'').replace(',','.'));else if((s.match(/\\./g)||[]).length>1)v=parseFloat(s.replace(/\\./g,''));else if(/^\\d{1,3}\\.\\d{3}$/.test(s))v=parseFloat(s.replace('.',''));else v=parseFloat(s);return isFinite(v)&&v>0&&v<100000000?v:null;}" +
                "function moneyText(s){s=clean(s);var re=/(?:₺\\s*)?(\\d{1,3}(?:[.\\s]\\d{3})*(?:,\\d{2})?|\\d{1,9}(?:[.,]\\d{2})?)\\s*(?:TL|₺)/ig,m,out=[];while((m=re.exec(s))!==null){var v=number(m[1]);if(v)out.push({num:v,txt:v.toLocaleString('tr-TR',{minimumFractionDigits:2,maximumFractionDigits:2})+' TL',index:m.index,raw:m[0]});}return out;}" +
                "function bad(s){return /kupon|indirim tutar|indirim kazan|kazan|taksit|ayda|puan|bonus|beden|numara|\\bkişi\\b|\\bgünde\\b|\\bgün\\b|son 30|en düşük fiyat|kargo bedel|teslimat ücret/i.test(s||'');}" +
                "function cls(e){return clean(((e&&e.className)||'')+' '+((e&&e.id)||'')+' '+((e&&e.getAttribute&&e.getAttribute('data-testid'))||'')+' '+((e&&e.getAttribute&&e.getAttribute('data-test-id'))||''));}" +
                "function priceOf(card){var cand=[];function add(e,bonus){if(!e)return;var raw=clean((e.getAttribute&&e.getAttribute('content'))||'');var txt=clean((e.innerText||e.textContent||'')+' '+((e.getAttribute&&e.getAttribute('aria-label'))||''));var vals=moneyText(txt);if(!vals.length&&raw){var rv=number(raw);if(rv)vals=[{num:rv,txt:rv.toLocaleString('tr-TR',{minimumFractionDigits:2,maximumFractionDigits:2})+' TL',index:0,raw:raw}];}for(var z=0;z<vals.length;z++){var meta=cls(e).toLowerCase();var score=bonus||0;score+=/price|fiyat|prc|amount|current/.test(meta)?35:0;score+=/dscntd|discounted|sale|selling|current|final|newprice|current-price/.test(meta)?35:0;score+=/old|original|strike|list-price|before/.test(meta)?-25:0;score+=(txt.indexOf('TL')>=0||txt.indexOf('₺')>=0)?15:0;score+=txt.length<45?10:0;var parent=e.parentElement;var ctx=txt;if(parent){var pt=clean(parent.innerText);if(pt.length<130)ctx+=' '+pt;}if(bad(ctx))score-=100;if(/coupon|kupon|installment|taksit|badge|saving/.test(meta))score-=100;cand.push({num:vals[z].num,txt:vals[z].txt,score:score});}}" +
                "var special='';var src=" + source + ";if(src==='Trendyol')special='.prc-box-dscntd,.prc-box-sllng,[class*=prc-box]';else if(src==='Hepsiburada')special='[data-test-id*=price],[data-testid*=price]';else if(src==='Amazon Türkiye')special='.a-price .a-offscreen,.a-price';else if(src==='N11')special='.newPrice ins,.newPrice,[class*=price]';else special='[class*=price],[class*=Price],[data-testid*=price],[data-test*=price]';" +
                "if(special)Array.prototype.slice.call(card.querySelectorAll(special)).slice(0,30).forEach(function(e){add(e,45);});" +
                "Array.prototype.slice.call(card.querySelectorAll('[itemprop=price],[class*=price],[class*=Price],[class*=fiyat],[class*=Fiyat],[data-testid*=price],[data-test*=price]')).slice(0,40).forEach(function(e){add(e,25);});" +
                "if(!cand.length){var full=clean(card.innerText);var vals=moneyText(full);for(var i=0;i<vals.length;i++){var a=Math.max(0,vals[i].index-45),b=Math.min(full.length,vals[i].index+vals[i].raw.length+45),ctx=full.substring(a,b);if(!bad(ctx))cand.push({num:vals[i].num,txt:vals[i].txt,score:5});}}" +
                "cand=cand.filter(function(x){return x.score>-20;});cand.sort(function(a,b){return b.score-a.score||(a.num-b.num);});return cand.length?cand[0]:null;}" +
                "var raw=n(" + q + ");var toks=raw.split(' ').filter(function(x){return x.length>0;});" +
                "var stop={'en':1,'ucuz':1,'fiyat':1,'fiyati':1,'fiyatı':1,'urun':1,'ürün':1,'satın':1,'al':1,'yeni':1,'orijinal':1};" +
                "var req=toks.filter(function(x){return !stop[x];});if(!req.length)req=toks;" +
                "function match(txt){var h=n(txt);if(!h)return false;var hits=0,words=0;for(var i=0;i<req.length;i++){var tok=req[i],has=h.indexOf(tok)>=0;if(/[0-9]/.test(tok)&&!has)return false;if(!/[0-9]/.test(tok)){words++;if(has)hits++;}}var need=words<=1?Math.min(words,1):Math.ceil(words*0.6);return hits>=need;}" +
                "var selector='article,li,[data-testid*=product],[data-test*=product],[class*=product],[class*=Product],[class*=prd],[class*=p-card],[class*=search-result],[class*=searchResult],[class*=s-result-item]';" +
                "var nodes=Array.prototype.slice.call(document.querySelectorAll(selector));if(nodes.length>1200)nodes=nodes.slice(0,1200);var out=[],seen={};" +
                "nodes.forEach(function(card){var txt=clean(card.innerText);if(txt.length<8||txt.length>1600)return;var te=card.querySelector('h1,h2,h3,h4,[data-testid*=title],[class*=title],[class*=Title],[class*=name],[class*=Name]');var title=clean(te?te.innerText:'');" +
                "var links=Array.prototype.slice.call(card.querySelectorAll('a[href]'));if(!title){for(var a=0;a<links.length;a++){var at=clean(links[a].innerText);if(at.length>4&&at.length<220){title=at;break;}}}if(!title)title=txt.substring(0,180);if(!match(title+' '+txt.substring(0,500)))return;" +
                "var pr=priceOf(card);if(!pr)return;var href='';for(var j=0;j<links.length;j++){var u=links[j].href||'';if(/^https?:/i.test(u)){href=u;break;}}if(!href)href=location.href;" +
                "var key=n(title).substring(0,100)+'|'+Math.round(pr.num*100)+'|'+href;if(seen[key])return;seen[key]=1;out.push({price:pr.num,priceText:pr.txt,source:" + source + ",title:title.substring(0,180),detail:txt.substring(0,260),url:href});" +
                "});out.sort(function(a,b){return a.price-b.price;});return JSON.stringify(out.slice(0,8));})()";

        webView.evaluateJavascript(script, value -> {
            if (id != searchId || !scanning || idx != sourceIndex) return;
            String decoded = decodeJsString(value);
            List<Offer> found = parseOffers(decoded);
            collected.addAll(found);
            sourceIndex++;
            extracting = false;
            loadCurrentSource();
        });
    }

    private List<Offer> parseOffers(String json) {
        List<Offer> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                Offer offer = new Offer();
                offer.price = o.optDouble("price", -1);
                offer.priceText = o.optString("priceText", "").trim();
                offer.source = o.optString("source", "Kaynak").trim();
                offer.title = o.optString("title", "Ürün").trim();
                offer.detail = o.optString("detail", "").trim();
                offer.url = o.optString("url", "").trim();
                if (offer.price > 0 && !offer.priceText.isEmpty() && !offer.url.isEmpty()) list.add(offer);
            }
        } catch (Exception ignored) { }
        return list;
    }

    private void finishSearch() {
        scanning = false;
        extracting = false;
        List<Offer> finalList = cleanAndSort(collected);
        if (finalList.isEmpty()) {
            setSearching(false, "Otomatik fiyat sonucu bulunamadı.");
            showNoResults();
            return;
        }
        if (finalList.size() > 10) finalList = new ArrayList<>(finalList.subList(0, 10));
        setSearching(false, finalList.size() + " doğrulanmış fiyat bulundu • En ucuzdan pahalıya");
        showOffers(finalList);
    }

    private List<Offer> cleanAndSort(List<Offer> input) {
        List<Offer> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Offer o : input) {
            if (o == null || o.price <= 0 || !isLikelyMatch(currentQuery, o.title + " " + o.detail)) continue;
            String key = normalize(o.title) + "|" + Math.round(o.price) + "|" + o.source;
            if (!seen.add(key)) continue;
            out.add(o);
        }
        Collections.sort(out, Comparator.comparingDouble(a -> a.price));
        return removeExtremeLowOutliers(out);
    }

    private List<Offer> removeExtremeLowOutliers(List<Offer> sorted) {
        if (sorted.size() < 5) return sorted;
        List<Double> prices = new ArrayList<>();
        for (Offer o : sorted) prices.add(o.price);
        Collections.sort(prices);
        double median = prices.get(prices.size() / 2);
        if (median <= 0) return sorted;
        List<Offer> out = new ArrayList<>();
        for (Offer o : sorted) {
            if (o.price < median * 0.18 && median - o.price > 150) continue;
            out.add(o);
        }
        return out;
    }

    private boolean isLikelyMatch(String query, String text) {
        String q = normalize(query);
        String t = normalize(text);
        if (q.isEmpty() || t.isEmpty()) return false;
        String[] parts = q.split(" ");
        int wordCount = 0;
        int hit = 0;
        for (String p : parts) {
            if (p.isEmpty() || isStop(p)) continue;
            boolean has = t.contains(p);
            if (p.matches(".*\\d.*") && !has) return false;
            if (!p.matches(".*\\d.*")) {
                wordCount++;
                if (has) hit++;
            }
        }
        int need = wordCount <= 1 ? Math.min(wordCount, 1) : (int) Math.ceil(wordCount * 0.6);
        return hit >= need;
    }

    private boolean isStop(String p) {
        return p.equals("en") || p.equals("ucuz") || p.equals("fiyat") || p.equals("fiyati") || p.equals("fiyatı") ||
                p.equals("urun") || p.equals("ürün") || p.equals("satın") || p.equals("al") || p.equals("yeni") || p.equals("orijinal");
    }

    private String normalize(String s) {
        return (s == null ? "" : s.toLowerCase(new Locale("tr", "TR")))
                .replaceAll("[-_/]+", " ")
                .replaceAll("[^a-z0-9çğıöşü ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private void showOffers(List<Offer> offers) {
        resultsContainer.removeAllViews();
        TextView heading = text("“" + currentQuery + "” için en ucuz sonuçlar", 19, TEXT, true);
        LinearLayout.LayoutParams hp = fullWidth(-2);
        hp.setMargins(0, dp(8), 0, dp(5));
        resultsContainer.addView(heading, hp);

        for (int i = 0; i < offers.size(); i++) addOfferCard(i, offers.get(i));
    }

    private void addOfferCard(int index, Offer offer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackgroundColor(Color.WHITE);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top, fullWidth(dp(48)));

        TextView rank = text(index == 0 ? "🥇" : (index + 1) + ".", index == 0 ? 22 : 18, TEXT, true);
        top.addView(rank, new LinearLayout.LayoutParams(dp(46), dp(48)));

        TextView source = text(offer.source, 14, TEXT, true);
        top.addView(source, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView price = text(offer.priceText, 17, GREEN, true);
        price.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(price, new LinearLayout.LayoutParams(dp(145), dp(48)));

        TextView product = text(shorten(offer.title, 120), 14, TEXT, true);
        LinearLayout.LayoutParams pp = fullWidth(-2);
        pp.setMargins(0, dp(2), 0, dp(4));
        card.addView(product, pp);

        TextView detail = text(shorten(offer.detail, 165), 12, MUTED, false);
        detail.setMaxLines(2);
        LinearLayout.LayoutParams dpv = fullWidth(dp(43));
        dpv.setMargins(0, 0, 0, dp(7));
        card.addView(detail, dpv);

        Button go = new Button(this);
        go.setText("ÜRÜNE GİT");
        go.setTextColor(Color.WHITE);
        go.setTextSize(13);
        go.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        go.setBackgroundColor(GREEN);
        go.setOnClickListener(v -> openUrl(offer.url));
        card.addView(go, fullWidth(dp(48)));

        LinearLayout.LayoutParams cp = fullWidth(-2);
        cp.setMargins(0, dp(7), 0, 0);
        resultsContainer.addView(card, cp);
    }

    private void showNoResults() {
        resultsContainer.removeAllViews();
        TextView m = text("Bu aramada kaynaklardan güvenilir bir fiyat eşleşmesi çıkaramadım. Yanlış ürün veya kupon fiyatı göstermek yerine sonucu boş bıraktım. Ürün adını marka + model + ölçü/kapasite ile biraz daha net yazabilirsin.", 14, TEXT, false);
        m.setPadding(0, dp(14), 0, dp(12));
        resultsContainer.addView(m, fullWidth(-2));

        Button fallback = new Button(this);
        fallback.setText("AKAKÇE'DE ARA");
        fallback.setTextColor(Color.WHITE);
        fallback.setBackgroundColor(GREEN);
        fallback.setOnClickListener(v -> openUrl("https://www.akakce.com/arama/?q=" + Uri.encode(currentQuery)));
        resultsContainer.addView(fallback, fullWidth(dp(52)));
    }

    private void setSearching(boolean on, String message) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!on);
        searchButton.setAlpha(on ? 0.65f : 1f);
        statusText.setText(message);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private String shorten(String s, int max) {
        String v = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return v.length() > max ? v.substring(0, max) + "…" : v;
    }

    private String decodeJsString(String value) {
        if (value == null || "null".equals(value) || "undefined".equals(value)) return null;
        try {
            JSONArray wrapper = new JSONArray("[" + value + "]");
            return wrapper.isNull(0) ? null : wrapper.optString(0, null);
        } catch (Exception e) {
            return null;
        }
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Bağlantıyı açacak tarayıcı bulunamadı.", Toast.LENGTH_SHORT).show();
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
        scanning = false;
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
