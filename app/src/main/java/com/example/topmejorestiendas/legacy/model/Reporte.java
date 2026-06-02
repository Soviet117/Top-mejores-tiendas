package com.example.topmejorestiendas.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reportes")
public class Reporte {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int idUsuario;
    public int idNegocio;
    public String motivo;
    public long fecha;

    public Reporte(int idUsuario, int idNegocio, String motivo, long fecha) {
        this.idUsuario = idUsuario;
        this.idNegocio = idNegocio;
        this.motivo = motivo;
        this.fecha = fecha;
    }
}
