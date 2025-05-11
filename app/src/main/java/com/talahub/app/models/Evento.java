package com.talahub.app.models;

public class Evento {
    private String id;
    private String nombre;
    private String descripcion;
    private String fecha;
    private String lugar;
    private String precio;
    private String imagenUrl;
    private boolean destacado;

    public Evento() {}

    public Evento(String id, String nombre, String descripcion, String fecha,
                  String lugar, String precio, String imagenUrl, boolean destacado) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.lugar = lugar;
        this.precio = precio;
        this.imagenUrl = imagenUrl;
        this.destacado = destacado;
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public String getPrecio() { return precio; }
    public void setPrecio(String precio) { this.precio = precio; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public boolean isDestacado() { return destacado; }
    public void setDestacado(boolean destacado) { this.destacado = destacado; }
}