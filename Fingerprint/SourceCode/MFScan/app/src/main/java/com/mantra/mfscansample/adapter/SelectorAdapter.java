package com.mantra.mfscansample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mantra.mfscansample.R;

import java.util.ArrayList;


/**
 * Created by SW11 on 9/23/2015.
 */
public class SelectorAdapter extends BaseAdapter {

    private final ArrayList<String> deviceList;

    public SelectorAdapter(Context context, ArrayList<String> deviceList) {
        this.deviceList = deviceList;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_adapter_view, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvDevice.setText(deviceList.get(position));
        return convertView;
    }

    static class ViewHolder {
        TextView tvDevice;
        ViewHolder(View view) {
            tvDevice = view.findViewById(R.id.tvDevice);
        }
    }
}
