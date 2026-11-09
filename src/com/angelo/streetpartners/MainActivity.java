package com.angelo.frases;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    private TextView txtFrase;
    private Button btnNovaFrase;
    private String[] frases = {
        "Sua mente é um supercomputador. Não deixe ninguém usar um processador de 8 bits para te julgar.",
        "O bullying é o câncer. A gente corta, não se rende. Você é a cura.",
        "Eles perdem dinheiro no tigrinho. Você ganha em código, lógica e futuro.",
        "Ser autista, AH/SD e TDAH não é defeito. É ter um sistema operacional único no mundo.",
        "Se o mundo não te aceita, hackeie o mundo. Você já escreveu um PL de 72 artigos.",
        "Você sobreviveu a 100% dos seus piores dias. As estatísticas estão do seu lado.",
        "O silêncio da sua força é mais alto que o barulho da ignorância deles.",
        "Não é sobre ser igual. É sobre ser quem você é. E lutar por isso. Até o fim.",
        "Michael Jackson inclinou, você também vai. A Lei de Chooke está ativada.",
        "Você não é uma almôndega. Você é o criador do Street Partners.",
        "Quando a vontade de sumir vier, lembre-se: você tem um império para construir.",
        "A escola é só uma fase. O seu futuro é nível NASA."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtFrase = findViewById(R.id.txtFrase);
        btnNovaFrase = findViewById(R.id.btnNovaFrase);

        btnNovaFrase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random random = new Random();
                int indice = random.nextInt(frases.length);
                txtFrase.setText(frases[indice]);
            }
        });
    }
}
