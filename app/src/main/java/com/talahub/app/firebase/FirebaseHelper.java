package com.talahub.app.firebase;

import android.os.Build;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.talahub.app.models.Evento;
import com.talahub.app.models.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private static final String COLLECTION_EVENTOS = "eventos";
    private final FirebaseFirestore db;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Cargar todos los eventos destacados desde Firestore.
     *
     * @param onSuccess función que recibe una lista de eventos
     * @param onFailure función que recibe un mensaje de error
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
     * Obtener todos los eventos, independientemente de si están destacados.
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

    public void apuntarseAEvento(Evento evento, String uid, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("agendas")
                .document(uid)
                .collection("eventos")
                .document(evento.getId())
                .set(evento)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void estaApuntadoAEvento(String uid, String eventoId, Consumer<Boolean> callback) {
        db.collection("agendas")
                .document(uid)
                .collection("eventos")
                .document(eventoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        callback.accept(documentSnapshot.exists());
                    }
                })
                .addOnFailureListener(e -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        callback.accept(false); // Si falla, asumimos que NO está apuntado
                    }
                });
    }

    public void guardarUsuario(Usuario usuario) {
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario guardado"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al guardar usuario", e));
    }

}