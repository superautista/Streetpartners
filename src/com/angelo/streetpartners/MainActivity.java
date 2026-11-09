package com.angelo.streetpartners;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    private MediaRecorder gravador;
    private boolean estaGravando = false;
    private boolean piscando = false;
    private String caminhoAudio;
    private String caminhoFoto;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSOS = findViewById(R.id.btnSOS);
        Button btnCacador = findViewById(R.id.btnCacador);
        Button btnSusto = findViewById(R.id.btnSusto);

        // Pede todas as permissões de uma vez
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || 
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO, 
                Manifest.permission.CAMERA, 
                Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }

        btnSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "🚨 SOS ACIONADO! Localização enviada.", Toast.LENGTH_LONG).show();
            }
        });

        btnCacador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!estaGravando) {
                    iniciarGravacao();
                    tirarFotoSecreta();
                } else {
                    pararGravacao();
                }
            }
        });

        btnSusto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!piscando) {
                    iniciarSusto();
                }
            }
        });
    }

    private void iniciarGravacao() {
        String local = "sem_gps";
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) local = loc.getLatitude() + "_" + loc.getLongitude();
            }
        } catch (Exception e) { }

        String nomeArquivo = "prova_" + System.currentTimeMillis() + "_" + local;
        File pasta = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        caminhoAudio = pasta.getAbsolutePath() + "/" + nomeArquivo + ".3gp";
        caminhoFoto = pasta.getAbsolutePath() + "/" + nomeArquivo + ".jpg";

        gravador = new MediaRecorder();
        gravador.setAudioSource(MediaRecorder.AudioSource.MIC);
        gravador.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        gravador.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        gravador.setOutputFile(caminhoAudio);

        try {
            gravador.prepare();
            gravador.start();
            estaGravando = true;
            Toast.makeText(this, "🕵️ Modo Caçador: Gravando áudio e GPS...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao iniciar gravação.", Toast.LENGTH_SHORT).show();
        }
    }

    private void tirarFotoSecreta() {
        try {
            camera = Camera.open();
            camera.setPreviewTexture(new SurfaceTexture(0)); 
            camera.startPreview();
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        FileOutputStream fos = new FileOutputStream(caminhoFoto);
                        fos.write(data);
                        fos.close();
                        camera.release();
                        Toast.makeText(MainActivity.this, "📸 Foto secreta salva!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { camera.release(); }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Câmera ocupada, apenas áudio rodando.", Toast.LENGTH_SHORT).show();
        }
    }

    private void pararGravacao() {
        try {
            gravador.stop();
            gravador.release();
            gravador = null;
            estaGravando = false;
            Toast.makeText(this, "🕵️ Gravação salva no cofre!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao parar gravação.", Toast.LENGTH_SHORT).show();
        }
    }

    private void iniciarSusto() {
        piscando = true;
        Toast.makeText(this, "💀 MODO SUSTO ATIVADO! 💀", Toast.LENGTH_SHORT).show();
        
        final View root = findViewById(R.id.telaPrincipal);
        final Handler handler = new Handler();
        
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) { v.vibrate(10000); }

        Runnable runnable = new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (count % 2 == 0) {
                    root.setBackgroundColor(0xFFFF0000);
                    Toast.makeText(MainActivity.this, "SISTEMA INVADIDO...", Toast.LENGTH_SHORT).show();
                } else {
                    root.setBackgroundColor(0xFF000000);
                }
                count++;
                if (count < 20) {
                    handler.postDelayed(this, 500);
                } else {
                    root.setBackgroundColor(0xFF000000);
                    piscando = false;
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissões concedidas. Sistema armado.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
