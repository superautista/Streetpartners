package com.angelo.libras;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText editTexto = findViewById(R.id.editTexto);
        Button btnTraduzir = findViewById(R.id.btnTraduzir);
        TextView txtResultado = findViewById(R.id.txtResultado);

        btnTraduzir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String texto = editTexto.getText().toString().toUpperCase();
                StringBuilder resultado = new StringBuilder();

                // Pega letra por letra e separa com espaço para simular a soletração
                for (int i = 0; i < texto.length(); i++) {
                    char letra = texto.charAt(i);
                    if (letra != ' ') {
                        resultado.append(letra).append("  ");
                    } else {
                        resultado.append("\n"); // Pula linha em espaços
                    }
                }

                txtResultado.setText(resultado.toString());
            }
        });
    }
}
