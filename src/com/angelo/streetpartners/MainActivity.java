package com.angelo.libras;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
                containerImagens.removeAllViews(); // Limpa a tela antes de traduzir

                if (texto.isEmpty()) return;

                // Para cada letra, busca a imagem correspondente
                for (int i = 0; i < texto.length(); i++) {
                    char letra = texto.charAt(i);
                    if (letra != ' ') {
                        // Cria um espaço para a imagem e um texto embaixo
                        ImageView imageView = new ImageView(MainActivity.this);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
                        params.setMargins(8, 8, 8, 8);
                        imageView.setLayoutParams(params);
                        containerImagens.addView(imageView);

                        // Chama a tarefa que baixa a imagem da internet
                        new BaixarImagem(imageView).execute(letra);
                    }
                }
            }
        });
    }

    // Tarefa que baixa a imagem da letra da internet sem travar o app
    private class BaixarImagem extends AsyncTask<Character, Void, Bitmap> {
        private ImageView imageView;

        public BaixarImagem(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Character... params) {
            char letra = params[0];
            Bitmap bitmap = null;
            try {
                // Link para o alfabeto manual de Libras (exemplo com imagens reais)
                // Usamos um servidor de imagens do GitHub
                String urlString = "https://raw.githubusercontent.com/eduardoprimo/libras-alfabeto/master/" + letra + ".png";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                imageView.setImageBitmap(result);
            } else {
                // Se a imagem não for encontrada, coloca uma imagem de erro
                imageView.setImageResource(android.R.drawable.ic_dialog_alert);
            }
        }
    }
}
