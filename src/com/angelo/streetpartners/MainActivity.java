
package com.angelo.streetuberautismos;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.*;
import android.view.View;
import android.graphics.Color;

import org.json.JSONObject;
import org.json.JSONArray;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

public class MainActivity extends Activity {

    private static final String SUPABASE_URL =
        "https://vxsxjddaswcgnknolpwz.supabase.co";

    // COLE AQUI SUA CHAVE PUBLICÁVEL sb_publishable_
    private static final String SUPABASE_KEY =
        "sb_publishable_wzEU2j3tsiNs15WAqvmFOw_QElCYbRu";

    private static final String PIX = "14988376670";

    private String accessToken = "";
    private String usuarioId = "";
    private String destinoAtual = "";

    private EditText editDestino;
    private Button btnChamar, btnWaze, btnPagar;
    private Button btnHistorico, btnConta;
    private TextView txtStatus;
    private WebView webViewMapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editDestino = findViewById(R.id.editDestino);
        btnChamar = findViewById(R.id.btnChamar);
        btnWaze = findViewById(R.id.btnWaze);
        btnPagar = findViewById(R.id.btnPagar);
        btnHistorico = findViewById(R.id.btnHistorico);
        btnConta = findViewById(R.id.btnConta);
        txtStatus = findViewById(R.id.txtStatus);
        webViewMapa = findViewById(R.id.webViewMapa);

        SharedPreferences prefs =
            getSharedPreferences("StreetUber", MODE_PRIVATE);

        accessToken = prefs.getString("token", "");
        usuarioId = prefs.getString("usuario", "");

        configurarMapa();

        btnConta.setOnClickListener(v -> abrirTelaConta());

        btnConta.setOnLongClickListener(v -> {
            abrirTelaMotorista();
            return true;
        });

        btnChamar.setOnClickListener(v -> registrarCorrida());

        btnWaze.setOnClickListener(v -> abrirWaze());

        btnPagar.setOnClickListener(v -> abrirPix());

        btnHistorico.setOnClickListener(v -> mostrarHistorico());
    }

    private JSONObject requisicao(
        String caminho,
        String metodo,
        JSONObject dados,
        boolean autenticado,
        String preferencia
    ) throws Exception {

        URL url = new URL(SUPABASE_URL + caminho);

        HttpURLConnection conexao =
            (HttpURLConnection) url.openConnection();

        conexao.setRequestMethod(metodo);
        conexao.setConnectTimeout(15000);
        conexao.setReadTimeout(15000);

        conexao.setRequestProperty("apikey", SUPABASE_KEY);
        conexao.setRequestProperty("Accept", "application/json");

        if (autenticado) {
            conexao.setRequestProperty(
                "Authorization", "Bearer " + accessToken
            );
        }

        if (dados != null) {
            conexao.setDoOutput(true);
            conexao.setRequestProperty(
                "Content-Type", "application/json"
            );

            if (preferencia != null) {
                conexao.setRequestProperty("Prefer", preferencia);
            }

            OutputStream saida = conexao.getOutputStream();
            saida.write(dados.toString().getBytes("UTF-8"));
            saida.close();
        }

        int codigo = conexao.getResponseCode();

        InputStream entrada =
            codigo >= 200 && codigo < 300
            ? conexao.getInputStream()
            : conexao.getErrorStream();

        String resposta = "";

        if (entrada != null) {
            Scanner scanner = new Scanner(entrada)
                .useDelimiter("\\A");

            resposta = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
            entrada.close();
        }

        conexao.disconnect();

        if (codigo < 200 || codigo >= 300) {
            throw new Exception(
                "HTTP " + codigo + ": " + resposta
            );
        }

        if (resposta.trim().isEmpty()) {
            return new JSONObject();
        }

        if (resposta.trim().startsWith("[")) {
            JSONObject resultado = new JSONObject();
            resultado.put("dados", new JSONArray(resposta));
            return resultado;
        }

        if (resposta.trim().startsWith("{")) {
            return new JSONObject(resposta);
        }

        JSONObject resultado = new JSONObject();
        resultado.put("resposta", resposta);
        return resultado;
    }

    private void autenticar(
        boolean cadastro,
        String nome,
        String email,
        String senha
    ) {

        new Thread(() -> {
            try {
                JSONObject dados = new JSONObject();

                dados.put("email", email);
                dados.put("password", senha);

                String caminho;

                if (cadastro) {
                    caminho = "/auth/v1/signup";

                    JSONObject metadata = new JSONObject();
                    metadata.put("nome", nome);
                    dados.put("data", metadata);

                } else {
                    caminho =
                        "/auth/v1/token?grant_type=password";
                }

                JSONObject resposta = requisicao(
                    caminho,
                    "POST",
                    dados,
                    false,
                    null
                );

                String token = resposta.optString(
                    "access_token", ""
                );

                String id = "";

                JSONObject usuario =
                    resposta.optJSONObject("user");

                if (usuario != null) {
                    id = usuario.optString("id", "");
                }

                if (token.isEmpty()) {
                    throw new Exception(
                        "Cadastro recebido. Se a confirmação de e-mail estiver ativa, confirme seu e-mail e faça login."
                    );
                }

                accessToken = token;
                usuarioId = id;

                getSharedPreferences(
                    "StreetUber", MODE_PRIVATE
                ).edit()
                    .putString("token", token)
                    .putString("usuario", id)
                    .apply();

                runOnUiThread(() -> {
                    txtStatus.setText("Login realizado com sucesso!");
                    Toast.makeText(
                        this,
                        "Bem-vindo ao Street UBER!",
                        Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception erro) {
                runOnUiThread(() ->
                    mostrarErro(erro.getMessage())
                );
            }
        }).start();
    }

    private void abrirTelaConta() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 10);

        EditText nome = new EditText(this);
        nome.setHint("Seu nome");

        EditText email = new EditText(this);
        email.setHint("E-mail");
        email.setInputType(33);

        EditText senha = new EditText(this);
        senha.setHint("Senha");
        senha.setInputType(129);

        layout.addView(nome);
        layout.addView(email);
        layout.addView(senha);

        new AlertDialog.Builder(this)
            .setTitle("Conta Street UBER")
            .setMessage("Entre ou crie sua conta.")
            .setView(layout)
            .setPositiveButton("Entrar", (d, w) ->
                autenticar(
                    false,
                    "",
                    email.getText().toString(),
                    senha.getText().toString()
                )
            )
            .setNegativeButton("Cadastrar", (d, w) ->
                autenticar(
                    true,
                    nome.getText().toString(),
                    email.getText().toString(),
                    senha.getText().toString()
                )
            )
            .setNeutralButton("Cancelar", null)
            .show();
    }

    private void registrarCorrida() {

        destinoAtual = editDestino.getText()
            .toString().trim();

        if (destinoAtual.isEmpty()) {
            editDestino.setError("Digite seu destino");
            return;
        }

        if (accessToken.isEmpty() || usuarioId.isEmpty()) {
            mostrarErro("Entre na sua conta antes de solicitar uma corrida.");
            return;
        }

        btnChamar.setEnabled(false);
        txtStatus.setText("Enviando solicitação...");

        new Thread(() -> {
            try {
                JSONObject dados = new JSONObject();

                dados.put("passageiro_id", usuarioId);
                dados.put("origem", "Origem não informada");
                dados.put("destino", destinoAtual);
                dados.put("valor", 15.50);
                dados.put("status", "pendente");

                JSONObject resposta = requisicao(
                    "/rest/v1/corridas",
                    "POST",
                    dados,
                    true,
                    "return=representation"
                );

                JSONArray corridas =
                    resposta.optJSONArray("dados");

                if (corridas == null || corridas.length() == 0) {
                    throw new Exception(
                        "Servidor não confirmou o registro."
                    );
                }

                long id = corridas.getJSONObject(0)
                    .getLong("id");

                salvarNoHistorico(destinoAtual);

                runOnUiThread(() -> {
                    txtStatus.setText(
                        "Corrida #" + id +
                        "\nSolicitação registrada!" +
                        "\nAguardando motorista."
                    );

                    btnPagar.setEnabled(true);
                    btnChamar.setEnabled(true);

                    Toast.makeText(
                        this,
                        "Corrida enviada ao servidor!",
                        Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception erro) {
                runOnUiThread(() -> {
                    btnChamar.setEnabled(true);
                    txtStatus.setText(
                        "Erro ao registrar: " + erro.getMessage()
                    );
                    mostrarErro(erro.getMessage());
                });
            }
        }).start();
    }

    private void abrirTelaMotorista() {

        if (accessToken.isEmpty()) {
            mostrarErro("Faça login primeiro.");
            return;
        }

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setPadding(24, 24, 24, 24);

        TextView titulo = new TextView(this);
        titulo.setText("CORRIDAS DISPONÍVEIS");
        titulo.setTextSize(23);
        titulo.setTextColor(Color.WHITE);
        tela.addView(titulo);

        Button atualizar = new Button(this);
        atualizar.setText("Atualizar corridas");
        tela.addView(atualizar);

        Button voltar = new Button(this);
        voltar.setText("Voltar");
        tela.addView(voltar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(lista);
        tela.addView(scroll);

        atualizar.setOnClickListener(v ->
            carregarCorridasMotorista(lista)
        );

        voltar.setOnClickListener(v -> {
            setContentView(R.layout.activity_main);
            recriarTela();
        });

        setContentView(tela);
        carregarCorridasMotorista(lista);
    }

    private void recriarTela() {
        editDestino = findViewById(R.id.editDestino);
        btnChamar = findViewById(R.id.btnChamar);
        btnWaze = findViewById(R.id.btnWaze);
        btnPagar = findViewById(R.id.btnPagar);
        btnHistorico = findViewById(R.id.btnHistorico);
        btnConta = findViewById(R.id.btnConta);
        txtStatus = findViewById(R.id.txtStatus);
        webViewMapa = findViewById(R.id.webViewMapa);

        configurarMapa();

        btnConta.setOnClickListener(v -> abrirTelaConta());
        btnConta.setOnLongClickListener(v -> {
            abrirTelaMotorista();
            return true;
        });
        btnChamar.setOnClickListener(v -> registrarCorrida());
        btnWaze.setOnClickListener(v -> abrirWaze());
        btnPagar.setOnClickListener(v -> abrirPix());
        btnHistorico.setOnClickListener(v -> mostrarHistorico());
    }

    private void carregarCorridasMotorista(
        LinearLayout lista
    ) {

        lista.removeAllViews();

        TextView carregando = new TextView(this);
        carregando.setText("Buscando corridas...");
        carregando.setTextColor(Color.WHITE);
        lista.addView(carregando);

        new Thread(() -> {
            try {
                JSONObject resposta = requisicao(
                    "/rest/v1/corridas?status=eq.pendente&motorista_id=is.null&select=id,origem,destino,valor&order=criada_em.desc",
                    "GET",
                    null,
                    true,
                    null
                );

                JSONArray corridas =
                    resposta.optJSONArray("dados");

                runOnUiThread(() -> {
                    lista.removeAllViews();

                    if (corridas == null || corridas.length() == 0) {
                        TextView vazio = new TextView(this);
                        vazio.setText("Nenhuma corrida pendente.");
                        vazio.setTextColor(Color.WHITE);
                        lista.addView(vazio);
                        return;
                    }

                    for (int i = 0; i < corridas.length(); i++) {
                        try {
                            JSONObject corrida =
                                corridas.getJSONObject(i);

                            long id = corrida.getLong("id");

                            LinearLayout card =
                                new LinearLayout(this);

                            card.setOrientation(
                                LinearLayout.VERTICAL
                            );
                            card.setPadding(15, 15, 15, 15);

                            TextView texto = new TextView(this);
                            texto.setTextColor(Color.WHITE);
                            texto.setTextSize(17);

                            texto.setText(
                                "Corrida #" + id +
                                "\nOrigem: " +
                                corrida.optString("origem") +
                                "\nDestino: " +
                                corrida.optString("destino") +
                                "\nValor: R$ " +
                                corrida.optString("valor")
                            );

                            card.addView(texto);

                            Button aceitar = new Button(this);
                            aceitar.setText("ACEITAR CORRIDA");

                            aceitar.setOnClickListener(v -> {
                                aceitar.setEnabled(false);
                                aceitarCorridaMotorista(id, lista);
                            });

                            card.addView(aceitar);
                            lista.addView(card);

                        } catch (Exception e) {
                            mostrarErro(e.getMessage());
                        }
                    }
                });

            } catch (Exception erro) {
                runOnUiThread(() -> {
                    lista.removeAllViews();

                    TextView mensagem = new TextView(this);
                    mensagem.setText(
                        "Erro ao buscar corridas: " +
                        erro.getMessage()
                    );
                    mensagem.setTextColor(Color.RED);
                    lista.addView(mensagem);
                });
            }
        }).start();
    }

    private void aceitarCorridaMotorista(
        long corridaId,
        LinearLayout lista
    ) {

        new Thread(() -> {
            try {
                JSONObject dados = new JSONObject();
                dados.put("p_corrida_id", corridaId);

                requisicao(
                    "/rest/v1/rpc/aceitar_corrida",
                    "POST",
                    dados,
                    true,
                    null
                );

                runOnUiThread(() -> {
                    Toast.makeText(
                        this,
                        "Corrida aceita!",
                        Toast.LENGTH_LONG
                    ).show();

                    carregarCorridasMotorista(lista);
                });

            } catch (Exception erro) {
                runOnUiThread(() -> {
                    mostrarErro(
                        "Não foi possível aceitar: " +
                        erro.getMessage()
                    );

                    carregarCorridasMotorista(lista);
                });
            }
        }).start();
    }

    private void configurarMapa() {

        String html =
            "<html><body style='margin:0'>" +
            "<iframe width='100%' height='100%' " +
            "src='https://www.openstreetmap.org/export/embed.html?bbox=-49.8%2C-22.0%2C-49.4%2C-21.5&layer=mapnik'>" +
            "</iframe></body></html>";

        webViewMapa.getSettings().setJavaScriptEnabled(true);
        webViewMapa.loadDataWithBaseURL(
            "https://www.openstreetmap.org",
            html,
            "text/html",
            "UTF-8",
            null
        );
    }

    private void abrirWaze() {

        String destino = editDestino.getText()
            .toString().trim();

        if (destino.isEmpty()) {
            Toast.makeText(
                this,
                "Digite um destino primeiro.",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            String consulta = URLEncoder.encode(
                destino,
                "UTF-8"
            );

            Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://waze.com/ul?q=" +
                    consulta + "&navigate=yes"
                )
            );

            startActivity(intent);

        } catch (Exception erro) {
            mostrarErro(erro.getMessage());
        }
    }

    private void abrirPix() {

        new AlertDialog.Builder(this)
            .setTitle("Pagamento via Pix")
            .setMessage(
                "Chave Pix:\n" + PIX +
                "\n\nValor previsto: R$ 15,50"
            )
            .setPositiveButton("Copiar chave", (d, w) -> {
                ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(
                        Context.CLIPBOARD_SERVICE
                    );

                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Pix", PIX)
                );

                Toast.makeText(
                    this,
                    "Chave Pix copiada!",
                    Toast.LENGTH_SHORT
                ).show();
            })
            .setNegativeButton("Fechar", null)
            .show();
    }

    private void salvarNoHistorico(String destino) {

        SharedPreferences prefs =
            getSharedPreferences("Historico", MODE_PRIVATE);

        String antigo = prefs.getString("corridas", "");

        String novo = destino + "\n" + antigo;

        prefs.edit()
            .putString("corridas", novo)
            .apply();
    }

    private void mostrarHistorico() {

        SharedPreferences prefs =
            getSharedPreferences("Historico", MODE_PRIVATE);

        String historico = prefs.getString(
            "corridas",
            "Nenhuma corrida registrada."
        );

        new AlertDialog.Builder(this)
            .setTitle("Histórico de corridas")
            .setMessage(historico)
            .setPositiveButton("Fechar", null)
            .show();
    }

    private void mostrarErro(String mensagem) {

        new AlertDialog.Builder(this)
            .setTitle("Street UBER")
            .setMessage(mensagem == null ? "Erro desconhecido." : mensagem)
            .setPositiveButton("OK", null)
            .show();
    }
}
