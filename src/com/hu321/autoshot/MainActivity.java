package com.hu321.autoshot;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.acra.ACRA;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

import com.baidu.oauth.BaiduOAuth;
import com.baidu.oauth.BaiduOAuth.BaiduOAuthResponse;
import com.baidu.oauth.BaiduOAuth.OAuthListener;
import com.baidu.pcs.BaiduPCSActionInfo;
import com.baidu.pcs.BaiduPCSClient;
import com.baidu.pcs.BaiduPCSStatusListener;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;


public class MainActivity extends Activity implements SurfaceHolder.Callback {
	protected final Context mainActivity = this;
	private String mbApiKey = "";// ���滻����ͻ���Ӧ��ʱ��ȡ��Api Key��
	private String mbRootPath = ""; // �û����Եĸ�Ŀ¼
	private String mbOauth = null;
	private String mbSecretKey = null;
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
	private Integer workCount = 0;
	private Integer failCount = 0;

	private static final int REQ_SYSTEM_SETTINGS = 0;

	SurfaceView mySurfaceView = null;
	SurfaceHolder mySurfaceHolder = null;
	
	private WifiAdmin mWifiAdmin;   

	Camera myCamera = null;
	Camera.Parameters myParameters;
	boolean isView = false;
	Bitmap bm;
	int cntSave = 0;
	private Camera.AutoFocusCallback mAutoFocusCallback;

	private Button login;
	//private Button logout;
	private Button settings;

	// final SharedPreferences sp =
	// PreferenceManager.getDefaultSharedPreferences(this);

	private Handler mbUiThreadHandler = null;

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
				// ת��Settings���ý���
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
					Toast.makeText(MainActivity.this, "�Խ��ɹ�",
							Toast.LENGTH_SHORT).show();
					mbUiThreadHandler.removeCallbacks(mPhoto);
					mbUiThreadHandler.postDelayed(mPhoto, 100);
				}

			}
		};

		mbUiThreadHandler.postDelayed(mUpdateConfig, 10);
		//getSettings();
		try{
			throw new Exception("��������");
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}

	}

	private void getSettings() {
		
		try {
			// ��ȡ���ý���PreferenceActivity�и���Preference��ֵ
			String netConfigKey = getResources().getString(R.string.net_config_key);
			//String localNameKey = getResources().getString(R.string.local_name_key);
			String phoneIDKey = getResources().getString(R.string.phone_id_key);
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			

			//localName = settings.getString(localNameKey, null);
			phone_id = Integer.valueOf(settings.getString(phoneIDKey, "20"));
			
			String netConfigUrl = settings.getString(netConfigKey,
					getResources().getString(R.string.net_config_default_value));
			long newNetConfigVersion = ConfigRequest(netConfigUrl);
			
			if( newNetConfigVersion < 1l)
			{
				if( newNetConfigVersion == 0l){
					Log.v("getSettings", "δ����");
					stop_work();
				}
				else{
					Log.v("getSettings", "��ȡʧ��");
				}
				if(netConfigVersion == 0l){
					netConfigVersion++;
				}
				
				mbUiThreadHandler.removeCallbacks(mUpdateConfig);
				mbUiThreadHandler.postDelayed(mUpdateConfig, 30000);
				return;
			}

			// ��ӡ���
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
				check_accessToken();
			} else {
				//login.setEnabled(true);
				//logout.setEnabled(false);
				stop_work();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//ACRA.getErrorReporter().handleSilentException(e);
			Log.v("getSettings", "error",e);
		}
	}

	private long ConfigRequest(String url) {
		Bundle params = new Bundle();
		String response = null;
		String m_szAndroidID = Secure.getString(getContentResolver(),
				Secure.ANDROID_ID);
		long newNetConfigVersion = -1;

		// ��ȡ���ý���PreferenceActivity�и���Preference��ֵ
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
		String androidIdKey = getResources().getString(R.string.android_id_key);
		//String phoneIDKey = getResources().getString(R.string.phone_id_key);
		String versionName = "2.0";

		PackageInfo info;
		try {
			info = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0);
			// ��ǰӦ�õİ汾����
			versionName = info.versionName;

		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}

		params.putString("sendbytes", "" + sendbytes);
		sendbytes = 0;

		params.putString(netConfigVersionKey, String.valueOf(netConfigVersion));
		// params.putString(localNameKey, localName);
		params.putString("phone_id", "" + phone_id);
		params.putString("version", versionName);
		params.putString("cur_time", "" + System.currentTimeMillis());
		if (null != m_szAndroidID) {
			params.putString(androidIdKey, m_szAndroidID);
		}
		try {
			response = Util.openUrl(url, "GET", params);
			Util.checkResponse(response);
			int result = 1;

			if (!Util.isEmpty(response)) {
				Log.v("response", response);
				JSONObject json;
				json = new JSONObject(response);
				
				if (json.has("result")) {
					result = json.getInt("result");
					if(result != 0){
						return 0;
					}
				}
				else{
					return -1;
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
			}

		} catch (Exception e) {
			//ACRA.getErrorReporter().handleSilentException(e);
			Log.v("ConfigRequest", "error:" ,e);
			return -1;
		}
		return newNetConfigVersion;
	}


	//
	// ��½�ɹ�,��ʼ����
	//
	private void start_work() {
		isWorking = true;
		mbUiThreadHandler.removeCallbacks(mRunnable);
		mbUiThreadHandler.postDelayed(mRunnable, 1000);
		return;
	}

	//
	// ֹͣ����
	//
	private void stop_work() {
		isWorking = false;
		mbUiThreadHandler.removeCallbacks(mRunnable);
		return;
	}

	//
	// ���accessToken�Ƿ����
	//
	private void check_accessToken() {

		final SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String accessTokenKey = getResources().getString(
				R.string.access_token_key);
		final String refreshTokenKey = getResources().getString(
				R.string.refresh_token_key);

		if (null != mbOauth) {

			Thread workThread = new Thread(new Runnable() {
				public void run() {
					BaiduPCSClient api = new BaiduPCSClient();
					api.setAccessToken(mbOauth);
					final BaiduPCSActionInfo.PCSQuotaResponse info = api
							.quota();

					mbUiThreadHandler.post(new Runnable() {
						public void run() {
							if (null != info) {
								if (0 == info.status.errorCode) {
									Toast.makeText(
											getApplicationContext(),
//											"Quota :" + info.total + "  used: "
//													+ info.used,
											"��¼�ɹ�����ʼ����",
											Toast.LENGTH_SHORT).show();
									start_work();
								} else {
									Toast.makeText(
											getApplicationContext(),
											"Quota failed: "
													+ info.status.errorCode
													+ "  "
													+ info.status.message,
											Toast.LENGTH_SHORT).show();
									//login.setEnabled(true);
									//logout.setEnabled(false);
									/*mbOauth = null;
									mbRefreshToken = null;
									SharedPreferences.Editor editor = sp.edit();
									editor.putString(accessTokenKey, mbOauth);
									editor.putString(refreshTokenKey,
											mbRefreshToken);
									editor.commit();*/
								}
							}
						}
					});
				}
			});

			workThread.start();
		}
	}

	// Settings���ý��淵�صĽ��
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_SYSTEM_SETTINGS) {
			getSettings();
		} else {
			// ����Intent���صĽ��
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
			String filename = System.currentTimeMillis() + ".jpg";
			System.out.println("onPictureTaken");
			File jpgFile = new File(Environment.getExternalStorageDirectory()
					+ "/ceshi");
			if (!jpgFile.exists()) {
				jpgFile.mkdir();
			}
			File jpgFile1 = new File(jpgFile.getAbsoluteFile(), filename);

			System.out.println(jpgFile1.getAbsolutePath());
			Toast.makeText(MainActivity.this,
					"����ɹ� !!" + jpgFile1.getAbsolutePath(), Toast.LENGTH_SHORT)
					.show();
			try {
				FileOutputStream outStream = new FileOutputStream(jpgFile1);
				outStream.write(data);
				outStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			test_upload(jpgFile1.getAbsolutePath());

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

	// ��ʱ����
	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			if (isWorking) {
				Log.v("mRunnable", "run");
				mbUiThreadHandler.postDelayed(this, shotFreq);
				if (isRuntime()) {
					if(workCount > 0)
					{
						if(workCount * shotFreq > 60*1000*5)
						{
							workCount = 0;
						}
						else
						{
							workCount++;
							return;
						}
						
					}
					workCount++;
					initCamera();
				}
			} else {
				Log.v("mRunnable", "stoped");
			}
		}
	};
	
	// ��ʱ����
	private Runnable mUpdateConfig = new Runnable() {
		@Override
		public void run() {
			
			mbUiThreadHandler.postDelayed(this, 300000);
			getSettings();	
		}
	};

	// ����
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

	private void test_login() {
		// ȡ����������Ӧ�ó����SharedPreferences
		final SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String accessTokenKey = getResources().getString(
				R.string.access_token_key);
		final String refreshTokenKey = getResources().getString(
				R.string.refresh_token_key);

		BaiduOAuth oauthClient = new BaiduOAuth();
		oauthClient.startOAuth(this, mbApiKey, new String[] { "basic",
				"netdisk" }, new BaiduOAuth.OAuthListener() {
			@Override
			public void onException(String msg) {
				Toast.makeText(getApplicationContext(), "Login failed " + msg,
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onComplete(BaiduOAuthResponse response) {
				if (null != response) {
					mbOauth = response.getAccessToken();
					mbRefreshToken = response.getRefreshToken();
					Log.v("mbOauth", mbOauth == null ? "null" : mbOauth);
					Log.v("mbRefreshToken", mbRefreshToken == null ? "null"
							: mbRefreshToken);
					Toast.makeText(
							getApplicationContext(),
							"Token: " + mbOauth + "Refresh: " + mbRefreshToken
									+ "    User name:" + response.getUserName(),
							Toast.LENGTH_SHORT).show();
					// ��ȡ���ý���PreferenceActivity�и���Preference��ֵ

					SharedPreferences.Editor editor = sp.edit();
					editor.putString(accessTokenKey, mbOauth);
					editor.putString(refreshTokenKey, mbRefreshToken);
					editor.commit();
					//login.setEnabled(false);
					//logout.setEnabled(true);
					start_work();
				}
			}

			@Override
			public void onCancel() {
				Toast.makeText(getApplicationContext(), "Login cancelled",
						Toast.LENGTH_SHORT).show();
				stop_work();
			}
		});
	}

	//
	// logout
	//
	public void test_logout() {
		final SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String accessTokenKey = getResources().getString(
				R.string.access_token_key);
		final String refreshTokenKey = getResources().getString(
				R.string.refresh_token_key);

		if (null != mbOauth) {

			Thread workThread = new Thread(new Runnable() {
				@Override
				public void run() {

					BaiduOAuth oauth = new BaiduOAuth();
					final boolean ret = oauth.logout(mbOauth);
					mbUiThreadHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(),
									"Logout " + ret, Toast.LENGTH_SHORT).show();
							mbOauth = null;
							mbRefreshToken = null;
							SharedPreferences.Editor editor = sp.edit();
							editor.putString(accessTokenKey, mbOauth);
							editor.putString(refreshTokenKey, mbRefreshToken);
							editor.commit();
							//login.setEnabled(true);
							//logout.setEnabled(false);
							stop_work();
						}
					});

				}
			});

			workThread.start();
		}

	}

	//
	// get quota
	//
	private void test_upload(String fileName) {

		if (null != mbOauth) {

			Thread workThread = new Thread(new UploadRunnable(fileName));

			workThread.start();
		}
	}

	private final class UploadRunnable implements Runnable {
		public UploadRunnable(String fileName) {
			super();
			this.fileName = fileName;
		}

		private String fileName;

		public void run() {
			BaiduPCSClient api = new BaiduPCSClient();
			api.setAccessToken(mbOauth);

			final Calendar c = Calendar.getInstance();
			c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
			int mYear = c.get(Calendar.YEAR); // ��ȡ��ǰ���
			int mMonth = c.get(Calendar.MONTH) + 1;// ��ȡ��ǰ�·�
			int mDay = c.get(Calendar.DAY_OF_MONTH);// ��ȡ��ǰ�·ݵ����ں���
			int mHour = c.get(Calendar.HOUR_OF_DAY);
			int mMinute = c.get(Calendar.MINUTE);
			int mSecond = c.get(Calendar.SECOND);
			String target = String
					.format("%s/%s/%04d-%02d-%02d/%04d-%02d-%02d-%s-%02d-%02d-%02d.jpg",
							mbRootPath, localName, mYear, mMonth, mDay, mYear,
							mMonth, mDay, localName, mHour, mMinute, mSecond);
			try {
				final BaiduPCSActionInfo.PCSFileInfoResponse response = api
						.uploadFile(fileName, target,
								new BaiduPCSStatusListener() {

									@Override
									public void onProgress(long bytes,
											long total) {
										// TODO Auto-generated method stub

										final long bs = bytes;
										final long tl = total;
										// sendbytes += bytes;

										mbUiThreadHandler.post(new Runnable() {
											public void run() {
												// Toast.makeText(getApplicationContext(),
												// "total: " + tl + "    sent:"
												// + bs,
												// Toast.LENGTH_SHORT).show();
											}
										});
									}

									@Override
									public long progressInterval() {
										return 1000;
									}
								});
				workCount = 0;
				mbUiThreadHandler.post(new Runnable() {
					public void run() {

						Toast.makeText(
								getApplicationContext(),
								"��Ƭ�ϴ��ɹ�" + response.status.errorCode + "  "
										+ response.status.message + "  "
										+ response.commonFileInfo.blockList,
								Toast.LENGTH_SHORT).show();
						try {

							if (0 != response.status.errorCode) {
								failCount++;
								if (failCount > 3) {
									failCount = 0;
									mWifiAdmin.closeWifi();
									mbUiThreadHandler.postDelayed(
											new Runnable() {
												@Override
												public void run() {

													mWifiAdmin.openWifi();
												}
											}, 10);
								}
							} else {
								failCount = 0;
							}
							File file = new File(fileName);
							if (file.delete()) {
								Toast.makeText(getApplicationContext(),
										"������Ƭɾ���ɹ���", Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(getApplicationContext(),
										"������Ƭɾ��ʧ�ܣ�", Toast.LENGTH_LONG).show();
							}
						} catch (Exception e) {
							failCount = 0;
							ACRA.getErrorReporter().handleSilentException(e);
							Toast.makeText(getApplicationContext(),
									"�����쳣��ɾ���ļ�ʧ�ܣ�", Toast.LENGTH_LONG).show();
						}
					}
				});
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
				Toast.makeText(getApplicationContext(), "�����쳣��",
						Toast.LENGTH_LONG).show();
			}
		}
	}

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
	                //��������� 
					myParameters.set("orientation", "portrait"); 
	                //��2.2���Ͽ���ʹ�� 
	                //camera.setDisplayOrientation(90); 
	            }else{ 
	            	myParameters.set("orientation", "landscape"); 
	                //��2.2���Ͽ���ʹ�� 
	                //camera.setDisplayOrientation(0);               
	            } */
				myParameters.set("orientation", "landscape"); 
	            //���Ȼ�ȡϵͳ�豸֧�ֵ�������ɫ��Ч���и������ǵģ������ã��������� 
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
	            //���������Ҫ�ٴε���setParameter����������Ч 
	            myCamera.setParameters(myParameters); 
				myCamera.setPreviewDisplay(mySurfaceHolder);
				/** 
	             * ����ʾ��Ԥ����������ʱ��ϣ������Ԥ����Size 
	             * ���ǲ������Լ�ָ��һ��SIze����ָ��һ��Size��Ȼ�� 
	             * ��ȡϵͳ֧�ֵ�SIZE��Ȼ��ѡ��һ����ָ��SIZEС����ӽ���ָ��SIZE��һ�� 
	             * Camera.Size������Ǹ�SIZE�� 
	             *  
	             */ 
	            int bestWidth = 0; 
	            int bestHeight = 0; 
	             
	            //List<Camera.Size> sizeList = myParameters.getSupportedPreviewSizes(); 
	            List<Camera.Size> sizeList = myParameters.getSupportedPictureSizes(); 
	            //���sizeListֻ��һ������Ҳû�б�Ҫ��ʲô�ˣ���Ϊ����һ������ѡ�� 
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
	                    //����ı���SIze�����ǻ�Ҫ����SurfaceView������Surface������ı��С������Camera��ͼ�������ܲ� 
	                	//mySurfaceView.setLayoutParams(new LinearLayout.LayoutParams(bestWidth, bestHeight)); 
	                } 
	            } 
	            bestWidth = 0; 
	            bestHeight = 0; 
	             
	            sizeList = myParameters.getSupportedPreviewSizes(); 
	            //List<Camera.Size> sizeList = myParameters.getSupportedPictureSizes(); 
	            //���sizeListֻ��һ������Ҳû�б�Ҫ��ʲô�ˣ���Ϊ����һ������ѡ�� 
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
	                    //����ı���SIze�����ǻ�Ҫ����SurfaceView������Surface������ı��С������Camera��ͼ�������ܲ� 
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
				Toast.makeText(MainActivity.this, "�쳣", Toast.LENGTH_SHORT)
						.show();
			}
		}

	}
	
	WakeLock wakeLock = null;
	//��ȡ��Դ�������ָ÷�������ĻϨ��ʱ��Ȼ��ȡCPUʱ����������
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
	
	//�ͷ��豸��Դ��
	private void releaseWakeLock()
	{
		if (null != wakeLock)
		{
			wakeLock.release();
			wakeLock = null;
		}
	}
}
