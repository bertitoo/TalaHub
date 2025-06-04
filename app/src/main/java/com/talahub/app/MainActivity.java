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

import com.google.android.gms.auth.api.signin.*;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.databinding.ActivityMainBinding;
import com.talahub.app.ui.ajustes.AjustesActivity;

/**
 * Actividad principal que gestiona la navegación dentro de la aplicación,
 * incluyendo la carga dinámica de fragmentos según el rol del usuario
 * (usuario normal o administrador).
 *
 * También gestiona la sesión del usuario y actualiza la información de cabecera
 * en el menú lateral.
 *
 * @author Alberto Martínez Vadillo
 */
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean esAdmin = false;

    /**
     * Método llamado al crear la actividad. Verifica el rol del usuario y configura la navegación.
     */
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
            configurarNavegacion(); // Fallback si no hay usuario
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build());
    }

    /**
     * Configura el menú de navegación lateral y los fragmentos visibles según el rol del usuario.
     */
    private void configurarNavegacion() {
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        Menu menu = navigationView.getMenu();

        if (esAdmin) {
            // Mostrar ítems adicionales para administradores
            menu.findItem(R.id.nav_destacados).setVisible(true);
            menu.findItem(R.id.nav_buscar).setVisible(true);
            menu.findItem(R.id.nav_agenda).setVisible(true);
            menu.findItem(R.id.nav_eventos).setVisible(true);
            menu.findItem(R.id.nav_usuarios).setVisible(true);

            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_destacados, R.id.nav_buscar, R.id.nav_agenda, R.id.nav_eventos, R.id.nav_usuarios)
                    .setOpenableLayout(drawer)
                    .build();
        } else {
            // Mostrar solo lo permitido para usuarios normales
            menu.findItem(R.id.nav_destacados).setVisible(true);
            menu.findItem(R.id.nav_buscar).setVisible(true);
            menu.findItem(R.id.nav_agenda).setVisible(true);
            menu.findItem(R.id.nav_eventos).setVisible(false);
            menu.findItem(R.id.nav_usuarios).setVisible(false);

            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_destacados, R.id.nav_buscar, R.id.nav_agenda)
                    .setOpenableLayout(drawer)
                    .build();
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Cambiar título según sección
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            String page = null;

            if (id == R.id.nav_destacados) page = "Destacados";
            else if (id == R.id.nav_buscar) page = "Buscar evento";
            else if (id == R.id.nav_agenda) page = "Agenda";
            else if (id == R.id.nav_eventos) page = "Gestión de eventos";
            else if (id == R.id.nav_usuarios) page = "Gestión de usuarios";

            getSupportActionBar().setTitle("TalaHub" + (page != null ? " - " + page : ""));
        });

        // Mostrar usuario y botón de logout
        actualizarHeaderUsuario();
        View headerView = navigationView.getHeaderView(0);
        Button logoutButton = headerView.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        });

        // Navegación automática al fragmento inicial según el rol
        if (esAdmin) {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.mobile_navigation, true)
                    .build();
            navController.navigate(R.id.nav_eventos, null, navOptions);
        }
    }

    /**
     * Actualiza el header del menú lateral con los datos del usuario actual.
     */
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

    /**
     * Infla el menú de la barra de acciones.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Maneja acciones del menú, como abrir la actividad de ajustes.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivityForResult(new Intent(this, AjustesActivity.class), 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Maneja el resultado de otras actividades, como ajustes o navegación a agenda.
     */
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

    /**
     * Soporte para el botón de navegación superior.
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    /**
     * Maneja intents nuevos, especialmente si hay que abrir directamente la agenda.
     */
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

    /**
     * Comportamiento personalizado del botón atrás. Si es admin, no cierra la app.
     */
    @Override
    public void onBackPressed() {
        if (esAdmin) {
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }
}