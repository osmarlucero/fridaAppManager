package com.example.fridaapp;

public class ArticuloCobro {
    private int id;
    private int cantidad;
    private double precio;
    private String nombre;

    public ArticuloCobro(int id, String nombre, int cantidad, double precio){
        this.id = id;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    // Getter para el ID
    public int getId() {
        return id;
    }

    // Setter para el ID
    public void setId(int id) {
        this.id = id;
    }

    // Getter para la cantidad
    public int getCantidad() {
        return cantidad;
    }

    // Setter para la cantidad
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    // Getter para el precio
    public double getPrecio() {
        return precio;
    }

    // Setter para el precio
    public void setPrecio(double precio) {
        this.precio = precio;
    }

    // Getter para el nombre
    public String getNombre() {
        return nombre;
    }

    // Setter para el nombre
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
