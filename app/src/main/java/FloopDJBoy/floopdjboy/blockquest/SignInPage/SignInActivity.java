package FloopDJBoy.floopdjboy.blockquest.SignInPage;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import FloopDJBoy.floopdjboy.blockquest.MainMenuActivity.MainMenuActivity;
import FloopDJBoy.floopdjboy.blockquest.R;

public class SignInActivity extends AppCompatActivity {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------


    private static final String WEB_CLIENT_ID = "399824134038-6go4rgqbuf0pp48hkbndjkm63o1vetdv.apps.googleusercontent.com";

    private static final String USERS_COLLECTION = "users";
    private static final int MIN_PASSWORD_LENGTH = 6;

    // -----------------------------------------------------------------------
    // UI references
    // -----------------------------------------------------------------------

    private TextInputLayout  usernameLayout;
    private TextInputLayout  emailLayout;
    private TextInputLayout  passwordLayout;
    private TextInputLayout  confirmPasswordLayout;

    private TextInputEditText usernameField;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private TextInputEditText confirmPasswordField;

    private Button   actionButton;
    private Button   googleSignInButton;
    private TextView switchMode;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** true = Sign Up mode, false = Sign In mode */
    private boolean isSignUpMode = false;

    // -----------------------------------------------------------------------
    // Firebase / Credential Manager
    // -----------------------------------------------------------------------

    private FirebaseAuth      firebaseAuth;
    private FirebaseFirestore firestore;
    private CredentialManager credentialManager;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        firebaseAuth      = FirebaseAuth.getInstance();
        firestore         = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        bindViews();
        setInitialMode();
        setListeners();
        if(firebaseAuth.getCurrentUser()!=null && !firebaseAuth.getCurrentUser().isAnonymous()){
            goToMain();
        }
    }

    // -----------------------------------------------------------------------
    // View binding
    // -----------------------------------------------------------------------

    private void bindViews() {
        usernameLayout        = findViewById(R.id.usernameLayout);
        emailLayout           = findViewById(R.id.emailLayout);
        passwordLayout        = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        usernameField        = findViewById(R.id.username);
        emailField           = findViewById(R.id.email);
        passwordField        = findViewById(R.id.password);
        confirmPasswordField = findViewById(R.id.confirmPassword);

        actionButton       = findViewById(R.id.actionButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        switchMode         = findViewById(R.id.switchMode);
    }

    // -----------------------------------------------------------------------
    // Mode management
    // -----------------------------------------------------------------------

    private void setInitialMode() {
        applyMode();
    }

    private void applyMode() {
        if (isSignUpMode) {
            usernameLayout.setVisibility(View.VISIBLE);
            confirmPasswordLayout.setVisibility(View.VISIBLE);
            actionButton.setText("Sign Up");
            switchMode.setText("Already have an account? Sign in");
        } else {
            usernameLayout.setVisibility(View.GONE);
            confirmPasswordLayout.setVisibility(View.GONE);
            actionButton.setText("Sign In");
            switchMode.setText("Don't have an account? Sign up");
        }
        clearAllErrors();
    }

    private void toggleMode() {
        isSignUpMode = !isSignUpMode;
        applyMode();
    }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------

    private void setListeners() {
        switchMode.setOnClickListener(v -> toggleMode());
        actionButton.setOnClickListener(v -> onActionButtonClicked());
        googleSignInButton.setOnClickListener(v -> onGoogleSignInClicked());
    }

    // -----------------------------------------------------------------------
    // Email/Password action
    // -----------------------------------------------------------------------

    private void onActionButtonClicked() {
        if (isSignUpMode) {
            attemptSignUp();
        } else {
            attemptSignIn();
        }
    }

    private void attemptSignIn() {
        clearAllErrors();

        String email    = getText(emailField);
        String password = getText(passwordField);

        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Required");
            valid = false;
        }

        if (!valid) return;

        setUiEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setUiEnabled(true);
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    setUiEnabled(true);
                    emailLayout.setError("Invalid email or password");
                    passwordLayout.setError("Invalid email or password");
                });
    }

    private void attemptSignUp() {
        clearAllErrors();

        String username         = getText(usernameField);
        String email            = getText(emailField);
        String password         = getText(passwordField);
        String confirmPassword  = getText(confirmPasswordField);

        boolean valid = true;

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Required");
            valid = false;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Required");
            valid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            passwordLayout.setError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            valid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Required");
            valid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError("Passwords do not match");
            valid = false;
        }

        if (!valid) return;

        setUiEnabled(false);

        // Step 1: check username uniqueness before creating any account
        checkUsernameUnique(username, isUnique -> {
            if (!isUnique) {
                setUiEnabled(true);
                usernameLayout.setError("Username already taken");
                return;
            }

            // Step 2: create Firebase Auth account
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user == null) {
                            // Should never happen, but guard anyway
                            setUiEnabled(true);
                            emailLayout.setError("Unexpected error. Please try again");
                            return;
                        }

                        // Step 3: write Firestore doc
                        writeUserDoc(user.getUid(), email, username,
                                /*onSuccess=*/ () -> {
                                    setUiEnabled(true);
                                    goToMain();
                                },
                                /*onFailure=*/ e -> {
                                    // Roll back: delete the auth account
                                    user.delete().addOnCompleteListener(task -> {
                                        setUiEnabled(true);
                                        emailLayout.setError("Failed to create account. Please try again");
                                    });
                                });
                    })
                    .addOnFailureListener(e -> {
                        setUiEnabled(true);
                        // Firebase surfaces duplicate-email errors here
                        emailLayout.setError(friendlyAuthError(e.getMessage()));
                    });
        });
    }

    // -----------------------------------------------------------------------
    // Google Sign-In (Credential Manager)
    // -----------------------------------------------------------------------

    private void onGoogleSignInClicked() {
        setUiEnabled(false);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // show all accounts, not just previously used
                .setServerClientId(WEB_CLIENT_ID)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse response) {
                        runOnUiThread(() -> handleGoogleCredential(response.getCredential()));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() -> {
                            setUiEnabled(true);
                            emailLayout.setError("Google sign-in failed. Please try again");
                        });
                    }
                }
        );
    }

    private void handleGoogleCredential(Credential credential) {
        if (!(credential instanceof CustomCredential)) {
            setUiEnabled(true);
            emailLayout.setError("Unexpected credential type");
            return;
        }

        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            setUiEnabled(true);
            emailLayout.setError("Unexpected credential type");
            return;
        }

        GoogleIdTokenCredential googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());

        String idToken = googleIdTokenCredential.getIdToken();
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        setUiEnabled(true);
                        emailLayout.setError("Unexpected error. Please try again");
                        return;
                    }

                    // Check whether a Firestore doc already exists for this user
                    firestore.collection(USERS_COLLECTION)
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    // Returning user — go straight to main
                                    setUiEnabled(true);
                                    goToMain();
                                } else {
                                    // New Google user — need a username
                                    setUiEnabled(true);
                                    showUsernameDialog(user);
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Could not verify — roll back to be safe
                                user.delete().addOnCompleteListener(task -> {
                                    setUiEnabled(true);
                                    emailLayout.setError("Failed to verify account. Please try again");
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    setUiEnabled(true);
                    emailLayout.setError("Google sign-in failed. Please try again");
                });
    }

    // -----------------------------------------------------------------------
    // Username dialog (new Google users)
    // -----------------------------------------------------------------------

    /**
     * Shows a dialog asking the user to pick a username.
     * If the user cancels, the Firebase Auth account is deleted to avoid
     * a half-created state.
     */
    private void showUsernameDialog(@NonNull FirebaseUser user) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_username, null);

        TextInputLayout    dialogUsernameLayout = dialogView.findViewById(R.id.dialogUsernameLayout);
        TextInputEditText  dialogUsernameField  = dialogView.findViewById(R.id.dialogUsernameField);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Choose a username")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Confirm", null) // set below to prevent auto-dismiss
                .setNegativeButton("Cancel", null)  // set below to prevent auto-dismiss
                .create();

        dialog.setOnShowListener(d -> {
            Button confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancel  = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            confirm.setOnClickListener(v -> {
                String username = getText(dialogUsernameField);

                if (TextUtils.isEmpty(username)) {
                    dialogUsernameLayout.setError("Required");
                    return;
                }

                dialogUsernameLayout.setError(null);
                confirm.setEnabled(false);
                cancel.setEnabled(false);

                checkUsernameUnique(username, isUnique -> {
                    if (!isUnique) {
                        confirm.setEnabled(true);
                        cancel.setEnabled(true);
                        dialogUsernameLayout.setError("Username already taken");
                        return;
                    }

                    String email = user.getEmail() != null ? user.getEmail() : "";

                    writeUserDoc(user.getUid(), email, username,
                            /*onSuccess=*/ () -> {
                                dialog.dismiss();
                                goToMain();
                            },
                            /*onFailure=*/ e -> {
                                // Roll back: delete the auth account
                                user.delete().addOnCompleteListener(task -> {
                                    dialog.dismiss();
                                    emailLayout.setError("Failed to create account. Please try again");
                                });
                            });
                });
            });

            cancel.setOnClickListener(v -> {
                confirm.setEnabled(false);
                cancel.setEnabled(false);
                // Roll back: delete the auth account so no half-created state remains
                user.delete().addOnCompleteListener(task -> {
                    dialog.dismiss();
                });
            });
        });

        dialog.show();
    }

    // -----------------------------------------------------------------------
    // Firestore helpers
    // -----------------------------------------------------------------------

    /**
     * Checks whether the given username is not yet taken in Firestore.
     * Calls back on the main thread.
     */
    private void checkUsernameUnique(String username, UsernameUniqueCallback callback) {
        firestore.collection("usernames")
                .document(username)
                .get()
                .addOnSuccessListener(doc -> {
                    // If document exists → username taken
                    callback.onResult(!doc.exists());
                })
                .addOnFailureListener(e -> {
                    // Treat failure as not unique (safe fallback)
                    callback.onResult(false);
                });
    }

    /**
     * Writes a new user document to Firestore using authId as the document ID.
     */
    private void writeUserDoc(String authId,
                              String email,
                              String username,
                              Runnable onSuccess,
                              FailureCallback onFailure) {

        Map<String, Object> userData = new HashMap<>();
        userData.put("authId", authId);
        userData.put("email", email);
        userData.put("username", username);

        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", authId);

        WriteBatch batch = firestore.batch();

        // 1. main user doc
        DocumentReference userRef =
                firestore.collection(USERS_COLLECTION).document(authId);

        // 2. username reservation doc
        DocumentReference usernameRef =
                firestore.collection("usernames").document(username);

        batch.set(userRef, userData);
        batch.set(usernameRef, usernameData);

        batch.commit()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(onFailure::onFailure);
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    private void goToMain() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    private void setUiEnabled(boolean enabled) {
        actionButton.setEnabled(enabled);
        googleSignInButton.setEnabled(enabled);
        switchMode.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmPasswordField.setEnabled(enabled);
    }

    private void clearAllErrors() {
        usernameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    /**
     * Converts a Firebase Auth error message into something slightly more user-friendly.
     * Firebase messages are already fairly readable, but we strip the prefix noise.
     */
    private String friendlyAuthError(String message) {
        if (message == null) return "An error occurred. Please try again";
        if (message.contains("email address is already in use")) return "An account with this email already exists";
        if (message.contains("badly formatted"))                 return "Invalid email address";
        return "An error occurred. Please try again";
    }

    // -----------------------------------------------------------------------
    // Callback interfaces
    // -----------------------------------------------------------------------

    private interface UsernameUniqueCallback {
        void onResult(boolean isUnique);
    }

    private interface FailureCallback {
        void onFailure(Exception e);
    }
}