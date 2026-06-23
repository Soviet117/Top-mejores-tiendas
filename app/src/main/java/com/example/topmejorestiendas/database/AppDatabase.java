package com.example.topmejorestiendas.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.topmejorestiendas.model.Negocio;
import com.example.topmejorestiendas.model.Reporte;
import com.example.topmejorestiendas.model.Resena;
import com.example.topmejorestiendas.model.Usuario;
import com.example.topmejorestiendas.model.Reserva;
import java.util.concurrent.Executors;

@Database(entities = {Usuario.class, Negocio.class, Resena.class, Reporte.class, Reserva.class}, version = 9)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UsuarioDao usuarioDao();
    public abstract NegocioDao negocioDao();
    public abstract ResenaDao resenaDao();
    public abstract ReporteDao reporteDao();
    public abstract ReservaDao reservaDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "top_tiendas_db")
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateData(getInstance(context));
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void prepopulateData(AppDatabase db) {
        db.usuarioDao().eliminarTodos();
        db.negocioDao().eliminarTodos();
        db.resenaDao().eliminarTodos();
        db.reporteDao().eliminarTodos();
        db.runInTransaction(() -> {
            db.query("DELETE FROM reservas", null);
        });
        // Base de datos vacía para empezar de cero según lo solicitado
    }
}