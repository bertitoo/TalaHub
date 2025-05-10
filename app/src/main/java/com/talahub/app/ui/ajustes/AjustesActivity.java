package com.talahub.app.ui.ajustes;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.talahub.app.databinding.ActivityAjustesBinding;

public class AjustesActivity extends AppCompatActivity {

    private ActivityAjustesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAjustesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Título en el ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ajustes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}