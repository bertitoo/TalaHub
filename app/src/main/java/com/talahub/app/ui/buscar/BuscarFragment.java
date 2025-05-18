package com.talahub.app.ui.buscar;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;
import com.talahub.app.ui.eventos.EventoDetalleActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BuscarFragment extends Fragment {

    private static final String PREFS_NAME = "random_prefs";
    private static final String KEY_LAST_ID = "ultimo_random";

    private LinearLayout layoutResultados;
    private final List<Evento> eventosOriginales = new ArrayList<>();
    private final Random random = new Random();
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_buscar, container, false);

        EditText   etBuscar   = root.findViewById(R.id.etBuscarEvento);
        ImageButton btnRandom = root.findViewById(R.id.btnRandom);
        layoutResultados      = root.findViewById(R.id.layout_resultados_busqueda);

        btnRandom.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Evento aleatorio")
                .setMessage("¿Quieres generar un evento aleatorio?")
                .setPositiveButton("Sí", (d, w) -> {

                    // 1. Mostrar overlay
                    View overlay = requireView().findViewById(R.id.overlayRandom);
                    overlay.setVisibility(View.VISIBLE);

                    // 2. Iniciar animación
                    v.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate_spin));

                    // 3. Ejecutar acción después de delay
                    v.postDelayed(() -> {
                        lanzarEventoAleatorio();
                        overlay.setVisibility(View.GONE); // ocultar overlay después
                    }, 800);

                })
                .setNegativeButton("Cancelar", null)
                .show());

        new FirebaseHelper().obtenerTodosLosEventos(eventos -> {
            eventosOriginales.clear();
            eventosOriginales.addAll(eventos);
            mostrarResultados(eventosOriginales, inflater);
        }, error -> Toast.makeText(getContext(),
                "Error al cargar eventos", Toast.LENGTH_SHORT).show());

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarEventos(s.toString().trim(), inflater);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return root;
    }

    private void lanzarEventoAleatorio() {
        if (eventosOriginales.isEmpty()) {
            Toast.makeText(getContext(), "Aún no hay eventos", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Evento> candidatos = new ArrayList<>(eventosOriginales);
        Evento elegido;
        String lastId = prefs.getString(KEY_LAST_ID, "");

        if (candidatos.size() == 1) {
            elegido = candidatos.get(0);
        } else {
            do {
                elegido = candidatos.get(random.nextInt(candidatos.size()));
            } while (elegido.getId().equals(lastId));
        }

        prefs.edit().putString(KEY_LAST_ID, elegido.getId()).apply();
        abrirDetalle(elegido);
    }

    private void abrirDetalle(Evento evento) {
        Intent intent = new Intent(requireContext(), EventoDetalleActivity.class);
        intent.putExtra("id", evento.getId());
        intent.putExtra("nombre", evento.getNombre());
        intent.putExtra("descripcion", evento.getDescripcion());
        intent.putExtra("fecha", evento.getFecha());
        intent.putExtra("hora", evento.getHora());
        intent.putExtra("lugar", evento.getLugar());
        intent.putExtra("precio", evento.getPrecio());
        intent.putExtra("imagen", evento.getImagenUrl());
        startActivity(intent);
    }

    private void filtrarEventos(String query, LayoutInflater inflater) {
        List<Evento> filtrados = new ArrayList<>();
        for (Evento e : eventosOriginales) {
            if (e.getNombre().toLowerCase().contains(query.toLowerCase()) ||
                    e.getLugar().toLowerCase().contains(query.toLowerCase()) ||
                    e.getDescripcion().toLowerCase().contains(query.toLowerCase())) {
                filtrados.add(e);
            }
        }
        mostrarResultados(filtrados, inflater);
    }

    private void mostrarResultados(List<Evento> eventos, LayoutInflater inflater) {
        layoutResultados.removeAllViews();

        if (eventos.isEmpty()) {
            TextView vacio = new TextView(getContext());
            vacio.setText("No se encontraron eventos.");
            vacio.setTextSize(18f);
            vacio.setTextColor(getResources().getColor(R.color.color_azul));
            vacio.setPadding(0, 48, 0, 0);
            layoutResultados.addView(vacio);
            return;
        }

        for (Evento evento : eventos) {
            View item = inflater.inflate(R.layout.item_evento_busqueda, layoutResultados, false);

            TextView nombre      = item.findViewById(R.id.tvNombreEventoBusqueda);
            TextView fechaHora   = item.findViewById(R.id.tvFechaHoraBusqueda);
            TextView lugar       = item.findViewById(R.id.tvLugarBusqueda);
            TextView precio      = item.findViewById(R.id.tvPrecioBusqueda);
            ImageView imagen     = item.findViewById(R.id.ivImagenBusqueda);
            ImageView ivDestacado= item.findViewById(R.id.ivDestacadoBusqueda);

            nombre.setText(evento.getNombre());
            fechaHora.setText(evento.getFecha() + " - " + evento.getHora());
            lugar.setText(evento.getLugar());

            if (evento.getPrecio() == null || evento.getPrecio().trim().isEmpty() ||
                    evento.getPrecio().trim().equalsIgnoreCase("gratis")) {
                precio.setText("Precio: Gratis");
            } else {
                precio.setText("Precio: " + evento.getPrecio());
            }

            if (evento.getImagenUrl() != null && !evento.getImagenUrl().isEmpty()) {
                Picasso.get().load(evento.getImagenUrl()).into(imagen);
            } else {
                imagen.setImageResource(R.drawable.user_placeholder);
            }

            ivDestacado.setVisibility(evento.isDestacado() ? View.VISIBLE : View.GONE);

            item.setForeground(ContextCompat.getDrawable(requireContext(),
                    R.drawable.ripple_effect));
            item.setOnClickListener(v -> abrirDetalle(evento));
            layoutResultados.addView(item);
        }
    }
}