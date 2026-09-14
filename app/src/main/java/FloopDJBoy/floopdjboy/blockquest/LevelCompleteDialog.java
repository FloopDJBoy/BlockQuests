package FloopDJBoy.floopdjboy.blockquest;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LevelCompleteDialog {

    public interface Listener {
        void onLike();
        void onDislike();
        void onNext();
    }

    public static void show(
            Activity activity,
            View view,
            int yourMoves,
            long bestMoves,
            boolean newRecord,
            LevelRepository.Reaction reaction,
            Listener listener
    ) {

        TextView movesView = view.findViewById(R.id.movesText);
        TextView bestView = view.findViewById(R.id.bestText);
        TextView recordView = view.findViewById(R.id.recordText);

        Button likeBtn = view.findViewById(R.id.likeBtn);
        Button dislikeBtn = view.findViewById(R.id.dislikeBtn);
        Button nextBtn = view.findViewById(R.id.nextBtn);

        movesView.setText("Your moves: " + yourMoves);
        bestView.setText("Best: " + bestMoves);

        recordView.setVisibility(newRecord ? View.VISIBLE : View.GONE);
        if (newRecord) recordView.setText("NEW RECORD!");

        applyReactionUI(reaction, likeBtn, dislikeBtn);

        likeBtn.setOnClickListener(v -> {
            listener.onLike();
            applyReactionUI(LevelRepository.Reaction.LIKE, likeBtn, dislikeBtn);
        });
        dislikeBtn.setOnClickListener(v -> {
                    listener.onDislike();
                    applyReactionUI(LevelRepository.Reaction.DISLIKE, likeBtn, dislikeBtn);
        });
        nextBtn.setOnClickListener(v -> listener.onNext());

        new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .show();
    }

    private static void applyReactionUI(LevelRepository.Reaction reaction, Button like, Button dislike) {
        if (reaction == null) {
            like.setAlpha(1f);
            dislike.setAlpha(1f);
        }
        else if ("like".equalsIgnoreCase(reaction.toString())) {
            like.setAlpha(1f);
            dislike.setAlpha(0.3f);
        } else if ("dislike".equalsIgnoreCase(reaction.toString())) {
            like.setAlpha(0.3f);
            dislike.setAlpha(1f);
        }
    }
}
