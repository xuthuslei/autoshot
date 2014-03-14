package com.hu321.autoshot;

import org.acra.ACRA;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Asynchronous HTTP connections
 * 
 * @author Greg Zavitz & Joseph Roth
 */
public class HttpConnection implements Runnable {
	public static final int DID_START = 0;
	public static final int DID_ERROR = 1;
	public static final int DID_SUCCEED = 2;

	private static final int OPEN_URL = 0;
	private static final int UPLOAD_FILE = 1;
	private Boolean isStop = true;
	private Thread mThread;
	private String method;
	private String url;
	private int operate;
	private int trynum;
	private Bundle params;
	private CallbackListener listener;
	
	public void create(int operate, String url, String method, Bundle params, CallbackListener listener, int trynum) {
		this.operate = operate;
		this.method = method;
		this.url = url;
		this.params = params;
		this.listener = listener;
		this.trynum = trynum;
		ConnectionManager.getInstance().push(this);
	}
	
	public interface CallbackListener {
		public void callBack(String result);
	}
	public void openUrl(String url, String method, Bundle params, CallbackListener listener) {
		create(OPEN_URL, url, method, params, listener, 5);
	}
	public void uploadFile(String url, Bundle params, CallbackListener listener) {
		create(UPLOAD_FILE, url, null, params, listener, 2);
	}
	
	public void start() {
        if (mThread == null) {
            mThread = new Thread(this);
            mThread.start();
            isStop = false;
        }
    }

    public void stop() {
        if (mThread != null) {
        	isStop = true;
            mThread.interrupt();
            mThread = null;
        }
    }

	private static final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {

			CallbackListener listener = (CallbackListener) message.obj;
			Object data = message.getData();
			if (listener != null) {
				if (data != null) {
					Bundle bundle = (Bundle) data;
					String result = bundle.getString("callbackkey");
					listener.callBack(result);
				}
			}
		}
	};

	@Override
	public void run() {
//		handler.sendMessage(Message.obtain(handler, HttpConnection.DID_START));
		int count = 0;
		while(!isStop)
		{
			try {
				String response = null;
				switch (operate) {
				case OPEN_URL:
					response = Util.openUrl(url, method, params);
					this.sendMessage(response);
					break;
				case UPLOAD_FILE:
					response = Util.uploadFile(url, params);
					this.sendMessage(response);
					break;
				}
				break;
			} catch (Exception e) {
				count++;
				if(count >this.trynum)
				{
					//ACRA.getErrorReporter().handleSilentException(e);
					this.sendMessage("{\"fail\":0}");
					break;
				}
				else
				{
					continue;
				}
			}
		}
		ConnectionManager.getInstance().didComplete(this);
	}

	// private void processBitmapEntity(HttpEntity entity) throws IOException {
	// BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
	// Bitmap bm = BitmapFactory.decodeStream(bufHttpEntity.getContent());
	// handler.sendMessage(Message.obtain(handler, DID_SUCCEED, bm));
	// }

	private void sendMessage(String result) {
		Message message = Message.obtain(handler, DID_SUCCEED,
				listener);
		Bundle data = new Bundle();
		data.putString("callbackkey", result);
		message.setData(data);
		handler.sendMessage(message);
		
	}
}
