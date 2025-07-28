package com.example.fridaapp;
public class FormaDePago {
    private String nombre;
    private double cantidad;
    private String observaciones;

    // Constructor
    public FormaDePago(String nombre, double cantidad, String observaciones) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.observaciones = observaciones;
    }

    // Getters y setters para nombre
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // Getters y setters para cantidad
    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    // Getters y setters para observaciones
    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
