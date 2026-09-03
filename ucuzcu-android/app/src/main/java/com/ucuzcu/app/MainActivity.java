package com.ucuzcu.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final int GREEN = Color.rgb(15, 107, 71);
    private static final int GREEN_DARK = Color.rgb(9, 78, 51);
    private static final int GREEN_SOFT = Color.rgb(232, 246, 239);
    private static final int TEXT = Color.rgb(30, 38, 34);
    private static final int MUTED = Color.rgb(99, 110, 104);
    private static final int BG = Color.rgb(246, 248, 247);
    private static final int BORDER = Color.rgb(225, 231, 228);
    private static final int GOLD = Color.rgb(224, 157, 31);

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
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        prepareWebView();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(brandRow, fullWidth(dp(62)));

        TextView mark = text("₺", 24, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(oval(GREEN));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        markParams.setMargins(0, 0, dp(12), 0);
        brandRow.addView(mark, markParams);

        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setGravity(Gravity.CENTER_VERTICAL);
        brandRow.addView(brandText, new LinearLayout.LayoutParams(0, dp(58), 1f));

        TextView title = text("Ucuzcu", 29, GREEN_DARK, true);
        brandText.addView(title, fullWidth(dp(36)));
        TextView tagline = text("En ucuzu bul. Fazla ödeme.", 12, MUTED, false);
        brandText.addView(tagline, fullWidth(dp(22)));

        TextView sourceCount = chip("6 KAYNAK", GREEN_SOFT, GREEN_DARK);
        brandRow.addView(sourceCount, new LinearLayout.LayoutParams(dp(92), dp(34)));

        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setOrientation(LinearLayout.VERTICAL);
        searchCard.setPadding(dp(14), dp(13), dp(14), dp(14));
        searchCard.setBackground(rounded(Color.WHITE, 18, BORDER, 1));
        LinearLayout.LayoutParams searchCardParams = fullWidth(-2);
        searchCardParams.setMargins(0, dp(12), 0, 0);
        root.addView(searchCard, searchCardParams);

        TextView searchLabel = text("NE ARIYORSUN?", 11, MUTED, true);
        searchCard.addView(searchLabel, fullWidth(dp(24)));

        searchInput = new EditText(this);
        searchInput.setHint("Ürün, marka veya model yaz...");
        searchInput.setHintTextColor(Color.rgb(150, 158, 154));
        searchInput.setTextColor(TEXT);
        searchInput.setTextSize(17);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.setBackground(rounded(Color.rgb(249, 250, 250), 13, BORDER, 1));
        LinearLayout.LayoutParams ip = fullWidth(dp(56));
        ip.setMargins(0, dp(2), 0, dp(10));
        searchCard.addView(searchInput, ip);

        searchButton = new Button(this);
        searchButton.setText("EN UCUZ 10'U BUL");
        searchButton.setTextSize(15);
        searchButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        searchButton.setTextColor(Color.WHITE);
        searchButton.setAllCaps(false);
        searchButton.setBackground(rounded(GREEN, 13, GREEN, 0));
        searchButton.setOnClickListener(v -> startSearch());
        searchCard.addView(searchButton, fullWidth(dp(54)));

        LinearLayout trustRow = new LinearLayout(this);
        trustRow.setOrientation(LinearLayout.HORIZONTAL);
        trustRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trustParams = fullWidth(dp(38));
        trustParams.setMargins(0, dp(9), 0, 0);
        root.addView(trustRow, trustParams);

        TextView verified = text("✓ Doğrulanmış fiyat", 11, GREEN_DARK, true);
        verified.setGravity(Gravity.CENTER);
        verified.setBackground(rounded(GREEN_SOFT, 18, GREEN_SOFT, 0));
        trustRow.addView(verified, new LinearLayout.LayoutParams(0, dp(30), 1f));

        TextView spacer = new TextView(this);
        trustRow.addView(spacer, new LinearLayout.LayoutParams(dp(8), dp(1)));

        TextView sorted = text("↕ En ucuzdan pahalıya", 11, MUTED, true);
        sorted.setGravity(Gravity.CENTER);
        sorted.setBackground(rounded(Color.WHITE, 18, BORDER, 1));
        trustRow.addView(sorted, new LinearLayout.LayoutParams(0, dp(30), 1f));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srp = fullWidth(dp(46));
        srp.setMargins(0, dp(5), 0, dp(2));
        root.addView(statusRow, srp);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        statusRow.addView(progressBar, new LinearLayout.LayoutParams(dp(28), dp(28)));

        statusText = text("Hazır. Aradığın ürünü yaz.", 12, MUTED, false);
        statusText.setGravity(Gravity.CENTER_VERTICAL);
        statusText.setPadding(dp(6), 0, 0, 0);
        statusRow.addView(statusText, new LinearLayout.LayoutParams(0, dp(46), 1f));

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
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0 Mobile Safari/537.36 Ucuzcu/0.6");

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

        LinearLayout welcome = new LinearLayout(this);
        welcome.setOrientation(LinearLayout.VERTICAL);
        welcome.setPadding(dp(16), dp(16), dp(16), dp(16));
        welcome.setBackground(rounded(Color.WHITE, 17, BORDER, 1));
        LinearLayout.LayoutParams wp = fullWidth(-2);
        wp.setMargins(0, dp(6), 0, 0);
        resultsContainer.addView(welcome, wp);

        TextView wTitle = text("Tek arama, en ucuz seçenekler", 17, TEXT, true);
        welcome.addView(wTitle, fullWidth(dp(30)));

        TextView wDesc = text("Ucuzcu farklı kaynaklardaki ürünleri karşılaştırır, yanlış eşleşmeleri ve şüpheli fiyatları eler.", 13, MUTED, false);
        wDesc.setLineSpacing(0, 1.1f);
        LinearLayout.LayoutParams wdp = fullWidth(-2);
        wdp.setMargins(0, dp(3), 0, dp(13));
        welcome.addView(wDesc, wdp);

        addFeatureRow(welcome, "01", "Ürünü normal şekilde yaz", "Nike ayakkabı, Samsung S26, Bosch matkap...");
        addFeatureRow(welcome, "02", "6 kaynağı otomatik tara", "Akakçe, Cimri ve büyük pazar yerleri");
        addFeatureRow(welcome, "03", "En ucuz 10'u sırala", "Doğrulanmış sonuçları tek listede gör");
    }

    private void addFeatureRow(LinearLayout parent, String no, String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = fullWidth(dp(56));
        rp.setMargins(0, dp(2), 0, 0);
        parent.addView(row, rp);

        TextView badge = text(no, 11, GREEN_DARK, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(GREEN_SOFT, 10, GREEN_SOFT, 0));
        row.addView(badge, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(11), 0, 0, 0);
        row.addView(txt, new LinearLayout.LayoutParams(0, dp(50), 1f));
        txt.addView(text(title, 13, TEXT, true), fullWidth(dp(25)));
        txt.addView(text(desc, 11, MUTED, false), fullWidth(dp(23)));
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
            setSearching(false, "Güvenilir fiyat sonucu bulunamadı.");
            showNoResults();
            return;
        }
        if (finalList.size() > 10) finalList = new ArrayList<>(finalList.subList(0, 10));
        setSearching(false, finalList.size() + " doğrulanmış fiyat • En ucuzdan pahalıya");
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

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(dp(14), dp(10), dp(12), dp(10));
        summary.setBackground(rounded(Color.WHITE, 15, BORDER, 1));
        LinearLayout.LayoutParams smp = fullWidth(dp(66));
        smp.setMargins(0, dp(5), 0, dp(8));
        resultsContainer.addView(summary, smp);

        LinearLayout summaryText = new LinearLayout(this);
        summaryText.setOrientation(LinearLayout.VERTICAL);
        summary.addView(summaryText, new LinearLayout.LayoutParams(0, dp(48), 1f));
        summaryText.addView(text("“" + shorten(currentQuery, 35) + "”", 15, TEXT, true), fullWidth(dp(26)));
        summaryText.addView(text(offers.size() + " doğrulanmış sonuç bulundu", 11, MUTED, false), fullWidth(dp(21)));

        TextView order = chip("FİYAT ↑", GREEN_SOFT, GREEN_DARK);
        summary.addView(order, new LinearLayout.LayoutParams(dp(82), dp(32)));

        for (int i = 0; i < offers.size(); i++) {
            double nextPrice = (i == 0 && offers.size() > 1) ? offers.get(1).price : -1;
            addOfferCard(i, offers.get(i), nextPrice);
        }
    }

    private void addOfferCard(int index, Offer offer, double nextPrice) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(rounded(Color.WHITE, 17, index == 0 ? GREEN : BORDER, index == 0 ? 2 : 1));

        LinearLayout.LayoutParams cp = fullWidth(-2);
        cp.setMargins(0, dp(7), 0, 0);
        resultsContainer.addView(card, cp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top, fullWidth(dp(36)));

        if (index == 0) {
            TextView best = chip("★ EN UCUZ", GREEN, Color.WHITE);
            top.addView(best, new LinearLayout.LayoutParams(dp(104), dp(30)));
        } else {
            TextView rank = chip((index + 1) + ". SIRA", Color.rgb(245, 247, 246), MUTED);
            top.addView(rank, new LinearLayout.LayoutParams(dp(76), dp(30)));
        }

        TextView flex = new TextView(this);
        top.addView(flex, new LinearLayout.LayoutParams(0, dp(1), 1f));

        TextView source = chip(offer.source.toUpperCase(new Locale("tr", "TR")), GREEN_SOFT, GREEN_DARK);
        top.addView(source, new LinearLayout.LayoutParams(dp(sourceWidth(offer.source)), dp(30)));

        TextView product = text(shorten(offer.title, 120), 15, TEXT, true);
        product.setMaxLines(2);
        product.setLineSpacing(0, 1.04f);
        LinearLayout.LayoutParams pp = fullWidth(-2);
        pp.setMargins(0, dp(10), 0, dp(5));
        card.addView(product, pp);

        TextView price = text(offer.priceText, 24, GREEN_DARK, true);
        LinearLayout.LayoutParams priceParams = fullWidth(dp(38));
        card.addView(price, priceParams);

        if (index == 0 && nextPrice > offer.price) {
            double saving = nextPrice - offer.price;
            TextView savingText = text("Sonraki fiyata göre " + formatPrice(saving) + " daha ucuz", 11, GREEN_DARK, true);
            savingText.setGravity(Gravity.CENTER_VERTICAL);
            savingText.setPadding(dp(9), 0, dp(9), 0);
            savingText.setBackground(rounded(GREEN_SOFT, 10, GREEN_SOFT, 0));
            LinearLayout.LayoutParams svp = fullWidth(dp(30));
            svp.setMargins(0, dp(2), 0, dp(8));
            card.addView(savingText, svp);
        }

        TextView detail = text(shorten(cleanDetail(offer.detail), 150), 11, MUTED, false);
        detail.setMaxLines(2);
        detail.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams detailParams = fullWidth(-2);
        detailParams.setMargins(0, dp(2), 0, dp(11));
        card.addView(detail, detailParams);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(bottom, fullWidth(dp(44)));

        TextView safe = text("✓ Fiyat doğrulandı", 11, MUTED, true);
        safe.setGravity(Gravity.CENTER_VERTICAL);
        bottom.addView(safe, new LinearLayout.LayoutParams(0, dp(40), 1f));

        Button go = new Button(this);
        go.setText("Siteye Git  →");
        go.setTextColor(Color.WHITE);
        go.setTextSize(12);
        go.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        go.setAllCaps(false);
        go.setPadding(dp(8), 0, dp(8), 0);
        go.setBackground(rounded(index == 0 ? GREEN : GREEN_DARK, 11, GREEN, 0));
        go.setOnClickListener(v -> openUrl(offer.url));
        bottom.addView(go, new LinearLayout.LayoutParams(dp(126), dp(42)));
    }

    private int sourceWidth(String source) {
        if (source == null) return 90;
        int w = 58 + source.length() * 5;
        return Math.max(82, Math.min(130, w));
    }

    private void showNoResults() {
        resultsContainer.removeAllViews();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(18), dp(17), dp(18));
        card.setBackground(rounded(Color.WHITE, 17, BORDER, 1));
        LinearLayout.LayoutParams cp = fullWidth(-2);
        cp.setMargins(0, dp(7), 0, 0);
        resultsContainer.addView(card, cp);

        TextView icon = text("⌕", 30, GREEN, true);
        icon.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(icon, fullWidth(dp(42)));

        TextView title = text("Güvenilir sonuç bulamadım", 17, TEXT, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(title, fullWidth(dp(32)));

        TextView m = text("Yanlış ürün veya kupon fiyatı göstermek yerine sonucu boş bıraktım. Marka + model + ölçü/kapasite ile biraz daha net arayabilirsin.", 13, MUTED, false);
        m.setGravity(Gravity.CENTER_HORIZONTAL);
        m.setLineSpacing(0, 1.1f);
        LinearLayout.LayoutParams mp = fullWidth(-2);
        mp.setMargins(0, dp(4), 0, dp(14));
        card.addView(m, mp);

        Button fallback = new Button(this);
        fallback.setText("Akakçe'de Ara  →");
        fallback.setTextColor(Color.WHITE);
        fallback.setTextSize(13);
        fallback.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fallback.setAllCaps(false);
        fallback.setBackground(rounded(GREEN, 12, GREEN, 0));
        fallback.setOnClickListener(v -> openUrl("https://www.akakce.com/arama/?q=" + Uri.encode(currentQuery)));
        card.addView(fallback, fullWidth(dp(50)));
    }

    private void setSearching(boolean on, String message) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!on);
        searchButton.setAlpha(on ? 0.72f : 1f);
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

    private TextView chip(String value, int bg, int fg) {
        TextView t = text(value, 10, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setBackground(rounded(bg, 20, bg, 0));
        return t;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeWidth > 0) d.setStroke(dp(strokeWidth), strokeColor);
        return d;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private String shorten(String s, int max) {
        String v = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return v.length() > max ? v.substring(0, max) + "…" : v;
    }

    private String cleanDetail(String s) {
        String v = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        v = v.replaceAll("(?i)en ucuz", "").replaceAll("(?i)fiyat", "").replaceAll("\\s+", " ").trim();
        return v;
    }

    private String formatPrice(double price) {
        return String.format(new Locale("tr", "TR"), "%,.2f TL", price);
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
