package com.talahub.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * TiempoActivity carga una WebView con la información meteorológica
 * histórica para una fecha específica y una ubicación predeterminada (Talavera de la Reina).
 *
 * Esta actividad recibe una fecha en formato dd-MM-yyyy mediante un Intent y construye
 * una URL para mostrar el clima correspondiente desde tiempo3.com.
 *
 * @author Alberto Martínez Vadillo
 */
public class TiempoActivity extends AppCompatActivity {

    /**
     * Método de ciclo de vida que se ejecuta al crear la actividad.
     * Configura la WebView y carga la URL del clima basada en la fecha proporcionada.
     *
     * Se espera que la fecha llegue con el formato {@code dd-MM-yyyy}.
     */
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

                Log.d("TiempoURL", "Accediendo a: " + url);

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