package com.mantra.miscansample.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mantra.miscansample.R;

import java.util.ArrayList;

/**
 * Created by software 121216 on 10/24/2017.
 */

public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.MyViewHolder> {

    private ArrayList<NavigationMenuItem> menuItemArrayList;
    private AlertDialog.Builder alertDialog;
    private Context act;
    ActionClickListener itemClickListener;

    public ActionAdapter(Context mainActivity, ArrayList<NavigationMenuItem> navigationMenuItemArrayList, ActionClickListener itemClickListener) {
        this.menuItemArrayList = navigationMenuItemArrayList;
        this.act = mainActivity;
        this.itemClickListener = itemClickListener;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtMenu;
        RelativeLayout rel_item;
        public MyViewHolder(View view) {
            super(view);
            txtMenu = (TextView) view.findViewById(R.id.txtMenu);
            rel_item = (RelativeLayout) view.findViewById(R.id.rel_item);
        }
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final NavigationMenuItem data = menuItemArrayList.get(position);
        holder.txtMenu.setText("" + data.menu_name);
        holder.rel_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int clickedPosition = holder.getAdapterPosition();
                itemClickListener.onActionClick(clickedPosition, data.menu_name);
            }
        });
    }


    public interface ActionClickListener {
        void onActionClick(int position, String item_name);
    }

    @Override
    public int getItemCount() {
        return menuItemArrayList.size();
    }
}

