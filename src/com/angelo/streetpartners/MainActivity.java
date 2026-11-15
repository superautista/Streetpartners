package com.angelo.streetlibras; // <--- O NOME DO PACOTE CORRIGIDO

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webViewLibras;
    private EditText editTexto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Se o seu layout não for activity_main, mude aqui
        setContentView(R.layout.activity_main);

        editTexto = findViewById(R.id.editTexto);
        Button btnTraduzir = findViewById(R.id.btnTraduzir);
        webViewLibras = findViewById(R.id.webViewLibras);

        // Configurações do WebView
        WebSettings webSettings = webViewLibras.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE); // Evita o erro de Cache

        btnTraduzir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Limpa o cache do WebView
                webViewLibras.clearCache(true);
                webViewLibras.clearHistory();
                
                // Carrega o site oficial do VLibras
                String urlVlibras = "https://www.vlibras.gov.br/";
                webViewLibras.loadUrl(urlVlibras);
                Toast.makeText(MainActivity.this, "Carregando VLibras...", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
