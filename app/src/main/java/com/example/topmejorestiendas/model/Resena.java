package com.example.topmejorestiendas.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "resenas")
public class Resena {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int idUsuario;
    public int idNegocio;
    public int calificacion;
    public String comentario;
    public long fecha;
    public int calidadAtencion;
    public int calidadProductos;
    public int costos;
    public String respuestaDuenio;
    public String nombreUsuario;

    public Resena(int idUsuario, int idNegocio, int calificacion, int calidadAtencion, int calidadProductos, int costos, String comentario, long fecha) {
        this.idUsuario = idUsuario;
        this.idNegocio = idNegocio;
        this.calificacion = calificacion;
        this.calidadAtencion = calidadAtencion;
        this.calidadProductos = calidadProductos;
        this.costos = costos;
        this.comentario = comentario;
        this.fecha = fecha;
        this.respuestaDuenio = null;
    }
}