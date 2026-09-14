package FloopDJBoy.floopdjboy.blockquest.LevelStoreActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import FloopDJBoy.floopdjboy.blockquest.MainActivity;
import FloopDJBoy.floopdjboy.blockquest.MainMenuActivity.MainMenuActivity;
import FloopDJBoy.floopdjboy.blockquest.R;
import FloopDJBoy.floopdjboy.blockquest.SignInPage.SignInActivity;

public class LevelStoreActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ImageButton logoutBtn, backBtn;
    FloatingActionButton addNewLevelBtn;
    TextView usernameText;
    RecyclerView levelsRecycler;
     LevelSaveAdaptor adaptor;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_level_store);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        findViewById(R.id.main).setOnClickListener(v-> {
            View focus = v.findFocus();
            if (focus != null) {
                focus.clearFocus();
            }
        });
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        db = FirebaseFirestore.getInstance();
        initViews();
        Context ctx = this;
        adaptor = new LevelSaveAdaptor(db, mAuth, this, new LevelSaveAdaptor.LevelActionListener(
        ) {
            @Override
            public void onDelete(LevelSaveAdaptor.LevelPreview item, int position) {
                new MaterialAlertDialogBuilder(ctx)
                        .setTitle("Delete level")
                        .setMessage("Are you sure you want to delete this level?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // perform delete here
                            deleteLevel(item);
                        })
                        .show();

            }

            @Override
            public void onChooseLevel(LevelSaveAdaptor.LevelPreview item, int position) {
                Intent intent = new Intent(ctx, MainActivity.class);
                intent.putExtra("levelId", item.id);
                intent.putExtra("levelName", item.name);
                intent.putExtra("levelJson", item.levelJson);
                startActivity(intent);
                finish();

            }
        });
        levelsRecycler.setAdapter(adaptor);
        levelsRecycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        levelsRecycler.setOnTouchListener((v, event) -> {
            View currentFocus = levelsRecycler.getFocusedChild();
            if (currentFocus != null) {
                currentFocus.clearFocus();
            }
            return false;
        });
    }


    private void deleteLevel(LevelSaveAdaptor.LevelPreview item) {
        db.collection("levels").document(item.id)
                .delete()
                .addOnSuccessListener(unused -> {

                    int index = adaptor.data.indexOf(item);
                    if (index != -1) {
                        adaptor.data.remove(index);
                        adaptor.notifyItemRemoved(index);
                    }
                });
    }

    private void initViews() {
        logoutBtn = findViewById(R.id.signOutBtn);
        backBtn = findViewById(R.id.backBtn);
        addNewLevelBtn = findViewById(R.id.addLevelFab);
        levelsRecycler = findViewById(R.id.levelsRecycler);
        usernameText = findViewById(R.id.usernameText);

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(documentSnapshot -> {
            usernameText.setText(documentSnapshot.getString("username"));
        }).addOnFailureListener(e -> {
            usernameText.setText("");
        });
        addNewLevelBtn.setOnClickListener(v -> {
            makeNewLevel();
        });
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
    private String loadDefaultLevel() {
        StringBuilder sb = new StringBuilder();

        try (InputStream is = getAssets().open("defaultLevel.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
    private void makeNewLevel() {

        Context ctx = this;

        EditText input = new EditText(ctx);
        input.setHint("Level name");

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(ctx)
                .setTitle("Create new level")
                .setView(input)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button createBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            createBtn.setOnClickListener(v -> {
                String name = input.getText().toString().trim();

                if (name.isEmpty()) {
                    input.setError("Enter a name");
                    return;
                }

                String uid = mAuth.getCurrentUser().getUid();

                Map<String, Object> levelData = new HashMap<>();
                levelData.put("name", name);
                levelData.put("createdAt", FieldValue.serverTimestamp());
                levelData.put("isPublished", false);
                levelData.put("authorId", uid);
                levelData.put("authorName", usernameText.getText().toString());
                levelData.put("levelJson", loadDefaultLevel());
                levelData.put("dislikeCount", 0);
                levelData.put("likeCount", 0);
                levelData.put("plays", 0);
                levelData.put("bestMoves", -1);

                db.collection("levels")
                        .add(levelData)
                        .addOnSuccessListener(docRef -> {
                            String levelId = docRef.getId();

                            Intent intent = new Intent(ctx, MainActivity.class);
                            intent.putExtra("levelId", levelId);
                            intent.putExtra("levelName", name);
                            intent.putExtra("levelJson", loadDefaultLevel());
                            startActivity(intent);
                        })
                        .addOnFailureListener(e -> { e.printStackTrace(); });

                dialog.dismiss();
            });
        });

        dialog.show();
    }
}