package com.example.fridaapp;

public class Articulo {
    int id,cantidad;
    String nombre,descripcion,dependency;
    public Articulo(int id, String nombre, int cantidad, String descripcion,String dependency){
        this.id=id;
        this.dependency=dependency;
        this.nombre=nombre;
        this.cantidad=cantidad;
        this.descripcion=descripcion;

    }
    // Getter
    public String getNombre() {
        return nombre;
    }
    public int getId() {
        return id;
    }
    public String getDependency() {
        return dependency;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public int getCantidad() {
        return cantidad;
    }
    // Setter
    public void setNombre(String newNombre) {
        this.nombre = newNombre;
    }

}
