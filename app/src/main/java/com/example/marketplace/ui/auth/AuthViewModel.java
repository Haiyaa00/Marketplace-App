package com.example.marketplace.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.repository.AuthRepository;
import com.example.marketplace.model.User;
import com.example.marketplace.utils.Resource;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<Resource<Void>> login(String email, String password) {
        return repository.login(email, password);
    }

    public LiveData<Resource<Void>> register(User user, String password) {
        return repository.register(user, password);
    }

    public LiveData<Boolean> checkVerificationStatus() {
        return repository.checkEmailVerificationStatus();
    }

    public void logout() {
        repository.logout();
    }
}