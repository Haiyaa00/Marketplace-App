package com.example.marketplace.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;
import com.example.marketplace.data.local.AppDatabase;
import com.example.marketplace.data.local.UserDao;
import com.example.marketplace.data.local.UserEntity;
import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.model.User;
import com.example.marketplace.utils.DataMapper;
import com.example.marketplace.utils.Resource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepository {

    private final FirebaseManager firebaseManager;
    private final UserDao userDao;
    private final ExecutorService executor;

    public AuthRepository(Application application) {
        this.firebaseManager = FirebaseManager.getInstance();
        AppDatabase db = AppDatabase.getInstance(application);
        this.userDao = db.userDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // Luôn lắng nghe User từ Room (Offline-first)
    public LiveData<UserEntity> getCachedUser() {
        return userDao.getCurrentUser();
    }

    // ================= Hàm Đăng Nhập =================
    public LiveData<Resource<Void>> login(String email, String password) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseManager.login(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser fbUser = task.getResult().getUser();
                if (fbUser != null) {
                    // KIỂM TRA ĐÃ XÁC THỰC EMAIL CHƯA
                    if (fbUser.isEmailVerified()) {
                        // Đã xác thực -> Kéo thông tin từ Firestore về và cho vào App
                        fetchAndCacheUser(fbUser.getUid(), result);
                    } else {
                        // Chưa xác thực -> Đăng xuất ngay và báo lỗi
                        firebaseManager.logout();
                        result.setValue(Resource.error("Vui lòng kiểm tra hộp thư @edu.vn và click link xác thực trước khi đăng nhập!", null));
                    }
                }
            } else {
                result.setValue(Resource.error(task.getException() != null ? task.getException().getMessage() : "Lỗi đăng nhập", null));
            }
        });
        return result;
    }

    // ================= Hàm Đăng Ký =================
    public LiveData<Resource<Void>> register(User user, String password) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseManager.register(user.getEmail(), password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser fbUser = task.getResult().getUser();
                if (fbUser != null) {
                    user.setUid(fbUser.getUid());
                    firebaseManager.saveUserInfo(user).addOnCompleteListener(saveTask -> {
                        if (saveTask.isSuccessful()) {
                            fbUser.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    // ĐÃ BỎ DÒNG FIREBASEMANAGER.LOGOUT() Ở ĐÂY
                                    result.postValue(Resource.success(null));
                                } else {
                                    result.setValue(Resource.error("Lỗi khi gửi email xác thực!", null));
                                }
                            });
                        } else {
                            result.setValue(Resource.error(saveTask.getException() != null ? saveTask.getException().getMessage() : "Lỗi lưu thông tin", null));
                        }
                    });
                }
            } else {
                result.setValue(Resource.error(task.getException() != null ? task.getException().getMessage() : "Lỗi đăng ký", null));
            }
        });
        return result;
    }

    // ================= Helper Method =================
    // Hàm này giúp kéo dữ liệu từ Firestore và lưu đè vào Room Local
    private void fetchAndCacheUser(String uid, MutableLiveData<Resource<Void>> result) {
        firebaseManager.getUserInfo(uid).addOnSuccessListener(documentSnapshot -> {
            User user = documentSnapshot.toObject(User.class);
            if (user != null) {
                // Ghi xuống Room trên background thread
                executor.execute(() -> {
                    userDao.insertUser(DataMapper.mapToUserEntity(user));
                    result.postValue(Resource.success(null)); // Update UI success
                });
            } else {
                result.setValue(Resource.error("Không tìm thấy dữ liệu người dùng trên server!", null));
            }
        }).addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
    }

    public LiveData<Boolean> checkEmailVerificationStatus() {
        MutableLiveData<Boolean> isVerified = new MutableLiveData<>();
        FirebaseUser currentUser = firebaseManager.getCurrentFirebaseUser();

        if (currentUser != null) {
            // Phải gọi reload() để lấy trạng thái mới nhất từ server Firebase
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    isVerified.setValue(currentUser.isEmailVerified());
                } else {
                    isVerified.setValue(false);
                }
            });
        } else {
            isVerified.setValue(false);
        }
        return isVerified;
    }

    public LiveData<Resource<Void>> updateAvatar(UserEntity currentUser, String newAvatarUrl) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        User updatedUser = new User(currentUser.uid, currentUser.name, currentUser.email, currentUser.phone, newAvatarUrl);

        firebaseManager.saveUserInfo(updatedUser).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                executor.execute(() -> {
                    userDao.insertUser(DataMapper.mapToUserEntity(updatedUser));
                    result.postValue(Resource.success(null));
                });
            } else {
                result.setValue(Resource.error(task.getException() != null ?
                        task.getException().getMessage() : "Lỗi cập nhật ảnh đại diện!", null));
            }
        });
        return result;
    }

    public LiveData<Resource<Void>> updateProfile(UserEntity currentUser, String newName, String newPhone) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // Tạo đối tượng User mới để đồng bộ
        User updatedUser = new User(currentUser.uid, newName, currentUser.email, newPhone, currentUser.avatarUrl);

        // 1. Lưu thông tin mới lên Firestore
        firebaseManager.saveUserInfo(updatedUser).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 2. Nếu thành công -> Cập nhật trực tiếp xuống SQLite cục bộ bằng Executor
                executor.execute(() -> {
                    userDao.insertUser(DataMapper.mapToUserEntity(updatedUser));
                    result.postValue(Resource.success(null));
                });
            } else {
                result.setValue(Resource.error(task.getException() != null ?
                        task.getException().getMessage() : "Không thể cập nhật thông tin!", null));
            }
        });
        return result;
    }

    // Đăng xuất: Clear Firebase Session và Clear Local Room DB
    public void logout() {
        firebaseManager.logout();
        executor.execute(userDao::clearUser);
    }

}