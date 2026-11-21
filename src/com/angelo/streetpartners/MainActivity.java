package com.angelo.streetuberautismos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends Activity {

    private EditText editDestino;
    private Button btnChamar, btnPagar, btnWaze, btnHistorico;
    private TextView txtStatus;
    private WebView webViewMapa;

    private static final double VALOR_CORRIDA = 15.50;
    private static final String CHAVE_PIX = "14988376670";
    private static final String NOME_RECEBEDOR = "ANGELO SILVA";
    private static final String CIDADE = "GETULINA";

    private SharedPreferences db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editDestino = findViewById(R.id.editDestino);
        btnChamar = findViewById(R.id.btnChamar);
        btnPagar = findViewById(R.id.btnPagar);
        btnWaze = findViewById(R.id.btnWaze);
        btnHistorico = findViewById(R.id.btnHistorico);
        txtStatus = findViewById(R.id.txtStatus);
        webViewMapa = findViewById(R.id.webViewMapa);

        btnPagar.setEnabled(false);
        db = getSharedPreferences("RotaFacilDB", Context.MODE_PRIVATE);

        // Configurar Mapa (OpenStreetMap para não precisar de API Key)
        webViewMapa.getSettings().setJavaScriptEnabled(true);
        String mapHtml = "<html><body style='margin:0; padding:0;'><iframe width='100%' height='100%' frameborder='0' scrolling='no' marginheight='0' marginwidth='0' src='https://www.openstreetmap.org/export/embed.html?bbox=-49.95%2C-21.82%2C-49.88%2C-21.77&amp;layer=mapnik'></iframe></body></html>";
        webViewMapa.loadData(mapHtml, "text/html", "UTF-8");

        btnChamar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simularChamada();
            }
        });

        btnWaze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirWaze();
            }
        });

        btnPagar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gerarPagamento();
            }
        });

        btnHistorico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarHistorico();
            }
        });
    }

    private void simularChamada() {
        String destino = editDestino.getText().toString().trim();
        if (destino.isEmpty()) {
            editDestino.setError("Digite seu destino");
            return;
        }

        btnChamar.setEnabled(false);
        txtStatus.setText("Solicitando motorista...");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText("Motorista encontrado!\nCarlos (Fiat Uno) está a 5 min.");
                btnPagar.setEnabled(true);
                salvarNoHistorico(destino);
                Toast.makeText(MainActivity.this, "Corrida salva no histórico!", Toast.LENGTH_SHORT).show();
            }
        }, 2000);
    }

    private void abrirWaze() {
        String destino = editDestino.getText().toString().trim();
        if (!destino.isEmpty()) {
            String url = "https://waze.com/ul?q=" + Uri.encode(destino) + "&navigate=yes";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                intent.setPackage("com.waze");
                startActivity(intent);
            } catch (Exception e) {
                intent.setPackage(null);
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, "Digite um destino!", Toast.LENGTH_SHORT).show();
        }
    }

    private void gerarPagamento() {
        String payload = gerarPix(CHAVE_PIX, NOME_RECEBEDOR, CIDADE, VALOR_CORRIDA, "ROTA" + System.currentTimeMillis());
        new AlertDialog.Builder(this)
                .setTitle("Pix Copia e Cola")
                .setMessage(payload)
                .setPositiveButton("Copiar", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Pix", payload));
                    Toast.makeText(this, "Código Pix copiado!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void salvarNoHistorico(String destino) {
        try {
            JSONArray array = new JSONArray(db.getString("corridas", "[]"));
            JSONObject corrida = new JSONObject();
            corrida.put("destino", destino);
            corrida.put("valor", VALOR_CORRIDA);
            corrida.put("data", new java.util.Date().toString());
            array.put(corrida);
            db.edit().putString("corridas", array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void mostrarHistorico() {
        try {
            JSONArray array = new JSONArray(db.getString("corridas", "[]"));
            if (array.length() == 0) {
                Toast.makeText(this, "Nenhuma corrida realizada.", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                JSONObject c = array.getJSONObject(i);
                sb.append("📍 ").append(c.getString("destino")).append(" - R$ ").append(c.getDouble("valor")).append("\n");
            }
            new AlertDialog.Builder(this)
                    .setTitle("Histórico de Corridas")
                    .setMessage(sb.toString())
                    .setPositiveButton("Fechar", null)
                    .show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String gerarPix(String chave, String nome, String cidade, double valor, String txid) {
        String conta = campo("00", "br.gov.bcb.pix") + campo("01", chave);
        String payload = campo("00", "01") + campo("26", conta) + campo("52", "0000") + campo("53", "986")
                + campo("54", String.format(Locale.US, "%.2f", valor)) + campo("58", "BR")
                + campo("59", nome) + campo("60", cidade) + campo("62", campo("05", txid)) + "6304";
        return payload + crc16(payload);
    }

    private String campo(String id, String valor) {
        return id + String.format(Locale.US, "%02d", valor.length()) + valor;
    }

    private String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021;
                else crc <<= 1;
                crc &= 0xFFFF;
            }
        }
        return String.format(Locale.US, "%04X", crc);
    }
}
