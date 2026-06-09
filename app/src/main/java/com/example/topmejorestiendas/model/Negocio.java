package com.example.topmejorestiendas.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "negocios")
public class Negocio {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nombreNegocio;
    public String rubro;
    public String direccion;
    public String horario;
    public float calificacionPromedio;
    public double latitud;
    public double longitud;
    public String fotoNegocio;
    public String descripcion;
    public int idDuenio;

    public Negocio(String nombreNegocio, String rubro, String direccion, String horario, String fotoNegocio, String descripcion, int idDuenio) {
        this.nombreNegocio = nombreNegocio;
        this.rubro = rubro;
        this.direccion = direccion;
        this.horario = horario;
        this.fotoNegocio = fotoNegocio;
        this.descripcion = descripcion;
        this.idDuenio = idDuenio;
        this.calificacionPromedio = 0;
    }
}