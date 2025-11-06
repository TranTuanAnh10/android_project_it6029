package vn.haui.android_project.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import vn.haui.android_project.entity.PaymentCard;
// Giả định DatabaseTable.USER_PAYMENT_METHOD tồn tại hoặc dùng hằng số
// import vn.haui.android_project.enums.DatabaseTable;

public class FirebasePaymentManager {

    private static final String TAG = "FirebasePaymentManager";
    private static final String ROOT_COLLECTION = "user_payment_method"; // Collection chính
    private static final String CARD_LIST_FIELD = "listCard"; // Tên field chứa Array of Cards

    private static FirebasePaymentManager instance;

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final String currentUserId;
    private final Gson gson;

    private FirebasePaymentManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        gson = new Gson();

        FirebaseUser user = mAuth.getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;
    }

    public static synchronized FirebasePaymentManager getInstance() {
        if (instance == null) {
            instance = new FirebasePaymentManager();
        }
        return instance;
    }

    /**
     * Tạo tham chiếu đến Document chứa danh sách thẻ của người dùng hiện tại.
     * Đường dẫn: user_payment_method/{uid}
     */
    private DocumentReference getUserDocumentRef() {
        if (currentUserId == null) {
            return null;
        }
        return db.collection(ROOT_COLLECTION)
                .document(currentUserId);
    }

    // -------------------------------------------------------------------
    // I. CREATE (Thêm mới thẻ)
    // -------------------------------------------------------------------

    /**
     * Thêm một thẻ thanh toán mới (sử dụng arrayUnion).
     */
    public Task<Void> addCard(@NonNull PaymentCard newCard) {
        DocumentReference userDocRef = getUserDocumentRef();
        if (userDocRef == null) {
            return Tasks.forException(new IllegalStateException("Người dùng chưa đăng nhập."));
        }

        // Gán ID duy nhất (sử dụng thời gian hoặc ID ngẫu nhiên)
        newCard.cardId = String.valueOf(System.currentTimeMillis());

        // Thử thêm vào array, nếu array chưa tồn tại, nó sẽ tạo mới
        return userDocRef.update(CARD_LIST_FIELD, FieldValue.arrayUnion(newCard))
                .addOnFailureListener(e -> {
                    // Nếu document chưa tồn tại (hoặc field chưa tồn tại và update thất bại)
                    if (e.getMessage() != null && e.getMessage().contains("NOT_FOUND")) {
                        // Tạo document mới với field listCard chứa card đầu tiên
                        userDocRef.set(new HashMap<String, Object>() {{
                            put(CARD_LIST_FIELD, List.of(newCard));
                        }});
                    } else {
                        Log.e(TAG, "Lỗi khi thêm thẻ: " + e.getMessage());
                    }
                });
    }

    // -------------------------------------------------------------------
    // II. READ (Đọc danh sách thẻ)
    // -------------------------------------------------------------------

    /**
     * Hàm hỗ trợ ánh xạ thủ công từ Map sang PaymentCard (cần thiết khi dùng Array trong Document).
     */
    private PaymentCard mapToPaymentCard(Map<String, Object> map) {
        // Ánh xạ thủ công cần chính xác theo tên các trường trong PaymentCard
        PaymentCard card = new PaymentCard();
        card.cardId = (String) map.get("cardId");
        card.nameOnCard = (String) map.get("nameOnCard");
        card.cardNumber = (String) map.get("cardNumber");
        card.expirationDate = (String) map.get("expirationDate");
        card.cvv = (String) map.get("cvv");
        card.cardType = (String) map.get("cardType");
        card.last4Digits = (String) map.get("last4Digits");
        return card;
    }

    /**
     * Lắng nghe và đọc toàn bộ danh sách thẻ (Realtime Updates).
     * @param onUpdate BiConsumer<Boolean, List<PaymentCard>> để xử lý danh sách.
     */
    public ListenerRegistration getCardsRealtime(BiConsumer<Boolean, List<PaymentCard>> onUpdate) {
        DocumentReference userDocRef = getUserDocumentRef();
        if (userDocRef == null) {
            onUpdate.accept(false, Collections.emptyList());
            return null;
        }

        return userDocRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Lỗi lắng nghe thẻ: " + error.getMessage());
                onUpdate.accept(false, Collections.emptyList());
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                List<Map<String, Object>> rawCards = (List<Map<String, Object>>) snapshot.get(CARD_LIST_FIELD);
                List<PaymentCard> cardList = new ArrayList<>();

                if (rawCards != null) {
                    for (Map<String, Object> cardMap : rawCards) {
                        try {
                            // Ánh xạ thủ công vì toObject() không hoạt động trực tiếp trên List<Map>
                            PaymentCard card = mapToPaymentCard(cardMap);
                            cardList.add(card);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Lỗi ánh xạ thẻ: " + e.getMessage());
                        }
                    }
                }

                // (Tùy chọn) Sắp xếp danh sách nếu cần (Client-side Sorting)
                // cardList.sort(Comparator.comparing(PaymentCard::getNameOnCard));

                onUpdate.accept(true, cardList);
            } else {
                onUpdate.accept(true, Collections.emptyList());
            }
        });
    }

    /**
     * Lấy dữ liệu của một thẻ cụ thể (One-time read). Cần Transaction để đảm bảo consistency.
     */
    public Task<PaymentCard> getCardById(String cardId) {
        DocumentReference userDocRef = getUserDocumentRef();
        if (userDocRef == null) {
            return Tasks.forException(new IllegalStateException("Người dùng chưa đăng nhập."));
        }

        return userDocRef.get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot snapshot = task.getResult();
                List<Map<String, Object>> rawCards = (List<Map<String, Object>>) snapshot.get(CARD_LIST_FIELD);

                if (rawCards != null) {
                    for (Map<String, Object> cardMap : rawCards) {
                        PaymentCard card = mapToPaymentCard(cardMap);
                        if (card.cardId != null && card.cardId.equals(cardId)) {
                            return card;
                        }
                    }
                }
            }
            throw new Exception("Không tìm thấy thẻ hoặc lỗi đọc dữ liệu.");
        });
    }


    public void updateCardByFields(String cardId, Map<String, Object> updates, BiConsumer<Boolean, String> onComplete) {
        DocumentReference userDocRef = getUserDocumentRef();
        String uid = currentUserId;

        if (userDocRef == null || uid == null) {
            onComplete.accept(false, "Người dùng chưa đăng nhập.");
            return;
        }

        // 1. Đọc thẻ hiện tại (One-time read)
        getCardDetails(cardId, (existingCard, error) -> {
            if (error != null || existingCard == null) {
                onComplete.accept(false, "Không thể tải thẻ hiện tại: " + (error != null ? error.getMessage() : "Card not found."));
                return;
            }

            // 2. Cập nhật các trường trên đối tượng PaymentCard đã có
            existingCard.nameOnCard = (String) updates.getOrDefault("nameOnCard", existingCard.nameOnCard);
            existingCard.cardNumber = (String) updates.getOrDefault("cardNumber", existingCard.cardNumber);
            existingCard.expirationDate = (String) updates.getOrDefault("expirationDate", existingCard.expirationDate);
            existingCard.cvv = (String) updates.getOrDefault("cvv", existingCard.cvv);
            existingCard.cardType = (String) updates.getOrDefault("cardType", existingCard.cardType);
            existingCard.last4Digits = (String) updates.getOrDefault("last4Digits", existingCard.last4Digits);

            // 3. Gọi hàm updateCard (logic transaction) hiện có của bạn
            updateCard(uid, existingCard, onComplete);
        });
    }
    public void updateCard(String uid, PaymentCard updatedCard, BiConsumer<Boolean, String> onComplete) {
        DocumentReference userDocRef = db.collection(ROOT_COLLECTION).document(uid);
        String targetId = updatedCard.cardId;

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            if (!snapshot.exists()) {
                try {
                    throw new Exception("Document người dùng không tồn tại.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            List<Map<String, Object>> rawCards = (List<Map<String, Object>>) snapshot.get(CARD_LIST_FIELD);
            if (rawCards == null) {
                try {
                    throw new Exception("Danh sách thẻ rỗng. Không tìm thấy ID để cập nhật.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            List<PaymentCard> currentCards = new ArrayList<>();
            for (Map<String, Object> map : rawCards) {
                // Ánh xạ qua Gson để đảm bảo độ chính xác
                currentCards.add(gson.fromJson(gson.toJson(map), PaymentCard.class));
            }

            // Tìm và thay thế thẻ
            boolean found = false;
            for (int i = 0; i < currentCards.size(); i++) {
                if (targetId.equals(currentCards.get(i).cardId)) {
                    currentCards.set(i, updatedCard); // Thay thế đối tượng cũ
                    found = true;
                    break;
                }
            }

            if (!found) {
                try {
                    throw new Exception("Không tìm thấy thẻ với ID: " + targetId + " để cập nhật.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // Ghi đè toàn bộ Array đã cập nhật
            transaction.update(userDocRef, CARD_LIST_FIELD, currentCards);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Cập nhật thẻ thành công cho UID: " + uid + ", ID: " + targetId);
            onComplete.accept(true, uid);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi cập nhật thẻ: " + e.getMessage());
            onComplete.accept(false, "Cập nhật thất bại: " + e.getMessage());
        });
    }

    public void deleteCard(@NonNull String cardId, BiConsumer<Boolean, String> onComplete) { // Bỏ tham số uid
        String uid = currentUserId; // Lấy UID từ biến instance

        if (uid == null || uid.isEmpty() || cardId.isEmpty()) {
            onComplete.accept(false, "Người dùng chưa đăng nhập hoặc Card ID rỗng");
            return;
        }

        DocumentReference userDocRef = db.collection(ROOT_COLLECTION).document(uid);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // ... (Logic Transaction giữ nguyên) ...
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            List<PaymentCard> currentCards = new ArrayList<>();

            // 1. Đọc và ánh xạ dữ liệu hiện tại
            if (snapshot.exists()) {
                List<Map<String, Object>> rawCards = (List<Map<String, Object>>) snapshot.get(CARD_LIST_FIELD);
                if (rawCards != null) {
                    for (Map<String, Object> map : rawCards) {
                        currentCards.add(gson.fromJson(gson.toJson(map), PaymentCard.class));
                    }
                }
            }

            // 2. Tìm và xóa thẻ theo ID
            boolean wasRemoved = currentCards.removeIf(card -> card.cardId.equals(cardId));

            if (!wasRemoved) {
                try {
                    throw new Exception("Card ID không được tìm thấy trong danh sách.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // 3. Ghi đè toàn bộ danh sách đã cập nhật (đã xóa)
            transaction.update(userDocRef, CARD_LIST_FIELD, currentCards);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Xóa thẻ thành công cho UID: " + uid + ", ID: " + cardId);
            onComplete.accept(true, "Xóa thẻ thành công.");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi transaction khi xóa thẻ: " + e.getMessage());
            onComplete.accept(false, "Lỗi xóa thẻ: " + e.getMessage());
        });
    }


    public void getCardDetails(String cardId, BiConsumer<PaymentCard, Exception> onResult) {
        DocumentReference userDocRef = getUserDocumentRef();
        if (userDocRef == null) {
            onResult.accept(null, new IllegalStateException("Người dùng chưa đăng nhập."));
            return;
        }

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot snapshot = task.getResult();
                // Lấy danh sách thô (List<Map<String, Object>>)
                List<Map<String, Object>> rawCards = (List<Map<String, Object>>) snapshot.get(CARD_LIST_FIELD);

                if (rawCards != null) {
                    for (Map<String, Object> cardMap : rawCards) {
                        try {
                            PaymentCard card = mapToPaymentCard(cardMap);
                            // Tìm thẻ có ID trùng khớp
                            if (card.cardId != null && card.cardId.equals(cardId)) {
                                onResult.accept(card, null); // Thành công
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi ánh xạ thẻ trong getCardDetails: " + e.getMessage());
                        }
                    }
                }

                // Không tìm thấy thẻ trong danh sách
                onResult.accept(null, new Exception("Không tìm thấy thẻ với ID: " + cardId));
            } else {
                // Lỗi đọc Firestore
                onResult.accept(null, new Exception("Lỗi đọc dữ liệu Firestore: " + task.getException().getMessage()));
            }
        });
    }
}