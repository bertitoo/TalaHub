package com.talahub.app.firebase;

import android.os.Build;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.talahub.app.models.Evento;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Clase de utilidad para gestionar operaciones con Firebase Firestore
 * relacionadas con la colección de eventos y la agenda de usuarios.
 *
 * Proporciona métodos para obtener eventos, filtrar, apuntarse o quitarse
 * de eventos, y comprobar estado de inscripción.
 *
 * @author Alberto Martínez Vadillo
 */
public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private static final String COLLECTION_EVENTOS = "eventos";
    private final FirebaseFirestore db;

    /**
     * Constructor que inicializa la instancia de FirebaseFirestore.
     */
    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Obtiene los eventos marcados como destacados en la base de datos.
     *
     * @param onSuccess Callback para manejar la lista de eventos destacados.
     * @param onFailure Callback para manejar errores.
     */
    public void obtenerEventosDestacados(Consumer<List<Evento>> onSuccess, Consumer<String> onFailure) {
        db.collection(COLLECTION_EVENTOS)
                .whereEqualTo("destacado", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Evento> eventos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Evento evento = doc.toObject(Evento.class);
                        eventos.add(evento);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onSuccess.accept(eventos);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar eventos", e);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onFailure.accept(e.getMessage());
                    }
                });
    }

    /**
     * Obtiene todos los eventos registrados en la base de datos.
     *
     * @param onSuccess Callback que recibe la lista de eventos.
     * @param onFailure Callback para manejar errores.
     */
    public void obtenerTodosLosEventos(Consumer<List<Evento>> onSuccess, Consumer<String> onFailure) {
        db.collection(COLLECTION_EVENTOS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Evento> eventos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Evento evento = doc.toObject(Evento.class);
                        eventos.add(evento);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onSuccess.accept(eventos);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar todos los eventos", e);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onFailure.accept(e.getMessage());
                    }
                });
    }

    /**
     * Registra al usuario en un evento.
     * Guarda el ID del evento y la marca de tiempo en la subcolección /agenda.
     *
     * @param eventoId  ID del evento.
     * @param uid       ID del usuario.
     * @param onSuccess Callback de éxito.
     * @param onFailure Callback de error.
     */
    public void apuntarseAEvento(String eventoId, String uid,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {
        Map<String,Object> datos = new HashMap<>();
        datos.put("apuntadoEn", FieldValue.serverTimestamp());

        db.collection("usuarios")
                .document(uid)
                .collection("agenda")
                .document(eventoId)
                .set(datos)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Verifica si el usuario está apuntado a un evento específico.
     *
     * @param uid      ID del usuario.
     * @param eventoId ID del evento.
     * @param callback Callback con resultado booleano.
     */
    public void estaApuntadoAEvento(String uid, String eventoId, Consumer<Boolean> callback) {
        db.collection("usuarios")
                .document(uid)
                .collection("agenda")
                .document(eventoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        callback.accept(documentSnapshot.exists());
                    }
                })
                .addOnFailureListener(e -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        callback.accept(false);
                    }
                });
    }

    /**
     * Obtiene la lista completa de eventos a los que el usuario está apuntado.
     * Realiza una consulta a la subcolección /agenda y luego una búsqueda en /eventos.
     *
     * @param uid       ID del usuario.
     * @param onSuccess Callback con la lista de eventos.
     * @param onFailure Callback en caso de error.
     */
    public void obtenerEventosEnAgenda(String uid, Consumer<List<Evento>> onSuccess, Consumer<String> onFailure) {
        db.collection("usuarios")
                .document(uid)
                .collection("agenda")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onSuccess.accept(new ArrayList<>());
                        }
                        return;
                    }

                    List<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ids.add(doc.getId());
                    }

                    db.collection("eventos")
                            .whereIn(FieldPath.documentId(), ids)
                            .get()
                            .addOnSuccessListener(eventosSnapshot -> {
                                List<Evento> listaEventos = new ArrayList<>();
                                for (QueryDocumentSnapshot eDoc : eventosSnapshot) {
                                    Evento e = eDoc.toObject(Evento.class);
                                    listaEventos.add(e);
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    onSuccess.accept(listaEventos);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    onFailure.accept(e.getMessage());
                                }
                            });

                })
                .addOnFailureListener(e -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onFailure.accept(e.getMessage());
                    }
                });
    }

    /**
     * Elimina el apunte de un usuario a un evento.
     *
     * @param eventoId  ID del evento.
     * @param uid       ID del usuario.
     * @param onSuccess Callback de éxito.
     * @param onFailure Callback de error.
     */
    public void quitarApunteEvento(String eventoId, String uid,
                                   OnSuccessListener<Void> onSuccess,
                                   OnFailureListener onFailure) {
        db.collection("usuarios")
                .document(uid)
                .collection("agenda")
                .document(eventoId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Busca eventos que coincidan con los filtros proporcionados:
     * texto (nombre/lugar/descripcion), fecha, hora y precio.
     *
     * @param texto        Texto a buscar.
     * @param fechaInicio  Fecha mínima (formato dd-MM-yyyy).
     * @param fechaFin     Fecha máxima (formato dd-MM-yyyy).
     * @param horaInicio   Hora mínima (formato HH:mm).
     * @param horaFin      Hora máxima (formato HH:mm).
     * @param precioFiltro Filtro de precio ("Gratis", "0 - 4,99 €", "5 € o más").
     * @param onSuccess    Callback con la lista de eventos filtrados.
     * @param onFailure    Callback en caso de error.
     */
    public void buscarEventosFiltrados(String texto, String fechaInicio, String fechaFin, String horaInicio, String horaFin, String precioFiltro,
                                       Consumer<List<Evento>> onSuccess, Consumer<String> onFailure) {

        db.collection(COLLECTION_EVENTOS)
                .get()
                .addOnSuccessListener(query -> {
                    List<Evento> resultado = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : query) {
                        Evento e = doc.toObject(Evento.class);

                        boolean coincideTexto = texto.isEmpty() ||
                                e.getNombre().toLowerCase().contains(texto.toLowerCase()) ||
                                e.getLugar().toLowerCase().contains(texto.toLowerCase()) ||
                                e.getDescripcion().toLowerCase().contains(texto.toLowerCase());

                        boolean coincideFecha = true;
                        try {
                            Date fechaEvento = sdf.parse(e.getFecha());
                            if (!fechaInicio.isEmpty()) {
                                Date inicio = sdf.parse(fechaInicio);
                                assert fechaEvento != null;
                                if (fechaEvento.before(inicio)) coincideFecha = false;
                            }
                            if (!fechaFin.isEmpty()) {
                                Date fin = sdf.parse(fechaFin);
                                assert fechaEvento != null;
                                if (fechaEvento.after(fin)) coincideFecha = false;
                            }
                        } catch (ParseException ex) {
                            coincideFecha = false;
                        }

                        boolean coincideHora = true;
                        try {
                            SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            Date horaEvento = sdfHora.parse(e.getHora());

                            if (!horaInicio.isEmpty()) {
                                Date inicio = sdfHora.parse(horaInicio);
                                assert horaEvento != null;
                                if (horaEvento.before(inicio)) coincideHora = false;
                            }
                            if (!horaFin.isEmpty()) {
                                Date fin = sdfHora.parse(horaFin);
                                assert horaEvento != null;
                                if (horaEvento.after(fin)) coincideHora = false;
                            }
                        } catch (Exception ex) {
                            coincideHora = false;
                        }

                        boolean coincidePrecio = true;
                        try {
                            String precioStr = e.getPrecio() == null ? "gratis" :
                                    e.getPrecio().toLowerCase().replace("€", "").replace(",", ".").trim();
                            double precio = precioStr.equals("gratis") ? 0 : Double.parseDouble(precioStr);

                            switch (precioFiltro) {
                                case "Gratis": coincidePrecio = precio == 0; break;
                                case "0 - 4,99 €": coincidePrecio = precio > 0 && precio <= 4.99; break;
                                case "5 € o más": coincidePrecio = precio >= 5; break;
                                default:
                            }
                        } catch (Exception ignored) {}

                        if (coincideTexto && coincideFecha && coincideHora && coincidePrecio) {
                            resultado.add(e);
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onSuccess.accept(resultado);
                    }
                })
                .addOnFailureListener(e -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onFailure.accept(e.getMessage());
                    }
                });
    }
}