package com.ucuzcu.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText searchInput;
    private LinearLayout resultsContainer;

    private final String[][] stores = new String[][]{
            {"Trendyol", "https://www.trendyol.com/sr?q="},
            {"Hepsiburada", "https://www.hepsiburada.com/ara?q="},
            {"Amazon Türkiye", "https://www.amazon.com.tr/s?k="},
            {"N11", "https://www.n11.com/arama?q="},
            {"MediaMarkt", "https://www.mediamarkt.com.tr/tr/search.html?query="},
            {"Teknosa", "https://www.teknosa.com/arama/?s="},
            {"Pazarama", "https://www.pazarama.com/arama?q="},
            {"ÇiçekSepeti", "https://www.ciceksepeti.com/arama?query="},
            {"Vatan Bilgisayar", "https://www.google.com/search?q=site%3Avatanbilgisayar.com+"},
            {"CarrefourSA", "https://www.google.com/search?q=site%3Acarrefoursa.com+"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16, 97, 62));
        buildUi();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(247, 249, 248));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(32));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("UCUZCU");
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(16, 97, 62));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, fullWidth(dp(54)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Bir ürünü yaz, 10 mağazada tek dokunuşla ara.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(74, 88, 80));
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subtitleParams = fullWidth(dp(64));
        subtitleParams.setMargins(0, dp(4), 0, dp(16));
        root.addView(subtitle, subtitleParams);

        searchInput = new EditText(this);
        searchInput.setHint("Örn: Samsung S26 Ultra 512 GB");
        searchInput.setTextSize(17);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams inputParams = fullWidth(dp(58));
        inputParams.setMargins(0, 0, 0, dp(12));
        root.addView(searchInput, inputParams);

        Button searchButton = new Button(this);
        searchButton.setText("10 MAĞAZADA ARA");
        searchButton.setTextSize(16);
        searchButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        searchButton.setTextColor(Color.WHITE);
        searchButton.setBackgroundColor(Color.rgb(16, 97, 62));
        searchButton.setOnClickListener(v -> showStores());
        root.addView(searchButton, fullWidth(dp(56)));

        TextView note = new TextView(this);
        note.setText("İlk sürüm: mağaza aramalarını tek ekranda toplar. Otomatik fiyat çekme ve en ucuz 10 sıralaması sonraki motor olarak eklenecek.");
        note.setTextSize(13);
        note.setTextColor(Color.rgb(104, 113, 108));
        LinearLayout.LayoutParams noteParams = fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        noteParams.setMargins(0, dp(16), 0, dp(14));
        root.addView(note, noteParams);

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsContainer, fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT));

        showWelcome();
        setContentView(scrollView);
    }

    private void showWelcome() {
        resultsContainer.removeAllViews();
        TextView ready = new TextView(this);
        ready.setText("🔎 Ürünü yaz ve aramayı başlat.");
        ready.setTextSize(16);
        ready.setTextColor(Color.rgb(52, 61, 56));
        ready.setGravity(Gravity.CENTER_HORIZONTAL);
        ready.setPadding(0, dp(24), 0, 0);
        resultsContainer.addView(ready, fullWidth(dp(72)));
    }

    private void showStores() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            searchInput.setError("Aramak istediğin ürünü yaz.");
            searchInput.requestFocus();
            return;
        }

        resultsContainer.removeAllViews();

        TextView heading = new TextView(this);
        heading.setText("10 mağaza hazır");
        heading.setTextSize(20);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(Color.rgb(35, 43, 39));
        LinearLayout.LayoutParams headingParams = fullWidth(dp(48));
        headingParams.setMargins(0, dp(8), 0, dp(4));
        resultsContainer.addView(heading, headingParams);

        for (int i = 0; i < stores.length; i++) {
            final String storeName = stores[i][0];
            final String url = stores[i][1] + Uri.encode(query);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(8), dp(8), dp(8));
            row.setBackgroundColor(Color.WHITE);

            TextView name = new TextView(this);
            name.setText((i + 1) + ".  " + storeName);
            name.setTextSize(16);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            name.setTextColor(Color.rgb(40, 49, 44));
            row.addView(name, new LinearLayout.LayoutParams(0, dp(52), 1f));

            Button open = new Button(this);
            open.setText("ARA");
            open.setTextColor(Color.WHITE);
            open.setTextSize(13);
            open.setBackgroundColor(Color.rgb(16, 97, 62));
            open.setOnClickListener(v -> openStore(url));
            row.addView(open, new LinearLayout.LayoutParams(dp(88), dp(48)));

            LinearLayout.LayoutParams rowParams = fullWidth(dp(68));
            rowParams.setMargins(0, dp(6), 0, 0);
            resultsContainer.addView(row, rowParams);
        }
    }

    private void openStore(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
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
}
