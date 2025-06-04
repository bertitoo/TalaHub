package com.talahub.app.ui.admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Actividad que permite editar los detalles de un evento ya existente
 * en Firestore. Solo modifica nombre, fecha, hora y descripción.
 *
 * Campos como lugar e imagen están deshabilitados para evitar su edición.
 *
 * Esta actividad es utilizada por administradores de la aplicación.
 *
 * @author Alberto Martínez Vadillo
 */
public class EditarEventoActivity extends AppCompatActivity {

    private EditText etNombre;
    private EditText etFecha;
    private EditText etHora;
    private EditText etDescripcion;
    private String eventoId;
    private String origNombre, origFecha, origHora, origDescripcion;

    /**
     * Método llamado al crear la actividad.
     * Inicializa la interfaz, carga los datos del evento desde el intent
     * y configura los listeners para la edición.
     *
     * @param savedInstanceState Estado guardado si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_evento);

        // Vinculación con vistas
        etNombre = findViewById(R.id.etNombreEvento);
        etFecha = findViewById(R.id.etFechaEvento);
        etHora = findViewById(R.id.etHoraEvento);
        etDescripcion = findViewById(R.id.etDescripcionEvento);
        EditText etUbicacion = findViewById(R.id.etUbicacionEvento);

        Button btnGuardar = findViewById(R.id.btnGuardarEvento);
        Button btnCancelar = findViewById(R.id.btnCancelarEvento);

        // Carga de datos del Intent
        eventoId = getIntent().getStringExtra("id");
        origNombre = getIntent().getStringExtra("nombre");
        origFecha = getIntent().getStringExtra("fecha");
        origHora = getIntent().getStringExtra("hora");
        origDescripcion = getIntent().getStringExtra("descripcion");
        String lugar = getIntent().getStringExtra("lugar");
        String imagenUrl = getIntent().getStringExtra("imagenUrl");

        // Rellenar campos con valores actuales
        etNombre.setText(origNombre);
        etFecha.setText(origFecha);
        etHora.setText(origHora);
        etDescripcion.setText(origDescripcion);
        etUbicacion.setText(lugar);
        etUbicacion.setEnabled(false);
        etUbicacion.setAlpha(0.5f);

        // Vista previa de imagen
        ImageView ivPreview = findViewById(R.id.ivPreviewImagen);
        if (imagenUrl != null && !imagenUrl.isEmpty()) {
            Picasso.get().load(imagenUrl).into(ivPreview);
        } else {
            ivPreview.setImageResource(R.drawable.user_placeholder);
        }
        ivPreview.setEnabled(false);
        ivPreview.setAlpha(0.5f);

        // Selector de fecha
        etFecha.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int anio = c.get(Calendar.YEAR);
            int mes = c.get(Calendar.MONTH);
            int dia = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dpd = new DatePickerDialog(
                    EditarEventoActivity.this,
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        String textoFecha = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year);
                        etFecha.setText(textoFecha);
                    }, anio, mes, dia
            );
            dpd.show();
        });

        // Selector de hora
        etHora.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog tpd = new TimePickerDialog(
                    EditarEventoActivity.this,
                    (TimePicker view, int hourOfDay, int minute1) -> {
                        String textoHora = String.format("%02d:%02d", hourOfDay, minute1);
                        etHora.setText(textoHora);
                    },
                    hour, minute, true
            );
            tpd.show();
        });

        // Botón guardar
        btnGuardar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevaFecha = etFecha.getText().toString().trim();
            String nuevaHora = etHora.getText().toString().trim();
            String nuevaDescripcion = etDescripcion.getText().toString().trim();

            if (TextUtils.isEmpty(nuevoNombre) ||
                    TextUtils.isEmpty(nuevaFecha) ||
                    TextUtils.isEmpty(nuevaHora)) {
                Toast.makeText(this, "Nombre, fecha y hora son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificación de cambios
            boolean cambio = false;
            if (!nuevoNombre.equals(origNombre)) cambio = true;
            if (!nuevaFecha.equals(origFecha)) cambio = true;
            if (!nuevaHora.equals(origHora)) cambio = true;
            if (!nuevaDescripcion.equals(origDescripcion)) cambio = true;

            if (!cambio) {
                Toast.makeText(this, "No se ha realizado ningún cambio", Toast.LENGTH_SHORT).show();
                return;
            }

            // Aplicar cambios a Firestore
            Map<String, Object> cambios = new HashMap<>();
            cambios.put("nombre", nuevoNombre);
            cambios.put("fecha", nuevaFecha);
            cambios.put("hora", nuevaHora);
            cambios.put("descripcion", nuevaDescripcion);

            FirebaseFirestore.getInstance()
                    .collection("eventos")
                    .document(eventoId)
                    .update(cambios)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Evento actualizado", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    );
        });

        // Botón cancelar
        btnCancelar.setOnClickListener(v -> finish());
    }
}