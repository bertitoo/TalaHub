package com.talahub.app.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.talahub.app.R;
import com.talahub.app.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class GestionUsuariosFragment extends Fragment {

    private LinearLayout layoutContenedor;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uidActual;
    private ListenerRegistration listenerUsuarios;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gestion_usuarios, container, false);
        layoutContenedor = root.findViewById(R.id.layout_usuarios_admin);
        setHasOptionsMenu(true);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        iniciarListenerUsuarios();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listenerUsuarios != null) {
            listenerUsuarios.remove();
        }
    }

    private void iniciarListenerUsuarios() {
        FirebaseUser usuarioActual = FirebaseAuth.getInstance().getCurrentUser();
        if (usuarioActual == null) return;

        uidActual = usuarioActual.getUid();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        listenerUsuarios = db.collection("usuarios")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        Toast.makeText(getContext(), "Error al escuchar usuarios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    layoutContenedor.removeAllViews();
                    List<View> admins = new ArrayList<>();
                    List<View> usuarios = new ArrayList<>();

                    for (var doc : snapshot) {
                        Usuario usuario = doc.toObject(Usuario.class);
                        usuario.setUid(doc.getId());

                        View card = inflater.inflate(R.layout.item_usuario_admin, layoutContenedor, false);

                        TextView nombre = card.findViewById(R.id.tvNombreUsuarioAdmin);
                        TextView correo = card.findViewById(R.id.tvCorreoUsuarioAdmin);
                        TextView rol = card.findViewById(R.id.tvRolUsuarioAdmin);
                        ImageView imagen = card.findViewById(R.id.ivFotoUsuarioAdmin);
                        ImageView btnEliminar = card.findViewById(R.id.btnEliminarUsuario);

                        nombre.setText(usuario.getNombre());
                        correo.setText(usuario.getCorreo());
                        imagen.setImageResource(R.drawable.user_placeholder);

                        // Mostrar rol con "(tú)" si es el actual
                        if (usuario.getUid().equals(uidActual)) {
                            rol.setText("admin (tú)");
                        } else {
                            rol.setText(usuario.getRol());
                        }

                        // Desactivar papelera si es admin
                        if ("admin".equalsIgnoreCase(usuario.getRol())) {
                            btnEliminar.setColorFilter(getResources().getColor(R.color.texto_secundario));
                            btnEliminar.setEnabled(false);
                        } else {
                            btnEliminar.setOnClickListener(v -> {
                                new AlertDialog.Builder(getContext())
                                        .setTitle("Eliminar usuario")
                                        .setMessage("¿Eliminar al usuario \"" + usuario.getNombre() + "\"?")
                                        .setPositiveButton("Sí", (dialog, which) -> {
                                            db.collection("usuarios")
                                                    .document(usuario.getUid())
                                                    .delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(getContext(), "Usuario eliminado", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(error ->
                                                            Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show());
                                        })
                                        .setNegativeButton("Cancelar", null)
                                        .show();
                            });
                        }

                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(getContext(), EditarUsuarioActivity.class);
                            intent.putExtra("uid", usuario.getUid());
                            intent.putExtra("nombre", usuario.getNombre());
                            intent.putExtra("correo", usuario.getCorreo());
                            intent.putExtra("rol", usuario.getRol());
                            startActivity(intent);
                        });

                        // Insertar en la posición correcta
                        if (usuario.getUid().equals(uidActual)) {
                            admins.add(0, card); // el admin actual primero
                        } else if ("admin".equalsIgnoreCase(usuario.getRol())) {
                            admins.add(card);
                        } else {
                            usuarios.add(card);
                        }
                    }

                    for (View v : admins) layoutContenedor.addView(v);
                    for (View v : usuarios) layoutContenedor.addView(v);
                });
    }
}