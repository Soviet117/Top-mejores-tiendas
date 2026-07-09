package com.example.topmejorestiendas.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "reservas",
    foreignKeys = {
        @ForeignKey(entity = Negocio.class, parentColumns = "id", childColumns = "idNegocio", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "idUsuario", onDelete = ForeignKey.CASCADE)
    }
)
public class Reserva {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int idNegocio;
    public int idUsuario;
    
    public String fecha; // Formato: YYYY-MM-DD
    public String horaInicio; // Formato: HH:MM
    public String horaFin; // Formato: HH:MM
    public int personas = 1; // Cantidad de personas
    
    // Estados posibles: PENDIENTE, CONFIRMADA, RECHAZADA, CANCELADA
    public String estado;
    
    public long fechaCreacion;

    public Integer idAmbiente;
    public Integer unidadNumero;
    public String nombreAmbiente;

    public Reserva(int idNegocio, int idUsuario, String fecha, String horaInicio, String horaFin, int personas, String estado, long fechaCreacion) {
        this.idNegocio = idNegocio;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }
}
