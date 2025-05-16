package com.talahub.app.ui.agenda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AgendaFragment extends Fragment {

    private LinearLayout layoutEventosAgenda;
    private List<Evento> eventosOriginales = new ArrayList<>();
    private List<Evento> eventosFiltrados = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_agenda, container, false);
        EditText etBuscar = root.findViewById(R.id.etBuscarAgenda);
        layoutEventosAgenda = root.findViewById(R.id.layout_eventos_agenda);

        cargarEventosAgenda(inflater);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarEventosAgenda(s.toString().trim(), inflater);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

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
                    eventosOriginales.clear();
                    for (var doc : queryDocumentSnapshots) {
                        Evento evento = doc.toObject(Evento.class);
                        eventosOriginales.add(evento);
                    }
                    filtrarEventosAgenda("", inflater);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar tu agenda", Toast.LENGTH_SHORT).show();
                });
    }

    private void filtrarEventosAgenda(String query, LayoutInflater inflater) {
        eventosFiltrados.clear();
        for (Evento e : eventosOriginales) {
            if (e.getNombre().toLowerCase().contains(query.toLowerCase()) ||
                    e.getLugar().toLowerCase().contains(query.toLowerCase())) {
                eventosFiltrados.add(e);
            }
        }
        mostrarEventosAgenda(eventosFiltrados, inflater);
    }

    private void mostrarEventosAgenda(List<Evento> eventos, LayoutInflater inflater) {
        layoutEventosAgenda.removeAllViews();

        if (eventos.isEmpty()) {
            TextView vacio = new TextView(getContext());
            vacio.setText("No hay eventos en tu agenda.");
            vacio.setTextSize(18f);
            vacio.setTextColor(getResources().getColor(R.color.color_azul));
            vacio.setPadding(0, 48, 0, 0);
            layoutEventosAgenda.addView(vacio);
            return;
        }

        // Ordenar eventos por fecha ascendente
        Collections.sort(eventos, (a, b) -> {
            Date fechaA = getDateFromEvento(a.getFecha(), a.getHora());
            Date fechaB = getDateFromEvento(b.getFecha(), b.getHora());
            if (fechaA == null || fechaB == null) return 0;
            return fechaA.compareTo(fechaB);
        });

        String ultimaFecha = "";
        for (Evento evento : eventos) {
            // Encabezado de fecha si cambia
            if (!evento.getFecha().equals(ultimaFecha)) {
                ultimaFecha = evento.getFecha();
                TextView fechaHeader = new TextView(getContext());
                fechaHeader.setText(formatearFechaHeader(evento.getFecha()));
                fechaHeader.setTextSize(16f);
                fechaHeader.setTextColor(getResources().getColor(R.color.color_naranja));
                fechaHeader.setPadding(0, 24, 0, 8);
                layoutEventosAgenda.addView(fechaHeader);
            }

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

            // Distinción visual si el evento es pasado
            if (esEventoPasado(evento.getFecha(), evento.getHora())) {
                item.setAlpha(0.5f);
                nombre.setTextColor(getResources().getColor(R.color.texto_secundario));
            } else {
                item.setAlpha(1.0f);
                nombre.setTextColor(getResources().getColor(R.color.texto_principal));
            }

            // Eliminar evento con animación fade out
            btnEliminar.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Eliminar evento")
                        .setMessage("¿Seguro que quieres eliminar este evento de tu agenda?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            eliminarEventoDeAgenda(evento.getId(), item, inflater);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });

            layoutEventosAgenda.addView(item);
        }
    }

    private void eliminarEventoDeAgenda(String eventoId, View itemView, LayoutInflater inflater) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("agendas")
                .document(uid)
                .collection("eventos")
                .document(eventoId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Animación fade out antes de quitar la vista y refrescar la lista
                    itemView.animate()
                            .alpha(0f)
                            .setDuration(350)
                            .withEndAction(() -> {
                                layoutEventosAgenda.removeView(itemView);
                                // Refresca lista por si hay encabezados vacíos
                                cargarEventosAgenda(inflater);
                            })
                            .start();
                    Toast.makeText(getContext(), "Evento eliminado de tu agenda", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar el evento", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean esEventoPasado(String fecha, String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date eventDate = sdf.parse(fecha + " " + hora);
            return eventDate.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private String formatearFechaHeader(String fechaOriginal) {
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = formatoEntrada.parse(fechaOriginal);
            SimpleDateFormat formatoSalida = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("es", "ES"));
            String salida = formatoSalida.format(fecha);
            return salida.substring(0, 1).toUpperCase() + salida.substring(1); // Primera mayúscula
        } catch (Exception e) {
            return fechaOriginal;
        }
    }

    private Date getDateFromEvento(String fecha, String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.parse(fecha + " " + hora);
        } catch (Exception e) {
            return null;
        }
    }
}