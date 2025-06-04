package com.talahub.app.ui.eventos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
import com.talahub.app.MainActivity;
import com.talahub.app.R;
import com.talahub.app.TiempoActivity;
import com.talahub.app.firebase.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Actividad que muestra los detalles de un evento, permite apuntarse,
 * ver información meteorológica y notifica al usuario si se apunta.
 */
public class EventoDetalleActivity extends AppCompatActivity {

    private MaterialButton btnApuntarse;

    /**
     * Método principal que se ejecuta al iniciar la actividad.
     * Carga los detalles del evento desde el intent y configura las acciones de los botones.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evento_detalle);

        // Obtener referencias a vistas
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

        // Acción para volver atrás
        btnVolver.setOnClickListener(v -> onBackPressed());

        // Obtener datos del intent
        final String id = getIntent().getStringExtra("id");
        final String nombre = getIntent().getStringExtra("nombre");
        final String descripcionTexto = getIntent().getStringExtra("descripcion");
        final String fecha = getIntent().getStringExtra("fecha");
        final String hora = getIntent().getStringExtra("hora");
        final String lugar = getIntent().getStringExtra("lugar");
        final String precioTexto = getIntent().getStringExtra("precio");
        final String imagenUrl = getIntent().getStringExtra("imagen");

        // Mostrar los datos en pantalla
        titulo.setText(nombre);
        precio.setText("Precio: " + precioTexto);
        descripcion.setText(descripcionTexto);
        fechaView.setText(formatearFecha(fecha));
        lugarView.setText(lugar);
        horaView.setText(hora);
        if (imagenUrl != null) Picasso.get().load(imagenUrl).into(imagen);

        // Comprobar si el usuario está autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && id != null) {
            String uid = user.getUid();
            FirebaseHelper helper = new FirebaseHelper();
            helper.estaApuntadoAEvento(uid, id, apuntado -> {
                if (apuntado) {
                    mostrarBotonVerAgenda();
                } else {
                    mostrarBotonApuntarse(uid, id, nombre, descripcionTexto, fecha, hora, lugar, precioTexto, imagenUrl);
                }
            });
        } else {
            btnApuntarse.setOnClickListener(v ->
                    Toast.makeText(this, "No has podido apuntarte al evento.", Toast.LENGTH_SHORT).show()
            );
        }

        // Botón para ver el clima
        btnVerTiempo.setOnClickListener(v -> {
            Intent intent = new Intent(this, TiempoActivity.class);
            intent.putExtra("fecha", fecha);
            startActivity(intent);
        });
    }

    /**
     * Muestra el botón "Apuntarme" y registra al usuario en el evento.
     */
    private void mostrarBotonApuntarse(String uid, String id, String nombre, String descripcion,
                                       String fecha, String hora, String lugar, String precio, String imagenUrl) {
        btnApuntarse.setText("Apuntarme");
        btnApuntarse.setOnClickListener(v -> {
            FirebaseHelper helper = new FirebaseHelper();
            helper.apuntarseAEvento(id, uid,
                    aVoid -> {
                        Toast.makeText(this, "Te has apuntado al evento", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        mostrarBotonVerAgenda();
                        mostrarNotificacionEvento("¡Te has apuntado!", "Has confirmado tu asistencia a "
                                + nombre + " el " + fecha);
                    },
                    e -> Toast.makeText(this, "Error al apuntarte", Toast.LENGTH_SHORT).show()
            );
        });
    }

    /**
     * Reemplaza el botón "Apuntarme" por "Ver agenda" tras haberse apuntado.
     */
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

    /**
     * Formatea la fecha a un formato largo y legible en español.
     */
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

    /**
     * Muestra una notificación local indicando que el usuario se ha apuntado a un evento.
     */
    private void mostrarNotificacionEvento(String titulo, String contenido) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String canalId = "eventos_talahub";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalId, "Eventos TalaHub", NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription("Notificaciones de eventos a los que te has apuntado");
            manager.createNotificationChannel(canal);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(R.drawable.ic_event)
                .setContentTitle(titulo)
                .setContentText(contenido)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}