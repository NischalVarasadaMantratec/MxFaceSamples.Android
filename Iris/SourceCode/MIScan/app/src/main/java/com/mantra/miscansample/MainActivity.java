package com.mantra.miscansample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
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
import com.mantra.miscan.DeviceInfo;
import com.mantra.miscan.IrisAnatomy;
import com.mantra.miscan.MIScan;
import com.mantra.miscan.MIScanNative;
import com.mantra.miscan.MIScan_Callback;
import com.mantra.miscan.enums.DeviceDetection;
import com.mantra.miscan.enums.DeviceModel;
import com.mantra.miscan.enums.ImageFormat;
import com.mantra.miscansample.adapter.ActionAdapter;
import com.mantra.miscansample.adapter.MenuAdapter;
import com.mantra.miscansample.adapter.NavigationMenuItem;
import com.mantra.miscansample.adapter.SelectorAdapter;
import com.mantra.miscansample.databinding.ActivityMainBinding;
import com.mantra.miscansample.dialog.SelectImageFormatDialog;
import com.mantra.miscansample.dialog.SelectQualityDialog;
import com.mantra.miscansample.dialog.SelectTimeoutDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
		implements MenuAdapter.ItemClickListener
		, ActionAdapter.ActionClickListener
		, NavigationView.OnNavigationItemSelectedListener
		, MIScan_Callback {

	private MenuAdapter menuAdapter;
	private ActionAdapter actionAdapter;
	ArrayList<NavigationMenuItem> navigationMenuItemArrayList, navigationMenuActionArrayList;

	private MIScan miScan;
	ArrayList<String> modelName;

	SelectorAdapter adapter;
	private static final String strSelect = "No Device";
	private byte[] lastCapIrisData;
	private DeviceInfo lastDeviceInfo;
	ImageFormat captureImageDatas;
	private final Paint paint = new Paint();

	private enum ScannerAction {
		Capture
	}

	private ScannerAction scannerAction = ScannerAction.Capture;
	private SelectImageFormatDialog selectImageFormatDialog;
	private SelectQualityDialog selectQualityDialog;
	private SelectTimeoutDialog selectTimeoutDialog;
	public static long lastClickTime = 0;
	public static int ClickThreshold = 1000;
	int minQuality = 85;
	int timeOut = 10000;
	int BmpHeaderlength = 1078;
	boolean isAutoCapture = false;

	public ActivityMainBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		View view = binding.getRoot();
		setContentView(view);

		binding.container.contentMain.txtApp.setText(getString(R.string.app_name)
				+ "(" + BuildConfig.VERSION_NAME + ")");

		RecyclerView.LayoutManager mLayoutManager1 = new LinearLayoutManager(MainActivity.this);
		binding.menuitemList.setLayoutManager(mLayoutManager1);
		addMenuItem();

		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
		binding.menuActionList.setLayoutManager(mLayoutManager);
		addActionItem();

		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(2);
		paint.setAntiAlias(true);

		binding.navView.setCheckedItem(R.id.nav_home);
		binding.navView.setNavigationItemSelectedListener(this);

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		modelName = new ArrayList<>();
		modelName.add(strSelect);
		adapter = new SelectorAdapter(this, modelName);
		binding.container.contentMain.spDeviceName.setAdapter(adapter);

		miScan = new MIScan(this, this);
		captureImageDatas = (ImageFormat.BMP);
		clearText();
		binding.container.contentMain.rlSideMenu.setOnClickListener(view1 -> binding.drawerLayout.openDrawer(Gravity.LEFT));
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
		if (miScan.IsCaptureRunning()) {
			StopCapture();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		try {
			miScan.Uninit();
			miScan.Dispose();
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
		binding.container.contentMain.imgIris.post(new Runnable() {
			@Override
			public void run() {
				binding.container.contentMain.imgIris.setImageResource(R.drawable.bg_clor_white);
			}
		});
		switch (position) {
			case 0://sdk version
				String sdkVersion = miScan.GetSDKVersion();
				setLogs("SDK Version : " + sdkVersion, false);
				break;
			case 1://supported device List
				List<String> supportedList = new ArrayList<>();
				int ret = miScan.GetSupportedDevices(supportedList);
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
					setLogs("Supported Devices Error: " + ret + " (" + miScan.GetErrorMessage(ret) + ")", true);
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
		}
	}

	@Override
	public void onActionClick(int position, String item_name) {
		binding.drawerLayout.closeDrawer(Gravity.LEFT);
		clearText();
		binding.container.contentMain.imgIris.post(new Runnable() {
			@Override
			public void run() {
				binding.container.contentMain.imgIris.setImageResource(R.drawable.bg_clor_white);
			}
		});
		switch (position) {
			case 0://check device,is device connected
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							String device = binding.container.contentMain.spDeviceName.getSelectedItem().toString();
							boolean ret = miScan.IsDeviceConnected(DeviceModel.valueFor(device));
							if (ret) {
								binding.container.contentMain.ivStatusFp.post(new Runnable() {
									@Override
									public void run() {
										binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.connect_1);
									}
								});
								setLogs("Device Connected", false);
							} else {
								binding.container.contentMain.ivStatusFp.post(new Runnable() {
									@Override
									public void run() {
										binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.disconnect_1);
									}
								});
								setLogs("Device Not Connected", true);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
							setLogs("Device not found", true);
						}
					}
				}).start();
				break;
			case 1://Init
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							String device = binding.container.contentMain.spDeviceName.getSelectedItem().toString();
							DeviceInfo info = new DeviceInfo();
							int ret = miScan.Init(DeviceModel.valueFor(device), info);
							if (ret != 0) {
								setLogs("Init: " + ret + " (" + miScan.GetErrorMessage(ret) + ")", true);
							} else {
//                                DeviceInfo info = midFingerAuth.getDeviceInfo();
								lastDeviceInfo = info;
								setLogs("Init Success", false);
								setDeviceInfo(info);
							}
						} catch (Exception e) {
							setLogs("Device not found", false);
						}
					}
				}).start();
				break;
			case 2://start Capture
				if (lastDeviceInfo == null) {
					setLogs("Please run device init first", true);
					return;
				}
				scannerAction = ScannerAction.Capture;
				isAutoCapture = false;
				StartCapture();
				break;
			case 3://stop Capture
				StopCapture();
				break;
			case 4://Auto Capture
				if (lastDeviceInfo == null) {
					setLogs("Please run device init first", true);
					return;
				}
				scannerAction = ScannerAction.Capture;
				isAutoCapture = true;
				StartSyncCapture();
				break;
			case 5://UnInit
				try {
					int ret = miScan.Uninit();
					if (ret == 0) {
						setLogs("UnInit Success", false);
					} else {
						setLogs("UnInit: " + ret + " (" + miScan.GetErrorMessage(ret) + ")", true);
					}
					lastDeviceInfo = null;
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
		if (miScan.IsCaptureRunning()) {
			setLogs("StartCapture Ret: " + MIScanNative.CAPTURE_ALREADY_STARTED
					+ " (" + miScan.GetErrorMessage(MIScanNative.CAPTURE_ALREADY_STARTED) + ")", true);
			return;
		}
		if (lastDeviceInfo == null) {
			setLogs("Please run device init first", true);
			return;
		}
		try {
			binding.container.contentMain.imgIris.setImageResource(R.drawable.bg_clor_white);
			int ret = miScan.StartCapture(timeOut, minQuality);
			setLogs("StartCapture Ret: " + ret + " (" + miScan.GetErrorMessage(ret) + ")", ret == 0 ? false : true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private Thread captureThread = null;

	private void StartSyncCapture() {
		if (miScan.IsCaptureRunning() || (captureThread != null && captureThread.isAlive())) {
			setLogs("Start sync Capture Ret: " + MIScanNative.CAPTURE_ALREADY_STARTED
					+ " (" + miScan.GetErrorMessage(MIScanNative.CAPTURE_ALREADY_STARTED) + ")", true);
			return;
		}
		if (lastDeviceInfo == null) {
			setLogs("Please run device init first", true);
			return;
		}
		captureThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					binding.container.contentMain.imgIris.post(new Runnable() {
						@Override
						public void run() {
							binding.container.contentMain.imgIris.setImageResource(R.drawable.bg_clor_white);
						}
					});
					int qty[] = new int[1];
					IrisAnatomy irisAnatomy = new IrisAnatomy();
					setLogs("Auto Capture Started", false);
					int ret = miScan.AutoCapture(timeOut, qty, irisAnatomy);
					if (ret != 0) {
						binding.container.contentMain.imgIris.post(new Runnable() {
							@Override
							public void run() {
								binding.container.contentMain.imgIris.setImageResource(R.drawable.bg_clor_white);
							}
						});
						setLogs("Start Sync Capture Ret: " + ret + " (" + miScan.GetErrorMessage(ret) + ")", true);
					} else {
						String log = "Capture Success ";
						String message = "Quality: " + qty[0] /*+ " NFIQ: " + nfiq[0]*/;
						setLogs(log, false);
						setTxtStatusMessage(message);
						if (scannerAction == ScannerAction.Capture) {
							int Size = lastDeviceInfo.Width * lastDeviceInfo.Height + BmpHeaderlength;
							byte[] bImage = new byte[Size];
							int[] tSize = new int[Size];
							ret = miScan.GetImage(bImage, tSize, 0, ImageFormat.BMP);
							if (ret == 0) {
								lastCapIrisData = new byte[Size];
								System.arraycopy(bImage, 0, lastCapIrisData, 0,
										bImage.length);
							} else {
								setLogs(miScan.GetErrorMessage(ret), true);
							}
						}
					}
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
			int ret = miScan.StopCapture();
			setLogs("StopCapture: " + ret + " (" + miScan.GetErrorMessage(ret) + ")", false);
		} catch (Exception e) {
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
		if (detection == DeviceDetection.CONNECTED) {
			if (DeviceName != null) {
				binding.container.contentMain.ivStatusFp.post(new Runnable() {
					@Override
					public void run() {
						binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.connect_1);
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
				}catch (Exception e){
				}
			}
			setLogs("Device connected", false);
		} else if (detection == DeviceDetection.DISCONNECTED) {
			try {
				lastDeviceInfo = null;
				setLogs("Device Not Connected", true);
				binding.container.contentMain.ivStatusFp.post(new Runnable() {
					@Override
					public void run() {
						binding.container.contentMain.ivStatusFp.setImageResource(R.drawable.disconnect_1);
					}
				});
				try {
					miScan.Uninit();
					for (String temp : modelName) {
						if (temp.equals(DeviceName)) {
							modelName.remove(temp);
							if (modelName.size() == 0) {
								modelName.add(strSelect);
							}
							break;
						}
					}
					adapter.notifyDataSetChanged();
					setClearDeviceInfo();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Bitmap previewBitmap;

	@Override
	public synchronized void OnPreview(int ErrorCode, int Quality, byte[] Image, IrisAnatomy irisAnatomy) {
		Log.e("OnPreview","OnPreview called");
		try {
			if (ErrorCode == 0 && Image != null) {

				int lowerValue = 0;
				int upperValue = minQuality;
				if(!isAutoCapture) {
					if (minQuality > 40) {
						lowerValue = minQuality - 30;
					} else {
						lowerValue = 10;
					}
				}
				else {
					lowerValue = 55;
					upperValue = 85;
				}

				if (Quality < lowerValue) {
					paint.setColor(Color.RED);
				} else if (Quality >= lowerValue && Quality <= upperValue) {
					paint.setColor(Color.BLUE);
				} else {
					paint.setColor(Color.GREEN);
				}

				Bitmap bitmap = BitmapFactory.decodeByteArray(Image, 0, Image.length);
				Bitmap bmOverlay = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_4444);

				Canvas canvas = new Canvas(bmOverlay);
				canvas.drawBitmap(bitmap, new Matrix(), null);

				int x = irisAnatomy.irisX;
				int y = irisAnatomy.irisY;
				int r = irisAnatomy.irisR;

				int fy = 0;
				int fx = 0;

				fy = y;
				fx = x;
				canvas.drawCircle(fx, fy, r, paint);

				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bmOverlay.compress(Bitmap.CompressFormat.PNG, 100, stream);
				byte[] byteArray = stream.toByteArray();

				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Log.e("jikadara","ShowImage 0");
				bitmap.recycle();
				Log.e("jikadara","ShowImage 1");
				bmOverlay.recycle();
				Log.e("jikadara","ShowImage 3");
				ShowImage(byteArray);

				setLogs("Preview Quality: " + Quality, false);
			} else {
				setLogs("Preview Error Code: " + ErrorCode + " (" + miScan.GetErrorMessage(ErrorCode) + ")", true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void OnComplete(int ErrorCode, int Quality, byte[] Image, IrisAnatomy anatomy) {
		try {
			if (ErrorCode == 0) {
				String log = "Capture Success";
				String quality = "Quality: " + Quality /*+ " NFIQ: " + NFIQ*/;
				setLogs(log, false);
				setTxtStatusMessage(quality);
				if (scannerAction == ScannerAction.Capture) {
					int Size = lastDeviceInfo.Width * lastDeviceInfo.Height + BmpHeaderlength;
					byte[] bImage = new byte[Size];
					int[] tSize = new int[Size];
					int ret = miScan.GetImage(bImage, tSize, 0, ImageFormat.BMP);
					if (ret == 0) {
						lastCapIrisData = new byte[Size];
						System.arraycopy(bImage, 0, lastCapIrisData, 0,
								bImage.length);
					} else {
						setLogs(miScan.GetErrorMessage(ret), true);
					}
				}
			} else {
				binding.container.contentMain.imgIris.post(new Runnable() {
					@Override
					public void run() {
						binding.container.contentMain.imgIris.setImageResource(R.drawable.bg_clor_white);
					}
				});
				setLogs("CaptureComplete: " + ErrorCode + " (" + miScan.GetErrorMessage(ErrorCode) + ")", true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void ShowImage(final byte[] image) {
		Log.e("jikadara","ShowImage 4");
		binding.container.contentMain.imgIris.post(new Runnable() {
			@Override
			public void run() {
				if (image != null) {
					Log.e("jikadara","ShowImage 5");
					if (previewBitmap != null) {
						Log.e("jikadara","ShowImage 6");
						previewBitmap.recycle();
						previewBitmap = null;
					}
					Log.e("jikadara","ShowImage 7");
					previewBitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
					Log.e("jikadara","ShowImage 8");
					binding.container.contentMain.imgIris.setImageBitmap(previewBitmap);
				} else {
					binding.container.contentMain.imgIris.setImageResource(R.drawable.ic_launcher_background);
				}
			}
		});
	}

	public void saveData() {
		try {
			int Size = lastDeviceInfo.Width * lastDeviceInfo.Height + BmpHeaderlength;
			int[] iSize = new int[Size];
			byte[] bImage1 = new byte[Size];
			int ret = miScan.GetImage(bImage1, iSize, 0, captureImageDatas);
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
						+ " (" + miScan.GetErrorMessage(ret) + ")", true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			setLogs("Error Saving Image.", true);
		}
	}

	private void WriteImageFile(String filename, byte[] bytes) {
		try {
			String path = getExternalFilesDir(null).toString() + "//IrisData//Image";
			File file = new File(path);
			if (!file.exists()) {
				file.mkdirs();
			}
			path = path + "//" + filename;
			file = new File(path);
			if (!file.exists()) {
				boolean isCreated = file.createNewFile();
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

		if(captureImageDatas == (ImageFormat.BMP)) {
			selectImageFormatDialog.holder.cbBmp.setChecked(true);
		}
		else if(captureImageDatas == (ImageFormat.RAW)) {
			selectImageFormatDialog.holder.cbRaw.setChecked(true);
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

	private void showSetQualityDialog() {
		selectQualityDialog = new SelectQualityDialog(this);
		selectQualityDialog.show();
		selectQualityDialog.holder.edtMinQuality.setText( "" + minQuality);
		selectQualityDialog.holder.edtMinQuality.setSelection(selectQualityDialog.holder.edtMinQuality.getText().length());
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
				try {
					minQuality = Integer.parseInt(selectQualityDialog.holder.edtMinQuality.getText().toString());
					if( minQuality <= 0 || minQuality > 100 ) {
						selectQualityDialog.holder.edtMinQuality.setError("Quality should be between 1-100");
						return;
					}

				} catch (NumberFormatException e) {
					selectQualityDialog.holder.edtMinQuality.setError("Quality should be between 1-100");
					return;
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

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}
}