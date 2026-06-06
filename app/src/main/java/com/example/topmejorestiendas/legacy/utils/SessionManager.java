package com.example.topmejorestiendas.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_FAVORITES = "favorites";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(int userId) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.commit();
    }

    public void createGuestSession() {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, -2);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public void logout() {
        editor.clear();
        editor.commit();
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
        editor.commit();
    }
}