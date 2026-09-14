package FloopDJBoy.floopdjboy.blockquest.PlayLevelSelectorActivity;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import FloopDJBoy.floopdjboy.blockquest.BlockSerialization.LevelPreviewRenderer;
import FloopDJBoy.floopdjboy.blockquest.R;

public class LevelStoreAdaptor extends RecyclerView.Adapter<LevelStoreAdaptor.ViewHolder> {

    private static final int PAGE_SIZE = 10;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private final FirebaseFirestore db;
    private final Context ctx;
    private final LevelActionListener listener;

    /** Items currently displayed on this page */
    private final ArrayList<PlayLevelItem> data = new ArrayList<>();

    /** Cursor snapshots for pagination */
    private DocumentSnapshot firstOnPage = null;
    private DocumentSnapshot lastOnPage  = null;

    /** Stack of "first doc of each previous page" so we can go backwards */
    private final ArrayList<DocumentSnapshot> pageHistory = new ArrayList<>();

    /** Whether a load is in progress (prevents double-taps) */
    private boolean isLoading = false;

    /** Callback so the host can update prev/next button states */
    public interface PaginationStateListener {
        void onStateChanged(boolean canGoPrev, boolean canGoNext, boolean isLoading);
    }
    private PaginationStateListener paginationStateListener;

    public void setPaginationStateListener(PaginationStateListener l) {
        this.paginationStateListener = l;
    }

    // -----------------------------------------------------------------------

    public interface LevelActionListener {
        void onPlayLevel(PlayLevelItem item, int position);
    }

    // -----------------------------------------------------------------------

    public LevelStoreAdaptor(FirebaseFirestore db, Context ctx, LevelActionListener listener) {
        this.db       = db;
        this.ctx      = ctx;
        this.listener = listener;
        loadPage(null, false);
    }

    // -----------------------------------------------------------------------
    // Pagination
    // -----------------------------------------------------------------------

    /**
     * Load a page of published levels.
     *
     * @param startAfter  cursor to start after (null = first page)
     * @param isGoingBack true when navigating backwards (so we don't push to history)
     */
    private void loadPage(DocumentSnapshot startAfter, boolean isGoingBack) {
        if (isLoading) return;
        isLoading = true;
        notifyPaginationState();

        Query query = db.collection("levels")
                .whereEqualTo("isPublished", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE + 1); // fetch one extra to know if a next page exists

        if (startAfter != null) {
            query = query.startAfter(startAfter);
        }

        query.get().addOnSuccessListener(snapshots -> {

            boolean hasNextPage = snapshots.size() > PAGE_SIZE;
            int visibleCount    = Math.min(snapshots.size(), PAGE_SIZE);

            data.clear();
            firstOnPage = null;
            lastOnPage  = null;

            if (visibleCount > 0) {
                firstOnPage = snapshots.getDocuments().get(0);
                lastOnPage  = snapshots.getDocuments().get(visibleCount - 1);
            }

            for (int i = 0; i < visibleCount; i++) {
                QueryDocumentSnapshot doc =
                        (QueryDocumentSnapshot) snapshots.getDocuments().get(i);

                PlayLevelItem item = new PlayLevelItem(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("authorName"),
                        doc.getString("authorId"),
                        doc.getString("levelJson"),
                        doc.getLong("likeCount")    != null ? doc.getLong("likeCount")    : 0L,
                        doc.getLong("dislikeCount") != null ? doc.getLong("dislikeCount") : 0L,
                        doc.getLong("plays")    != null ? doc.getLong("plays")    : 0L,
                        doc.getTimestamp("createdAt") !=null ? doc.getTimestamp("createdAt").toDate().getTime() : 0L
                );
                data.add(item);
            }

            // Mark whether there is a next page
            for (PlayLevelItem item : data) {
                item.hasNextPage = hasNextPage;
            }

            notifyDataSetChanged();

            // Kick off async work (thumbnails + author names) for each item
            for (int i = 0; i < data.size(); i++) {
                loadThumbnail(i);
            }

            isLoading = false;
            notifyPaginationState();

        }).addOnFailureListener(e -> {
            isLoading = false;
            notifyPaginationState();
        });
    }

    /** Navigate to the next page. */
    public void loadNextPage() {
        if (isLoading || lastOnPage == null) return;
        // Push the current first doc so we can come back
        if (firstOnPage != null) {
            pageHistory.add(firstOnPage);
        }
        loadPage(lastOnPage, false);
    }

    /** Navigate to the previous page. */
    public void loadPrevPage() {
        if (isLoading || pageHistory.isEmpty()) return;
        // Pop the cursor for the page before this one
        DocumentSnapshot prevFirst = pageHistory.remove(pageHistory.size() - 1);
        // We want to start BEFORE prevFirst, which means startAfter the doc before it.
        // Simplest correct approach: if we're on page 1 history is empty → first page (null cursor).
        // Otherwise we start after the doc that preceded prevFirst — but we don't have it stored.
        // The standard pattern: go back by using the cursor that was used to reach prevFirst,
        // which is pageHistory.isEmpty() ? null : pageHistory.last().
        DocumentSnapshot cursorForPrevPage = pageHistory.isEmpty()
                ? null
                : pageHistory.get(pageHistory.size() - 1);
        loadPage(cursorForPrevPage, true);
    }

    public boolean canGoPrev() {
        return !pageHistory.isEmpty();
    }

    public boolean canGoNext() {
        return !data.isEmpty() && !data.isEmpty() && data.get(0).hasNextPage;
    }

    private void notifyPaginationState() {
        if (paginationStateListener != null) {
            paginationStateListener.onStateChanged(canGoPrev(), canGoNext(), isLoading);
        }
    }

    // -----------------------------------------------------------------------
    // Async loaders
    // -----------------------------------------------------------------------

    private void loadThumbnail(int position) {
        if (position >= data.size()) return;
        PlayLevelItem item = data.get(position);

        int sizePx = dpToPx(160);
        LevelPreviewRenderer.render(
                ctx.getResources(),
                item.levelJson,
                sizePx,
                bitmap -> {
                    item.previewBitmap = bitmap;
                    item.isThumbnailLoading = false;
                    notifyItemChanged(position);
                }
        );
    }



    // -----------------------------------------------------------------------
    // Adapter
    // -----------------------------------------------------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.level_play_selector_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayLevelItem item = data.get(position);

        // --- Level name ---
        holder.levelName.setText(item.name != null ? item.name : "");

        // --- Author ---
        holder.creatorName.setText("by " + (item.authorName != null ? item.authorName : ""));

        // --- Date ---
        if (item.createdAt > 0) {
            holder.date.setText(DATE_FORMAT.format(new Date(item.createdAt)));
        } else {
            holder.date.setText("");
        }

        // --- Plays ---
        holder.plays.setText("▶ " + formatCount(item.plays));

        // --- Likes / Dislikes ---
        holder.likes.setText(formatCount(item.likes));
        holder.dislikes.setText(formatCount(item.dislikes));

        // --- Thumbnail ---
        if (item.isThumbnailLoading) {
            holder.levelPreview.setImageBitmap(null);
            holder.levelPreview.setBackgroundColor(Color.parseColor("#3C3C3C"));
        } else {
            holder.levelPreview.setBackgroundColor(Color.TRANSPARENT);
            if (item.previewBitmap != null) {
                holder.levelPreview.setImageBitmap(item.previewBitmap);
            }
        }

        // --- Click ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayLevel(item, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                ctx.getResources().getDisplayMetrics());
    }

    private static String formatCount(long n) {
        if (n >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format(Locale.getDefault(), "%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    // -----------------------------------------------------------------------
    // ViewHolder
    // -----------------------------------------------------------------------

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView levelPreview;
        final TextView  levelName;
        final TextView  creatorName;
        final TextView  date;
        final TextView  plays;
        final TextView  likes;
        final TextView  dislikes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            levelPreview = itemView.findViewById(R.id.levelPreview);
            levelName    = itemView.findViewById(R.id.levelName);
            creatorName  = itemView.findViewById(R.id.creatorName);
            date         = itemView.findViewById(R.id.date);
            plays        = itemView.findViewById(R.id.plays);
            likes        = itemView.findViewById(R.id.likes);
            dislikes     = itemView.findViewById(R.id.dislikes);
        }
    }

    // -----------------------------------------------------------------------
    // Data class
    // -----------------------------------------------------------------------

    public static class PlayLevelItem {
        public final String id;
        public final String name;
        public final String authorId;
        public final String levelJson;
        public final long   likes;
        public final long   dislikes;
        public final long   plays;
        public final long   createdAt;
        public String  authorName;

        // Async-loaded fields
        public android.graphics.Bitmap previewBitmap = null;
        public boolean isThumbnailLoading = true;

        // Set after page load completes
        public boolean hasNextPage = false;

        public PlayLevelItem(String id, String name,String authorName ,String authorId, String levelJson,
                             long likes, long dislikes, long plays, long createdAt) {
            this.id        = id;
            this.authorName = authorName;
            this.name      = name;
            this.authorId  = authorId;
            this.levelJson = levelJson;
            this.likes     = likes;
            this.dislikes  = dislikes;
            this.plays     = plays;
            this.createdAt = createdAt;
        }
    }
}