package com.example.marketplace.data.remote;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.example.marketplace.model.Product;
import com.example.marketplace.model.User;

import java.util.UUID;

public class FirebaseManager {

    private static volatile FirebaseManager INSTANCE;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    // Tên các Collections trên Firestore
    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_PRODUCTS = "Products";

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static FirebaseManager getInstance() {
        if (INSTANCE == null) {
            synchronized (FirebaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FirebaseManager();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== AUTHENTICATION ====================

    public Task<AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }

    // ==================== FIRESTORE: USERS ====================

    public Task<Void> saveUserInfo(User user) {
        return db.collection(COLLECTION_USERS).document(user.getUid()).set(user);
    }

    public Task<DocumentSnapshot> getUserInfo(String uid) {
        return db.collection(COLLECTION_USERS).document(uid).get();
    }

    // ==================== FIRESTORE: PRODUCTS ====================

    // Lấy tất cả sản phẩm, sắp xếp mới nhất lên đầu
    public Task<QuerySnapshot> getAllProducts() {
        return db.collection(COLLECTION_PRODUCTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    // Lưu sản phẩm mới lên Firestore
    public Task<Void> saveProduct(Product product) {
        // Nếu ID rỗng, ta tạo mới 1 ID ngẫu nhiên
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId(UUID.randomUUID().toString());
        }
        return db.collection(COLLECTION_PRODUCTS).document(product.getId()).set(product);
    }

    // ==================== STORAGE: IMAGES ====================

    // Upload ảnh sản phẩm lên Firebase Storage và trả về URL tải xuống
    public UploadTask uploadProductImage(Uri imageUri, String fileName) {
        StorageReference ref = storage.getReference().child("product_images/" + fileName);
        return ref.putFile(imageUri);
    }

    // Lấy URL tải xuống sau khi upload thành công
    public Task<Uri> getDownloadUrl(String fileName) {
        return storage.getReference().child("product_images/" + fileName).getDownloadUrl();
    }

    public Task<Void> deleteProduct(String productId) {
        return db.collection(COLLECTION_PRODUCTS).document(productId).delete();
    }
}
