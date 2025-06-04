package com.talahub.app.models;

/**
 * Modelo que representa un evento en la aplicación.
 * Contiene información relevante como nombre, descripción, fecha, hora,
 * lugar, precio, imagen y si es destacado.
 *
 * @author Alberto Martínez Vadillo
 */
public class Evento {
    private String id;
    private String nombre;
    private String descripcion;
    private String fecha;
    private String hora;
    private String lugar;
    private String precio;
    private String imagenUrl;
    private boolean destacado;

    /**
     * Constructor vacío requerido para Firebase y serialización.
     */
    public Evento() {}

    /**
     * Constructor completo para crear un objeto Evento con todos sus campos.
     *
     * @param id          ID único del evento.
     * @param nombre      Nombre del evento.
     * @param descripcion Descripción del evento.
     * @param fecha       Fecha del evento (formato "dd-MM-yyyy").
     * @param hora        Hora del evento (formato "HH:mm").
     * @param lugar       Lugar donde se realiza el evento.
     * @param precio      Precio del evento como texto (ej. "Gratis", "5 €").
     * @param imagenUrl   URL de la imagen representativa del evento.
     * @param destacado   true si el evento debe destacarse en la interfaz.
     */
    public Evento(String id, String nombre, String descripcion, String fecha,
                  String hora, String lugar, String precio, String imagenUrl, boolean destacado) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.hora = hora;
        this.lugar = lugar;
        this.precio = precio;
        this.imagenUrl = imagenUrl;
        this.destacado = destacado;
    }

    /**
     * Obtiene el ID del evento.
     *
     * @return ID del evento.
     */
    public String getId() { return id; }

    /**
     * Establece el ID del evento.
     *
     * @param id ID del evento.
     */
    public void setId(String id) { this.id = id; }

    /**
     * Obtiene el nombre del evento.
     *
     * @return Nombre del evento.
     */
    public String getNombre() { return nombre; }

    /**
     * Establece el nombre del evento.
     *
     * @param nombre Nombre del evento.
     */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Obtiene la descripción del evento.
     *
     * @return Descripción del evento.
     */
    public String getDescripcion() { return descripcion; }

    /**
     * Establece la descripción del evento.
     *
     * @param descripcion Descripción del evento.
     */
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    /**
     * Obtiene la fecha del evento.
     *
     * @return Fecha en formato texto (ej. "dd-MM-yyyy").
     */
    public String getFecha() { return fecha; }

    /**
     * Establece la fecha del evento.
     *
     * @param fecha Fecha del evento (formato "dd-MM-yyyy").
     */
    public void setFecha(String fecha) { this.fecha = fecha; }

    /**
     * Obtiene la hora del evento.
     *
     * @return Hora en formato texto (ej. "HH:mm").
     */
    public String getHora() { return hora; }

    /**
     * Establece la hora del evento.
     *
     * @param hora Hora del evento (formato "HH:mm").
     */
    public void setHora(String hora) { this.hora = hora; }

    /**
     * Obtiene el lugar donde se celebrará el evento.
     *
     * @return Lugar del evento.
     */
    public String getLugar() { return lugar; }

    /**
     * Establece el lugar del evento.
     *
     * @param lugar Lugar del evento.
     */
    public void setLugar(String lugar) { this.lugar = lugar; }

    /**
     * Obtiene el precio del evento.
     *
     * @return Precio como texto (puede ser "Gratis" o valor con €).
     */
    public String getPrecio() { return precio; }

    /**
     * Establece el precio del evento.
     *
     * @param precio Precio como texto (ej. "5 €").
     */
    public void setPrecio(String precio) { this.precio = precio; }

    /**
     * Obtiene la URL de la imagen asociada al evento.
     *
     * @return URL de la imagen.
     */
    public String getImagenUrl() { return imagenUrl; }

    /**
     * Establece la URL de la imagen del evento.
     *
     * @param imagenUrl URL de la imagen.
     */
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    /**
     * Indica si el evento está marcado como destacado.
     *
     * @return true si es destacado, false en caso contrario.
     */
    public boolean isDestacado() { return destacado; }

    /**
     * Establece si el evento es destacado.
     *
     * @param destacado true si debe ser destacado.
     */
    public void setDestacado(boolean destacado) { this.destacado = destacado; }
}