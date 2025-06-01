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
import com.talahub.app.models.Usuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private static final String COLLECTION_EVENTOS = "eventos";
    private final FirebaseFirestore db;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    /** Sigue igual: carga eventos destacados desde /eventos */
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

    /** Sigue igual: carga todos los eventos desde /eventos */
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
     * Apuntar al evento: ahora guardamos solo un campo mínimo en
     * /usuarios/{uid}/agenda/{eventoId}, sin duplicar todo el objeto Evento.
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
     * Comprobar si existe /usuarios/{uid}/agenda/{eventoId}
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
     * Obtener la lista de Eventos (completos) a los que el usuario está apuntado.
     * Pasos:
     *  1) Leer /usuarios/{uid}/agenda → lista de IDs de eventos.
     *  2) Con un whereIn a /eventos traigo los datos completos.
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

                    // Segundo paso: leer datos reales de /eventos
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

    /** Guardar/editar perfil de usuario sigue igual en /usuarios/{uid} */
    public void guardarUsuario(Usuario usuario) {
        db.collection("usuarios")
                .document(usuario.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario guardado"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al guardar usuario", e));
    }

    /**
     * (Opcional) Dar de baja un apunte: borra /usuarios/{uid}/agenda/{eventoId}
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
}