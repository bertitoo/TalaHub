package com.talahub.app.ui.eventos;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;

public class EventoDetalleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evento_detalle);

        ImageView imagen = findViewById(R.id.detalle_imagen);
        TextView titulo = findViewById(R.id.detalle_titulo);
        TextView precio = findViewById(R.id.detalle_precio);
        TextView descripcion = findViewById(R.id.detalle_descripcion);

        MaterialButton btnVolver = findViewById(R.id.boton_volver);
        btnVolver.setOnClickListener(v -> onBackPressed());

        // Obtener datos del intent
        String nombre = getIntent().getStringExtra("nombre");
        String fecha = getIntent().getStringExtra("fecha");
        String lugar = getIntent().getStringExtra("lugar");
        String precioTexto = getIntent().getStringExtra("precio");
        String imagenUrl = getIntent().getStringExtra("imagen");
        String descripcionTexto = getIntent().getStringExtra("descripcion");

        titulo.setText(nombre);
        precio.setText(precioTexto);
        descripcion.setText(descripcionTexto);

        if (imagenUrl != null) {
            Picasso.get().load(imagenUrl).into(imagen);
        }

        findViewById(R.id.boton_apuntarse).setOnClickListener(v ->
                Toast.makeText(this, "Añadido a tu agenda (simulado)", Toast.LENGTH_SHORT).show()
        );
    }
}