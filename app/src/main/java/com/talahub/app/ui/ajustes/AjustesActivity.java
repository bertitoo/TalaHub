package com.talahub.app.ui.ajustes;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.talahub.app.LoginActivity;
import com.talahub.app.databinding.ActivityAjustesBinding;

public class AjustesActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final String PREFS_NAME = "talahub_settings";
    private static final String KEY_SHOW_EMAIL = "show_email";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ActivityAjustesBinding binding;
    private SharedPreferences prefs;

    // --- Campos de Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Recuperar y aplicar estado del switch de notificaciones
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

        // Mostrar correo
        boolean mostrarCorreo = prefs.getBoolean(KEY_SHOW_EMAIL, true);
        binding.switchShowEmail.setChecked(mostrarCorreo);
        binding.switchShowEmail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SHOW_EMAIL, isChecked).apply();
            Toast.makeText(this, "Mostrar correo: " + (isChecked ? "Sí" : "No"), Toast.LENGTH_SHORT).show();
        });

        // Modo oscuro
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        binding.switchDarkMode.setChecked(darkMode);
        AppCompatDelegate.setDefaultNightMode(darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Versión e idioma
        binding.textVersion.setText("Versión: 1.0");
        binding.textIdioma.setText("Idioma: Español");

        // Configurar botón "Eliminar cuenta"
        binding.buttonDeleteAccount.setOnClickListener(v -> {
            mostrarDialogoConfirmacionEliminar();
        });
    }

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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

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

    private void borrarCuentaYDatos() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No hay ningún usuario autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        // 1) Eliminar documento en "usuarios/{uid}"
        DocumentReference usuarioRef = db.collection("usuarios").document(uid);
        usuarioRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // 2) Eliminar todos los documentos en "agendas/{uid}/eventos"
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
                                // 3) Una vez borrados los datos de Firestore, borrar el usuario de Firebase Auth
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

    private void eliminarUsuarioAuth(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cuenta eliminada correctamente.", Toast.LENGTH_SHORT).show();
                    // Limpiamos el stack y vamos a LoginActivity:
                    Intent intent = new Intent(AjustesActivity.this, LoginActivity.class);
                    // Estas flags borran todas las activities anteriores:
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar cuenta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}