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

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;

import java.util.List;

public class EventosFragment extends Fragment {

    private LinearLayout layoutContenedor;
    private final FirebaseHelper firebaseHelper = new FirebaseHelper();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_eventos, container, false);
        layoutContenedor = root.findViewById(R.id.layout_eventos_admin);

        cargarEventos(inflater);
        return root;
    }

    private void cargarEventos(LayoutInflater inflater) {
        firebaseHelper.obtenerTodosLosEventos(eventos -> {
            layoutContenedor.removeAllViews();
            for (Evento evento : eventos) {
                View card = inflater.inflate(R.layout.item_evento_admin, layoutContenedor, false);

                TextView nombre = card.findViewById(R.id.tvNombreEventoAdmin);
                TextView fecha = card.findViewById(R.id.tvFechaEventoAdmin);
                ImageView imagen = card.findViewById(R.id.ivImagenEventoAdmin);
                ImageView btnEditar = card.findViewById(R.id.btnEditarEvento);
                ImageView btnEliminar = card.findViewById(R.id.btnEliminarEvento);

                nombre.setText(evento.getNombre());
                fecha.setText(evento.getFecha() + " • " + evento.getHora());

                if (evento.getImagenUrl() != null && !evento.getImagenUrl().isEmpty()) {
                    Picasso.get().load(evento.getImagenUrl()).into(imagen);
                } else {
                    imagen.setImageResource(R.drawable.user_placeholder);
                }

                btnEditar.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Editar: " + evento.getNombre(), Toast.LENGTH_SHORT).show();
                    // TODO: implementar navegación a pantalla de edición
                });

                btnEliminar.setOnClickListener(v -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Eliminar evento")
                            .setMessage("¿Estás seguro de que deseas eliminar \"" + evento.getNombre() + "\"?")
                            .setPositiveButton("Sí", (dialog, which) -> {
                                FirebaseFirestore.getInstance().collection("eventos")
                                        .document(evento.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Evento eliminado", Toast.LENGTH_SHORT).show();
                                            cargarEventos(inflater); // recargar lista
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });

                layoutContenedor.addView(card);
            }
        }, error -> Toast.makeText(getContext(), "Error al cargar eventos", Toast.LENGTH_SHORT).show());
    }
}