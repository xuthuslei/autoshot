package com.hu321.autoshot;

import java.io.DataOutputStream;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.acra.ACRA;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

import com.hu321.autoshot.HttpConnection.CallbackListener;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class MainActivity extends Activity implements SurfaceHolder.Callback {
	protected final Context mainActivity = this;
	private String mbApiKey = "";// 请替换申请客户端应用时获取的Api Key串
	private String mbRootPath = ""; // 用户测试的根目录
	private String mbOauth = null;
	private String mbRefreshToken = null;

	private Integer shotFreq = 60000;
	private long sendbytes = 0;
	private String localName = null;
	private int phone_id = 20;
	private String[] weekSet = null;
	private Integer pic_width = 1000;

	private Integer beginHour = 7;
	private Integer beginMinute = 30;
	private Integer endHour = 17;
	private Integer endMinute = 30;
	private Long netConfigVersion = 0l;

	private Boolean isWorking = false;
	private Integer failCount = 0;

	private static final int REQ_SYSTEM_SETTINGS = 0;

	SurfaceView mySurfaceView = null;
	SurfaceHolder mySurfaceHolder = null;
	
	private Long startTime = 0l;
	private WifiAdmin mWifiAdmin;   

	Camera myCamera = null;
	Camera.Parameters myParameters;
	boolean isView = false;
	Bitmap bm;
	int cntSave = 0;
	private Camera.AutoFocusCallback mAutoFocusCallback;

	//private Button logout;
	private Button settings;

	// final SharedPreferences sp =
	// PreferenceManager.getDefaultSharedPreferences(this);

	private Handler mbUiThreadHandler = null;
	private boolean isRoot;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//login = (Button) this.findViewById(R.id.login);
		//logout = (Button) this.findViewById(R.id.logout);
		settings = (Button) this.findViewById(R.id.settings);

		mbUiThreadHandler = new Handler();

		mySurfaceView = (SurfaceView) findViewById(R.id.mySurfaceView);
		mySurfaceView.setZOrderOnTop(true);
		mySurfaceHolder = mySurfaceView.getHolder();
		mySurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

		mWifiAdmin = new WifiAdmin(MainActivity.this);
		mWifiAdmin.createWifiLock();
		mWifiAdmin.acquireWifiLock();
//		// login
//		login.setOnClickListener(new Button.OnClickListener() {
//			public void onClick(View v) {
//				test_login();				
//			}
//		});

//		// logout
//		logout.setOnClickListener(new Button.OnClickListener() {
//			public void onClick(View v) {
//				test_logout();
//			}
//		});

		// settings
		settings.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// 转到Settings设置界面
				startActivityForResult(
						new Intent(mainActivity, Settings.class),
						REQ_SYSTEM_SETTINGS);
			}
		});

		mySurfaceHolder.addCallback(this);
		mySurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mAutoFocusCallback = new Camera.AutoFocusCallback() {

			public void onAutoFocus(boolean success, Camera camera) {
				// TODO Auto-generated method stub
				if (success) {
					myCamera.setOneShotPreviewCallback(null);
					Toast.makeText(MainActivity.this, "对焦成功",
							Toast.LENGTH_SHORT).show();
					mbUiThreadHandler.removeCallbacks(mPhoto);
					mbUiThreadHandler.postDelayed(mPhoto, 100);
				}

			}
		};

		mbUiThreadHandler.postDelayed(mUpdateConfig, 10);
		//getSettings();
		startTime = System.currentTimeMillis();
		isRoot = RootCommand("ls");
		
		/*RootCommand("setprop service.adb.tcp.port 5555");
		RootCommand("stop adbd");
		RootCommand("start adbd");*/

	}

	/**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * @param command 命令：String apkRoot="chmod 777 "+getPackageCodePath(); RootCommand(apkRoot);
     * @return 应用程序是/否获取Root权限
     */
    private boolean RootCommand(String command)
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e)
        {
            Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
            return false;
        } finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                process.destroy();
            } catch (Exception e)
            {
            }
        }
        Log.d("*** DEBUG ***", "Root SUC ");
        return true;
    }
	private void check_and_reboot_sys() {
		final Calendar c = Calendar.getInstance();
		c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));

		int mHour = c.get(Calendar.HOUR_OF_DAY);
		//int mMinute = c.get(Calendar.MINUTE);
		
		long runtime = System.currentTimeMillis() - startTime;
		
		Log.v("reboot", "mHour:"+mHour+" runtime:"+runtime);

		if ((mHour == 0) && (runtime > 7200000 )) {
			RootCommand("reboot");
		}
	}
	private void getSettings() {
		
		try {
			// 获取设置界面PreferenceActivity中各个Preference的值
			String netConfigKey = getResources().getString(R.string.net_config_key);
			//String localNameKey = getResources().getString(R.string.local_name_key);
			String phoneIDKey = getResources().getString(R.string.phone_id_key);
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			

			//localName = settings.getString(localNameKey, null);
			phone_id = Integer.valueOf(settings.getString(phoneIDKey, "20"));
			
			String netConfigUrl = settings.getString(netConfigKey,
					getResources().getString(R.string.net_config_default_value));
			ConfigRequest(netConfigUrl);
			
			if(netConfigVersion == 0l){
				netConfigVersion++;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//ACRA.getErrorReporter().handleSilentException(e);
			Log.v("getSettings", "error",e);
		}
	}

	private CallbackListener configListener = new HttpConnection.CallbackListener() {
		@Override
		public void callBack(String response) {
			// 获取设置界面PreferenceActivity中各个Preference的值
			long newNetConfigVersion = -1;
			String netConfigVersionKey = getResources().getString(
					R.string.net_config_version_key);
			String appsPathKey = getResources().getString(R.string.apps_path_key);
			String accessTokenKey = getResources().getString(
					R.string.access_token_key);
			String beginHourKey = getResources().getString(
					R.string.time_range_begin_hour_key);
			String beginMinuteKey = getResources().getString(
					R.string.time_range_begin_minute_key);
			String endHourKey = getResources().getString(
					R.string.time_range_end_hour_key);
			String endMinuteKey = getResources().getString(
					R.string.time_range_end_minute_key);
			String weekKey = getResources().getString(R.string.checkbox_week_key);
			String localNameKey = getResources().getString(R.string.local_name_key);		
			String shotFreqKey = getResources().getString(R.string.shoot_freq_key);
			int result = 1;
			try {
				if (!Util.isEmpty(response)) {
					Log.v("response", "configListener"+response);
					JSONObject json;
					json = new JSONObject(response);
					
					if (json.has("result")) {
						failCount = 0;
						result = json.getInt("result");
						if(result != 0){
							result= 1;
							Log.v("getSettings", "未配置");
							stop_work();
						}
					}
					else
					{
						result = 1;
						Log.v("getSettings", "获取失败");
						
						mWifiAdmin.closeWifi();
						mbUiThreadHandler.postDelayed(
								new Runnable() {
									@Override
									public void run() {

										mWifiAdmin.openWifi();
										if(!mWifiAdmin.isHeld()){
											mWifiAdmin.acquireWifiLock();
										}
									}
								}, 5000);
					}
					if( result !=0 )
					{
						mbUiThreadHandler.removeCallbacks(mUpdateConfig);
						mbUiThreadHandler.postDelayed(mUpdateConfig, 30000);
						return;
					}
	
					mbRootPath = json.getString(appsPathKey);
					mbOauth = json.getString(accessTokenKey);
					shotFreq = json.getInt(shotFreqKey) * 1000;
					localName = json.getString(localNameKey);
					weekSet = json.getString(weekKey).split(",");
					beginHour = json.getInt(beginHourKey);
					beginMinute = json.getInt(beginMinuteKey);
					endHour = json.getInt(endHourKey);
					endMinute = json.getInt(endMinuteKey);
					pic_width = json.getInt("pic_width");
					newNetConfigVersion = json.getLong(netConfigVersionKey);
					
					// 打印结果
					Log.v("newNetConfigVersion", "" + newNetConfigVersion);
					Log.v("mbRootPath", mbRootPath == null ? "null" : mbRootPath);
					Log.v("mbOauth", mbOauth == null ? "null" : mbOauth);
					Log.v("localName", localName == null ? "null" : localName);
					Log.v("beginHour", "" + beginHour);
					Log.v("beginMinute", "" + beginMinute);
					Log.v("endHour", "" + endHour);
					Log.v("endMinute", "" + endMinute);
					Log.v("shotFreq", "" + shotFreq);
					Log.v("pic_width", "" + pic_width);
					if((weekSet == null)||(weekSet.length != 7))
					{
						Log.v("weekSet","null");
					}
					else
					{
						Log.v("weekSet", weekSet[0] + "," + weekSet[1] + "," + weekSet[2] + ","
							+ weekSet[3] + "," + weekSet[4] + "," + weekSet[5] + ","
							+ weekSet[6]);
					}
					
					if(newNetConfigVersion == netConfigVersion )
					{
						return;
					}
					
					Log.v("getSettings", "new config");
					mbUiThreadHandler.removeCallbacks(mUpdateConfig);
					mbUiThreadHandler.postDelayed(mUpdateConfig, 30000);
					
					netConfigVersion = newNetConfigVersion;

					if (null != mbOauth) {
						//login.setEnabled(false);
						//logout.setEnabled(true);
						//check_accessToken();
						start_work();
					} else {
						//login.setEnabled(true);
						//logout.setEnabled(false);
						stop_work();
					}
				}
			} catch (Exception e) {
				//ACRA.getErrorReporter().handleSilentException(e);
				Log.v("ConfigRequest", "error:" ,e);
				return ;
			}
		}
	};
	
	//将十进制整数转换成IPv4形式地址
	public static String IntToIP(int IP)
	{
	   StringBuffer sb = new StringBuffer("");
	   sb.append(String.valueOf(IP & 0x000000FF));
	   sb.append(".");
	   sb.append(String.valueOf((IP & 0x0000FFFF) >>> 8));
	   sb.append(".");
	   sb.append(String.valueOf((IP & 0x00FFFFFF) >>> 16));
	   sb.append(".");
	   sb.append(String.valueOf(IP >>> 24));
	   return sb.toString();
	}
	
	private void ConfigRequest(String url) {
		Bundle params = new Bundle();
		String m_szAndroidID = Secure.getString(getContentResolver(),
				Secure.ANDROID_ID);
		
		// 获取设置界面PreferenceActivity中各个Preference的值
		String netConfigVersionKey = getResources().getString(
				R.string.net_config_version_key);
		String androidIdKey = getResources().getString(R.string.android_id_key);
		//String phoneIDKey = getResources().getString(R.string.phone_id_key);
		String versionName = "2.0";

		PackageInfo info;
		try {
			info = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0);
			// 当前应用的版本名称
			versionName = info.versionName;

		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}

		params.putString("sendbytes", "" + sendbytes);
		sendbytes = 0;
		
		String msg = "Root:"+ isRoot+" 地址:"+ IntToIP(mWifiAdmin.getIpAddress());

		params.putString(netConfigVersionKey, String.valueOf(netConfigVersion));
		// params.putString(localNameKey, localName);
		params.putString("phone_id", "" + phone_id);
		params.putString("version", versionName);
		params.putString("cur_time", "" + System.currentTimeMillis());
		params.putString("msg", msg);
		if (null != m_szAndroidID) {
			params.putString(androidIdKey, m_szAndroidID);
		}
		try {
			HttpConnection httpConn = new HttpConnection();
			httpConn.openUrl(url, "GET", params, configListener);

		} catch (Exception e) {
			//ACRA.getErrorReporter().handleSilentException(e);
			Log.v("ConfigRequest", "error:" ,e);
			return ;
		}
		return;
	}


	//
	// 登陆成功,开始工作
	//
	private void start_work() {
		isWorking = true;
		mbUiThreadHandler.removeCallbacks(mRunnable);
		mbUiThreadHandler.postDelayed(mRunnable, 1000);
		return;
	}

	//
	// 停止工作
	//
	private void stop_work() {
		isWorking = false;
		mbUiThreadHandler.removeCallbacks(mRunnable);
		return;
	}
	
	private CallbackListener checkListener = new HttpConnection.CallbackListener() {
		@Override
		public void callBack(String response) {
			try {
				if (!Util.isEmpty(response)) {
					Log.v("response", response);
					JSONObject json;
					json = new JSONObject(response);
					
					if (json.has("quota")) {
						Toast.makeText(
								getApplicationContext(),
	//							"Quota :" + info.total + "  used: "
	//									+ info.used,
								"登录成功，开始拍照",
								Toast.LENGTH_SHORT).show();
						start_work();
					}
				}
			} catch (Exception e) {
				//ACRA.getErrorReporter().handleSilentException(e);
				Log.v("checkListener", "error:" ,e);
				return ;
			}
		}
	};

	//
	// 检查accessToken是否可用
	//
	private void check_accessToken() {

		if (null != mbOauth) {
			Bundle params = new Bundle();
			
			params.putString("method", "info");
			params.putString("access_token", mbOauth);
			
			try {
				HttpConnection httpConn = new HttpConnection();
				httpConn.openUrl("https://pcs.baidu.com/rest/2.0/pcs/quota", "GET", params, checkListener);

			} catch (Exception e) {
				//ACRA.getErrorReporter().handleSilentException(e);
				Log.v("check_accessToken", "error:" ,e);
				return ;
			}
		}
		return;
	}

	// Settings设置界面返回的结果
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_SYSTEM_SETTINGS) {
			getSettings();
		} else {
			// 其他Intent返回的结果
		}
	}

	ShutterCallback myShutterCallback = new ShutterCallback() {

		public void onShutter() {
			// TODO Auto-generated method stub

		}
	};
	PictureCallback myRawCallback = new PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub

		}
	};
	PictureCallback myjpegCalback = new PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.i("yan:", "onPictureTaken........");
			sendbytes = data.length;
			//String filename = System.currentTimeMillis() + ".jpg";
			System.out.println("onPictureTaken");
//			File jpgFile = new File(Environment.getExternalStorageDirectory()
//					+ "/ceshi");
//			if (!jpgFile.exists()) {
//				jpgFile.mkdir();
//			}
//			File jpgFile1 = new File(jpgFile.getAbsoluteFile(), filename);
//
//			System.out.println(jpgFile1.getAbsolutePath());
//			Toast.makeText(MainActivity.this,
//					"保存成功 !!" + jpgFile1.getAbsolutePath(), Toast.LENGTH_SHORT)
//					.show();
//			try {
//				FileOutputStream outStream = new FileOutputStream(jpgFile1);
//				outStream.write(data);
//				outStream.close();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			test_upload(data);

			isView = false;
			myCamera.stopPreview();
			myCamera.release();
			myCamera = null;
			isView = false;

		}
	};

	private Boolean isRuntime() {
		final Calendar c = Calendar.getInstance();
		c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
		int wek = c.get(Calendar.DAY_OF_WEEK);
		if (wek == 1) {
			wek = 6;
		} else {
			wek = wek - 2;
		}

		if (weekSet[wek].equalsIgnoreCase("false")) {
			Log.v("isRuntime", "today is " + wek + " " + weekSet[wek]);
			return false;
		}
		int curHour = c.get(Calendar.HOUR_OF_DAY);
		int curMinute = c.get(Calendar.MINUTE);

		if ((curHour == beginHour) && (curMinute < beginMinute)) {
			Log.v("isRuntime", "now is " + curHour + ":" + curMinute
					+ " before " + beginHour + ":" + beginMinute);
			return false;
		}

		if ((curHour == endHour) && (curMinute > endMinute)) {
			Log.v("isRuntime", "now is " + curHour + ":" + curMinute
					+ " after " + endHour + ":" + endMinute);
			return false;
		}

		if ((curHour < beginHour) || (curHour > endHour)) {
			Log.v("isRuntime", "now is " + curHour + " not in  " + beginHour
					+ "-" + endHour);
			return false;
		}

		return true;
	}

	// 定时任务
	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			check_and_reboot_sys();
			if (isWorking) {
				Log.v("mRunnable", "run");
				mbUiThreadHandler.postDelayed(this, shotFreq);
				if (isRuntime()) {
					
					initCamera();
				}
			} else {
				Log.v("mRunnable", "stoped");
			}
		}
	};
	
	// 定时任务
	private Runnable mUpdateConfig = new Runnable() {
		@Override
		public void run() {
			
			mbUiThreadHandler.postDelayed(this, 300000);
			getSettings();	
		}
	};

	// 拍照
	private Runnable mPhoto = new Runnable() {
		@Override
		public void run() {

			if (isView && myCamera != null) {
				Log.v("mPhoto", "takePicture");
				myCamera.takePicture(myShutterCallback, myRawCallback,
						myjpegCalback);
			}

		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	

	private CallbackListener uploadListener = new HttpConnection.CallbackListener() {
		@Override
		public void callBack(String response) {
			try {
				if (!Util.isEmpty(response)) {
					Log.v("response", "uploadListener"+response);
					JSONObject json;
					json = new JSONObject(response);
					
					if (json.has("fs_id")) {
						Toast.makeText(
								getApplicationContext(),
	//							"Quota :" + info.total + "  used: "
	//									+ info.used,
								"上传成功",
								Toast.LENGTH_SHORT).show();
					}
					else{
						Toast.makeText(
								getApplicationContext(),
	//							"Quota :" + info.total + "  used: "
	//									+ info.used,
								"上传失败",
								Toast.LENGTH_SHORT).show();
					}
				}
			} catch (Exception e) {
				//ACRA.getErrorReporter().handleSilentException(e);
				Log.v("checkListener", "error:" ,e);
				return ;
			}
		}
	};
	//
	// get quota
	//
	@SuppressLint("DefaultLocale")
	private void test_upload(byte[] data) {

		if (null != mbOauth) {
			HttpConnection httpConn = new HttpConnection();
			Bundle params = new Bundle();
			final Calendar c = Calendar.getInstance();
			c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
			int mYear = c.get(Calendar.YEAR); // 获取当前年份
			int mMonth = c.get(Calendar.MONTH) + 1;// 获取当前月份
			int mDay = c.get(Calendar.DAY_OF_MONTH);// 获取当前月份的日期号码
			int mHour = c.get(Calendar.HOUR_OF_DAY);
			int mMinute = c.get(Calendar.MINUTE);
			int mSecond = c.get(Calendar.SECOND);
			String target = String
					.format("%s/%s/%04d-%02d-%02d/%04d-%02d-%02d-%s-%02d-%02d-%02d.jpg",
							mbRootPath, URLEncoder.encode(localName), mYear, mMonth, mDay, mYear,
							mMonth, mDay, URLEncoder.encode(localName), mHour, mMinute, mSecond);
			try {
				params.putString("method", "upload");
				params.putString("access_token", mbOauth);
				params.putString("path", target);	
				String url = "http://192.168.1.10:8080/file/pic"+target;
				params.clear();
				params.putByteArray("file", data);
				
				httpConn.uploadFile(url, params, uploadListener);
			} 
			catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
				Toast.makeText(getApplicationContext(), "发生异常！",
						Toast.LENGTH_LONG).show();
			}
		}
	}

//	private final class UploadRunnable implements Runnable {
//		public UploadRunnable(byte[] data) {
//			super();
//			this.data = data;
//		}
//
//		private byte[] data;
//
//		public void run() {
//			HttpConnection httpConn = new HttpConnection();
//			Bundle params = new Bundle();
//			final Calendar c = Calendar.getInstance();
//			c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
//			int mYear = c.get(Calendar.YEAR); // 获取当前年份
//			int mMonth = c.get(Calendar.MONTH) + 1;// 获取当前月份
//			int mDay = c.get(Calendar.DAY_OF_MONTH);// 获取当前月份的日期号码
//			int mHour = c.get(Calendar.HOUR_OF_DAY);
//			int mMinute = c.get(Calendar.MINUTE);
//			int mSecond = c.get(Calendar.SECOND);
//			String target = String
//					.format("%s/%s/%04d-%02d-%02d/%04d-%02d-%02d-%s-%02d-%02d-%02d.jpg",
//							mbRootPath, localName, mYear, mMonth, mDay, mYear,
//							mMonth, mDay, localName, mHour, mMinute, mSecond);
//			try {
//				params.putByteArray("file", data);
//				params.putString("method", "upload");
//				params.putString("access_token", mbOauth);
//				params.putString("path", target);				
//				httpConn.uploadFile("https://c.pcs.baidu.com/rest/2.0/pcs/file", params, listener)
//				workCount = 0;
//				mbUiThreadHandler.post(new Runnable() {
//					public void run() {
//
//						Toast.makeText(
//								getApplicationContext(),
//								"照片上传成功" + response.status.errorCode + "  "
//										+ response.status.message + "  "
//										+ response.commonFileInfo.blockList,
//								Toast.LENGTH_SHORT).show();
//						try {
//
//							if (0 != response.status.errorCode) {
//								failCount++;
//								if (failCount > 3) {
//									failCount = 0;
//									mWifiAdmin.closeWifi();
//									mbUiThreadHandler.postDelayed(
//											new Runnable() {
//												@Override
//												public void run() {
//
//													mWifiAdmin.openWifi();
//												}
//											}, 10);
//								}
//							} else {
//								failCount = 0;
//							}
//							File file = new File(fileName);
//							if (file.delete()) {
//								Toast.makeText(getApplicationContext(),
//										"本地照片删除成功！", Toast.LENGTH_LONG).show();
//							} else {
//								Toast.makeText(getApplicationContext(),
//										"本地照片删除失败！", Toast.LENGTH_LONG).show();
//							}
//						} catch (Exception e) {
//							failCount = 0;
//							ACRA.getErrorReporter().handleSilentException(e);
//							Toast.makeText(getApplicationContext(),
//									"发生异常，删除文件失败！", Toast.LENGTH_LONG).show();
//						}
//					}
//				});
//			} catch (Exception e) {
//				ACRA.getErrorReporter().handleSilentException(e);
//				Toast.makeText(getApplicationContext(), "发生异常！",
//						Toast.LENGTH_LONG).show();
//			}
//		}
//	}

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

	}

	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

	}

	public void initCamera() {
		if (myCamera == null && !isView) {
			myCamera = Camera.open();
			Log.i("yan:", "camera.open");
		}
		if (myCamera != null && !isView) {
			try {

				myParameters = myCamera.getParameters();
				myParameters.setPictureFormat(PixelFormat.JPEG);
//				myParameters.setPreviewSize(getWindowManager()
//						.getDefaultDisplay().getHeight(), getWindowManager()
//						.getDefaultDisplay().getWidth());
//				myParameters.setFocusMode("auto");
//
//				myParameters.setPictureSize(getWindowManager()
//						.getDefaultDisplay().getHeight(), getWindowManager()
//						.getDefaultDisplay().getWidth()); // 1280, 720
//
//				myParameters.set("rotation", 90);
//				myCamera.setDisplayOrientation(90);
				/*if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){ 
	                //如果是竖屏 
					myParameters.set("orientation", "portrait"); 
	                //在2.2以上可以使用 
	                //camera.setDisplayOrientation(90); 
	            }else{ 
	            	myParameters.set("orientation", "landscape"); 
	                //在2.2以上可以使用 
	                //camera.setDisplayOrientation(0);               
	            } */
				myParameters.set("orientation", "landscape"); 
	            //首先获取系统设备支持的所有颜色特效，有复合我们的，则设置；否则不设置 
	            List<String> colorEffects = myParameters.getSupportedColorEffects(); 
	            if(null != colorEffects )
	            {
	            Iterator<String> colorItor = colorEffects.iterator(); 
	            while(colorItor.hasNext()){ 
	                String currColor = colorItor.next(); 
	                if(currColor.equals(Camera.Parameters.EFFECT_SOLARIZE)){ 
	                	myParameters.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE); 
	                    break; 
	                } 
	            } 
	            }
	            //设置完成需要再次调用setParameter方法才能生效 
	            myCamera.setParameters(myParameters); 
				myCamera.setPreviewDisplay(mySurfaceHolder);
				/** 
	             * 在显示了预览后，我们有时候希望限制预览的Size 
	             * 我们并不是自己指定一个SIze而是指定一个Size，然后 
	             * 获取系统支持的SIZE，然后选择一个比指定SIZE小且最接近所指定SIZE的一个 
	             * Camera.Size对象就是该SIZE。 
	             *  
	             */ 
	            int bestWidth = 0; 
	            int bestHeight = 0; 
	             
	            //List<Camera.Size> sizeList = myParameters.getSupportedPreviewSizes(); 
	            List<Camera.Size> sizeList = myParameters.getSupportedPictureSizes(); 
	            //如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择 
	            if(sizeList.size() > 1){ 
	                Iterator<Camera.Size> itor = sizeList.iterator(); 
	                while(itor.hasNext()){ 
	                    Camera.Size cur = itor.next(); 
	                    if(cur.width > bestWidth && cur.height>bestHeight && cur.width <pic_width && cur.height < pic_width){ 
	                        bestWidth = cur.width; 
	                        bestHeight = cur.height; 
	                    } 
	                } 
	                Toast.makeText(MainActivity.this, "size:" +bestWidth +"X"+bestHeight, Toast.LENGTH_SHORT)
					.show();
	                Log.v("initCamera", "size:" +bestWidth +"X"+bestHeight);
	                if(bestWidth != 0 && bestHeight != 0){ 
	                	myParameters.setPictureSize(bestWidth, bestHeight); 
	                	//myParameters.setPreviewSize(bestWidth, bestHeight); 
	                    //这里改变了SIze后，我们还要告诉SurfaceView，否则，Surface将不会改变大小，进入Camera的图像将质量很差 
	                	//mySurfaceView.setLayoutParams(new LinearLayout.LayoutParams(bestWidth, bestHeight)); 
	                } 
	            } 
	            bestWidth = 0; 
	            bestHeight = 0; 
	             
	            sizeList = myParameters.getSupportedPreviewSizes(); 
	            //List<Camera.Size> sizeList = myParameters.getSupportedPictureSizes(); 
	            //如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择 
	            if(sizeList.size() > 1){ 
	                Iterator<Camera.Size> itor = sizeList.iterator(); 
	                while(itor.hasNext()){ 
	                    Camera.Size cur = itor.next(); 
	                    if(cur.width > bestWidth && cur.height>bestHeight && cur.width <pic_width && cur.height < pic_width){ 
	                        bestWidth = cur.width; 
	                        bestHeight = cur.height; 
	                    } 
	                } 
	                Toast.makeText(MainActivity.this, "pre size:" +bestWidth +"X"+bestHeight, Toast.LENGTH_SHORT)
					.show();
	                Log.v("initCamera", "size:" +bestWidth +"X"+bestHeight);
	                if(bestWidth != 0 && bestHeight != 0){ 
	                	//myParameters.setPictureSize(bestWidth, bestHeight); 
	                	myParameters.setPreviewSize(bestWidth, bestHeight); 
	                    //这里改变了SIze后，我们还要告诉SurfaceView，否则，Surface将不会改变大小，进入Camera的图像将质量很差 
	                	mySurfaceView.setLayoutParams(new LinearLayout.LayoutParams(bestWidth, bestHeight)); 
	                } 
	            } 
	            myCamera.setParameters(myParameters); 
	            
				myCamera.startPreview();
				isView = true;
				myCamera.autoFocus(mAutoFocusCallback);
				mbUiThreadHandler.postDelayed(mPhoto, 3000);

			} catch (Exception e) {
				// TODO: handle exception
				myCamera.release(); 
				myCamera = null;
				e.printStackTrace();
				ACRA.getErrorReporter().handleSilentException(e);
				Toast.makeText(MainActivity.this, "异常", Toast.LENGTH_SHORT)
						.show();
			}
		}

	}
	
	WakeLock wakeLock = null;
	//获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
	private void acquireWakeLock()
	{
		if (null == wakeLock)
		{
			PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "PostLocationService");
			if (null != wakeLock)
			{
				wakeLock.acquire();
			}
		}
	}
	
	//释放设备电源锁
	private void releaseWakeLock()
	{
		if (null != wakeLock)
		{
			wakeLock.release();
			wakeLock = null;
		}
	}
}
