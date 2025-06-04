package com.talahub.app.models;

/**
 * Modelo que representa a un usuario en la aplicación.
 * Contiene información básica como UID, correo, nombre y rol.
 *
 * @author Alberto Martínez Vadillo
 */
public class Usuario {
    private String uid;
    private String correo;
    private String nombre;
    private String rol;

    /**
     * Constructor vacío necesario para Firebase y serialización.
     */
    public Usuario() {}

    /**
     * Constructor completo del modelo Usuario.
     *
     * @param uid    Identificador único del usuario.
     * @param correo Correo electrónico del usuario.
     * @param nombre Nombre del usuario.
     * @param rol    Rol del usuario (ej. "usuario", "admin").
     */
    public Usuario(String uid, String correo, String nombre, String rol) {
        this.uid = uid;
        this.correo = correo;
        this.nombre = nombre;
        this.rol = rol;
    }

    /**
     * Obtiene el UID del usuario.
     *
     * @return UID como cadena.
     */
    public String getUid() { return uid; }

    /**
     * Establece el UID del usuario.
     *
     * @param uid Identificador único.
     */
    public void setUid(String uid) { this.uid = uid; }

    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return Correo como cadena.
     */
    public String getCorreo() { return correo; }

    /**
     * Establece el correo electrónico del usuario.
     *
     * @param correo Correo electrónico.
     */
    public void setCorreo(String correo) { this.correo = correo; }

    /**
     * Obtiene el nombre del usuario.
     *
     * @return Nombre del usuario.
     */
    public String getNombre() { return nombre; }

    /**
     * Establece el nombre del usuario.
     *
     * @param nombre Nombre del usuario.
     */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Obtiene el rol asignado al usuario.
     *
     * @return Rol como cadena (ej. "usuario", "admin").
     */
    public String getRol() { return rol; }

    /**
     * Establece el rol del usuario.
     *
     * @param rol Rol del usuario.
     */
    public void setRol(String rol) { this.rol = rol; }
}