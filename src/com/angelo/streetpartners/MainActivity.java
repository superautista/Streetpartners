package com.angelo.streetlibras;

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
        setContentView(R.layout.activity_main);

        editTexto = findViewById(R.id.editTexto);
        Button btnTraduzir = findViewById(R.id.btnTraduzir);
        webViewLibras = findViewById(R.id.webViewLibras);

        // Configura o WebView para rodar JavaScript e carregar o VLibras
        WebSettings webSettings = webViewLibras.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        btnTraduzir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String texto = editTexto.getText().toString();
                
                if (texto.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Digite um texto primeiro!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // MÁGICA AQUI: O VLibras tem uma API que aceita texto via URL
                // Nós passamos o texto digitado para o player do VLibras
                String urlVlibras = "https://www.vlibras.gov.br/app/?texto=" + texto;
                
                webViewLibras.loadUrl(urlVlibras);
                Toast.makeText(MainActivity.this, "Traduzindo com o Avatar...", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
