package com.talahub.app.ui.destacados;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;
import com.talahub.app.ui.eventos.EventoDetalleActivity;

import java.util.List;

public class DestacadosFragment extends Fragment {

    private LinearLayout layoutEventos;
    private FirebaseHelper firebaseHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_destacados, container, false);
        layoutEventos = root.findViewById(R.id.layout_eventos);
        firebaseHelper = new FirebaseHelper();

        // 🔁 Cargar eventos destacados desde Firestore
        firebaseHelper.obtenerEventosDestacados(
                eventos -> mostrarEventos(eventos, inflater),
                error -> Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show()
        );

        return root;
    }

    private void mostrarEventos(List<Evento> eventos, LayoutInflater inflater) {
        layoutEventos.removeAllViews();

        for (Evento evento : eventos) {
            View tarjeta = inflater.inflate(R.layout.item_evento, layoutEventos, false);
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

            if (evento.getImagenUrl() != null && !evento.getImagenUrl().isEmpty()) {
                Picasso.get().load(evento.getImagenUrl()).into(imagen);
            }

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

            layoutEventos.addView(tarjeta);
        }
    }
}