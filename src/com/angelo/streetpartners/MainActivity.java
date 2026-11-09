package com.angelo.frases;

import com.angelo.streetpartners.R;
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
        "Sua mente e um supercomputador.",
        "O bullying e o cancer. A gente corta, nao se rende.",
        "Eles perdem no tigrinho. Voce ganha em codigo.",
        "Ser autista, AH/SD e TDAH nao e defeito.",
        "Se o mundo nao te aceita, hackeie o mundo.",
        "Voce sobreviveu a 100% dos seus piores dias.",
        "O silencio da sua forca e maior que o barulho deles.",
        "Nao e sobre ser igual. E sobre ser quem voce e.",
        "Michael Jackson inclinou, voce tambem vai.",
        "Voce nao e uma almondega. Voce e o criador.",
        "Quando a vontade de sumir vier, lembre-se do seu imperio.",
        "A escola e so uma fase. O seu futuro e nivel NASA."
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
