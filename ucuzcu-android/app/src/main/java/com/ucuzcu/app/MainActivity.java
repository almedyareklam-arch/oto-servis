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

        TextView subtitle = text("Ürünü yaz. Ucuzcu doğru ürünü bulup en ucuz satıcıları sıralasın.", 16, Color.rgb(74, 88, 80), false);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams sp = fullWidth(-2);
        sp.setMargins(0, dp(4), 0, dp(20));
        root.addView(subtitle, sp);

        searchInput = new EditText(this);
        searchInput.setHint("Örn: Samsung Galaxy S26 256 GB");
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
        LinearLayout.LayoutParams srp = fullWidth(dp(54));
        srp.setMargins(0, dp(12), 0, dp(4));
        root.addView(statusRow, srp);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        statusRow.addView(progressBar, new LinearLayout.LayoutParams(dp(34), dp(34)));

        statusText = text("Hazır. Bir ürün yazıp aramayı başlat.", 13, MUTED, false);
        statusText.setPadding(dp(8), 0, 0, 0);
        statusRow.addView(statusText, new LinearLayout.LayoutParams(0, dp(54), 1f));

        TextView info = text("V3 Beta • Ürün adı doğrulanmadan fiyat gösterilmez. Yanlış ürün eşleşirse sonuç reddedilir.", 12, Color.rgb(117, 126, 121), false);
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
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0 Mobile Safari/537.36 Ucuzcu/0.3");

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                final int id = searchId;
                handler.postDelayed(() -> {
                    if (id != searchId) return;
                    if (stage == 1) findProductPage();
                    else if (stage == 2) extractOffers();
                }, 1000);
            }
        });
    }

    private void showWelcome() {
        resultsContainer.removeAllViews();
        TextView t = text("🔎 Modeli net yaz.\nÖrnek: Samsung Galaxy S26 256 GB Siyah", 15, Color.rgb(62, 72, 67), false);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(0, dp(24), 0, dp(8));
        resultsContainer.addView(t, fullWidth(dp(90)));
    }

    private void startSearch() {
        String q = searchInput.getText().toString().trim();
        if (q.length() < 3) {
            searchInput.setError("Ürün adını biraz daha açık yaz.");
            searchInput.requestFocus();
            return;
        }
        currentQuery = q;
        sourceSearchUrl = "https://www.akakce.com/arama/?q=" + Uri.encode(q);
        selectedProductUrl = "";
        stage = 1;
        searchId++;
        final int id = searchId;
        resultsContainer.removeAllViews();
        setSearching(true, "Doğru ürün aranıyor…");
        webView.loadUrl(sourceSearchUrl);
        handler.postDelayed(() -> {
            if (id == searchId && stage != 0) showFailure("Tarama zaman aşımına uğradı.");
        }, 25000);
    }

    private void findProductPage() {
        if (stage != 1) return;
        String q = JSONObject.quote(currentQuery);
        String script = "(function(){" +
                "function n(s){return (s||'').toLocaleLowerCase('tr-TR').replace(/[\\-_\\/]+/g,' ').replace(/[^a-z0-9çğıöşü ]/gi,' ').replace(/\\s+/g,' ').trim();}" +
                "function ignored(x){return ['gb','tb','ram','telefon','cep','akilli','akıllı','siyah','beyaz','mavi','gri','yesil','yeşil','kirmizi','kırmızı'].indexOf(x)>=0;}" +
                "var raw=n(" + q + ");var req=raw.split(' ').filter(function(x){return x.length>1&&!ignored(x);});" +
                "var best=null,bestScore=-1;" +
                "Array.prototype.slice.call(document.querySelectorAll('a[href]')).forEach(function(a){" +
                "var href=a.href||'';if(href.indexOf('akakce.com')<0)return;if(href.indexOf('fiyati')<0&&href.indexOf('en-ucuz')<0)return;" +
                "var bits=[a.innerText,a.title,a.getAttribute('aria-label'),href];var im=a.querySelector('img[alt]');if(im)bits.push(im.alt);" +
                "var p=a.parentElement,depth=0;while(p&&depth<4){var pt=(p.innerText||'').trim();if(pt.length>5&&pt.length<500){bits.push(pt);break;}p=p.parentElement;depth++;}" +
                "var hay=n(bits.join(' '));var hit=0;var numericOk=true;req.forEach(function(tok){var has=hay.indexOf(tok)>=0;if(has)hit++;if(/[0-9]/.test(tok)&&!has)numericOk=false;});" +
                "if(!numericOk)return;var need=req.length<=1?1:2;if(hit<need)return;" +
                "var first=req.length?req[0]:'';if(first&&/[a-zçğıöşü]/i.test(first)&&hay.indexOf(first)<0)return;" +
                "var score=hit*10;if(hay.indexOf(raw)>=0)score+=25;req.forEach(function(tok){if(n(href).indexOf(tok)>=0)score+=4;});" +
                "if(score>bestScore){bestScore=score;best={url:href,score:score,hit:hit,total:req.length};}" +
                "});return best?JSON.stringify(best):null;})()";

        final int id = searchId;
        webView.evaluateJavascript(script, value -> {
            if (id != searchId || stage != 1) return;
            String decoded = decodeJsString(value);
            try {
                if (decoded == null) { showFailure("Aramayla yeterince eşleşen ürün bulunamadı."); return; }
                JSONObject o = new JSONObject(decoded);
                String url = o.optString("url", "");
                if (url.length() < 8) { showFailure("Doğru ürün otomatik seçilemedi."); return; }
                selectedProductUrl = url;
                stage = 2;
                setSearching(true, "Ürün doğrulandı, fiyatlar taranıyor…");
                webView.loadUrl(url);
            } catch (Exception e) {
                showFailure("Ürün eşleştirmesi işlenemedi.");
            }
        });
    }

    private void extractOffers() {
        if (stage != 2) return;
        String script = "(function(){" +
                "function c(s){return (s||'').replace(/\\s+/g,' ').trim();}" +
                "function price(s){var ms=s.match(/(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)\\s*TL/ig);if(!ms)return null;for(var i=0;i<ms.length;i++){var r=ms[i].replace(/\\s*TL/i,'');var num=parseFloat(r.replace(/\\./g,'').replace(',','.'));if(num>=10&&num<=10000000)return {txt:r+' TL',num:num};}return null;}" +
                "var h=document.querySelector('h1');var title=c(h?h.innerText:document.title);var out=[],seen={};" +
                "Array.prototype.slice.call(document.querySelectorAll('li,article,section,div')).forEach(function(card){" +
                "var t=c(card.innerText);if(t.length<20||t.length>550||t.indexOf('TL')<0)return;if((t.match(/Satıcıya Git/gi)||[]).length!==1)return;var pr=price(t);if(!pr)return;" +
                "var links=Array.prototype.slice.call(card.querySelectorAll('a[href]'));var go=links.find(function(a){return /Satıcıya Git/i.test(c(a.innerText));});if(!go)return;" +
                "var seller='Satıcı';var el=card.querySelector('[class*=merchant],[class*=seller],[class*=store]');if(el){var et=c(el.innerText);if(et.length>1&&et.length<70)seller=et;}" +
                "if(seller==='Satıcı'){for(var j=0;j<links.length;j++){var lt=c(links[j].innerText);if(lt.length>1&&lt.length<55&&!/Satıcıya Git|Ürün Özellikleri|TL|Son /i.test(lt)){seller=lt;break;}}}" +
                "var href=go.href||location.href;var key=pr.txt+'|'+seller+'|'+href;if(seen[key])return;seen[key]=1;out.push({price:pr.num,priceText:pr.txt,seller:seller,detail:t.substring(0,220),url:href});" +
                "});out.sort(function(a,b){return a.price-b.price;});return JSON.stringify({title:title,offers:out.slice(0,10)});})()";

        final int id = searchId;
        webView.evaluateJavascript(script, value -> {
            if (id != searchId || stage != 2) return;
            String decoded = decodeJsString(value);
            try {
                JSONObject obj = new JSONObject(decoded == null ? "{}" : decoded);
                String title = obj.optString("title", "").trim();
                if (!isProductMatch(currentQuery, title)) {
                    showFailure("Yanlış ürün eşleşmesi engellendi: " + (title.isEmpty() ? "ürün adı okunamadı" : title));
                    return;
                }
                List<Offer> offers = parseOffers(obj.optJSONArray("offers"));
                if (offers.isEmpty()) { showFailure("Doğru ürün bulundu ama satıcı fiyatları okunamadı."); return; }
                stage = 0;
                showOffers(offers, title);
            } catch (Exception e) {
                showFailure("Fiyat verisi işlenemedi.");
            }
        });
    }

    private boolean isProductMatch(String query, String title) {
        String q = normalize(query);
        String t = normalize(title);
        if (q.isEmpty() || t.isEmpty()) return false;
        String[] parts = q.split(" ");
        List<String> required = new ArrayList<>();
        for (String p : parts) {
            if (p.length() <= 1 || isIgnored(p)) continue;
            required.add(p);
        }
        if (required.isEmpty()) return t.contains(q);
        int hit = 0;
        for (String p : required) {
            boolean has = t.contains(p);
            if (p.matches(".*\\d.*") && !has) return false;
            if (has) hit++;
        }
        String first = required.get(0);
        if (first.matches(".*[a-zçğıöşü].*") && !t.contains(first)) return false;
        return hit >= (required.size() <= 1 ? 1 : 2);
    }

    private boolean isIgnored(String p) {
        return p.equals("gb") || p.equals("tb") || p.equals("ram") || p.equals("telefon") || p.equals("cep") ||
                p.equals("akilli") || p.equals("akıllı") || p.equals("siyah") || p.equals("beyaz") || p.equals("mavi") ||
                p.equals("gri") || p.equals("yesil") || p.equals("yeşil") || p.equals("kirmizi") || p.equals("kırmızı");
    }

    private String normalize(String s) {
        return (s == null ? "" : s.toLowerCase(new Locale("tr", "TR")))
                .replaceAll("[-_/]+", " ")
                .replaceAll("[^a-z0-9çğıöşü ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private List<Offer> parseOffers(JSONArray array) {
        List<Offer> list = new ArrayList<>();
        if (array == null) return list;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i); if (o == null) continue;
            double price = o.optDouble("price", -1);
            String priceText = o.optString("priceText", "").trim();
            String seller = o.optString("seller", "Satıcı").trim();
            String detail = o.optString("detail", "").trim();
            String url = o.optString("url", selectedProductUrl).trim();
            if (price <= 0 || priceText.isEmpty()) continue;
            String key = String.format(Locale.US, "%.2f|%s|%s", price, seller, url);
            if (!seen.add(key)) continue;
            Offer x = new Offer(); x.price=price; x.priceText=priceText; x.seller=seller.isEmpty()?"Satıcı":seller; x.detail=detail; x.url=url.isEmpty()?selectedProductUrl:url;
            list.add(x);
        }
        Collections.sort(list, Comparator.comparingDouble(a -> a.price));
        return list.size() > 10 ? new ArrayList<>(list.subList(0,10)) : list;
    }

    private void showOffers(List<Offer> offers, String productTitle) {
        setSearching(false, offers.size() + " doğru fiyat bulundu • En ucuzdan pahalıya sıralandı");
        resultsContainer.removeAllViews();
        TextView product = text(productTitle, 19, TEXT, true);
        LinearLayout.LayoutParams pp = fullWidth(-2); pp.setMargins(0, dp(8), 0, dp(4)); resultsContainer.addView(product, pp);
        TextView source = text("Kaynak: Akakçe • Ürün eşleşmesi V3 doğrulamasından geçti", 12, MUTED, false);
        LinearLayout.LayoutParams src = fullWidth(dp(34)); src.setMargins(0,0,0,dp(5)); resultsContainer.addView(source, src);
        for (int i=0;i<offers.size();i++) addOfferCard(i, offers.get(i));
    }

    private void addOfferCard(int index, Offer offer) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(12),dp(12),dp(12)); card.setBackgroundColor(Color.WHITE);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); card.addView(top, fullWidth(dp(46)));
        TextView rank = text(index==0?"🥇":(index+1)+".", index==0?22:18, TEXT, true); top.addView(rank,new LinearLayout.LayoutParams(dp(46),dp(46)));
        TextView seller = text(offer.seller,15,TEXT,true); top.addView(seller,new LinearLayout.LayoutParams(0,dp(46),1f));
        TextView price = text(offer.priceText,18,GREEN,true); price.setGravity(Gravity.END|Gravity.CENTER_VERTICAL); top.addView(price,new LinearLayout.LayoutParams(dp(142),dp(46)));
        if (!offer.detail.isEmpty()) { TextView d=text(shorten(offer.detail),12,MUTED,false); d.setMaxLines(2); LinearLayout.LayoutParams dpv=fullWidth(dp(44)); dpv.setMargins(0,dp(2),0,dp(7)); card.addView(d,dpv); }
        Button go=new Button(this); go.setText("ÜRÜNE GİT"); go.setTextColor(Color.WHITE); go.setTextSize(13); go.setTypeface(Typeface.DEFAULT,Typeface.BOLD); go.setBackgroundColor(GREEN); go.setOnClickListener(v->openUrl(offer.url)); card.addView(go,fullWidth(dp(48)));
        LinearLayout.LayoutParams cp=fullWidth(-2); cp.setMargins(0,dp(7),0,0); resultsContainer.addView(card,cp);
    }

    private void showFailure(String reason) {
        stage=0; setSearching(false, reason); resultsContainer.removeAllViews();
        TextView m=text("Yanlış fiyat göstermemek için bu sonuç listeye alınmadı. Arama sayfasını açıp ürünü kontrol edebilirsin.",14,TEXT,false); m.setPadding(0,dp(12),0,dp(10)); resultsContainer.addView(m,fullWidth(dp(86)));
        Button b=new Button(this); b.setText("AKAKÇE ARAMASINI AÇ"); b.setTextColor(Color.WHITE); b.setBackgroundColor(GREEN); b.setOnClickListener(v->openUrl(sourceSearchUrl)); resultsContainer.addView(b,fullWidth(dp(52)));
    }

    private void setSearching(boolean searching,String message) {
        progressBar.setVisibility(searching?View.VISIBLE:View.GONE); searchButton.setEnabled(!searching); searchButton.setAlpha(searching?0.65f:1f); statusText.setText(message);
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }

    private String shorten(String s) { String d=s.replaceAll("\\s+"," ").trim(); return d.length()>180?d.substring(0,180)+"…":d; }

    private String decodeJsString(String value) {
        if(value==null||"null".equals(value)||"undefined".equals(value))return null;
        try{JSONArray a=new JSONArray("["+value+"]");return a.isNull(0)?null:a.optString(0,null);}catch(Exception e){return null;}
    }

    private void openUrl(String url) {
        if(url==null||url.trim().isEmpty())return;
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"Bağlantıyı açacak tarayıcı bulunamadı.",Toast.LENGTH_SHORT).show();}
    }

    private LinearLayout.LayoutParams fullWidth(int height){return new LinearLayout.LayoutParams(-1,height);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    @Override protected void onDestroy(){searchId++;handler.removeCallbacksAndMessages(null);if(webView!=null){webView.stopLoading();webView.destroy();}super.onDestroy();}
}
