package com.bhuvan.shadowbrowser;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

/** Shows the list of open tabs in the tab switcher dialog. */
public class TabsAdapter extends BaseAdapter {

    public interface Actions {
        void onSelect(int index);

        void onClose(int index);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final TabManager manager;
    private final Actions actions;

    public TabsAdapter(Context context, TabManager manager, Actions actions) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.manager = manager;
        this.actions = actions;
    }

    @Override
    public int getCount() {
        return manager.size();
    }

    @Override
    public Object getItem(int position) {
        return manager.get(position);
    }

    @Override
    public long getItemId(int position) {
        return manager.get(position).id;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView != null ? convertView : inflater.inflate(R.layout.item_tab, parent, false);
        Tab tab = manager.get(position);

        TextView title = row.findViewById(R.id.tabTitle);
        TextView url = row.findViewById(R.id.tabUrl);
        ImageButton close = row.findViewById(R.id.tabClose);

        title.setText(tab.title == null || tab.title.isEmpty()
                ? context.getString(R.string.new_tab) : tab.title);
        url.setText(tab.url);

        if (tab == manager.current()) {
            row.setBackgroundResource(R.color.tab_current);
        } else {
            row.setBackgroundColor(Color.TRANSPARENT);
        }

        row.setOnClickListener(v -> actions.onSelect(position));
        close.setOnClickListener(v -> actions.onClose(position));
        return row;
    }
          }
