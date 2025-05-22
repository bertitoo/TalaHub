package com.talahub.app.ui.agenda;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.talahub.app.R;
import com.talahub.app.models.Evento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_agenda, container, false);
        EditText etBuscar = root.findViewById(R.id.etBuscarAgenda);
        layoutEventosAgenda = root.findViewById(R.id.layout_eventos_agenda);

        Button btnExportarPdf = root.findViewById(R.id.btnExportarPdf);
        btnExportarPdf.setOnClickListener(v -> exportarAgendaComoPdf());

        cargarEventosAgenda(inflater);

        etBuscar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarEventosAgenda(s.toString().trim(), inflater);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        return root;
    }

    private void cargarEventosAgenda(LayoutInflater inflater) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

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
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al cargar tu agenda", Toast.LENGTH_SHORT).show());
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

        Collections.sort(eventos, (a, b) -> getDateFromEvento(a.getFecha(), a.getHora()).compareTo(getDateFromEvento(b.getFecha(), b.getHora())));

        for (Evento evento : eventos) {
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

    private void exportarAgendaComoPdf() {
        if (eventosOriginales.isEmpty()) {
            Toast.makeText(getContext(), "No hay eventos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener nombre del usuario y fecha actual
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

        File pdfFile = new File(getContext().getExternalFilesDir(null), "Agenda_TalaHub.pdf");

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

    private void eliminarEventoDeAgenda(String eventoId, View itemView, LayoutInflater inflater) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("agendas")
                .document(uid)
                .collection("eventos")
                .document(eventoId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    itemView.animate()
                            .alpha(0f)
                            .setDuration(350)
                            .withEndAction(() -> {
                                layoutEventosAgenda.removeView(itemView);
                                cargarEventosAgenda(inflater);
                            })
                            .start();
                    Toast.makeText(getContext(), "Evento eliminado de tu agenda", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al eliminar el evento", Toast.LENGTH_SHORT).show());
    }

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

    private Date getDateFromEvento(String fecha, String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.parse(fecha + " " + hora);
        } catch (Exception e) {
            return new Date();
        }
    }
}