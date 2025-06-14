package com.talahub.app.ui.ajustes;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.talahub.app.LoginActivity;
import com.talahub.app.databinding.ActivityAjustesBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Actividad que gestiona los ajustes de la aplicación, incluyendo:
 * - Modo oscuro
 * - Visibilidad del correo electrónico
 * - Activación de notificaciones (con permisos)
 * - Eliminación de cuenta del usuario actual
 *
 * También permite ver los términos y condiciones y guarda preferencias usando SharedPreferences.
 * Utiliza FirebaseAuth y FirebaseFirestore para gestionar los datos del usuario.
 *
 * @author Alberto Martínez Vadillo
 */
public class AjustesActivity extends AppCompatActivity {
    // Constantes de configuración y preferencias
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final String PREFS_NAME = "talahub_settings";
    private static final String KEY_SHOW_EMAIL = "show_email";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ActivityAjustesBinding binding;
    private SharedPreferences prefs;

    // Instancias de Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * Método principal llamado al iniciar la actividad.
     * Configura la interfaz, carga las preferencias y establece los listeners de los switches.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAjustesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ajustes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configuración de notificaciones
        boolean notificacionesActivadas = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
        binding.switchNotifications.setChecked(notificacionesActivadas);

        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply();
            if (isChecked) {
                solicitarPermisoNotificaciones();
            } else {
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
            }
        });

        // Mostrar u ocultar correo electrónico
        boolean mostrarCorreo = prefs.getBoolean(KEY_SHOW_EMAIL, true);
        binding.switchShowEmail.setChecked(mostrarCorreo);
        binding.switchShowEmail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SHOW_EMAIL, isChecked).apply();
            Toast.makeText(this, "Mostrar correo: " + (isChecked ? "Sí" : "No"), Toast.LENGTH_SHORT).show();
        });

        // Configuración del modo oscuro
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        binding.switchDarkMode.setChecked(darkMode);
        AppCompatDelegate.setDefaultNightMode(darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Evitamos que el cambio de tema vuelva a activar el listener en bucle
            binding.switchDarkMode.setOnCheckedChangeListener(null);

            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Información general
        binding.textVersion.setText("Versión: 1.0");
        binding.textIdioma.setText("Idioma: Español");

        // Botón para eliminar cuenta
        binding.buttonDeleteAccount.setOnClickListener(v -> {
            mostrarDialogoConfirmacionEliminar();
        });

        // Botón para mostrar términos y condiciones
        binding.buttonTerminos.setOnClickListener(v -> mostrarDialogoTerminos());

        // Deshabilitar opción de eliminar cuenta si es admin
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String rol = documentSnapshot.getString("rol");
                            if ("admin".equalsIgnoreCase(rol)) {
                                binding.buttonDeleteAccount.setEnabled(false);
                                binding.buttonDeleteAccount.setBackgroundColor(Color.parseColor("#FFB3B3"));
                                binding.buttonDeleteAccount.setTextColor(Color.parseColor("#80000000"));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Solicita el permiso POST_NOTIFICATIONS si es necesario en Android 13+.
     */
    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso ya concedido, notificaciones activadas.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        } else {
            Toast.makeText(this, "Permiso no necesario en esta versión de Android", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback del sistema para recibir el resultado de una solicitud de permiso.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                binding.switchNotifications.setChecked(false);
                prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, false).apply();
            }
        }
    }

    /**
     * Maneja el comportamiento del botón de retroceso en la app bar.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Muestra un diálogo con los términos y condiciones cargados desde un archivo en assets.
     */
    private void mostrarDialogoTerminos() {
        try {
            InputStream is = getAssets().open("terminos.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) {
                builder.append(linea).append("\n");
            }
            reader.close();

            new AlertDialog.Builder(this)
                    .setTitle("Términos y condiciones")
                    .setMessage(builder.toString())
                    .setPositiveButton("Aceptar", null)
                    .show();

        } catch (IOException e) {
            Toast.makeText(this, "Error al cargar los términos", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar la cuenta del usuario.
     */
    private void mostrarDialogoConfirmacionEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Se borrarán todos tus datos y no podrás recuperarlos.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    borrarCuentaYDatos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Elimina todos los datos del usuario en Firestore y luego elimina el usuario de Firebase Auth.
     */
    private void borrarCuentaYDatos() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No hay ningún usuario autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        DocumentReference usuarioRef = db.collection("usuarios").document(uid);
        usuarioRef.delete()
                .addOnSuccessListener(aVoid -> {
                    CollectionReference eventosAgendadosRef = db
                            .collection("agendas")
                            .document(uid)
                            .collection("eventos");

                    eventosAgendadosRef.get()
                            .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                                if (!querySnapshot.isEmpty()) {
                                    for (QueryDocumentSnapshot doc : querySnapshot) {
                                        eventosAgendadosRef.document(doc.getId()).delete();
                                    }
                                }
                                eliminarUsuarioAuth(currentUser);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al obtener eventos agendados: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar datos de usuario: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Elimina al usuario actual de Firebase Authentication y redirige a LoginActivity.
     */
    private void eliminarUsuarioAuth(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cuenta eliminada correctamente.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AjustesActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar cuenta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}