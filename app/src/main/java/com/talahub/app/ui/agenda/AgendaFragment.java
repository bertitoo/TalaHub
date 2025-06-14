package com.talahub.app.ui.agenda;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.firebase.FirebaseHelper;
import com.talahub.app.models.Evento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Fragmento que muestra al usuario la lista de eventos a los que está apuntado (su agenda).
 * Permite buscar eventos por texto, eliminarlos de la agenda, compartirlos y exportarlos a PDF.
 * Utiliza FirebaseAuth y FirebaseFirestore para manejar los datos del usuario.
 *
 * @author Alberto Martínez Vadillo
 */
public class AgendaFragment extends Fragment {

    private LinearLayout layoutEventosAgenda;
    private List<Evento> eventosOriginales = new ArrayList<>();
    private List<Evento> eventosFiltrados = new ArrayList<>();

    /**
     * Método que infla el layout del fragmento, carga la agenda del usuario y
     * configura los listeners para búsqueda y exportación.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_agenda, container, false);
        EditText etBuscar = root.findViewById(R.id.etBuscarAgenda);
        layoutEventosAgenda = root.findViewById(R.id.layout_eventos_agenda);

        TextView btnExportarPdf = root.findViewById(R.id.btnExportarPdf);
        btnExportarPdf.setOnClickListener(v -> exportarAgendaComoPdf());

        cargarEventosAgenda(inflater);

        etBuscar.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarEventosAgenda(s.toString().trim(), inflater);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        return root;
    }

    /**
     * Obtiene los eventos apuntados del usuario actual y actualiza la vista.
     */
    private void cargarEventosAgenda(LayoutInflater inflater) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para ver tu agenda", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper helper = new FirebaseHelper();
        helper.obtenerEventosEnAgenda(uid,
                eventos -> {
                    eventosOriginales.clear();
                    eventosOriginales.addAll(eventos);
                    filtrarEventosAgenda("", inflater);
                },
                error -> Toast.makeText(getContext(), "Error al cargar tu agenda", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Filtra los eventos por texto (nombre o lugar) y muestra los resultados.
     *
     * @param query     Texto de búsqueda.
     * @param inflater  LayoutInflater para inflar las vistas de eventos.
     */
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

    /**
     * Muestra en pantalla los eventos pasados como parámetro.
     * Si la lista está vacía, se muestra un mensaje.
     *
     * @param eventos   Lista de eventos a mostrar.
     * @param inflater  LayoutInflater para inflar las vistas.
     */
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

        List<Evento> eventosOrdenados = new ArrayList<>(eventos);
        Collections.sort(eventosOrdenados, (a, b) -> getDateFromEvento(a.getFecha(), a.getHora()).compareTo(getDateFromEvento(b.getFecha(), b.getHora())));

        for (Evento evento : eventosOrdenados) {
            View item = inflater.inflate(R.layout.item_evento_agenda, layoutEventosAgenda, false);

            TextView nombre = item.findViewById(R.id.tvAgendaNombreEvento);
            TextView fechaHora = item.findViewById(R.id.tvAgendaFechaHora);
            TextView lugar = item.findViewById(R.id.tvAgendaLugar);
            TextView precio = item.findViewById(R.id.tvAgendaPrecio);
            ImageView imagen = item.findViewById(R.id.ivAgendaImagenEvento);
            ImageButton btnEliminar = item.findViewById(R.id.btnEliminarEvento);
            ImageButton btnCompartir = item.findViewById(R.id.btnCompartirEvento);

            nombre.setText(evento.getNombre());
            fechaHora.setText(evento.getFecha() + " - " + evento.getHora());
            lugar.setText(evento.getLugar());
            precio.setText("Precio: " + (evento.getPrecio() == null || evento.getPrecio().isEmpty() ? "Gratis" : evento.getPrecio()));

            if (evento.getImagenUrl() != null && !evento.getImagenUrl().isEmpty()) {
                Picasso.get().load(evento.getImagenUrl()).into(imagen);
            } else {
                imagen.setImageResource(R.drawable.user_placeholder);
            }

            btnEliminar.setOnClickListener(v -> eliminarEventoDeAgenda(evento.getId(), item, inflater));
            btnCompartir.setOnClickListener(v -> compartirEvento(evento));

            layoutEventosAgenda.addView(item);
        }
    }

    /**
     * Exporta la agenda del usuario como un archivo PDF con los eventos listados.
     */
    private void exportarAgendaComoPdf() {
        if (eventosOriginales.isEmpty()) {
            Toast.makeText(getContext(), "No hay eventos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombreUsuario = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getDisplayName()
                : "Usuario";
        if (nombreUsuario == null || nombreUsuario.isEmpty()) {
            nombreUsuario = "Usuario";
        }

        String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(16);
        titlePaint.setFakeBoldText(true);

        int pageNumber = 1;
        int y = 50;
        int pageHeight = 1120;
        int pageWidth = 792;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_logo);
        Bitmap logoEscalado = Bitmap.createScaledBitmap(logoBitmap, 120, 120, false);
        int centerX = (pageWidth - logoEscalado.getWidth()) / 2;
        canvas.drawBitmap(logoEscalado, centerX, y, paint);
        y += logoEscalado.getHeight() + 40;

        canvas.drawText("Agenda de Eventos - TalaHub", 40, y, titlePaint); y += 25;
        canvas.drawText("Usuario: " + nombreUsuario, 40, y, paint); y += 25;
        canvas.drawText("Fecha de exportación: " + fechaActual, 40, y, paint); y += 40;

        for (Evento e : eventosOriginales) {
            if (y > pageHeight - 100) {
                pdfDocument.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            canvas.drawText("Evento: " + e.getNombre(), 40, y, paint); y += 25;
            canvas.drawText("Fecha: " + e.getFecha() + " " + e.getHora(), 40, y, paint); y += 25;
            canvas.drawText("Lugar: " + e.getLugar(), 40, y, paint); y += 25;
            canvas.drawText("Precio: " + (e.getPrecio() == null || e.getPrecio().isEmpty() ? "Gratis" : e.getPrecio()), 40, y, paint); y += 40;
        }

        pdfDocument.finishPage(page);

        String fechaNombre = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        File pdfFile = new File(getContext().getExternalFilesDir(null), "Agenda_TalaHub_" + fechaNombre + ".pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(getContext(), "PDF generado." , Toast.LENGTH_LONG).show();

            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", pdfFile);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Exportación completada")
                    .setMessage("¿Qué deseas hacer con el PDF?")
                    .setPositiveButton("Abrir PDF", (dialog, which) -> {
                        Intent abrirIntent = new Intent(Intent.ACTION_VIEW);
                        abrirIntent.setDataAndType(uri, "application/pdf");
                        abrirIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            startActivity(abrirIntent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "No hay visor de PDF disponible", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Compartir", (dialog, which) -> {
                        Intent compartirIntent = new Intent(Intent.ACTION_SEND);
                        compartirIntent.setType("application/pdf");
                        compartirIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        compartirIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(compartirIntent, "Compartir PDF con..."));
                    })
                    .setNeutralButton("Cancelar", null)
                    .show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Elimina un evento de la agenda del usuario actual.
     *
     * @param eventoId  ID del evento a eliminar.
     * @param itemView  Vista del item en la UI.
     * @param inflater  LayoutInflater para recargar la lista.
     */
    private void eliminarEventoDeAgenda(String eventoId, View itemView, LayoutInflater inflater) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseHelper helper = new FirebaseHelper();
        helper.quitarApunteEvento(eventoId, uid,
                aVoid -> {
                    itemView.animate()
                            .alpha(0f)
                            .setDuration(350)
                            .withEndAction(() -> {
                                layoutEventosAgenda.removeView(itemView);
                                cargarEventosAgenda(inflater);
                            })
                            .start();
                    Toast.makeText(getContext(), "Evento eliminado de tu agenda", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(getContext(), "Error al eliminar el evento", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Crea un intent para compartir un evento por otras apps.
     *
     * @param evento Evento a compartir.
     */
    private void compartirEvento(Evento evento) {
        String mensaje = "¡Mira este evento!\n\n" +
                "" + evento.getNombre() + "\n" +
                "Fecha: " + evento.getFecha() + " " + evento.getHora() + "\n" +
                "Lugar: " + evento.getLugar() + "\n" +
                "Precio: " + evento.getPrecio() + "\n\n" +
                "¿Te apuntas?";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mensaje);
        startActivity(Intent.createChooser(shareIntent, "Compartir evento con..."));
    }

    /**
     * Convierte la fecha y hora de un evento en un objeto Date para ordenación.
     *
     * @param fecha Fecha del evento (formato dd/MM/yyyy).
     * @param hora  Hora del evento (formato HH:mm).
     * @return Objeto Date representando la fecha y hora.
     */
    private Date getDateFromEvento(String fecha, String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            return sdf.parse(fecha + " " + hora);
        } catch (Exception e) {
            return new Date();
        }
    }
}