package com.talahub.app.models;

public class Usuario {
    private String uid;
    private String correo;
    private String nombre;
    private String rol;

    public Usuario() {}

    public Usuario(String uid, String correo, String nombre, String rol) {
        this.uid = uid;
        this.correo = correo;
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
