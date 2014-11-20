package com.ipcamer.demo;

import java.nio.ByteBuffer;

import vstc2.nativecaller.NativeCaller;

import com.ipcamer.demo.BridgeService.ControllerBinder;
import com.ipcamer.demo.BridgeService.PlayBackInterface;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 *
 * */
public class PlayBackActivity extends Activity implements OnTouchListener,
		OnGestureListener,PlayBackInterface {
	private ImageView playImg;
	private BridgeService mBridgeService;
	private TextView showtftime;
	private String strDID;
	private String strFilePath;
	private String videotime;
	private Button btnBack;
	private final int VIDEO = 0;
	private byte[] videodata = null;
	private int videoDataLen = 0;
	private RelativeLayout top;
	private GestureDetector gt = new GestureDetector(this);
	private int nVideoWidth = 0;
	private int nVideoHeight = 0;
	private boolean isPlaySeekBar = false;
	private LinearLayout layoutConnPrompt;
	private SeekBar playSeekBar;


	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			layoutConnPrompt.setVisibility(View.GONE);
			switch (msg.what) {
			case 1: {// h264
				byte[] rgb = new byte[nVideoWidth * nVideoHeight * 2];
				NativeCaller.YUV4202RGB565(videodata, rgb, nVideoWidth,
						nVideoHeight);
				ByteBuffer buffer = ByteBuffer.wrap(rgb);
				rgb = null;
				Bitmap bmp = Bitmap.createBitmap(nVideoWidth, nVideoHeight,
						Bitmap.Config.RGB_565);
				bmp.copyPixelsFromBuffer(buffer);

				Bitmap bitmap = null;
				int width = getWindowManager().getDefaultDisplay().getWidth();
				int height = getWindowManager().getDefaultDisplay().getHeight();
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					bitmap = Bitmap.createScaledBitmap(bmp, width,
							width * 3 / 4, true);
				} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					bitmap = Bitmap
							.createScaledBitmap(bmp, width, height, true);
				}
				playImg.setImageBitmap(bitmap);

			}
				break;
			case 2: {// jpeg
				Bitmap bmp = BitmapFactory.decodeByteArray(videodata, 0,
						videoDataLen);
				// Drawable drawable = new BitmapDrawable(bmp);
				Bitmap bitmap = null;
				int width = getWindowManager().getDefaultDisplay().getWidth();
				int height = getWindowManager().getDefaultDisplay().getHeight();
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					bitmap = Bitmap.createScaledBitmap(bmp, width,
							width * 3 / 4, true);
				} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					bitmap = Bitmap
							.createScaledBitmap(bmp, width, height, true);
				}
				playImg.setImageBitmap(bitmap);
			}
				break;

			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getDataFromOther();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.playback);
		findView();
		setListener();
		
		BridgeService.getPlayBackVideo(this);
		NativeCaller.StartPlayBack(strDID, strFilePath, 0,0);
	}

	private void setListener() {
		playSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int progress = seekBar.getProgress();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {

			}
		});
		btnBack.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				finish();
			}
		});

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			if (isPlaySeekBar) {
				isPlaySeekBar = false;
				playSeekBar.setVisibility(View.GONE);
			} else {
				isPlaySeekBar = true;
				playSeekBar.setVisibility(View.VISIBLE);
			}
			break;

		default:
			break;
		}

		return false;
	}

	private void getDataFromOther() {
		Intent intent = getIntent();
		strDID = intent.getStringExtra("did");
		strFilePath = intent.getStringExtra("filepath");
		videotime = intent.getStringExtra("videotime");
		Log.i("info", "time:" + videotime);
		Log.i("info", "strFilePath:" + strFilePath);
	}

	private String getTime(String time) {
		String mess = time.substring(0, 4);
		String mes = time.substring(4, 6);
		String me = time.substring(6, 8);
		String hou = time.substring(8, 10);
		String min = time.substring(10, 12);
		String miao = time.substring(12, 14);
		return mess + "-" + mes + "-" + me + " " + hou + ":" + min + ":" + miao;
	}

	private void findView() {
		playImg = (ImageView) findViewById(R.id.playback_img);
		layoutConnPrompt = (LinearLayout) findViewById(R.id.layout_connect_prompt);// connection銆傘�銆�
		playSeekBar = (SeekBar) findViewById(R.id.playback_seekbar);
		showtftime = (TextView) findViewById(R.id.showvideotimetf);
		showtftime.setText(getTime(videotime));
		btnBack = (Button) findViewById(R.id.back);

		top = (RelativeLayout) findViewById(R.id.top);
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.top_bg);
		BitmapDrawable drawable = new BitmapDrawable(bitmap);
		drawable.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		drawable.setDither(true);
		top.setBackgroundDrawable(drawable);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

	}

	/**
	 * BridgeService callback video
	 * **/
	public void CallBack_PlaybackVideoData(byte[] videobuf, int h264Data,
			int len, int width, int height) {
		Log.d("tag", "playback  len:" + len + " width:" + width + " height:"
				+ height);
		videodata = videobuf;
		videoDataLen = len;
		nVideoWidth = width;
		nVideoHeight = height;
		if (h264Data == 1) { // H264
			mHandler.sendEmptyMessage(1);
		} else { // MJPEG
			mHandler.sendEmptyMessage(2);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		NativeCaller.StopPlayBack(strDID);
		Log.d("tag", "PlayBackActivity  onDestroy()");
	}

	private boolean isShow = false;

	@Override
	public boolean onDown(MotionEvent e) {// 杩斿洖閿�
		// TODO Auto-generated method stub
		if (isShow) {
			isShow = false;
			top.setVisibility(View.GONE);
		} else {
			isShow = true;
			top.setVisibility(View.VISIBLE);
		}
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void callBackPlaybackVideoData(byte[] videobuf, int h264Data,
			int len, int width, int height,int streamid, int frameType) {
		// TODO Auto-generated method stub
		videodata = videobuf;
		videoDataLen = len;
		nVideoWidth = width;
		nVideoHeight = height;
		if (h264Data == 1) { // H264
			mHandler.sendEmptyMessage(1);
		} else { // MJPEG
			mHandler.sendEmptyMessage(2);
		}
		
	}

}
