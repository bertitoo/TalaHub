package com.talahub.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.databinding.ActivityMainBinding;
import com.talahub.app.ui.ajustes.AjustesActivity;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean esAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String rol = documentSnapshot.getString("rol");
                            esAdmin = "admin".equalsIgnoreCase(rol);
                        }

                        configurarNavegacion();
                    })
                    .addOnFailureListener(e -> configurarNavegacion());
        } else {
            configurarNavegacion(); // fallback por si no hay usuario
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build());
    }

    private void configurarNavegacion() {
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        if (esAdmin) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_eventos)
                    .setOpenableLayout(drawer)
                    .build();

            // Oculta otros ítems
            Menu menu = navigationView.getMenu();
            menu.findItem(R.id.nav_destacados).setVisible(false);
            menu.findItem(R.id.nav_buscar).setVisible(false);
            menu.findItem(R.id.nav_agenda).setVisible(false);
            menu.findItem(R.id.nav_eventos).setVisible(true);
        } else {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_destacados, R.id.nav_buscar, R.id.nav_agenda)
                    .setOpenableLayout(drawer)
                    .build();

            // Oculta eventos
            Menu menu = navigationView.getMenu();
            menu.findItem(R.id.nav_eventos).setVisible(false);
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            String page = null;

            if (id == R.id.nav_destacados) page = "Destacados";
            else if (id == R.id.nav_buscar) page = "Buscar evento";
            else if (id == R.id.nav_agenda) page = "Agenda";
            else if (id == R.id.nav_eventos) page = "Gestión de eventos";

            if (page != null) {
                getSupportActionBar().setTitle("TalaHub - " + page);
            } else {
                getSupportActionBar().setTitle("TalaHub");
            }
        });

        // Mostrar info usuario
        actualizarHeaderUsuario();

        // Logout
        View headerView = navigationView.getHeaderView(0);
        Button logoutButton = headerView.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        });

        // Navegar directamente al fragmento adecuado
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        if (esAdmin) {
            navController.navigate(R.id.nav_eventos);
        }
    }

    private void actualizarHeaderUsuario() {
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.textView);
        TextView userEmail = headerView.findViewById(R.id.user_email);
        ImageView userPhoto = headerView.findViewById(R.id.imageView);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("talahub_settings", MODE_PRIVATE);
            boolean mostrarCorreo = prefs.getBoolean("show_email", true);

            if (mostrarCorreo && user.getEmail() != null) {
                userEmail.setText(user.getEmail());
                userEmail.setVisibility(View.VISIBLE);
            } else {
                userEmail.setVisibility(View.GONE);
            }

            userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");

            if (user.getPhotoUrl() != null) {
                Picasso.get().load(user.getPhotoUrl()).into(userPhoto);
            } else {
                userPhoto.setImageResource(R.drawable.user_placeholder);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivityForResult(new Intent(this, AjustesActivity.class), 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            actualizarHeaderUsuario();
        } else if (requestCode == 200 && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("abrirAgenda", false)) {
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_agenda);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(new Intent(this, MainActivity.class));
        if (intent != null && intent.getBooleanExtra("abrirAgenda", false)) {
            intent.removeExtra("abrirAgenda");
            binding.drawerLayout.post(() -> {
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.mobile_navigation, true)
                        .build();
                navController.navigate(R.id.nav_agenda, null, navOptions);
            });
        }
    }
}