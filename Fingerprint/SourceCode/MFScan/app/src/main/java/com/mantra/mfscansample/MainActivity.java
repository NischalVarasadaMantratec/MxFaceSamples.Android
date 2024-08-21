package com.mantra.mfscansample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.mantra.mfscan.DeviceInfo;
import com.mantra.mfscan.MFScan;
import com.mantra.mfscan.MFScanNative;
import com.mantra.mfscan.MFScan_Callback;
import com.mantra.mfscan.enums.DeviceDetection;
import com.mantra.mfscan.enums.DeviceModel;
import com.mantra.mfscan.enums.FingerPosition;
import com.mantra.mfscan.enums.ImageFormat;
import com.mantra.mfscan.enums.LogLevel;
import com.mantra.mfscansample.adapter.ActionAdapter;
import com.mantra.mfscansample.adapter.MenuAdapter;
import com.mantra.mfscansample.adapter.NavigationMenuItem;
import com.mantra.mfscansample.adapter.SelectorAdapter;
import com.mantra.mfscansample.databinding.ActivityMainBinding;
import com.mantra.mfscansample.dialog.SelectImageFormatDialog;
import com.mantra.mfscansample.dialog.SelectQualityDialog;
import com.mantra.mfscansample.dialog.SelectTimeoutDialog;
import com.mantra.mfscansample.dialog.SetKeyDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements MenuAdapter.ItemClickListener
        , ActionAdapter.ActionClickListener
        , NavigationView.OnNavigationItemSelectedListener
        , MFScan_Callback {

    public ActivityMainBinding binding;

    private MenuAdapter menuAdapter;
    private ActionAdapter actionAdapter;
    ArrayList<NavigationMenuItem> navigationMenuItemArrayList, navigationMenuActionArrayList;
    private MFScan mfScan;
    ArrayList<String> modelName;
    SelectorAdapter adapter;
    private static final String strSelect = "No Device";
    private byte[] lastCapFingerData;
    private DeviceInfo lastDeviceInfo;
    ImageFormat captureImageDatas;
    private boolean isStartCaptureRunning;
    private boolean isStopCaptureRunning;

    private enum ScannerAction {
        Capture
    }

    private ScannerAction scannerAction = ScannerAction.Capture;
    private SelectImageFormatDialog selectImageFormatDialog;
    private SelectQualityDialog selectQualityDialog;
    private SetKeyDialog selectKeyDialog;
    private SelectTimeoutDialog selectTimeoutDialog;
    private String clientKey = "";
    public static long lastClickTime = 0;
    public static int ClickThreshold = 1000;
    int minQuality = 60;
    int timeOut = 10000;

    public boolean InitCalled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.container.contentMain.txtApp.setText(getString(R.string.app_name) + " (" + BuildConfig.VERSION_NAME + ")");

        RecyclerView.LayoutManager mLayoutManager1 = new LinearLayoutManager(MainActivity.this);
        binding.menuitemList.setLayoutManager(mLayoutManager1);
        addMenuItem();

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
        binding.menuActionList.setLayoutManager(mLayoutManager);
        addActionItem();

        binding.navView.setCheckedItem(R.id.nav_home);
        binding.navView.setNavigationItemSelectedListener(this);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        modelName = new ArrayList<>();
        modelName.add(strSelect);
        adapter = new SelectorAdapter(this, modelName);
        binding.container.contentMain.spDeviceName.setAdapter(adapter);

        mfScan = new MFScan(this, this);
        String file = getExternalFilesDir(null).toString();
        mfScan.SetLogProperties(file, LogLevel.OFF);
        captureImageDatas = (ImageFormat.BMP);
        clearText();
        setonClick();
    }

    private void setonClick() {
        binding.container.contentMain.ivFpLogo.setOnClickListener(view -> onLogoClicked());
        binding.container.contentMain.rlSideMenu.setOnClickListener(view -> onViewClicked());
        binding.ivSettingMenu.setOnClickListener(view -> onCloseMenuClicked());
    }

    public void onLogoClicked() {
        binding.drawerLayout.openDrawer(Gravity.LEFT);
    }

    public void onViewClicked() {
        binding.drawerLayout.openDrawer(Gravity.LEFT);
    }

    public void onCloseMenuClicked() {
        binding.drawerLayout.closeDrawer(Gravity.LEFT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return false;
    }

    @Override
    protected void onStop() {
        if (isStartCaptureRunning) {
            StopCapture();
        }
        isStartCaptureRunning = false;
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (isStartCaptureRunning) {
            StopCapture();
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isStartCaptureRunning) {
            StopCapture();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            isStartCaptureRunning = false;
            isStopCaptureRunning = false;
            mfScan.Uninit();
            mfScan.Dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void addMenuItem() {
        try {
            navigationMenuItemArrayList = new ArrayList<>();
            NavigationMenuItem navigationMenuItem;
            try {
                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.menu_get_sdk_version);
                navigationMenuItemArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.menu_supported_device_list);
                navigationMenuItemArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.menu_select_image_format);
                navigationMenuItemArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.menu_set_quality);
                navigationMenuItemArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.menu_set_timeout);
                navigationMenuItemArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.menu_set_key);
                navigationMenuItemArrayList.add(navigationMenuItem);

                menuAdapter = new MenuAdapter(MainActivity.this, navigationMenuItemArrayList, this);
                binding.menuitemList.setAdapter(menuAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addActionItem() {
        try {
            navigationMenuActionArrayList = new ArrayList<>();
            NavigationMenuItem navigationMenuItem;
            try {
                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.action_check_device);
                navigationMenuActionArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.init);
                navigationMenuActionArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.SyncCapture);
                navigationMenuActionArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.StopSyncCapture);
                navigationMenuActionArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.AutoCapture);
                navigationMenuActionArrayList.add(navigationMenuItem);

                navigationMenuItem = new NavigationMenuItem();
                navigationMenuItem.menu_name = getString(R.string.uninit);
                navigationMenuActionArrayList.add(navigationMenuItem);

                actionAdapter = new ActionAdapter(MainActivity.this, navigationMenuActionArrayList, this);
                binding.menuActionList.setAdapter(actionAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onItemClick(int position, String item_name) {
        binding.drawerLayout.closeDrawer(Gravity.LEFT);
        clearText();
        switch (position) {
            case 0://sdk version
                String sdkVersion = mfScan.GetSDKVersion();
                setLogs("SDK Version : " + sdkVersion, false);
                break;
            case 1://supported device List
                List<String> supportedList = new ArrayList<>();
                int ret = mfScan.GetSupportedDevices(supportedList);
                if (ret == 0) {
                    StringBuilder str = new StringBuilder();
                    for (String list : supportedList) {
                        if (str.length() != 0) {
                            str.append(", ");
                        }
                        str.append(list);
                    }
                    setLogs("Supported Devices: " + str.toString(), false);
                } else {
                    setLogs("Supported Devices Error: " + ret + " (" + mfScan.GetErrorMessage(ret) + ")", true);
                }
                break;
            case 2://select Image Format
                showImageFormatDialog();
                break;
            case 3://Set Quality
                showSetQualityDialog();
                break;
            case 4://set Timeout
                showSetTimeoutDialog();
                break;
            case 5://set key
                showSetKeyDialog();
                break;
        }
    }

    @Override
    public void onActionClick(int position, String item_name) {
        binding.drawerLayout.closeDrawer(Gravity.LEFT);
        clearText();
        binding.container.contentMain.imgFinger.post(new Runnable() {
            @Override
            public void run() {
                binding.container.contentMain.imgFinger.setImageResource(R.drawable.bg_clor_white);
            }
        });
        switch (position) {
            case 0://check device,is device connected
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String device = binding.container.contentMain.spDeviceName.getSelectedItem().toString();
                            boolean ret = mfScan.IsDeviceConnected(DeviceModel.valueFor(device));
                            if (ret) {
                                binding.container.contentMain.ivStatusFp.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.finger_green);
                                    }
                                });
                                setLogs("Device Connected", false);
                            } else {
                                binding.container.contentMain.ivStatusFp.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.finger_red);
                                    }
                                });
                                setLogs("Device Not Connected", true);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            setLogs("Device not found", true);
                        }
                    }
                }).start();
                break;
            case 1://Init
                if (!InitCalled) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InitCalled = true;
                            try {
                                String device = binding.container.contentMain.spDeviceName.getSelectedItem().toString();
                                DeviceInfo info = new DeviceInfo();
                                int ret = mfScan.Init(DeviceModel.valueFor(device), (clientKey.isEmpty()) ? null : clientKey, info);
                                if (ret != 0) {
                                    InitCalled = false;
                                    setLogs("Init: " + ret + " (" + mfScan.GetErrorMessage(ret) + ")", true);
                                } else {
                                    InitCalled = false;
                                    lastDeviceInfo = info;
                                    setLogs("Init Success", false);
                                    setDeviceInfo(info);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                InitCalled = false;
                                setLogs("Device not found", false);
                            }
                        }
                    }).start();
                }
                break;
            case 2://start Capture
                if (lastDeviceInfo == null) {
                    setLogs("Please run device init first", true);
                    return;
                }
                if (!mfScan.IsCaptureRunning()) {
                    scannerAction = ScannerAction.Capture;
                    StartCapture();
                } else {
                    setLogs("Capture Ret: " + MFScanNative.CAPTURE_ALREADY_STARTED
                            + " (" + mfScan.GetErrorMessage(MFScanNative.CAPTURE_ALREADY_STARTED) + ")", true);
                }
                break;
            case 3://stop Capture
                StopCapture();
                break;
            case 4://Auto Capture
                if (lastDeviceInfo == null) {
                    setLogs("Please run device init first", true);
                    return;
                }
                if (!mfScan.IsCaptureRunning()) {
                    scannerAction = ScannerAction.Capture;
                    StartSyncCapture();
                } else {
                    setLogs("Capture Ret: " + MFScanNative.CAPTURE_ALREADY_STARTED
                            + " (" + mfScan.GetErrorMessage(MFScanNative.CAPTURE_ALREADY_STARTED) + ")", true);
                }
                break;
            case 5://UnInit
                try {
                    int ret = mfScan.Uninit();
                    isStartCaptureRunning = false;
                    isStopCaptureRunning = false;
                    if (ret == 0) {
                        setLogs("UnInit Success", false);
                    } else {
                        setLogs("UnInit: " + ret + " (" + mfScan.GetErrorMessage(ret) + ")", true);
                    }
                    lastDeviceInfo = null;
                    lastCapFingerData = null;
                    setClearDeviceInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    captureThread = null;
                }
                break;
        }
    }


    private void setDeviceInfo(DeviceInfo info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (info == null)
                        return;
                    binding.container.contentMain.txtMake.setText(getString(R.string.make) + " " + info.Make);
                    binding.container.contentMain.txtModel.setText(getString(R.string.model) + " " + info.Model);
                    binding.container.contentMain.txtSerialNo.setText(getString(R.string.serial_no) + " " + info.SerialNo);
                    binding.container.contentMain.txtWH.setText(getString(R.string.w_h) + " " + info.Width + " / " + info.Height);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setClearDeviceInfo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    binding.container.contentMain.txtMake.setText(getString(R.string.make));
                    binding.container.contentMain.txtModel.setText(getString(R.string.model));
                    binding.container.contentMain.txtSerialNo.setText(getString(R.string.serial_no));
                    binding.container.contentMain.txtWH.setText(getString(R.string.w_h));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void StartCapture() {
        if (isStartCaptureRunning) {
            setLogs("StartCapture Ret: " + MFScanNative.CAPTURE_ALREADY_STARTED
                    + " (" + mfScan.GetErrorMessage(MFScanNative.CAPTURE_ALREADY_STARTED) + ")", true);
            return;
        }
        if (isStopCaptureRunning) {
            return;
        }
        if (lastDeviceInfo == null) {
            setLogs("Please run device init first", true);
            return;
        }
        isStartCaptureRunning = true;
        try {
            binding.container.contentMain.imgFinger.setImageResource(R.drawable.bg_clor_white);
            int ret = mfScan.StartCapture(minQuality, timeOut);
            if (ret != 0) {
                isStartCaptureRunning = false;
            }
            setLogs("StartCapture Ret: " + ret + " (" + mfScan.GetErrorMessage(ret) + ")", ret == 0 ? false : true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Thread captureThread = null;

    private void StartSyncCapture() {
        if (isStartCaptureRunning || (captureThread != null && captureThread.isAlive())) {
            setLogs("Start sync Capture Ret: " + MFScanNative.CAPTURE_ALREADY_STARTED
                    + " (" + mfScan.GetErrorMessage(MFScanNative.CAPTURE_ALREADY_STARTED) + ")", true);
            return;
        }
        if (isStopCaptureRunning) {
            return;
        }
        if (lastDeviceInfo == null) {
            setLogs("Please run device init first", true);
            return;
        }
        isStartCaptureRunning = true;
        captureThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    binding.container.contentMain.imgFinger.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.container.contentMain.imgFinger.setImageResource(R.drawable.bg_clor_white);
                        }
                    });
                    int qty[] = new int[1];
                    int nfiq[] = new int[1];
                    setLogs("Auto Capture Started", false);
                    int ret = mfScan.AutoCapture(minQuality, timeOut, qty, nfiq);
                    if (ret != 0) {
                        binding.container.contentMain.imgFinger.post(new Runnable() {
                            @Override
                            public void run() {
                                binding.container.contentMain.imgFinger.setImageResource(R.drawable.bg_clor_white);
                            }
                        });
                        if (ret == -2057) {
                            setLogs("Device Not Connected", true);
                        } else {
                            setLogs("Start Sync Capture Ret: " + ret + " (" + mfScan.GetErrorMessage(ret) + ")", true);
                        }
                    } else {
                        String log = "Capture Success";
                        String message = "Quality: " + qty[0] + " NFIQ: " + nfiq[0];
                        setLogs(log, false);
                        setTxtStatusMessage(message);
                        if (scannerAction == ScannerAction.Capture) {
                            int Size = lastDeviceInfo.Width * lastDeviceInfo.Height + 1111;
                            byte[] bImage = new byte[Size];
                            int[] tSize = new int[Size];
                            ret = mfScan.GetImage(bImage, tSize, 0,captureImageDatas);
                            if (ret == 0) {
                                lastCapFingerData = new byte[Size];
                                System.arraycopy(bImage, 0, lastCapFingerData, 0,
                                        bImage.length);
                            } else {
                                setLogs(mfScan.GetErrorMessage(ret), true);
                            }
                        }
                    }
                    isStartCaptureRunning = false;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    captureThread = null;
                }
            }
        });
        captureThread.start();
    }

    private void StopCapture() {
        try {
            isStopCaptureRunning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret = mfScan.StopCapture();
                    isStopCaptureRunning = false;
                    isStartCaptureRunning = false;
                    setLogs("StopCapture: " + ret + " (" + mfScan.GetErrorMessage(ret) + ")", false);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            setLogs("Error", true);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    private void clearText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.container.contentMain.txtCaptureStatus.setText("");
                binding.container.contentMain.txtStatusMessage.setText("");
            }
        });
    }

    private void setLogs(final String logs, boolean isError) {
        binding.container.contentMain.txtCaptureStatus.post(new Runnable() {
            @Override
            public void run() {
                if (isError) {
                    binding.container.contentMain.txtCaptureStatus.setTextColor(Color.RED);
                } else {
                    binding.container.contentMain.txtCaptureStatus.setTextColor(Color.WHITE);
                }
                binding.container.contentMain.txtCaptureStatus.setText(logs);
            }
        });
    }

    private void setTxtStatusMessage(final String logs) {
        binding.container.contentMain.txtStatusMessage.post(new Runnable() {
            @Override
            public void run() {
                binding.container.contentMain.txtStatusMessage.setText(logs);
            }
        });
    }

    @Override
    public void OnDeviceDetection(String DeviceName, DeviceDetection detection) {
        isStartCaptureRunning = false;
        isStopCaptureRunning = false;
        if (detection == DeviceDetection.CONNECTED) {
            if (DeviceName != null) {
                binding.container.contentMain.ivStatusFp.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.finger_green);
                    }
                });
                boolean exist = false;
                for (String string : modelName) {
                    if (string.equals(DeviceName)) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    modelName.add(DeviceName);
                    modelName.remove(strSelect);
                }
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            setLogs("Device connected", false);
        } else if (detection == DeviceDetection.DISCONNECTED) {
            try {
                lastCapFingerData = null;
                lastDeviceInfo = null;
                setLogs("Device Not Connected", true);
                setTxtStatusMessage("");
                binding.container.contentMain.ivStatusFp.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.finger_red);
                    }
                });
                try {
                    mfScan.Uninit();
                    for (String temp : modelName) {
                        if (temp.equals(DeviceName)) {
                            modelName.remove(temp);
                            if (modelName.size() == 0) {
                                modelName.add(strSelect);
                            }
                            break;
                        }
                    }
                    try {
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    setClearDeviceInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                binding.container.contentMain.imgFinger.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.container.contentMain.imgFinger.setImageResource(R.drawable.bg_clor_white);
                    }
                });
            }
        }
    }

    @Override
    public void OnPreview(int errorCode, int quality, byte[] image) {
        try {
            if (errorCode == 0 && image != null) {
                Bitmap previewBitmap = BitmapFactory.decodeByteArray(image, 0,
                        image.length);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.container.contentMain.imgFinger.setImageBitmap(previewBitmap);
                    }
                });
                setLogs("Preview Quality: " + quality, false);
            } else {
                if (errorCode == -2057) {
                    setLogs("Device Not Connected", true);
                } else {
                    setLogs("Preview Error Code: " + errorCode + " (" + mfScan.GetErrorMessage(errorCode) + ")", true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void OnComplete(int errorCode, int Quality, int NFIQ) {
        try {
            isStartCaptureRunning = false;
            if (errorCode == 0) {
                String log = "Capture Success";
                String quality = "Quality: " + Quality + " NFIQ: " + NFIQ;
                setLogs(log, false);
                setTxtStatusMessage(quality);
                if (scannerAction == ScannerAction.Capture) {
                    int Size = lastDeviceInfo.Width * lastDeviceInfo.Height + 1111;
                    byte[] bImage = new byte[Size];
                    int[] tSize = new int[Size];
                    int ret = mfScan.GetImage(bImage, tSize, 1,captureImageDatas);
                    if (ret == 0) {
                        lastCapFingerData = new byte[Size];
                        System.arraycopy(bImage, 0, lastCapFingerData, 0,
                                bImage.length);
                    } else {
                        setLogs(mfScan.GetErrorMessage(ret), true);
                    }
                }
            } else {
                setTxtStatusMessage("");
                binding.container.contentMain.imgFinger.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.container.contentMain.imgFinger.setImageResource(R.drawable.bg_clor_white);
                    }
                });
                if (errorCode == -2057) {
                    setLogs("Device Not Connected", true);
                } else {
                    setLogs("CaptureComplete: " + errorCode + " (" + mfScan.GetErrorMessage(errorCode) + ")", true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void OnFingerPosition(int errorCode, int position) {
        if (position == FingerPosition.POSTION_OK.getValue()) {
            setTxtStatusMessage("POSITION OK");
        } else if (position == FingerPosition.POSTION_LEFT.getValue()) {
            setTxtStatusMessage("POSITION LEFT");
        } else if (position == FingerPosition.POSTION_RIGHT.getValue()) {
            setTxtStatusMessage("POSITION RIGHT");
        } else if (position == FingerPosition.POSTION_TOP.getValue()) {
            setTxtStatusMessage("POSITION TOP");
        } else if (position == FingerPosition.POSTION_NOT_IN_BOTTOM.getValue()) {
            setTxtStatusMessage("POSITION NOT IN BOTTOM");
        } else if (position == FingerPosition.POSTION_NOT_OK.getValue()) {
            setTxtStatusMessage("POSITION NOT OK");
        }
    }

    public void saveData() {
        try {
            int Size = lastDeviceInfo.Width * lastDeviceInfo.Height + 1111;
            int[] iSize = new int[Size];
            byte[] bImage1 = new byte[Size];
            int ret = mfScan.GetImage(bImage1, iSize, 1, captureImageDatas);
            byte[] bImage = new byte[iSize[0]];
            System.arraycopy(bImage1, 0, bImage, 0, iSize[0]);
            if (ret == 0) {
                switch (captureImageDatas) {
                    case RAW:
                        WriteImageFile("Raw.raw", bImage);
                        break;
                    case BMP:
                        WriteImageFile("Bitmap.bmp", bImage);
                        break;
                }
            } else {
                setLogs("Save Template Ret: " + ret
                        + " (" + mfScan.GetErrorMessage(ret) + ")", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setLogs("Error Saving Image.", true);
        }
    }

    private void WriteImageFile(String filename, byte[] bytes) {
        try {
            String path = getExternalFilesDir(null).toString() + "//FingerData//Image";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(bytes);
            stream.close();
            setLogs("Image Saved", false);
        } catch (Exception e1) {
            e1.printStackTrace();
            setLogs("Error Saving Image", false);
        }
    }

    private void showImageFormatDialog() {
        selectImageFormatDialog = new SelectImageFormatDialog(this);
        selectImageFormatDialog.show();
        try {
            if (captureImageDatas != null) {
                switch (captureImageDatas) {
                    case BMP:
                        selectImageFormatDialog.holder.cbBmp.setChecked(true);
                        break;
                    case RAW:
                        selectImageFormatDialog.holder.cbRaw.setChecked(true);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        selectImageFormatDialog.holder.txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                selectImageFormatDialog.dismiss();
            }
        });

        selectImageFormatDialog.holder.txtSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                captureImageDatas = ImageFormat.BMP;
                if (selectImageFormatDialog.holder.cbBmp.isChecked()) {
                    captureImageDatas = (ImageFormat.BMP);
                } else if (selectImageFormatDialog.holder.cbRaw.isChecked()) {
                    captureImageDatas = (ImageFormat.RAW);
                }
                selectImageFormatDialog.dismiss();
            }
        });
    }

    private void showSetKeyDialog() {
        selectKeyDialog = new SetKeyDialog(this);
        selectKeyDialog.show();
        selectKeyDialog.holder.edtKey.setText("" + clientKey);
        selectKeyDialog.holder.edtKey.setSelection(selectKeyDialog.holder.edtKey.getText().length());
        selectKeyDialog.holder.txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                selectKeyDialog.dismiss();
            }
        });

        selectKeyDialog.holder.txtSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                try {
                    clientKey = (selectKeyDialog.holder.edtKey.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                selectKeyDialog.dismiss();
            }
        });
    }

    private void showSetQualityDialog() {
        selectQualityDialog = new SelectQualityDialog(this);
        selectQualityDialog.show();
        selectQualityDialog.holder.edtQuality.setText("" + minQuality);
        selectQualityDialog.holder.edtQuality.setSelection(selectQualityDialog.holder.edtQuality.getText().length());
        selectQualityDialog.holder.txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                selectQualityDialog.dismiss();
            }
        });

        selectQualityDialog.holder.txtSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                try {
                    minQuality = Integer.parseInt(selectQualityDialog.holder.edtQuality.getText().toString());
                    if (minQuality > 100 || (minQuality < 1)) {
                        selectQualityDialog.holder.edtQuality.setError("Quality should be between 1-100");
                        return;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                selectQualityDialog.dismiss();
            }
        });
    }

    private void showSetTimeoutDialog() {
        selectTimeoutDialog = new SelectTimeoutDialog(this);
        selectTimeoutDialog.show();
        selectTimeoutDialog.holder.edtTimeout.setText("" + timeOut);
        selectTimeoutDialog.holder.edtTimeout.setSelection(selectTimeoutDialog.holder.edtTimeout.getText().length());
        selectTimeoutDialog.holder.txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                selectTimeoutDialog.dismiss();
            }
        });

        selectTimeoutDialog.holder.txtSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < ClickThreshold) {
                    return;
                }
                lastClickTime = SystemClock.elapsedRealtime();
                try {
                    timeOut = Integer.parseInt(selectTimeoutDialog.holder.edtTimeout.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                selectTimeoutDialog.dismiss();
            }
        });
    }
}