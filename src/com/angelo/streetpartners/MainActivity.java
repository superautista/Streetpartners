package com.angelo.streetpartners;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText editTextNotes;
    private Button btnPanic;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextNotes = findViewById(R.id.editTextNotes);
        btnPanic = findViewById(R.id.btnPanic);
        
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            requestPermissions(new String[]{
                Manifest.permission.SEND_SMS, 
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        }

        btnPanic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textoDigitado = editTextNotes.getText().toString();
                
                if (textoDigitado.toUpperCase().contains("AJUDA")) {
                    pegarLocalizacaoEDisparar();
                } else {
                    Toast.makeText(MainActivity.this, "Nota salva.", Toast.LENGTH_SHORT).show();
                    editTextNotes.setText(""); 
                }
            }
        });
    }

    private void pegarLocalizacaoEDisparar() {
        String linkMaps = " (Localizacao indisponivel)";
        
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                
                if (location != null) {
                    linkMaps = " Minha localizacao: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                }
            }
        } catch (Exception e) {
            linkMaps = " (Erro ao pegar GPS)";
        }
        
        dispararAlerta(linkMaps);
    }

    private void dispararAlerta(String localizacao) {
        String mensagemSocorro = "SOS! Estou sofrendo agressao/bullying. Preciso de ajuda." + localizacao;
        
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, mensagemSocorro);
        whatsappIntent.setPackage("com.whatsapp");
        
        try {
            startActivity(whatsappIntent);
            Toast.makeText(this, "ALERTA ENVIADO PARA O WHATSAPP!", Toast.LENGTH_LONG).show();
            editTextNotes.setText(""); 
        } catch (Exception e) {
            try {
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("smsto:")); 
                smsIntent.putExtra("sms_body", mensagemSocorro);
                startActivity(smsIntent);
                Toast.makeText(this, "ALERTA ENVIADO POR SMS!", Toast.LENGTH_LONG).show();
                editTextNotes.setText(""); 
            } catch (Exception e2) {
                Toast.makeText(this, "Erro ao abrir mensageiro.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
