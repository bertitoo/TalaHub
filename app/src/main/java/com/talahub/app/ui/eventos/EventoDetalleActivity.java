package com.talahub.app.ui.eventos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.TiempoActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventoDetalleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evento_detalle);

        ImageView imagen = findViewById(R.id.detalle_imagen);
        TextView titulo = findViewById(R.id.detalle_titulo);
        TextView precio = findViewById(R.id.detalle_precio);
        TextView descripcion = findViewById(R.id.detalle_descripcion);
        TextView fechaView = findViewById(R.id.detalle_fecha);
        TextView lugarView = findViewById(R.id.detalle_lugar);
        TextView horaView = findViewById(R.id.detalle_hora);

        MaterialButton btnVolver = findViewById(R.id.boton_volver);
        btnVolver.setOnClickListener(v -> onBackPressed());

        // Obtener datos del intent
        String nombre = getIntent().getStringExtra("nombre");
        String fecha = getIntent().getStringExtra("fecha");
        String lugar = getIntent().getStringExtra("lugar");
        String precioTexto = getIntent().getStringExtra("precio");
        String imagenUrl = getIntent().getStringExtra("imagen");
        String descripcionTexto = getIntent().getStringExtra("descripcion");
        String hora = getIntent().getStringExtra("hora");

        titulo.setText(nombre);
        precio.setText("Precio: " + precioTexto);
        descripcion.setText(descripcionTexto);
        fechaView.setText(formatearFecha(fecha));
        lugarView.setText(lugar);
        horaView.setText(hora);

        if (imagenUrl != null) {
            Picasso.get().load(imagenUrl).into(imagen);
        }

        findViewById(R.id.boton_apuntarse).setOnClickListener(v ->
                Toast.makeText(this, "Añadido a tu agenda (simulado)", Toast.LENGTH_SHORT).show()
        );

        MaterialButton btnVerTiempo = findViewById(R.id.boton_ver_tiempo);
        btnVerTiempo.setOnClickListener(v -> {
            Intent intent = new Intent(this, TiempoActivity.class);
            intent.putExtra("fecha", fecha);
            startActivity(intent);
        });

    }

    private String formatearFecha(String fechaOriginal) {
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = formatoEntrada.parse(fechaOriginal);

            SimpleDateFormat formatoSalida = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            return formatoSalida.format(fecha);
        } catch (Exception e) {
            return fechaOriginal; // en caso de error, se muestra como venga
        }
    }

}