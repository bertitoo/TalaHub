package com.talahub.app.ui.destacados;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.models.Evento;
import com.talahub.app.ui.eventos.EventoDetalleActivity;

import java.util.ArrayList;
import java.util.List;

public class DestacadosFragment extends Fragment {

    private LinearLayout layoutEventos;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_destacados, container, false);
        layoutEventos = root.findViewById(R.id.layout_eventos);

        mostrarEventos(obtenerEventosSimulados(), inflater);

        return root;
    }

    private List<Evento> obtenerEventosSimulados() {
        List<Evento> lista = new ArrayList<>();
        lista.add(new Evento("1", "Concierto Indie", "", "2025-06-01", "Auditorio", "Gratis", "https://picsum.photos/400/200?1", true));
        lista.add(new Evento("2", "Feria de Artesanía", "", "2025-06-05", "Plaza Mayor", "Gratis", "https://picsum.photos/400/200?2", true));
        lista.add(new Evento("3", "Teatro Clásico", "", "2025-06-10", "Centro Cultural", "5€", "https://picsum.photos/400/200?3", true));
        lista.add(new Evento("4", "Festival de Cine", "", "2025-06-15", "Multicines", "3€", "https://picsum.photos/400/200?4", true));
        return lista;
    }

    private void mostrarEventos(List<Evento> eventos, LayoutInflater inflater) {
        layoutEventos.removeAllViews();

        for (Evento evento : eventos) {
            View tarjeta = inflater.inflate(R.layout.item_evento, layoutEventos, false);

            // Aplicar ancho fijo para que no se estire a toda pantalla
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 24);
            tarjeta.setLayoutParams(params);

            ImageView imagen = tarjeta.findViewById(R.id.ivImagenEvento);
            TextView nombre = tarjeta.findViewById(R.id.tvNombreEvento);
            TextView fechaLugar = tarjeta.findViewById(R.id.tvFechaLugar);
            TextView precio = tarjeta.findViewById(R.id.tvPrecio);

            nombre.setText(evento.getNombre());
            fechaLugar.setText(evento.getFecha() + " • " + evento.getLugar());
            precio.setText(evento.getPrecio());

            if (evento.getImagenUrl() != null) {
                Picasso.get().load(evento.getImagenUrl()).into(imagen);
            }

            layoutEventos.addView(tarjeta);

            tarjeta.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), EventoDetalleActivity.class);
                intent.putExtra("nombre", evento.getNombre());
                intent.putExtra("fecha", evento.getFecha());
                intent.putExtra("lugar", evento.getLugar());
                intent.putExtra("precio", evento.getPrecio());
                intent.putExtra("imagen", evento.getImagenUrl());
                intent.putExtra("descripcion", evento.getDescripcion());
                startActivity(intent);
            });

        }
    }
}