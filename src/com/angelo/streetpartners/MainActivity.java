package com.angelo.streetlibras;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView webViewLibras = findViewById(R.id.webViewLibras);

        // Configurações para permitir o 3D e JavaScript
        WebSettings webSettings = webViewLibras.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        
        // Liga a aceleração de hardware no WebView
        webViewLibras.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

        // O código HTML que chama o bonequinho do VLibras
        String htmlCode = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset='utf-8'>\n" +
                "<meta name='viewport' content='width=device-width, initial-scale=1'>\n" +
                "<style> body { background-color: #121212; margin: 0; padding: 0; } </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div vw class='enabled'>\n" +
                "    <div vw-access-button class='active'></div>\n" +
                "    <div vw-plugin-wrapper>\n" +
                "      <div class='vw-plugin-top-wrapper'></div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script src='https://vlibras.gov.br/app/vlibras-plugin.js'></script>\n" +
                "  <script>\n" +
                "    new window.VLibras.Widget('https://vlibras.gov.br/app');\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";

        // Carrega o HTML no WebView
        webViewLibras.loadDataWithBaseURL("https://vlibras.gov.br/", htmlCode, "text/html", "UTF-8", null);
    }
}
