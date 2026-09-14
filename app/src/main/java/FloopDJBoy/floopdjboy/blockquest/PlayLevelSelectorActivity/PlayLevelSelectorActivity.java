package FloopDJBoy.floopdjboy.blockquest.PlayLevelSelectorActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import FloopDJBoy.floopdjboy.blockquest.MainActivity;
import FloopDJBoy.floopdjboy.blockquest.MainMenuActivity.MainMenuActivity;
import FloopDJBoy.floopdjboy.blockquest.R;
import FloopDJBoy.floopdjboy.blockquest.SignInPage.SignInActivity;

public class PlayLevelSelectorActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ImageButton logoutBtn, backBtn;
    TextView usernameText;
    RecyclerView levelsRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play_level_selector);
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
            gotToActivity(SignInActivity.class);
        }
        db = FirebaseFirestore.getInstance();
        initViews();
    }
    private void gotToActivity(Class<?> cls){
        Intent intent = new Intent(this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        logoutBtn = findViewById(R.id.signOutBtn);
        backBtn = findViewById(R.id.backBtn);
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
        backBtn.setOnClickListener(v -> {
            gotToActivity(MainMenuActivity.class);
        });
        levelsRecycler = findViewById(R.id.levelsRecycler);
        levelsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        levelsRecycler.setAdapter(new LevelStoreAdaptor(db,this,(item,position) -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("levelId",item.id);
            intent.putExtra("levelName",item.name);
            intent.putExtra("levelJson",item.levelJson);
            intent.putExtra("isPublished",true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }));

    }
}