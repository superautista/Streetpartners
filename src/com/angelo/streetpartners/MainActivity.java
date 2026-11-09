package com.angelo.calmquest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity {

    private GridLayout gridJogo;
    private TextView txtStatus;
    private Button primeiraCarta, segundaCarta;
    private int acertos = 0;

    private String[] emojis = {"🐢", "🐢", "🐌", "🐌", "🦉", "🦉", "🐳", "🐳", "🌻", "🌻", "☁️", "☁️"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridJogo = findViewById(R.id.gridJogo);
        txtStatus = findViewById(R.id.txtStatus);

        criarTabuleiro();
    }

    private void criarTabuleiro() {
        gridJogo.removeAllViews();
        acertos = 0;
        txtStatus.setText("Encontre os pares. Sem pressa.");

        // Embaralha os emojis
        ArrayList<String> lista = new ArrayList<>();
        for (String e : emojis) lista.add(e);
        Collections.shuffle(lista);

        // Cria as 12 cartas na tela
        for (int i = 0; i < 12; i++) {
            Button carta = new Button(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 200;
            params.height = 250;
            params.setMargins(8, 8, 8, 8);
            carta.setLayoutParams(params);
            
            carta.setBackgroundColor(Color.parseColor("#457B9D")); // Cor do verso da carta
            carta.setText("?");
            carta.setTextSize(40);
            carta.setTextColor(Color.WHITE);
            
            final String emojiReal = lista.get(i);
            final Button cartaAtual = carta;

            carta.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (primeiraCarta == null) {
                        // Primeira carta escolhida
                        primeiraCarta = cartaAtual;
                        primeiraCarta.setText(emojiReal);
                        primeiraCarta.setBackgroundColor(Color.parseColor("#F1FAEE"));
                    } else if (segundaCarta == null && cartaAtual != primeiraCarta) {
                        // Segunda carta escolhida
                        segundaCarta = cartaAtual;
                        segundaCarta.setText(emojiReal);
                        segundaCarta.setBackgroundColor(Color.parseColor("#F1FAEE"));
                        
                        verificarPar(emojiReal);
                    }
                }
            });

            gridJogo.addView(carta);
        }
    }

    private void verificarPar(String emoji) {
        // Espera meio segundo antes de virar as cartas de volta
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (primeiraCarta.getText().toString().equals(segundaCarta.getText().toString())) {
                    // Acertou o par
                    primeiraCarta.setVisibility(View.INVISIBLE);
                    segundaCarta.setVisibility(View.INVISIBLE);
                    acertos++;
                    
                    if (acertos == 6) {
                        txtStatus.setText("Você conseguiu! Muito bem. 🌿");
                    } else {
                        txtStatus.setText("Par encontrado! Respire fundo.");
                    }
                } else {
                    // Errou, mas sem punição. Só vira de volta.
                    primeiraCarta.setText("?");
                    primeiraCarta.setBackgroundColor(Color.parseColor("#457B9D"));
                    segundaCarta.setText("?");
                    segundaCarta.setBackgroundColor(Color.parseColor("#457B9D"));
                    txtStatus.setText("Tudo bem, tente outra vez.");
                }
                primeiraCarta = null;
                segundaCarta = null;
            }
        }, 800); // 800 milissegundos de pausa
    }
}
