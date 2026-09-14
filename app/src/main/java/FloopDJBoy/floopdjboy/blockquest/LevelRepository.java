package FloopDJBoy.floopdjboy.blockquest;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

import FloopDJBoy.floopdjboy.blockquest.BlockSerialization.Serializer;

public class LevelRepository {
    private static volatile LevelRepository instance;

    public static LevelRepository getInstance() {
        if (instance == null) {
            synchronized (LevelRepository.class) {
                if (instance == null) {
                    instance = new LevelRepository();
                }
            }
        }
        return instance;
    }
    public enum Reaction {
        LIKE,
        DISLIKE;
        public static Reaction fromString(String str) {
            if (str == null) return null;
            return switch (str.toLowerCase()) {
                case "like" -> LIKE;
                case "dislike" -> DISLIKE;
                default -> null;
            };
        }
    }
    private LevelRepository() {}

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<DocumentSnapshot> getLevel(String levelId) {
        return db.collection("levels")
                .document(levelId)
                .get();
    }

    public Task<Void> publishLevel(String levelId, String json, int bestMoves) {
        return db.collection("levels")
                .document(levelId)
                .update(
                        "levelJson", json,
                        "isPublished", true,
                        "bestMoves", bestMoves
                );
    }

    public Task<Void> sendReaction(String levelId, String uid, Reaction reaction) {
        DocumentReference ref = db.collection("levels")
                .document(levelId)
                .collection("reactions")
                .document(uid);

        if (reaction == null) {
            return ref.delete();
        }

        return ref.set(Map.of(
                "reaction", reaction.toString().toLowerCase(),
                "updatedAt", FieldValue.serverTimestamp()
        ));
    }
    public void saveLevel(Context context, String levelId, World world) {
        String json = Serializer.Serialize(world);

        db.collection("levels")
                .document(levelId)
                .update("levelJson", json)
                .addOnSuccessListener(unused ->
                        Toast.makeText(context, "Level saved successfully", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to save level", Toast.LENGTH_SHORT).show()
                );
    }
    public Task<DocumentSnapshot> getReaction(String levelId, String uid) {
        return db.collection("levels")
                .document(levelId)
                .collection("reactions")
                .document(uid)
                .get();
    }
}
