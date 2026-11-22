
package com.angelo.streetuberautismos;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Looper;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String SUPABASE_URL =
        "https://vxsxjddaswcgnknolpwz.supabase.co";

    private static final String SUPABASE_KEY =
        "sb_publishable_wzEU2j3tsiNs15WAqvmFOw_QElCYbRu";

    private static final double TARIFA_INICIAL = 7.70;
    private static final double VALOR_KM = 1.50;
    private static final double VALOR_MINUTO = 0.30;

    private String accessToken = "";
    private String usuarioId = "";

    private EditText editDestino;
    private Button btnChamar, btnWaze, btnPagar;
    private Button btnHistorico, btnConta;
    private TextView txtStatus;
    private WebView webViewMapa;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private double latitudeOrigem;
    private double longitudeOrigem;
    private double latitudeDestino;
    private double longitudeDestino;

    private double distanciaKm;
    private double duracaoMin;
    private double valorCorrida;

    private String destinoAtual = "";
    private String origemAtual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarTela();
    }

    private void inicializarTela() {

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

        btnChamar.setOnClickListener(v -> solicitarLocalizacao());

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
        conexao.setConnectTimeout(20000);
        conexao.setReadTimeout(20000);

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
                conexao.setRequestProperty(
                    "Prefer", preferencia
                );
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

        return new JSONObject();
    }

    // LOGIN E CADASTRO

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
                    caminho, "POST", dados, false, null
                );

                String token =
                    resposta.optString("access_token", "");

                JSONObject usuario =
                    resposta.optJSONObject("user");

                String id = usuario == null
                    ? ""
                    : usuario.optString("id", "");

                if (token.isEmpty() || id.isEmpty()) {
                    throw new Exception(
                        "Se o cadastro foi realizado, confirme o e-mail antes de entrar."
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
                    txtStatus.setText("Conta conectada!");
                    Toast.makeText(
                        this,
                        "Login realizado!",
                        Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> mostrarErro(e.getMessage()));
            }
        }).start();
    }

    private void abrirTelaConta() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 15, 25, 10);

        EditText nome = new EditText(this);
        nome.setHint("Nome completo");

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
            .setView(layout)
            .setPositiveButton("Entrar", (d, w) ->
                autenticar(
                    false, "",
                    email.getText().toString().trim(),
                    senha.getText().toString()
                )
            )
            .setNegativeButton("Cadastrar", (d, w) ->
                autenticar(
                    true,
                    nome.getText().toString().trim(),
                    email.getText().toString().trim(),
                    senha.getText().toString()
                )
            )
            .setNeutralButton("Cancelar", null)
            .show();
    }

    // GPS

    private void solicitarLocalizacao() {

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

        if (checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                100
            );

            return;
        }

        obterLocalizacao();
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == 100) {
            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                obterLocalizacao();

            } else {
                mostrarErro(
                    "A autorização de localização é necessária para calcular a rota."
                );
            }
        }
    }

    private void obterLocalizacao() {

        try {
            locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

            Location ultima = null;

            if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
                ultima = locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                );
            }

            if (ultima == null &&
                checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

                ultima = locationManager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                );
            }

            if (ultima != null &&
                System.currentTimeMillis() -
                ultima.getTime() < 120000) {

                usarLocalizacao(ultima);
                return;
            }

            solicitarAtualizacaoGPS();

        } catch (Exception e) {
            mostrarErro("Erro ao obter GPS: " + e.getMessage());
        }
    }

    private void solicitarAtualizacaoGPS() {

        txtStatus.setText("Obtendo localização GPS...");

        try {
            locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    locationManager.removeUpdates(this);
                    usarLocalizacao(location);
                }

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}

                @Override
                public void onStatusChanged(
                    String provider, int status, Bundle extras
                ) {}
            };

            boolean gps =
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                );

            String provedor = gps
                ? LocationManager.GPS_PROVIDER
                : LocationManager.NETWORK_PROVIDER;

            locationManager.requestSingleUpdate(
                provedor,
                locationListener,
                Looper.getMainLooper()
            );

        } catch (Exception e) {
            mostrarErro(
                "Ative a localização do celular e tente novamente."
            );
        }
    }

    private void usarLocalizacao(Location location) {

        latitudeOrigem = location.getLatitude();
        longitudeOrigem = location.getLongitude();

        origemAtual = String.format(
            Locale.US,
            "%.6f, %.6f",
            latitudeOrigem,
            longitudeOrigem
        );

        txtStatus.setText(
            "Origem localizada. Calculando rota..."
        );

        calcularRota();
    }

    // GEOCODIFICAÇÃO E ROTA

    private void calcularRota() {

        new Thread(() -> {
            try {
                String consulta = URLEncoder.encode(
                    destinoAtual,
                    "UTF-8"
                );

                String endereco =
                    "https://nominatim.openstreetmap.org/search?q="
                    + consulta
                    + "&format=json&limit=1";

                HttpURLConnection geo =
                    (HttpURLConnection)
                    new URL(endereco).openConnection();

                geo.setRequestProperty(
                    "User-Agent",
                    "StreetUber/1.0 Android"
                );

                geo.setConnectTimeout(20000);
                geo.setReadTimeout(20000);

                String textoGeo = lerResposta(geo);
                JSONArray resultados = new JSONArray(textoGeo);

                if (resultados.length() == 0) {
                    throw new Exception(
                        "Destino não encontrado. Digite um endereço mais completo."
                    );
                }

                JSONObject lugar =
                    resultados.getJSONObject(0);

                latitudeDestino = Double.parseDouble(
                    lugar.getString("lat")
                );

                longitudeDestino = Double.parseDouble(
                    lugar.getString("lon")
                );

                String rota =
                    "https://router.project-osrm.org/route/v1/driving/"
                    + longitudeOrigem + "," + latitudeOrigem
                    + ";"
                    + longitudeDestino + "," + latitudeDestino
                    + "?overview=false";

                HttpURLConnection conexao =
                    (HttpURLConnection)
                    new URL(rota).openConnection();

                conexao.setConnectTimeout(20000);
                conexao.setReadTimeout(20000);

                String textoRota = lerResposta(conexao);

                JSONObject resposta =
                    new JSONObject(textoRota);

                JSONArray rotas =
                    resposta.getJSONArray("routes");

                if (rotas.length() == 0) {
                    throw new Exception("Rota não encontrada.");
                }

                JSONObject primeira = rotas.getJSONObject(0);

                distanciaKm =
                    primeira.getDouble("distance") / 1000.0;

                duracaoMin =
                    primeira.getDouble("duration") / 60.0;

                valorCorrida = TARIFA_INICIAL
                    + (distanciaKm * VALOR_KM)
                    + (duracaoMin * VALOR_MINUTO);

                valorCorrida =
                    Math.round(valorCorrida * 100.0) / 100.0;

                runOnUiThread(() -> mostrarEstimativa());

            } catch (Exception e) {
                runOnUiThread(() -> {
                    txtStatus.setText("Erro ao calcular a rota.");
                    mostrarErro(e.getMessage());
                });
            }
        }).start();
    }

    private String lerResposta(HttpURLConnection conexao)
        throws Exception {

        int codigo = conexao.getResponseCode();

        InputStream entrada =
            codigo >= 200 && codigo < 300
            ? conexao.getInputStream()
            : conexao.getErrorStream();

        if (entrada == null) {
            throw new Exception("Servidor sem resposta.");
        }

        Scanner scanner = new Scanner(entrada)
            .useDelimiter("\\A");

        String resposta =
            scanner.hasNext() ? scanner.next() : "";

        scanner.close();
        entrada.close();
        conexao.disconnect();

        if (codigo < 200 || codigo >= 300) {
            throw new Exception(
                "Erro HTTP " + codigo + ": " + resposta
            );
        }

        return resposta;
    }

    private void mostrarEstimativa() {

        String resumo = String.format(
            Locale.forLanguageTag("pt-BR"),
            "Distância: %.2f km\n" +
            "Duração estimada: %.0f minutos\n\n" +
            "Tarifa inicial: R$ 7,70\n" +
            "Distância: R$ %.2f\n" +
            "Tempo: R$ %.2f\n\n" +
            "TOTAL ESTIMADO: R$ %.2f",
            distanciaKm,
            duracaoMin,
            distanciaKm * VALOR_KM,
            duracaoMin * VALOR_MINUTO,
            valorCorrida
        );

        txtStatus.setText(resumo);

        new AlertDialog.Builder(this)
            .setTitle("Confirmar corrida")
            .setMessage(resumo)
            .setPositiveButton("Solicitar", (d, w) ->
                registrarCorrida()
            )
            .setNegativeButton("Cancelar", null)
            .show();
    }

    // REGISTRO DA CORRIDA

    private void registrarCorrida() {

        btnChamar.setEnabled(false);

        new Thread(() -> {
            try {
                JSONObject dados = new JSONObject();

                dados.put("passageiro_id", usuarioId);
                dados.put("origem", origemAtual);
                dados.put("destino", destinoAtual);
                dados.put("distancia_km", distanciaKm);
                dados.put("duracao_min", duracaoMin);
                dados.put("valor", valorCorrida);
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

                if (corridas == null ||
                    corridas.length() == 0) {
                    throw new Exception(
                        "Servidor não confirmou o registro."
                    );
                }

                long id = corridas.getJSONObject(0)
                    .getLong("id");

                salvarNoHistorico(
                    "Corrida #" + id +
                    " - " + destinoAtual +
                    " - R$ " +
                    String.format(Locale.US, "%.2f", valorCorrida)
                );

                runOnUiThread(() -> {
                    btnChamar.setEnabled(true);

                    txtStatus.setText(
                        "Corrida #" + id +
                        "\nSolicitação registrada!" +
                        "\nAguardando motorista."
                    );

                    Toast.makeText(
                        this,
                        "Corrida enviada!",
                        Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnChamar.setEnabled(true);
                    mostrarErro(
                        "Erro ao registrar corrida: " +
                        e.getMessage()
                    );
                });
            }
        }).start();
    }

    // MOTORISTA

    private void abrirTelaMotorista() {

        if (accessToken.isEmpty()) {
            mostrarErro("Faça login primeiro.");
            return;
        }

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setPadding(20, 20, 20, 20);

        TextView titulo = new TextView(this);
        titulo.setText("CORRIDAS DISPONÍVEIS");
        titulo.setTextSize(22);
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
            inicializarTela();
        });

        setContentView(tela);

        carregarCorridasMotorista(lista);
    }

    private void carregarCorridasMotorista(
        LinearLayout lista
    ) {

        lista.removeAllViews();

        TextView carregando = new TextView(this);
        carregando.setText("Buscando corridas...");
        lista.addView(carregando);

        new Thread(() -> {
            try {
                JSONObject resposta = requisicao(
                    "/rest/v1/corridas?status=eq.pendente"
                    + "&motorista_id=is.null"
                    + "&select=id,origem,destino,valor,"
                    + "distancia_km,duracao_min"
                    + "&order=criada_em.desc",
                    "GET",
                    null,
                    true,
                    null
                );

                JSONArray corridas =
                    resposta.optJSONArray("dados");

                runOnUiThread(() -> {
                    lista.removeAllViews();

                    if (corridas == null ||
                        corridas.length() == 0) {

                        TextView vazio = new TextView(this);
                        vazio.setText(
                            "Nenhuma corrida disponível."
                        );
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

                            card.setPadding(12, 15, 12, 15);

                            TextView texto = new TextView(this);

                            texto.setText(
                                "Corrida #" + id +
                                "\nOrigem: " +
                                corrida.optString("origem") +
                                "\nDestino: " +
                                corrida.optString("destino") +
                                "\nDistância: " +
                                corrida.optString("distancia_km") +
                                " km\nDuração: " +
                                corrida.optString("duracao_min") +
                                " min\nValor: R$ " +
                                corrida.optString("valor")
                            );

                            texto.setTextSize(16);
                            card.addView(texto);

                            Button aceitar = new Button(this);
                            aceitar.setText("ACEITAR CORRIDA");

                            aceitar.setOnClickListener(v -> {
                                aceitar.setEnabled(false);
                                aceitarCorridaMotorista(
                                    id, lista
                                );
                            });

                            card.addView(aceitar);
                            lista.addView(card);

                        } catch (Exception e) {
                            mostrarErro(e.getMessage());
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    lista.removeAllViews();

                    TextView erro = new TextView(this);
                    erro.setText(e.getMessage());
                    lista.addView(erro);
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

            } catch (Exception e) {
                runOnUiThread(() -> {
                    mostrarErro(e.getMessage());
                    carregarCorridasMotorista(lista);
                });
            }
        }).start();
    }

    // MAPA E WAZE

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
            mostrarErro("Digite um destino primeiro.");
            return;
        }

        try {
            String consulta = URLEncoder.encode(
                destino, "UTF-8"
            );

            Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://waze.com/ul?q=" +
                    consulta + "&navigate=yes"
                )
            );

            startActivity(intent);

        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    // PIX

   
private void abrirPix() {

    if (valorCorrida <= 0) {
        new AlertDialog.Builder(this)
            .setTitle("Pagamento via Pix")
            .setMessage(
                "Primeiro, calcule uma rota e solicite uma corrida."
            )
            .setPositiveButton("OK", null)
            .show();
        return;
    }

    double valorDistancia = distanciaKm * VALOR_KM;
    double valorTempo = duracaoMin * VALOR_MINUTO;

    String resumo = String.format(
        Locale.forLanguageTag("pt-BR"),
        "Resumo do pagamento:\n\n" +
        "Taxa inicial: R$ 7,70\n" +
        "Distância (%.2f km): R$ %.2f\n" +
        "Tempo (%.0f min): R$ %.2f\n\n" +
        "TOTAL PARA PAGAMENTO: R$ %.2f\n\n" +
        "Use esse valor ao realizar o Pix.",
        distanciaKm,
        valorDistancia,
        duracaoMin,
        valorTempo,
        valorCorrida
    );

    new AlertDialog.Builder(this)
        .setTitle("Pagamento via Pix")
        .setMessage(resumo)
        .setPositiveButton("OK", null)
        .show();
}


    // HISTÓRICO

    private void salvarNoHistorico(String destino) {

        SharedPreferences prefs =
            getSharedPreferences("Historico", MODE_PRIVATE);

        String antigo =
            prefs.getString("corridas", "");

        String novo = destino + "\n\n" + antigo;

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
            .setMessage(
                mensagem == null
                ? "Erro desconhecido."
                : mensagem
            )
            .setPositiveButton("OK", null)
            .show();
    }
}
