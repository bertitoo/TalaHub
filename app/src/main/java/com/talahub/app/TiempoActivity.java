package com.talahub.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.talahub.app.R;

public class TiempoActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiempo);

        String fecha = getIntent().getStringExtra("fecha");

        if (fecha != null && fecha.contains("-")) {
            String[] partes = fecha.split("-");
            if (partes.length >= 2) {
                String dia = partes[0];
                String mes = partes[1];

                String url = "https://www.tiempo3.com/europe/spain/castilla-la-mancha/talavera-de-la-reina?page=past-weather#day=" + dia + "&month=" + mes;

                Log.d("TiempoURL", "Accediendo a: " + url); // ← Aquí ves la URL

                WebView webView = findViewById(R.id.webview_tiempo);
                WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);

                webView.setWebViewClient(new WebViewClient());
                webView.loadUrl(url);


            } else {
                Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Fecha no disponible", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}