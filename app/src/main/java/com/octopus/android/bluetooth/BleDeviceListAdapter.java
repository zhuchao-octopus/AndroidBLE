package com.octopus.android.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class BleDeviceListAdapter extends BaseAdapter {
    private final Context context;
    private final List<ScanResult> list;

    public BleDeviceListAdapter(Context context) {
        this.context = context;
        list = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DeviceViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_lv_devices_item, null);
            viewHolder = new DeviceViewHolder();
            viewHolder.tvDeviceName = view.findViewById(R.id.tv_device_name);
            viewHolder.tvDeviceAddress = view.findViewById(R.id.tv_device_address);
            viewHolder.tvDeviceRSSI = view.findViewById(R.id.tv_device_rssi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (DeviceViewHolder) view.getTag();
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return TODO;
        }
        if (list.get(i).getDevice().getName() == null) {
            viewHolder.tvDeviceName.setText("NULL");
        } else {
            viewHolder.tvDeviceName.setText(list.get(i).getDevice().getName());
        }

        viewHolder.tvDeviceAddress.setText(list.get(i).getDevice().getAddress());
        viewHolder.tvDeviceRSSI.setText("RSSI：" + list.get(i).getRssi());

        return view;
    }

    public void addDevice(ScanResult bleDevice) {
        if (!list.contains(bleDevice)) {
            list.add(bleDevice);
        }
        notifyDataSetChanged();   //刷新
    }

    public void clear() {
        list.clear();
        notifyDataSetChanged(); //刷新
    }

    static class DeviceViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        TextView tvDeviceRSSI;
    }

}
