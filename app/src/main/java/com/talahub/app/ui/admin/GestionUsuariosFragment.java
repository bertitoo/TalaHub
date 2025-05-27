package com.talahub.app.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.talahub.app.R;
import com.talahub.app.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class GestionUsuariosFragment extends Fragment {

    private LinearLayout layoutContenedor;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gestion_usuarios, container, false);
        layoutContenedor = root.findViewById(R.id.layout_usuarios_admin);

        cargarUsuarios(inflater);
        return root;
    }

    private void cargarUsuarios(LayoutInflater inflater) {
        FirebaseUser usuarioActual = FirebaseAuth.getInstance().getCurrentUser();
        if (usuarioActual == null) return;

        String uidActual = usuarioActual.getUid();

        db.collection("usuarios")
                .get()
                .addOnSuccessListener(snapshot -> {
                    layoutContenedor.removeAllViews();
                    List<View> otrosUsuarios = new ArrayList<>();

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

                        if (usuario.getUid().equals(uidActual)) {
                            rol.setText("admin (tú)");
                            btnEliminar.setColorFilter(getResources().getColor(R.color.texto_secundario));
                            btnEliminar.setEnabled(false);
                            layoutContenedor.addView(card, 0);
                        } else {
                            rol.setText(usuario.getRol());

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
                                                        cargarUsuarios(inflater); // recargar lista
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show());
                                        })
                                        .setNegativeButton("Cancelar", null)
                                        .show();
                            });

                            otrosUsuarios.add(card);
                        }
                    }

                    // Añadir todos los demás usuarios después del admin
                    for (View v : otrosUsuarios) {
                        layoutContenedor.addView(v);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show());
    }
}