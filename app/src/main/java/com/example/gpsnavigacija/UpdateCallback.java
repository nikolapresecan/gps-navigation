package com.example.gpsnavigacija;

public interface UpdateCallback {
    void onSuccess();
    void onFailure(String errorMessage);
}
