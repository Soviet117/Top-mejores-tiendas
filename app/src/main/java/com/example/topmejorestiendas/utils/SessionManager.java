package com.example.topmejorestiendas.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/**
 * SessionManager — Gestión de sesión local del usuario.
 *
 * Almacena el JWT (authToken) recibido del backend junto con
 * los datos básicos del usuario para uso en toda la app.
 */
public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    // Claves legacy (se mantienen para compatibilidad con código Java existente)
    private static final String KEY_USER_ID    = "userId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_FAVORITES  = "favorites";
    // Claves nuevas para el backend
    private static final String KEY_AUTH_TOKEN    = "authToken";
    private static final String KEY_USER_NAME     = "userName";
    private static final String KEY_USER_EMAIL    = "userEmail";
    private static final String KEY_IS_OWNER      = "isOwner";
    private static final String KEY_PROFILE_PHOTO = "profilePhoto";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref   = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // ─── Nueva API para el backend ─────────────────────────────

    /**
     * Guarda todos los datos de sesión después del login/registro exitoso.
     */
    public void saveSession(String token, int userId, boolean isOwner, String userName, String userEmail, String fotoPerfil) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.putBoolean(KEY_IS_OWNER, isOwner);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.putString(KEY_PROFILE_PHOTO, fotoPerfil);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Limpia toda la sesión (logout completo).
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    /** Retorna el JWT Bearer token, o null si no hay sesión. */
    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public boolean isOwner() {
        return pref.getBoolean(KEY_IS_OWNER, false);
    }

    public String getProfilePhoto() {
        return pref.getString(KEY_PROFILE_PHOTO, "");
    }

    public void setProfilePhoto(String fotoPerfil) {
        editor.putString(KEY_PROFILE_PHOTO, fotoPerfil);
        editor.apply();
    }

    // ─── API legacy (compatibilidad con código existente) ──────

    public void createLoginSession(int userId) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.apply();
    }

    public void createGuestSession() {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, -2);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    /** @deprecated Usar clearSession() */
    @Deprecated
    public void logout() {
        clearSession();
    }

    public Set<String> getFavorites() {
        return pref.getStringSet(KEY_FAVORITES, new HashSet<>());
    }

    public void toggleFavorite(String businessId) {
        Set<String> favorites = new HashSet<>(getFavorites());
        if (favorites.contains(businessId)) {
            favorites.remove(businessId);
        } else {
            favorites.add(businessId);
        }
        editor.putStringSet(KEY_FAVORITES, favorites);
        editor.apply();
    }
}