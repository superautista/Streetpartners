package com.angelo.ubers;

import android.app.Activity;
import android.graphics.Color;
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

        EditText editValor = findViewById(R.id.editValor);
        EditText editDistancia = findViewById(R.id.editDistancia);
        Button btnCalcular = findViewById(R.id.btnCalcular);
        final TextView txtFarol = findViewById(R.id.txtFarol);
        final TextView txtResultado = findViewById(R.id.txtResultado);

        // CUSTOS FIXOS (Podem ser ajustados)
        final double PRECO_COMBUSTIVEL = 5.50;
        final double CONSUMO_KM_LITRO = 10.0;
        final double CUSTO_MANUTENCAO_KM = 0.50; // Desgaste do carro

        btnCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String valorStr = editValor.getText().toString();
                String distanciaStr = editDistancia.getText().toString();

                if (valorStr.isEmpty() || distanciaStr.isEmpty()) {
                    txtResultado.setText("Preencha todos os campos!");
                    return;
                }

                try {
                    double valorCorrida = Double.parseDouble(valorStr);
                    double distancia = Double.parseDouble(distanciaStr);

                    // 1. Custo do Combustível
                    double custoCombustivel = (distancia / CONSUMO_KM_LITRO) * PRECO_COMBUSTIVEL;
                    
                    // 2. Custo de Manutenção (desgaste)
                    double custoManutencao = distancia * CUSTO_MANUTENCAO_KM;

                    // 3. Lucro Líquido
                    double custoTotal = custoCombustivel + custoManutencao;
                    double lucroLiquido = valorCorrida - custoTotal;
                    double margemLucro = (lucroLiquido / valorCorrida) * 100;

                    // Lógica do Farol
                    if (margemLucro >= 30.0) {
                        txtFarol.setText("🟢");
                        txtFarol.setBackgroundColor(Color.parseColor("#00FF00"));
                        txtResultado.setText(String.format("ACEITE!\nLucro: R$ %.2f\nMargem: %.1f%%", lucroLiquido, margemLucro));
                    } else if (margemLucro >= 15.0 && margemLucro < 30.0) {
                        txtFarol.setText("🟡");
                        txtFarol.setBackgroundColor(Color.parseColor("#FFFF00"));
                        txtResultado.setText(String.format("CUIDADO!\nLucro: R$ %.2f\nMargem: %.1f%%", lucroLiquido, margemLucro));
                    } else {
                        txtFarol.setText("🔴");
                        txtFarol.setBackgroundColor(Color.parseColor("#FF0000"));
                        txtResultado.setText(String.format("REJEITE!\nLucro: R$ %.2f\nMargem: %.1f%%\nPrejuízo ou custo alto.", lucroLiquido, margemLucro));
                    }

                } catch (NumberFormatException e) {
                    txtResultado.setText("Use apenas números. Ex: 15.50");
                }
            }
        });
    }
}
