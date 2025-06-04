package com.talahub.app.ui.buscar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;
import com.talahub.app.ui.eventos.EventoDetalleActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Fragmento encargado de la funcionalidad de búsqueda de eventos.
 * Permite buscar por texto, aplicar filtros (fecha, hora, precio),
 * y seleccionar un evento aleatorio.
 *
 * También incluye lógica para mostrar los eventos encontrados
 * y abrir su detalle.
 *
 * @author Alberto Martínez Vadillo
 */
public class BuscarFragment extends Fragment {

    private static final String PREFS_NAME = "random_prefs";
    private static final String KEY_LAST_ID = "ultimo_random";

    private LinearLayout layoutResultados;
    private final List<Evento> eventosOriginales = new ArrayList<>();
    private final Random random = new Random();
    private SharedPreferences prefs;

    private String filtroFechaInicio = "", filtroFechaFin = "", filtroHora = "", filtroPrecio = "";
    private String filtroHoraInicio = "", filtroHoraFin = "";

    private EditText etBuscar;

    /**
     * Se llama al crear el fragmento. Inicializa las preferencias.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
    }

    /**
     * Infla la vista del fragmento, configura los listeners y carga los eventos desde Firebase.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_buscar, container, false);

        etBuscar = root.findViewById(R.id.etBuscarEvento);
        ImageButton btnRandom = root.findViewById(R.id.btnRandom);
        Button btnFiltros = root.findViewById(R.id.btnFiltros);
        layoutResultados = root.findViewById(R.id.layout_resultados_busqueda);

        btnRandom.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Evento aleatorio")
                .setMessage("¿Quieres generar un evento aleatorio?")
                .setPositiveButton("Sí", (d, w) -> {
                    View overlay = requireView().findViewById(R.id.overlayRandom);
                    overlay.setVisibility(View.VISIBLE);
                    v.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate_spin));
                    v.postDelayed(() -> {
                        lanzarEventoAleatorio();
                        overlay.setVisibility(View.GONE);
                    }, 800);
                })
                .setNegativeButton("Cancelar", null)
                .show());

        btnFiltros.setOnClickListener(v -> mostrarDialogoFiltros(inflater));

        // Carga de eventos desde Firebase
        new FirebaseHelper().obtenerTodosLosEventos(eventos -> {
            eventosOriginales.clear();
            eventosOriginales.addAll(eventos);
            mostrarResultados(eventos, inflater);
        }, error -> Toast.makeText(getContext(), "Error al cargar eventos", Toast.LENGTH_SHORT).show());

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltros(inflater);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return root;
    }

    /**
     * Selecciona un evento aleatorio distinto al último mostrado, y abre su detalle.
     */
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

    /**
     * Abre la pantalla de detalle de un evento.
     */
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
        startActivityForResult(intent, 101);
    }

    /**
     * Aplica los filtros activos y muestra los resultados filtrados.
     */
    private void aplicarFiltros(LayoutInflater inflater) {
        String texto = etBuscar.getText().toString().trim();

        new FirebaseHelper().buscarEventosFiltrados(
                texto,
                filtroFechaInicio,
                filtroFechaFin,
                filtroHoraInicio,
                filtroHoraFin,
                filtroPrecio,
                eventos -> mostrarResultados(eventos, inflater),
                error -> Toast.makeText(getContext(), "Error al filtrar eventos", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Muestra los resultados en el layout principal. Si no hay resultados, muestra un mensaje.
     */
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
            TextView tvApuntado = item.findViewById(R.id.tvApuntadoBusqueda);
            tvApuntado.setVisibility(View.GONE);

            TextView nombre = item.findViewById(R.id.tvNombreEventoBusqueda);
            TextView fechaHora = item.findViewById(R.id.tvFechaHoraBusqueda);
            TextView lugar = item.findViewById(R.id.tvLugarBusqueda);
            TextView precio = item.findViewById(R.id.tvPrecioBusqueda);
            ImageView imagen = item.findViewById(R.id.ivImagenBusqueda);
            ImageView ivDestacado = item.findViewById(R.id.ivDestacadoBusqueda);

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

            ivDestacado.setVisibility(evento.isDestacado() ? View.VISIBLE : View.GONE);

            item.setForeground(ContextCompat.getDrawable(requireContext(), R.drawable.ripple_effect));
            item.setOnClickListener(v -> abrirDetalle(evento));

            String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            new FirebaseHelper().estaApuntadoAEvento(uid, evento.getId(), apuntado -> {
                if (apuntado) {
                    tvApuntado.setVisibility(View.VISIBLE);
                }
            });

            layoutResultados.addView(item);
        }
    }

    /**
     * Muestra el diálogo de filtros con campos de fecha, hora y precio.
     */
    private void mostrarDialogoFiltros(LayoutInflater inflater) {
        View dialogView = inflater.inflate(R.layout.dialog_filtros, null);
        EditText etInicio = dialogView.findViewById(R.id.etFechaInicio);
        EditText etFin = dialogView.findViewById(R.id.etFechaFin);
        EditText etHoraInicio = dialogView.findViewById(R.id.etHoraInicio);
        EditText etHoraFin = dialogView.findViewById(R.id.etHoraFin);
        Spinner spinnerPrecio = dialogView.findViewById(R.id.spinnerPrecio);

        String[] opciones = {"Cualquiera", "Gratis", "0 - 4,99 €", "5 € o más"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrecio.setAdapter(adapter);

        etInicio.setText(filtroFechaInicio);
        etFin.setText(filtroFechaFin);
        etHoraInicio.setText(filtroHoraInicio);
        etHoraFin.setText(filtroHoraFin);
        int pos = List.of(opciones).indexOf(filtroPrecio);
        if (pos >= 0) spinnerPrecio.setSelection(pos);

        etInicio.setOnClickListener(v -> mostrarDatePicker(etInicio));
        etFin.setOnClickListener(v -> mostrarDatePicker(etFin));
        etHoraInicio.setOnClickListener(v -> mostrarTimePicker(etHoraInicio));
        etHoraFin.setOnClickListener(v -> mostrarTimePicker(etHoraFin));

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Aplicar", (dialog, which) -> {
                    filtroFechaInicio = etInicio.getText().toString().trim();
                    filtroFechaFin = etFin.getText().toString().trim();
                    filtroHoraInicio = etHoraInicio.getText().toString().trim();
                    filtroHoraFin = etHoraFin.getText().toString().trim();
                    filtroPrecio = spinnerPrecio.getSelectedItem().toString();
                    aplicarFiltros(inflater);
                })
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Limpiar", (dialog, which) -> {
                    filtroFechaInicio = filtroFechaFin = filtroHoraInicio = filtroHoraFin = filtroPrecio = "";
                    aplicarFiltros(inflater);
                })
                .show();
    }

    /**
     * Muestra un selector de fecha y coloca el resultado en el EditText correspondiente.
     */
    private void mostrarDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            String fecha = String.format(Locale.getDefault(), "%02d-%02d-%04d", d, m + 1, y);
            target.setText(fecha);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Muestra un selector de hora y coloca el resultado en el EditText correspondiente.
     */
    private void mostrarTimePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog tp = new TimePickerDialog(requireContext(), (view, h, m) -> {
            String hora = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            target.setText(hora);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        tp.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == AppCompatActivity.RESULT_OK) {
            // Recarga los eventos aplicando los filtros activos
            if (getView() != null) {
                aplicarFiltros(LayoutInflater.from(getContext()));
            }
        }
    }
}