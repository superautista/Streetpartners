package com.angelo.libras;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private LinearLayout containerImagens;
    private EditText editTexto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTexto = findViewById(R.id.editTexto);
        Button btnTraduzir = findViewById(R.id.btnTraduzir);
        containerImagens = findViewById(R.id.containerImagens);

        btnTraduzir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String texto = editTexto.getText().toString().toUpperCase().trim();
                containerImagens.removeAllViews();

                if (texto.isEmpty()) return;

                // Para cada letra, adiciona um "desenho" de mão na tela
                for (int i = 0; i < texto.length(); i++) {
                    char letra = texto.charAt(i);
                    if (letra != ' ') {
                        TextView mao = new TextView(MainActivity.this);
                        
                        // Define o tamanho gigante para o emoji
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, 
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(16, 16, 16, 16);
                        mao.setLayoutParams(params);
                        mao.setTextSize(64); // Tamanho do emoji

                        // Pega o emoji correspondente à letra
                        mao.setText(getEmojiLibras(letra));
                        
                        containerImagens.addView(mao);
                    }
                }
            }
        });
    }

    // Aqui a gente mapeia a letra para o emoji de mão que mais se parece com o sinal
    private String getEmojiLibras(char letra) {
        switch (letra) {
            case 'A': return "✊"; // A
            case 'B': return "✋"; // B (mão aberta)
            case 'C': return "🤏"; // C (dedos curvados)
            case 'D': return "☝️"; // D (indicador pra cima)
            case 'E': return "🤜"; // E
            case 'F': return "👌"; // F
            case 'G': return "👆"; // G
            case 'H': return "✌️"; // H
            case 'I': return "🤙"; // I
            case 'J': return "🤟"; // J
            case 'K': return "✋"; // K
            case 'L': return "👆"; // L (indicador e polegar)
            case 'M': return "🤛"; // M
            case 'N': return "🤜"; // N
            case 'O': return "👌"; // O
            case 'P': return "👇"; // P
            case 'Q': return "👇"; // Q
            case 'R': return "🤞"; // R (dedos cruzados)
            case 'S': return "✊"; // S
            case 'T': return "👍"; // T
            case 'U': return "☝️"; // U
            case 'V': return "✌️"; // V
            case 'W': return "🤟"; // W
            case 'X': return "👆"; // X
            case 'Y': return "🤙"; // Y
            case 'Z': return "👆"; // Z
            default: return "❓"; 
        }
    }
}
