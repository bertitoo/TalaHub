package com.talahub.app.ui.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.talahub.app.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Actividad para que un administrador edite los datos de un usuario,
 * incluyendo su nombre, rol y, si es el propio usuario autenticado, su correo electrónico.
 *
 * Actualiza tanto Firestore como, opcionalmente, Firebase Authentication.
 *
 * Usado en la sección administrativa de la aplicación.
 *
 * @author Alberto Martínez Vadillo
 */
public class EditarUsuarioActivity extends AppCompatActivity {

    private EditText etNombre;
    private EditText etCorreo;
    private RadioGroup radioGroupRol;
    private String uid;
    private String correoOriginal;
    private boolean esUsuarioActual;

    /**
     * Método llamado al iniciar la actividad.
     * Configura la interfaz, carga los datos del usuario a editar
     * y establece el comportamiento de los botones.
     *
     * @param savedInstanceState Estado anterior de la actividad (si existe).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_usuario);

        // Inicialización de vistas
        etNombre = findViewById(R.id.etNombreUsuario);
        etCorreo = findViewById(R.id.tvCorreoUsuario);
        radioGroupRol = findViewById(R.id.radioGroupRol);
        RadioButton radioUsuario = findViewById(R.id.radioUsuario);
        RadioButton radioAdmin = findViewById(R.id.radioAdmin);
        Button btnGuardar = findViewById(R.id.btnGuardarCambios);
        Button btnCancelar = findViewById(R.id.btnCancelarCambios);

        // Comprobación de usuario autenticado
        FirebaseUser usuarioActual = FirebaseAuth.getInstance().getCurrentUser();
        String uidActual = usuarioActual != null ? usuarioActual.getUid() : "";

        // Carga de datos desde el intent
        uid = getIntent().getStringExtra("uid");
        String nombre = getIntent().getStringExtra("nombre");
        correoOriginal = getIntent().getStringExtra("correo");
        String rol = getIntent().getStringExtra("rol");

        esUsuarioActual = uid.equals(uidActual);

        // Rellenar campos
        etNombre.setText(nombre);
        etCorreo.setText(correoOriginal);
        etCorreo.setEnabled(esUsuarioActual); // Solo editable si es el propio usuario

        // Preseleccionar rol actual
        if ("admin".equalsIgnoreCase(rol)) {
            radioAdmin.setChecked(true);
        } else {
            radioUsuario.setChecked(true);
        }

        // Acción del botón Guardar
        btnGuardar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevoCorreo = etCorreo.getText().toString().trim();
            String nuevoRol = radioGroupRol.getCheckedRadioButtonId() == R.id.radioAdmin ? "admin" : "usuario";

            if (nuevoNombre.isEmpty()) {
                etNombre.setError("El nombre no puede estar vacío");
                return;
            }

            Map<String, Object> cambios = new HashMap<>();
            cambios.put("nombre", nuevoNombre);
            cambios.put("rol", nuevoRol);

            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .update(cambios)
                    .addOnSuccessListener(aVoid -> {
                        // Si el usuario actual ha cambiado su propio correo, actualizar también en Auth
                        if (esUsuarioActual && !nuevoCorreo.equalsIgnoreCase(correoOriginal)) {
                            if (usuarioActual != null) {
                                usuarioActual.updateEmail(nuevoCorreo)
                                        .addOnSuccessListener(unused -> {
                                            actualizarCorreoEnFirestore(nuevoCorreo);
                                            Toast.makeText(this, "Correo actualizado", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Error al actualizar el correo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        }

                        Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error al guardar cambios", Toast.LENGTH_SHORT).show());
        });

        // Acción del botón Cancelar
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * Método auxiliar para actualizar el campo 'correo' del usuario en Firestore.
     *
     * @param nuevoCorreo Nuevo correo electrónico.
     */
    private void actualizarCorreoEnFirestore(String nuevoCorreo) {
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .update("correo", nuevoCorreo)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar correo en Firestore", Toast.LENGTH_SHORT).show());
    }
}