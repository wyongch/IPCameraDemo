package com.ipcamer.demo;

import java.util.Date;

import vstc2.nativecaller.NativeCaller;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class StartActivity extends Activity {
	private static final String LOG_TAG = "StartActivity";
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Intent in = new Intent(StartActivity.this, AddCameraActivity.class);
			startActivity(in);
			finish();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "StartActivity onCreate");
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.start11);
		Intent intent = new Intent();
		intent.setClass(StartActivity.this, BridgeService.class);
		//启动BridgeService服务
		startService(intent);
		new Thread(new Runnable() {
		
			@Override
			public void run() {
				try {
					Log.i("11111111", "wwwwwwwww");
					NativeCaller.PPPPInitial("ABC");//初始化默认服务器,如无定制服务器则不需要修改 
					long lStartTime = new Date().getTime();
					Log.i("11111111", "rrrrrrrrrrrr");
					//调用本地网络探测，并得到返回结果
					int nRes = NativeCaller.PPPPNetworkDetect();
					Log.i("start", "222222222");
					long lEndTime = new Date().getTime();
					Log.i("start", "555555");
					//Toast.makeText(StartActivity.this,"开始时间："+ lStartTime + "结束时间：" + lEndTime, Toast.LENGTH_LONG).show();
					Log.i("start", "333333333");
					//if (lEndTime - lStartTime <= 1000) {
						//Thread.sleep(3000);
					//}
					Message msg = new Message();
					mHandler.sendMessage(msg);
					Log.i("start", "44444444");
					
				} catch (Exception e) {

				}
			}
		}).start();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return true;
		return super.onKeyDown(keyCode, event);
	}

}