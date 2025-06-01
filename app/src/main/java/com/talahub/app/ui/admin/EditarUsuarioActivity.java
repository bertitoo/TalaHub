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

public class EditarUsuarioActivity extends AppCompatActivity {

    private EditText etNombre;
    private EditText etCorreo;
    private RadioGroup radioGroupRol;
    private RadioButton radioUsuario, radioAdmin;
    private String uid;
    private String correoOriginal;
    private boolean esUsuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_usuario);

        etNombre = findViewById(R.id.etNombreUsuario);
        etCorreo = findViewById(R.id.tvCorreoUsuario);  // ahora es editable
        radioGroupRol = findViewById(R.id.radioGroupRol);
        radioUsuario = findViewById(R.id.radioUsuario);
        radioAdmin = findViewById(R.id.radioAdmin);
        Button btnGuardar = findViewById(R.id.btnGuardarCambios);
        Button btnCancelar = findViewById(R.id.btnCancelarCambios);

        FirebaseUser usuarioActual = FirebaseAuth.getInstance().getCurrentUser();
        String uidActual = usuarioActual != null ? usuarioActual.getUid() : "";

        // Obtener datos recibidos
        uid = getIntent().getStringExtra("uid");
        String nombre = getIntent().getStringExtra("nombre");
        correoOriginal = getIntent().getStringExtra("correo");
        String rol = getIntent().getStringExtra("rol");

        esUsuarioActual = uid.equals(uidActual);

        etNombre.setText(nombre);
        etCorreo.setText(correoOriginal);
        etCorreo.setEnabled(esUsuarioActual); // Solo editable si es el usuario actual

        if ("admin".equalsIgnoreCase(rol)) {
            radioAdmin.setChecked(true);
        } else {
            radioUsuario.setChecked(true);
        }

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
                        if (esUsuarioActual && !nuevoCorreo.equalsIgnoreCase(correoOriginal)) {
                            // Cambiar email en Firebase Authentication
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

        btnCancelar.setOnClickListener(v -> finish());
    }

    private void actualizarCorreoEnFirestore(String nuevoCorreo) {
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .update("correo", nuevoCorreo)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar correo en Firestore", Toast.LENGTH_SHORT).show());
    }
}