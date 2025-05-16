package com.talahub.app.ui.buscar;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;

import java.util.ArrayList;
import java.util.List;

public class BuscarFragment extends Fragment {

    private LinearLayout layoutResultados;
    private List<Evento> eventosOriginales = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_buscar, container, false);

        EditText etBuscar = root.findViewById(R.id.etBuscarEvento);
        layoutResultados = root.findViewById(R.id.layout_resultados_busqueda);

        // Cargar todos los eventos una vez
        new FirebaseHelper().obtenerTodosLosEventos(eventos -> {
            eventosOriginales.clear();
            eventosOriginales.addAll(eventos);
            mostrarResultados(eventosOriginales, inflater);
        }, error -> Toast.makeText(getContext(), "Error al cargar eventos", Toast.LENGTH_SHORT).show());

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarEventos(s.toString().trim(), inflater);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return root;
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

            TextView nombre = item.findViewById(R.id.tvNombreEventoBusqueda);
            TextView fechaHora = item.findViewById(R.id.tvFechaHoraBusqueda);
            TextView lugar = item.findViewById(R.id.tvLugarBusqueda);
            TextView precio = item.findViewById(R.id.tvPrecioBusqueda);
            ImageView imagen = item.findViewById(R.id.ivImagenBusqueda);

            nombre.setText(evento.getNombre());
            fechaHora.setText(evento.getFecha() + " - " + evento.getHora());
            lugar.setText(evento.getLugar());

            if (evento.getPrecio() == null || evento.getPrecio().trim().isEmpty() || evento.getPrecio().trim().equalsIgnoreCase("gratis")) {
                precio.setText("Precio: Gratis");
            } else {
                precio.setText("Precio: " + evento.getPrecio());
            }

            if (evento.getImagenUrl() != null && !evento.getImagenUrl().isEmpty()) {
                Picasso.get().load(evento.getImagenUrl()).into(imagen);
            } else {
                imagen.setImageResource(R.drawable.user_placeholder);
            }

            layoutResultados.addView(item);
        }
    }
}