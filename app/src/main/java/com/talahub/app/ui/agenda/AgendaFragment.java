package com.talahub.app.ui.agenda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.models.Evento;

import java.util.ArrayList;
import java.util.List;

public class AgendaFragment extends Fragment {

    private LinearLayout layoutEventosAgenda;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_agenda, container, false);
        layoutEventosAgenda = root.findViewById(R.id.layout_eventos_agenda);

        cargarEventosAgenda(inflater);

        return root;
    }

    private void cargarEventosAgenda(LayoutInflater inflater) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para ver tu agenda", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("agendas")
                .document(uid)
                .collection("eventos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Evento> eventos = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots) {
                        Evento evento = doc.toObject(Evento.class);
                        eventos.add(evento);
                    }
                    mostrarEventosAgenda(eventos, inflater);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar tu agenda", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarEventosAgenda(List<Evento> eventos, LayoutInflater inflater) {
        layoutEventosAgenda.removeAllViews();

        if (eventos.isEmpty()) {
            TextView vacio = new TextView(getContext());
            vacio.setText("No tienes eventos en tu agenda.");
            vacio.setTextSize(18f);
            vacio.setTextColor(getResources().getColor(R.color.color_azul));
            vacio.setPadding(0, 48, 0, 0);
            layoutEventosAgenda.addView(vacio);
            return;
        }

        for (Evento evento : eventos) {
            View item = inflater.inflate(R.layout.item_evento_agenda, layoutEventosAgenda, false);

            TextView nombre = item.findViewById(R.id.tvAgendaNombreEvento);
            TextView fechaHora = item.findViewById(R.id.tvAgendaFechaHora);
            TextView lugar = item.findViewById(R.id.tvAgendaLugar);
            ImageView imagen = item.findViewById(R.id.ivAgendaImagenEvento);
            ImageButton btnEliminar = item.findViewById(R.id.btnEliminarEvento);

            nombre.setText(evento.getNombre());
            fechaHora.setText(evento.getFecha() + " - " + evento.getHora());
            lugar.setText(evento.getLugar());

            if (evento.getImagenUrl() != null && !evento.getImagenUrl().isEmpty()) {
                Picasso.get().load(evento.getImagenUrl()).into(imagen);
            } else {
                imagen.setImageResource(R.drawable.user_placeholder);
            }

            // Listener para eliminar evento
            btnEliminar.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Eliminar evento")
                        .setMessage("¿Seguro que quieres eliminar este evento de tu agenda?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            eliminarEventoDeAgenda(evento.getId(), item);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });

            layoutEventosAgenda.addView(item);
        }
    }

    private void eliminarEventoDeAgenda(String eventoId, View itemView) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("agendas")
                .document(uid)
                .collection("eventos")
                .document(eventoId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Quita la vista de la UI
                    layoutEventosAgenda.removeView(itemView);
                    Toast.makeText(getContext(), "Evento eliminado de tu agenda", Toast.LENGTH_SHORT).show();
                    // Si la lista queda vacía, muestra mensaje de vacío
                    if (layoutEventosAgenda.getChildCount() == 0) {
                        TextView vacio = new TextView(getContext());
                        vacio.setText("No tienes eventos en tu agenda.");
                        vacio.setTextSize(18f);
                        vacio.setTextColor(getResources().getColor(R.color.color_azul));
                        vacio.setPadding(0, 48, 0, 0);
                        layoutEventosAgenda.addView(vacio);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar el evento", Toast.LENGTH_SHORT).show();
                });
    }
}