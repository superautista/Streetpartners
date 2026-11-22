
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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private EditText editDestino;
    private Button btnChamar, btnPagar, btnWaze, btnHistorico;
    private TextView txtStatus;
    private WebView webViewMapa;
    private SharedPreferences db;

    private static final double VALOR_CORRIDA = 15.50;
    private static final String CHAVE_PIX = "14988376670";
    private static final String NOME_RECEBEDOR = "ANGELO SILVA";
    private static final String CIDADE = "GETULINA";

    private static final String API =
            "https://streetuber.freedev.app/api/corridas.php";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

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

        db = getSharedPreferences("RotaFacilDB", MODE_PRIVATE);

        btnPagar.setEnabled(false);
        btnConta = findViewById(R.id.btnConta);

        btnConta.setOnClickListener(v -> abrirTelaConta());
        webViewMapa.getSettings().setJavaScriptEnabled(true);

        String mapHtml =
                "<html><body style='margin:0;padding:0;'>" +
                "<iframe width='100%' height='100%' frameborder='0' " +
                "src='https://www.openstreetmap.org/export/embed.html?" +
                "bbox=-49.95%2C-21.82%2C-49.88%2C-21.77&amp;layer=mapnik'>" +
                "</iframe></body></html>";

        webViewMapa.loadData(mapHtml, "text/html", "UTF-8");

        btnChamar.setOnClickListener(v -> simularChamada());

        btnWaze.setOnClickListener(v -> abrirWaze());

        btnPagar.setOnClickListener(v -> gerarPagamento());

        btnHistorico.setOnClickListener(v -> mostrarHistorico());
    }

    private void simularChamada() {
        String destino = editDestino.getText().toString().trim();

        if (destino.isEmpty()) {
            editDestino.setError("Digite seu destino");
            return;
        }

        btnChamar.setEnabled(false);
        btnPagar.setEnabled(false);
        txtStatus.setText("Enviando solicitação ao servidor...");

        executor.execute(() -> {
            HttpURLConnection conexao = null;

            try {
                URL url = new URL(API);
                conexao = (HttpURLConnection) url.openConnection();

                conexao.setRequestMethod("POST");
                conexao.setConnectTimeout(15000);
                conexao.setReadTimeout(15000);
                conexao.setDoOutput(true);
                conexao.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8"
                );

                JSONObject dados = new JSONObject();
                dados.put("origem", "Origem não informada");
                dados.put("destino", destino);
                dados.put("valor", VALOR_CORRIDA);

                byte[] corpo = dados.toString().getBytes(
                        StandardCharsets.UTF_8
                );

                try (OutputStream os = conexao.getOutputStream()) {
                    os.write(corpo);
                }

                int codigo = conexao.getResponseCode();

                InputStream entrada = codigo >= 200 && codigo < 300
                        ? conexao.getInputStream()
                        : conexao.getErrorStream();

                StringBuilder resposta = new StringBuilder();

                if (entrada != null) {
                    BufferedReader leitor = new BufferedReader(
                            new InputStreamReader(
                                    entrada, StandardCharsets.UTF_8
                            )
                    );

                    String linha;
                    while ((linha = leitor.readLine()) != null) {
                        resposta.append(linha);
                    }
                    leitor.close();
                }

                if (codigo >= 200 && codigo < 300) {
                    JSONObject resultado =
                            new JSONObject(resposta.toString());

                    if (resultado.optBoolean("sucesso")) {
                        runOnUiThread(() -> {
                            txtStatus.setText(
                                    "Solicitação registrada!\nAguardando motorista."
                            );

                            salvarNoHistorico(destino);
                            btnPagar.setEnabled(true);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Corrida enviada ao servidor!",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    } else {
                        throw new Exception("Servidor não confirmou a corrida.");
                    }
                } else {
                    throw new Exception("Erro HTTP: " + codigo);
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    txtStatus.setText("Não foi possível solicitar a corrida.");
                    btnChamar.setEnabled(true);

                    Toast.makeText(
                            MainActivity.this,
                            "Erro de conexão: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (conexao != null) {
                    conexao.disconnect();
                }
            }
        });
    }

    private void abrirWaze() {
        String destino = editDestino.getText().toString().trim();

        if (!destino.isEmpty()) {
            String url = "https://waze.com/ul?q="
                    + Uri.encode(destino) + "&navigate=yes";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            try {
                intent.setPackage("com.waze");
                startActivity(intent);
            } catch (Exception e) {
                intent.setPackage(null);
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, "Digite um destino!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void gerarPagamento() {
        String payload = gerarPix(
                CHAVE_PIX, NOME_RECEBEDOR, CIDADE,
                VALOR_CORRIDA,
                "ROTA" + System.currentTimeMillis()
        );

        new AlertDialog.Builder(this)
                .setTitle("Pix Copia e Cola")
                .setMessage(payload)
                .setPositiveButton("Copiar", (dialog, which) -> {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(
                                    CLIPBOARD_SERVICE
                            );

                    clipboard.setPrimaryClip(
                            ClipData.newPlainText("Pix", payload)
                    );

                    Toast.makeText(this, "Código Pix copiado!",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void salvarNoHistorico(String destino) {
        try {
            JSONArray array = new JSONArray(
                    db.getString("corridas", "[]")
            );

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
            JSONArray array = new JSONArray(
                    db.getString("corridas", "[]")
            );

            if (array.length() == 0) {
                Toast.makeText(this, "Nenhuma corrida realizada.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < array.length(); i++) {
                JSONObject c = array.getJSONObject(i);

                sb.append("📍 ")
                        .append(c.getString("destino"))
                        .append(" - R$ ")
                        .append(c.getDouble("valor"))
                        .append("\n");
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

    private String gerarPix(
            String chave, String nome, String cidade,
            double valor, String txid
    ) {
        String conta = campo("00", "br.gov.bcb.pix")
                + campo("01", chave);

        String payload = campo("00", "01")
                + campo("26", conta)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", String.format(Locale.US, "%.2f", valor))
                + campo("58", "BR")
                + campo("59", nome)
                + campo("60", cidade)
                + campo("62", campo("05", txid))
                + "6304";

        return payload + crc16(payload);
    }

    private String campo(String id, String valor) {
        return id + String.format(Locale.US, "%02d", valor.length())
                + valor;
    }

    private String crc16(String texto) {
        int crc = 0xFFFF;

        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }

                crc &= 0xFFFF;
            }
        }

        return String.format(Locale.US, "%04X", crc);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    
    private void abrirTelaConta() {
    EditText email = new EditText(this);
    email.setHint("Seu e-mail");
    email.setInputType(33);

    EditText senha = new EditText(this);
    senha.setHint("Sua senha");
    senha.setInputType(129);

    EditText nome = new EditText(this);
    nome.setHint("Seu nome (para cadastro)");

    android.widget.LinearLayout layout =
            new android.widget.LinearLayout(this);

    layout.setOrientation(
            android.widget.LinearLayout.VERTICAL
    );
    layout.setPadding(40, 20, 40, 10);

    layout.addView(nome);
    layout.addView(email);
    layout.addView(senha);

    new AlertDialog.Builder(this)
            .setTitle("Conta Street UBER")
            .setView(layout)
            .setPositiveButton("Criar conta", (d, w) -> {
                Toast.makeText(this,
                        "Cadastro será conectado ao Supabase na próxima etapa.",
                        Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Entrar", (d, w) -> {
                Toast.makeText(this,
                        "Login será conectado ao Supabase na próxima etapa.",
                        Toast.LENGTH_LONG).show();
            })
            .setNeutralButton("Cancelar", null)
            .show();
    }
}
