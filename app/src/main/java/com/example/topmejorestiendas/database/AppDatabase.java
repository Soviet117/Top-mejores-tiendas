package com.example.topmejorestiendas.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.topmejorestiendas.model.Negocio;
import com.example.topmejorestiendas.model.Resena;
import com.example.topmejorestiendas.model.Usuario;
import java.util.concurrent.Executors;

@Database(entities = {Usuario.class, Negocio.class, Resena.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UsuarioDao usuarioDao();
    public abstract NegocioDao negocioDao();
    public abstract ResenaDao resenaDao();

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
        // Solo 2 cuentas como solicitaste
        Usuario cliente = new Usuario("Cliente de Prueba", "cliente@test.com", "123456", "999000111", null, false);
        Usuario duenio = new Usuario("Dueño de Prueba", "duenio@test.com", "123456", "999000222", null, true);
        
        db.usuarioDao().registrar(cliente);
        long duenioId = db.usuarioDao().registrar(duenio);

        // Negocio inicial para el dueño
        Negocio n1 = new Negocio("Ferretería Central", "Ferreterías", "Calle Ejemplo 123", "08:00 - 19:00", null, (int) duenioId);
        long n1Id = db.negocioDao().insertar(n1);

        // Reseñas iniciales
        db.resenaDao().insertar(new Resena(1, (int) n1Id, 5, "Excelente atención, muy recomendado.", System.currentTimeMillis()));
        db.resenaDao().insertar(new Resena(1, (int) n1Id, 4, "Buenos precios, regresaré pronto.", System.currentTimeMillis()));
        
        n1.calificacionPromedio = 4.5f;
        n1.id = (int) n1Id;
        db.negocioDao().actualizar(n1);
    }
}