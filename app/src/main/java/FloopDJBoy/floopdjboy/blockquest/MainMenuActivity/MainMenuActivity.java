package FloopDJBoy.floopdjboy.blockquest.MainMenuActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import FloopDJBoy.floopdjboy.blockquest.LevelStoreActivity.LevelStoreActivity;
import FloopDJBoy.floopdjboy.blockquest.PlayLevelSelectorActivity.PlayLevelSelectorActivity;
import FloopDJBoy.floopdjboy.blockquest.R;
import FloopDJBoy.floopdjboy.blockquest.RegisterAllBlock;
import FloopDJBoy.floopdjboy.blockquest.SignInPage.SignInActivity;

public class MainMenuActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    Button playBtn, createBtn;
    ImageButton logoutBtn, backBtn;
    TextView usernameText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
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

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        db = FirebaseFirestore.getInstance();
        RegisterAllBlock.loadBlocks(this);
        initViews();
    }
    private void gotToActivity(Class<? extends Activity> cls){
        Intent intent = new Intent(this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        logoutBtn = findViewById(R.id.signOutBtn);
        backBtn = findViewById(R.id.backBtn);
        playBtn = findViewById(R.id.playBtn);
        createBtn = findViewById(R.id.createBtn);
        usernameText = findViewById(R.id.usernameText);

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(documentSnapshot -> {
            usernameText.setText(documentSnapshot.getString("username"));
        }).addOnFailureListener(e -> {
            usernameText.setText("");
        });
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            gotToActivity(SignInActivity.class);
        });
        createBtn.setOnClickListener(v -> {
            gotToActivity(LevelStoreActivity.class);
        });
        playBtn.setOnClickListener(v -> {
            gotToActivity(PlayLevelSelectorActivity.class);
        });




    }
}