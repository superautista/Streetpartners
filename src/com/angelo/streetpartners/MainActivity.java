package com.angelo.streetpartners;
    
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.Manifest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText editDestino;
    private Button btnChamar, btnPagar, btnWaze;
    private TextView txtStatus;

    private FirebaseAuth auth;
    private DatabaseReference banco;

    private LocationManager locationManager;
    private Location minhaLocalizacao;

    private String uid;
    private String corridaId;

    private static final int PEDIDO_GPS = 100;

    // Mude para "motorista" no aplicativo do motorista.
    private static final String TIPO_USUARIO = "passageiro";

    private static final double VALOR_CORRIDA = 15.50;

    // Configure com seus próprios dados Pix.
    private static final String CHAVE_PIX = "14988376670";
    private static final String NOME_RECEBEDOR = "ANGELO SILVA";
    private static final String CIDADE = "GETULINA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editDestino = findViewById(R.id.editDestino);
        btnChamar = findViewById(R.id.btnChamar);
        btnPagar = findViewById(R.id.btnPagar);
        btnWaze = findViewById(R.id.btnWaze);
        txtStatus = findViewById(R.id.txtStatus);

        btnPagar.setEnabled(false);

        banco = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        iniciarFirebase();

        btnChamar.setOnClickListener(v -> chamarCorrida());

        btnWaze.setOnClickListener(v -> {
            String destino = editDestino.getText().toString().trim();

            if (!destino.isEmpty()) {
                abrirWaze(destino);
            } else {
                Toast.makeText(this,
                        "Digite um destino!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnPagar.setOnClickListener(v -> gerarPagamento());
    }

    // AUTENTICAÇÃO FIREBASE
    private void iniciarFirebase() {
        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
            iniciarGPS();
            return;
        }

        auth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uid = auth.getCurrentUser().getUid();
                txtStatus.setText("Conectado ao servidor!");
                iniciarGPS();
            } else {
                txtStatus.setText("Erro ao conectar ao Firebase.");
            }
        });
    }

    // LOCALIZAÇÃO GPS
    private void iniciarGPS() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PEDIDO_GPS
            );
            return;
        }

        try {
            String provedor =
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
                            ? LocationManager.GPS_PROVIDER
                            : LocationManager.NETWORK_PROVIDER;

            locationManager.requestLocationUpdates(
                    provedor,
                    5000,
                    5,
                    ouvinteGPS
            );

            Location ultima =
                    locationManager.getLastKnownLocation(provedor);

            if (ultima != null) {
                atualizarLocalizacao(ultima);
            }

        } catch (Exception e) {
            txtStatus.setText("Não foi possível iniciar o GPS.");
        }
    }

    private final LocationListener ouvinteGPS = location -> {
        atualizarLocalizacao(location);
    };

    private void atualizarLocalizacao(Location location) {
        minhaLocalizacao = location;

        if (uid == null) return;

        Map<String, Object> dados = new HashMap<>();
        dados.put("latitude", location.getLatitude());
        dados.put("longitude", location.getLongitude());
        dados.put("atualizadoEm", System.currentTimeMillis());

        String caminho = TIPO_USUARIO.equals("motorista")
                ? "motoristas"
                : "passageiros";

        banco.child(caminho)
                .child(uid)
                .child("localizacao")
                .updateChildren(dados);
    }

    // SOLICITAR CORRIDA
    private void chamarCorrida() {
        if (uid == null) {
            Toast.makeText(this,
                    "Aguardando conexão com o servidor.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String destino = editDestino.getText().toString().trim();

        if (destino.isEmpty()) {
            editDestino.setError("Digite seu destino");
            return;
        }

        if (TIPO_USUARIO.equals("motorista")) {
            Toast.makeText(this,
                    "Este botão é para solicitar corridas.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnChamar.setEnabled(false);
        txtStatus.setText("Solicitando motorista...");

        DatabaseReference novaCorrida =
                banco.child("corridas").push();

        corridaId = novaCorrida.getKey();

        Map<String, Object> dados = new HashMap<>();

        dados.put("id", corridaId);
        dados.put("passageiroId", uid);
        dados.put("destino", destino);
        dados.put("valor", VALOR_CORRIDA);
        dados.put("status", "aguardando_motorista");
        dados.put("criadaEm", System.currentTimeMillis());

        if (minhaLocalizacao != null) {
            dados.put("origemLatitude",
                    minhaLocalizacao.getLatitude());
            dados.put("origemLongitude",
                    minhaLocalizacao.getLongitude());
        }

        novaCorrida.setValue(dados)
                .addOnSuccessListener(unused -> {
                    txtStatus.setText("Corrida solicitada!");

                    btnPagar.setEnabled(true);

                    Toast.makeText(this,
                            "Corrida salva no Firebase!",
                            Toast.LENGTH_SHORT).show();

                    acompanharCorrida();
                })
                .addOnFailureListener(e -> {
                    btnChamar.setEnabled(true);
                    txtStatus.setText("Erro ao solicitar corrida.");
                });
    }

    // ACOMPANHAR STATUS DA CORRIDA
    private void acompanharCorrida() {
        if (corridaId == null) return;

        banco.child("corridas")
                .child(corridaId)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        String status =
                                snapshot.child("status")
                                        .getValue(String.class);

                        if (status == null) return;

                        switch (status) {
                            case "aguardando_motorista":
                                txtStatus.setText(
                                        "Aguardando motorista...");
                                break;

                            case "motorista_a_caminho":
                                txtStatus.setText(
                                        "Motorista a caminho!");
                                break;

                            case "em_andamento":
                                txtStatus.setText(
                                        "Corrida em andamento!");
                                break;

                            case "concluida":
                                txtStatus.setText(
                                        "Corrida concluída!");
                                btnPagar.setEnabled(true);
                                break;

                            case "cancelada":
                                txtStatus.setText(
                                        "Corrida cancelada.");
                                break;
                        }

                        String motoristaId =
                                snapshot.child("motoristaId")
                                        .getValue(String.class);

                        if (motoristaId != null) {
                            acompanharMotorista(motoristaId);
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                        txtStatus.setText(
                                "Erro ao acompanhar corrida.");
                    }
                });
    }

    // ACOMPANHAR LOCALIZAÇÃO DO MOTORISTA
    private void acompanharMotorista(String motoristaId) {
        banco.child("motoristas")
                .child(motoristaId)
                .child("localizacao")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        Double lat = snapshot.child("latitude")
                                .getValue(Double.class);

                        Double lon = snapshot.child("longitude")
                                .getValue(Double.class);

                        if (lat != null && lon != null) {
                            txtStatus.setText(
                                    "Motorista localizado: "
                                    + lat + ", " + lon);
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                    }
                });
    }

    // ABRIR WAZE
    private void abrirWaze(String destino) {
        String url = "https://waze.com/ul?q="
                + Uri.encode(destino)
                + "&navigate=yes";

        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
        );

        try {
            intent.setPackage("com.waze");
            startActivity(intent);
        } catch (Exception e) {
            intent.setPackage(null);
            startActivity(intent);
        }
    }

    // GERAR PIX
    private void gerarPagamento() {
        if (corridaId == null) {
            Toast.makeText(this,
                    "Nenhuma corrida encontrada.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String txid = "ROTA"
                + String.valueOf(System.currentTimeMillis());

        String payload = gerarPix(
                CHAVE_PIX,
                NOME_RECEBEDOR,
                CIDADE,
                VALOR_CORRIDA,
                txid
        );

        new AlertDialog.Builder(this)
                .setTitle("Pix Copia e Cola")
                .setMessage(payload)
                .setPositiveButton("Copiar", (dialog, which) -> {

                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(
                                    CLIPBOARD_SERVICE);

                    clipboard.setPrimaryClip(
                            ClipData.newPlainText("Pix", payload));

                    Toast.makeText(this,
                            "Código Pix copiado!",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private String gerarPix(
            String chave,
            String nome,
            String cidade,
            double valor,
            String txid) {

        String conta = campo("00", "br.gov.bcb.pix")
                + campo("01", chave);

        String payload =
                campo("00", "01")
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
        return id
                + String.format(Locale.US, "%02d", valor.length())
                + valor;
    }

    private String crc16(String texto) {
        int crc = 0xFFFF;

        for (byte b : texto.getBytes(
                java.nio.charset.StandardCharsets.UTF_8)) {

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
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == PEDIDO_GPS) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                iniciarGPS();
            } else {
                Toast.makeText(this,
                        "GPS não autorizado.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            locationManager.removeUpdates(ouvinteGPS);
        }
    }
}
