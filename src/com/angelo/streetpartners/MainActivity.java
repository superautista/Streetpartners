package com.angelo.streetpartners;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    private int hpJogador = 100;
    private int hpInimigo = 100;
    private boolean defendendo = false;

    private ProgressBar barraJogador, barraInimigo;
    private TextView txtJogador, txtInimigo, txtLog;
    private Button btnSoco, btnChute, btnDefender;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barraJogador = findViewById(R.id.barraJogador);
        barraInimigo = findViewById(R.id.barraInimigo);
        txtJogador = findViewById(R.id.txtJogador);
        txtInimigo = findViewById(R.id.txtInimigo);
        txtLog = findViewById(R.id.txtLog);

        btnSoco = findViewById(R.id.btnSoco);
        btnChute = findViewById(R.id.btnChute);
        btnDefender = findViewById(R.id.btnDefender);

        btnSoco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                atacar(15, "Soco");
            }
        });

        btnChute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                atacar(25, "Chute");
            }
        });

        btnDefender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defendendo = true;
                txtLog.setText("Você se defendeu! O próximo ataque será bloqueado.");
                turnoInimigo();
            }
        });
    }

    private void atacar(int dano, String golpe) {
        // Adiciona um pequeno fator aleatório no dano (para não ser sempre igual)
        int danoFinal = dano + random.nextInt(10);
        hpInimigo -= danoFinal;
        if (hpInimigo < 0) hpInimigo = 0;
        
        txtLog.setText("Você usou " + golpe + " e causou " + danoFinal + " de dano!");
        atualizarUI();

        if (hpInimigo <= 0) {
            vencer();
            return;
        }
        turnoInimigo();
    }

    private void turnoInimigo() {
        // Inimigo ataca de volta
        if (defendendo) {
            int danoBloqueado = random.nextInt(10); // Dano mínimo no bloqueio
            hpJogador -= danoBloqueado;
            txtLog.append("\nInimigo atacou, mas você bloqueou! Perdeu só " + danoBloqueado + " HP.");
            defendendo = false;
        } else {
            int danoInimigo = 10 + random.nextInt(15); // Dano entre 10 e 24
            hpJogador -= danoInimigo;
            txtLog.append("\nInimigo atacou e causou " + danoInimigo + " de dano!");
        }

        if (hpJogador < 0) hpJogador = 0;
        atualizarUI();

        if (hpJogador <= 0) {
            perder();
        }
    }

    private void atualizarUI() {
        barraJogador.setProgress(hpJogador);
        barraInimigo.setProgress(hpInimigo);
        txtJogador.setText("VOCÊ (" + hpJogador + "/100 HP)");
        txtInimigo.setText("INIMIGO (" + hpInimigo + "/100 HP)");
    }

    private void vencer() {
        txtLog.setText("K.O.! VOCÊ VENCEU A LUTA! 🏆");
        desativarBotoes();
    }

    private void perder() {
        txtLog.setText("VOCÊ FOI NOCAUTEADO... GAME OVER. 💀");
        desativarBotoes();
    }

    private void desativarBotoes() {
        btnSoco.setEnabled(false);
        btnChute.setEnabled(false);
        btnDefender.setEnabled(false);
    }
}
