package com.talahub.app.ui.ajustes;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.talahub.app.databinding.ActivityAjustesBinding;

public class AjustesActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final String PREFS_NAME = "talahub_settings";
    private static final String KEY_SHOW_EMAIL = "show_email";

    private ActivityAjustesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAjustesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ajustes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Notificaciones
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                solicitarPermisoNotificaciones();
            } else {
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
            }
        });

        // Mostrar correo: carga y guarda en SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean mostrarCorreo = prefs.getBoolean(KEY_SHOW_EMAIL, true);
        binding.switchShowEmail.setChecked(mostrarCorreo);

        binding.switchShowEmail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SHOW_EMAIL, isChecked).apply();
            Toast.makeText(this, "Mostrar correo: " + (isChecked ? "Sí" : "No"), Toast.LENGTH_SHORT).show();
        });

        // Versión
        binding.textVersion.setText("Versión: 1.0");

        // Idioma (visual, sin acción)
        binding.textIdioma.setText("Idioma: Español");
    }

    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            } else {
                Toast.makeText(this, "Permiso ya concedido", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No es necesario pedir permiso en esta versión", Toast.LENGTH_SHORT).show();
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
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}