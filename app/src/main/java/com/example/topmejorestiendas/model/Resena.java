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

    public Resena(int idUsuario, int idNegocio, int calificacion, String comentario, long fecha) {
        this.idUsuario = idUsuario;
        this.idNegocio = idNegocio;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.fecha = fecha;
    }
}