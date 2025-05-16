package com.talahub.app.ui.eventos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.MainActivity;
import com.talahub.app.R;
import com.talahub.app.TiempoActivity;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventoDetalleActivity extends AppCompatActivity {

    private MaterialButton btnApuntarse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evento_detalle);

        // Vistas
        ImageView imagen = findViewById(R.id.detalle_imagen);
        TextView titulo = findViewById(R.id.detalle_titulo);
        TextView precio = findViewById(R.id.detalle_precio);
        TextView descripcion = findViewById(R.id.detalle_descripcion);
        TextView fechaView = findViewById(R.id.detalle_fecha);
        TextView lugarView = findViewById(R.id.detalle_lugar);
        TextView horaView = findViewById(R.id.detalle_hora);
        btnApuntarse = findViewById(R.id.boton_apuntarse);
        MaterialButton btnVolver = findViewById(R.id.boton_volver);
        MaterialButton btnVerTiempo = findViewById(R.id.boton_ver_tiempo);

        btnVolver.setOnClickListener(v -> onBackPressed());

        // Datos recibidos
        final String id = getIntent().getStringExtra("id");
        final String nombre = getIntent().getStringExtra("nombre");
        final String descripcionTexto = getIntent().getStringExtra("descripcion");
        final String fecha = getIntent().getStringExtra("fecha");
        final String hora = getIntent().getStringExtra("hora");
        final String lugar = getIntent().getStringExtra("lugar");
        final String precioTexto = getIntent().getStringExtra("precio");
        final String imagenUrl = getIntent().getStringExtra("imagen");

        // Mostrar datos
        titulo.setText(nombre);
        precio.setText("Precio: " + precioTexto);
        descripcion.setText(descripcionTexto);
        fechaView.setText(formatearFecha(fecha));
        lugarView.setText(lugar);
        horaView.setText(hora);
        if (imagenUrl != null) Picasso.get().load(imagenUrl).into(imagen);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && id != null) {
            String uid = user.getUid();
            FirebaseFirestore.getInstance()
                    .collection("agendas")
                    .document(uid)
                    .collection("eventos")
                    .document(id)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            mostrarBotonVerAgenda();
                        } else {
                            mostrarBotonApuntarse(uid, id, nombre, descripcionTexto, fecha, hora, lugar, precioTexto, imagenUrl);
                        }
                    });
        } else {
            btnApuntarse.setOnClickListener(v ->
                    Toast.makeText(this, "Debes iniciar sesión para apuntarte", Toast.LENGTH_SHORT).show()
            );
        }

        btnVerTiempo.setOnClickListener(v -> {
            Intent intent = new Intent(this, TiempoActivity.class);
            intent.putExtra("fecha", fecha);
            startActivity(intent);
        });
    }

    private void mostrarBotonApuntarse(String uid, String id, String nombre, String descripcion,
                                       String fecha, String hora, String lugar, String precio, String imagenUrl) {

        btnApuntarse.setText("Apuntarme");
        btnApuntarse.setOnClickListener(v -> {
            Evento evento = new Evento(id, nombre, descripcion, fecha, hora, lugar, precio, imagenUrl, true);
            FirebaseHelper helper = new FirebaseHelper();
            helper.apuntarseAEvento(evento, uid,
                    aVoid -> {
                        Toast.makeText(this, "Te has apuntado al evento", Toast.LENGTH_SHORT).show();
                        mostrarBotonVerAgenda();
                    },
                    e -> Toast.makeText(this, "Error al apuntarte", Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void mostrarBotonVerAgenda() {
        btnApuntarse.setText("Ver agenda");
        btnApuntarse.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("abrirAgenda", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }


    private String formatearFecha(String fechaOriginal) {
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = formatoEntrada.parse(fechaOriginal);
            SimpleDateFormat formatoSalida = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            return formatoSalida.format(fecha);
        } catch (Exception e) {
            return fechaOriginal;
        }
    }
}