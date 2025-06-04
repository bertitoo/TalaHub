package com.talahub.app.ui.admin;

import android.app.Activity;
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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;

import java.util.List;

/**
 * Fragmento que permite a administradores visualizar, editar y eliminar
 * eventos desde la interfaz administrativa.
 *
 * Utiliza Firebase Firestore para obtener y modificar los datos.
 * Cada evento se muestra como una tarjeta con controles de edición y borrado.
 *
 * @author Alberto Martínez Vadillo
 */
public class GestionEventosFragment extends Fragment {

    private static final int REQUEST_EDIT_EVENTO = 1001;
    private LinearLayout layoutContenedor;
    private final FirebaseHelper firebaseHelper = new FirebaseHelper();

    /**
     * Crea e infla el layout del fragmento. Inicia la carga de eventos.
     *
     * @param inflater           Objeto para inflar layouts.
     * @param container          Contenedor padre del fragmento.
     * @param savedInstanceState Estado guardado (si lo hay).
     * @return Vista raíz del fragmento.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_eventos, container, false);
        layoutContenedor = root.findViewById(R.id.layout_eventos_admin);
        cargarEventos(inflater);
        return root;
    }

    /**
     * Carga los eventos desde Firestore y genera tarjetas visuales para cada uno.
     * Cada tarjeta incluye opciones para editar o eliminar el evento.
     *
     * @param inflater Usado para inflar las vistas de eventos.
     */
    private void cargarEventos(LayoutInflater inflater) {
        firebaseHelper.obtenerTodosLosEventos(eventos -> {
                    layoutContenedor.removeAllViews();  // Limpiar antes de repintar
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

                        // Acciones de los botones
                        btnEditar.setOnClickListener(v -> {
                            Intent intent = new Intent(getContext(), EditarEventoActivity.class);
                            intent.putExtra("id", evento.getId());
                            intent.putExtra("nombre", evento.getNombre());
                            intent.putExtra("fecha", evento.getFecha());
                            intent.putExtra("hora", evento.getHora());
                            intent.putExtra("descripcion", evento.getDescripcion());
                            intent.putExtra("lugar", evento.getLugar());
                            intent.putExtra("imagenUrl", evento.getImagenUrl());
                            startActivityForResult(intent, REQUEST_EDIT_EVENTO);
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
                                                    cargarEventos(inflater);  // Recargar tras eliminar
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                                                );
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();
                        });

                        layoutContenedor.addView(card);
                    }
                }, error ->
                        Toast.makeText(getContext(), "Error al cargar eventos", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Método llamado al volver desde una actividad de edición.
     * Si se editó un evento con éxito, recarga la lista.
     *
     * @param requestCode Código de solicitud enviado.
     * @param resultCode  Código de resultado devuelto.
     * @param data        Intent de datos (si aplica).
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_EVENTO && resultCode == Activity.RESULT_OK) {
            cargarEventos(LayoutInflater.from(getContext()));
        }
    }
}