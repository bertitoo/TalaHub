package com.talahub.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private EditText emailInput, passwordInput;
    private MaterialButton loginButton, registerButton;
    private SignInButton googleButton;

    private int logoClickCount = 0;
    private boolean bloquearAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain(false);
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.editTextEmail);
        passwordInput = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerButton = findViewById(R.id.buttonRegister);
        googleButton = findViewById(R.id.sign_in_button);
        ImageView appLogo = findViewById(R.id.app_logo);

        appLogo.setOnClickListener(v -> {
            if (bloquearAdmin) {
                Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show();
                return;
            }

            logoClickCount++;
            if (logoClickCount == 3) {
                logoClickCount = 0;
                mostrarDialogoAdmin();
            }
        });

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Introduce un correo electrónico válido", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            guardarUsuarioEnFirestore("usuario");
                            goToMain(false);
                        } else {
                            Toast.makeText(this, "Correo o contraseña incorrectos.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Introduce un correo electrónico válido", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            guardarUsuarioEnFirestore("usuario");
                            Toast.makeText(this, "Cuenta creada exitosamente.", Toast.LENGTH_SHORT).show();
                            goToMain(false);
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(this, "El correo ya está registrado.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    private void mostrarDialogoAdmin() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_login, null);
        EditText inputEmail = dialogView.findViewById(R.id.editTextAdminEmail);
        EditText inputPassword = dialogView.findViewById(R.id.editTextAdminPassword);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Acceder", (dialog, which) -> {
                    String email = inputEmail.getText().toString().trim();
                    String password = inputPassword.getText().toString().trim();

                    if (email.equals("admin") && password.equals("adminTalaHub0708")) {
                        String realEmail = "admin@talahub.com";
                        mAuth.signInWithEmailAndPassword(realEmail, password)
                                .addOnSuccessListener(authResult -> {
                                    guardarUsuarioEnFirestore("admin");
                                    Toast.makeText(this, "Bienvenido, administrador", Toast.LENGTH_SHORT).show();
                                    goToMain(true);
                                })
                                .addOnFailureListener(e -> {
                                    // Si no existe la cuenta, la registramos automáticamente
                                    mAuth.createUserWithEmailAndPassword(realEmail, password)
                                            .addOnSuccessListener(authResult -> {
                                                guardarUsuarioEnFirestore("admin");
                                                Toast.makeText(this, "Cuenta admin creada y autenticada", Toast.LENGTH_SHORT).show();
                                                goToMain(true);
                                            })
                                            .addOnFailureListener(error -> {
                                                bloquearAdmin = true;
                                                Toast.makeText(this, "Error al registrar cuenta admin", Toast.LENGTH_SHORT).show();
                                                Log.e("LoginAdmin", "Error: ", error);
                                            });
                                });
                    } else {
                        bloquearAdmin = true;
                        Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void goToMain(boolean esAdmin) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("abrirEventosFragment", esAdmin);
        startActivity(intent);
        finish();
    }

    private void guardarUsuarioEnFirestore(String rol) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String correo = user.getEmail() != null ? user.getEmail() : "sin_correo@talahub.com";
        String nombre = user.getDisplayName();

        if (nombre == null || nombre.isEmpty()) {
            String base = correo.split("@")[0];
            nombre = base.substring(0, 1).toUpperCase() + base.substring(1) + "_" + (int) (Math.random() * 1000);
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", correo);
        datos.put("nombre", nombre);
        datos.put("rol", rol);

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .set(datos)
                .addOnSuccessListener(aVoid -> Log.d("Login", "Usuario guardado"))
                .addOnFailureListener(e -> Log.e("Login", "Error al guardar usuario", e));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult();
                if (account != null) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    mAuth.signInWithCredential(credential)
                            .addOnCompleteListener(this, signInTask -> {
                                if (signInTask.isSuccessful()) {
                                    guardarUsuarioEnFirestore("usuario");
                                    goToMain(false);
                                } else {
                                    Toast.makeText(this, "Fallo al iniciar con Google", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } else {
                Toast.makeText(this, "Google Sign-In cancelado o fallido", Toast.LENGTH_SHORT).show();
            }
        }
    }
}