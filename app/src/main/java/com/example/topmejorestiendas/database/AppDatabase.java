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

@Database(entities = {Usuario.class, Negocio.class, Resena.class}, version = 1)
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
        Usuario c1 = new Usuario("Cliente Uno", "cliente1@test.com", "123456", "999888777", null, false);
        Usuario c2 = new Usuario("Cliente Dos", "cliente2@test.com", "123456", "999888776", null, false);
        Usuario d1 = new Usuario("Dueño Uno", "duenio@test.com", "123456", "999888775", null, true);
        
        db.usuarioDao().registrar(c1);
        db.usuarioDao().registrar(c2);
        long d1Id = db.usuarioDao().registrar(d1);

        Negocio n1 = new Negocio("Ferretería Ejemplo", "Ferreterías", "Av. Test 123", "08:00 - 18:00", null, (int) d1Id);
        long n1Id = db.negocioDao().insertar(n1);

        db.resenaDao().insertar(new Resena(1, (int) n1Id, 5, "Excelente servicio", System.currentTimeMillis()));
        db.resenaDao().insertar(new Resena(2, (int) n1Id, 4, "Muy bueno pero falta stock", System.currentTimeMillis()));
        db.resenaDao().insertar(new Resena(1, (int) n1Id, 5, "Lo mejor de la zona", System.currentTimeMillis()));
        
        n1.calificacionPromedio = 4.6f;
        n1.id = (int) n1Id;
        db.negocioDao().actualizar(n1);
    }
}