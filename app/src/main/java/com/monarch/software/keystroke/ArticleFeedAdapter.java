package com.monarch.software.keystroke;

import com.monarch.software.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Simple RecyclerView adapter that displays a list of news-style articles. */
public class ArticleFeedAdapter extends RecyclerView.Adapter<ArticleFeedAdapter.ViewHolder> {

    private final String[] titles;
    private final String[] bodies;

    ArticleFeedAdapter(String[] titles, String[] bodies) {
        this.titles = titles;
        this.bodies = bodies;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ks_item_feed_article, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvTitle.setText(titles[position]);
        holder.tvBody.setText(bodies[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody;
        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_article_title);
            tvBody  = v.findViewById(R.id.tv_article_body);
        }
    }
}
