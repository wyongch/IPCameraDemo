package com.ipcamer.demo;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import vstc2.nativecaller.NativeCaller;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamer.demo.BridgeService.PlayInterface;

public class PlayActivity extends Activity implements OnTouchListener,
		OnGestureListener, OnClickListener, PlayInterface {

	private static final String LOG_TAG = "PlayActivity";
	private static final int FULLSCREEN = 0;
	private static final int STANDARD = 1;
	private static final int MAGNIFY = 2;
	private int playmode = 1;
	private static final int AUDIO_BUFFER_START_CODE = 0xff00ff;
	private SurfaceView playSurface = null;
	private SurfaceHolder playHolder = null;
	private byte[] videodata = null;
	private int videoDataLen = 0;
	private int nVideoWidth = 0;
	private int nVideoHeight = 0;
	
	public int nVideoWidths = 0;
	//public byte[] videodatas = null;
	public int nVideoHeights = 0;
	private View progressView = null;
	private boolean bProgress = true;
	private GestureDetector gt = new GestureDetector(this);
	@SuppressWarnings("unused")
	private int nSurfaceHeight = 0;
	private int nResolution = 0;
	private int nBrightness = 0;
	private int nContrast = 0;
	@SuppressWarnings("unused")
	private int nMode = 0;
	@SuppressWarnings("unused")
	private int nFlip = 0;
	@SuppressWarnings("unused")
	private int nFramerate = 0;
	private boolean bInitCameraParam = false;
	private boolean bManualExit = false;
	private TextView textosd = null;
	private String strName = null;;
	private String strDID = null;;
	private int streamType = ContentCommon.MJPEG_SUB_STREAM;
	private PopupWindow popupWindow_about = null;
	private View osdView = null;
	private boolean bDisplayFinished = true;
	private surfaceCallback videoCallback = new surfaceCallback();
	private int nPlayCount = 0;
	private CustomBuffer AudioBuffer = null;
	private AudioPlayer audioPlayer = null;
	private boolean bAudioStart = false;
	private int nStreamCodecType;
	private int nP2PMode = ContentCommon.PPPP_MODE_P2P_NORMAL;
	private TextView textTimeoutTextView = null;
	private boolean bTimeoutStarted = false;
	private int nTimeoutRemain = 180;
	private boolean isTakeVideo = false;
	private boolean isLeftRight = false;
	private boolean isUpDown = false;
	private PopupWindow mPopupWindowProgress;
	private final int BRIGHT = 1;
	private final int CONTRAST = 2;
	private boolean isHorizontalMirror = false;
	private boolean isVerticalMirror = false;
	private boolean isUpDownPressed = false;
	private boolean isShowtoping = false;
	private ImageView vidoeView;
	private ImageView videoViewStandard;
	private ImageButton ptzAudio;
	private ImageButton ptzPlayMode;
//	添加图片按钮对象
	private ImageButton ptzRevolve ;
	private Button ptzResolutoin;
	private Animation showAnim;
	private boolean isTakepic = false;
	private boolean isMcriophone = false;
	private boolean isExit = false;
	private PopupWindow resolutionPopWindow;
	private Animation dismissAnim;
	private View ptzOtherSetAnimView;
	private int timeTag = 0;
	private int timeOne = 0;
	private int timeTwo = 0;
	private ImageButton button_back;
	private BitmapDrawable drawable = null;
	// private LinkedList<byte[]> bs = null;
	private MyBrodCast brodCast = null;

	//定义MyBrodCast类，继承自广播接受者
	class MyBrodCast extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.getIntExtra("ifdrop", 2) != 2) {
				PPPPMsgHandler.sendEmptyMessage(1004);
			}

		}
	}

	/**
	 * 在UI线程中刷新界面状态
	 * **/
	private Handler PPPPMsgHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case 1004:
				Toast.makeText(PlayActivity.this, "相机断线", 0).show();
				PlayActivity.this.finish();
				break;
			default:
				break;
			}

		}
	};

	//定义接口接口回调
	private class surfaceCallback implements SurfaceHolder.Callback {
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			if (holder == playHolder) {
				streamType = 10;
				NativeCaller.StartPPPPLivestream(strDID, streamType);
			}
		}

		public void surfaceCreated(SurfaceHolder holder) {
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// finish();
		}
	}

	//按钮按下监听
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mPopupWindowProgress != null && mPopupWindowProgress.isShowing()) {
			mPopupWindowProgress.dismiss();

		}
		if (resolutionPopWindow != null && resolutionPopWindow.isShowing()) {
			resolutionPopWindow.dismiss();
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!bProgress) {
				Date date = new Date();
				if (timeTag == 0) {
					timeOne = date.getSeconds();
					timeTag = 1;
					Toast.makeText(PlayActivity.this, R.string.main_show_back,
							0).show();
				} else if (timeTag == 1) {
					timeTwo = date.getSeconds();
					if (timeTwo - timeOne <= 3) {
						Intent intent = new Intent("finish");
						sendBroadcast(intent);
						PlayActivity.this.finish();
						timeTag = 0;
					} else {
						timeTag = 1;
						Toast.makeText(PlayActivity.this,
								R.string.main_show_back, 0).show();
					}
				}
			} else {
				showSureDialog1();
			}

			return true;

		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (!bProgress) {
				showTop();
				showBottom();
			} else {
				showSureDialog1();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/****
	 * 退出确定dialog
	 * */
	public void showSureDialog1() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.app);
		builder.setTitle(getResources().getString(R.string.exit)
				+ getResources().getString(R.string.app_name));
		builder.setMessage(R.string.exit_alert);
		builder.setPositiveButton(R.string.str_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Process.killProcess(Process.myPid());
						Intent intent = new Intent("finish");
						sendBroadcast(intent);
						PlayActivity.this.finish();
					}
				});
		builder.setNegativeButton(R.string.str_cancel, null);
		builder.show();
	}

	//显示顶部菜单栏
	private void showTop() {
		if (isShowtoping) {
			isShowtoping = false;
			topbg.setVisibility(View.GONE);
			topbg.startAnimation(dismissTopAnim);
		} else {
			isShowtoping = true;
			topbg.setVisibility(View.VISIBLE);
			topbg.startAnimation(showTopAnim);
		}
	}

	//默认视频参数
	private void defaultVideoParams() {
		nBrightness = 1;
		nContrast = 128;
		NativeCaller.PPPPCameraControl(strDID, 1, 0);
		NativeCaller.PPPPCameraControl(strDID, 2, 128);
		showToast(R.string.ptz_default_vedio_params);
	}

	private void showToast(int i) {
		Toast.makeText(PlayActivity.this, i, 0).show();

	}

	//更新时间
	private void updateTimeout() {
		textTimeoutTextView.setText(getString(R.string.p2p_relay_mode_time_out)
				+ nTimeoutRemain + getString(R.string.str_second));
	}

	private Handler timeoutHandle = new Handler() {
		public void handleMessage(Message msg) {

			if (nTimeoutRemain > 0) {
				nTimeoutRemain = nTimeoutRemain - 1;
				updateTimeout();
				Message msgMessage = new Message();
				timeoutHandle.sendMessageDelayed(msgMessage, 1000);
			} else {
				if (!isExit) {
					Toast.makeText(getApplicationContext(),
							R.string.p2p_view_time_out, Toast.LENGTH_SHORT)
							.show();
				}
				finish();
			}
		}
	};

	private void startTimeout() {
		if (!bTimeoutStarted) {
			Message msgMessage = new Message();
			timeoutHandle.sendMessageDelayed(msgMessage, 1000);
			bTimeoutStarted = true;
		}
	}

	//设置视图可见
	private void setViewVisible() {
		if (bProgress) {
			bProgress = false;
			progressView.setVisibility(View.INVISIBLE);
			osdView.setVisibility(View.VISIBLE);
			if (nP2PMode == ContentCommon.PPPP_MODE_P2P_RELAY) {
				updateTimeout();
				textTimeoutTextView.setVisibility(View.VISIBLE);
				startTimeout();
			}
			getCameraParams();
		}
	}

	//用handler不断的刷新图片
	private Bitmap mBmp;
	private Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			if (msg.what == 1 || msg.what == 2) {
				setViewVisible();
			}
			if (!isPTZPrompt) {
				isPTZPrompt = true;
				showToast(R.string.ptz_control);
			}

			
			switch (msg.what) {
			
			case 1: // h264
			{
				//得到视频数据
				//nVideoWidths为图片的宽度1280
				//nVideoHeights为图片的高度720
				byte[] rgb = new byte[nVideoWidths * nVideoHeights * 2];
				NativeCaller.YUV4202RGB565(videodata, rgb, nVideoWidths,
						nVideoHeights);
				ByteBuffer buffer = ByteBuffer.wrap(rgb);
				rgb = null;
				/* ByteBuffer buffer = ByteBuffer.wrap(videodata); */
				mBmp = Bitmap.createBitmap(nVideoWidths, nVideoHeights,
						Bitmap.Config.RGB_565);
				mBmp.copyPixelsFromBuffer(buffer);
				int width = getWindowManager().getDefaultDisplay().getWidth();
				int height = getWindowManager().getDefaultDisplay().getHeight();
												
				vidoeView.setVisibility(View.GONE);
				Bitmap bitmap = null;
			
				//系统配置信息为竖屏时
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//				if(ptzRevolve.isClickable()){
//					FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//							nVideoWidths, nVideoHeights * 3 / 4);
					FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
							nVideoWidths, nVideoHeights * 3 / 4 );
					lp.gravity = Gravity.CENTER;
					myGlSurfaceView.setLayoutParams(lp);
//					bitmap = Bitmap.createScaledBitmap(mBmp, 640,
//							480, true);
//					bitmap = Bitmap.createScaledBitmap(mBmp, width,
//							(nVideoWidths/width)*(height/2), true);
					
//					bitmap = Bitmap.createScaledBitmap(mBmp, width,
//							width*3/4, true);
					
					bitmap = Bitmap.createScaledBitmap(mBmp, width,
							width/nVideoWidths*nVideoHeights, true);
					
					Toast.makeText(PlayActivity.this, "高度："+height+"nVideoHeights"+nVideoHeights+"宽度："+width+"nwidth"+nVideoWidths, Toast.LENGTH_LONG).show();
					
					Log.i("info", "竖屏");
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				} 
				//系统信息为横屏时
				else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || ptzRevolve.isClickable()) {
//				else if(!ptzRevolve.isClickable()){	
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
							width, height);
					lp.gravity = Gravity.CENTER;
					myGlSurfaceView.setLayoutParams(lp);
					bitmap = Bitmap
							.createScaledBitmap(mBmp, width, height, true);
					Log.i("info", "横屏");
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} 
				
				//修改切换
		/*		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					
//					FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//					nVideoWidths, nVideoHeights * 3 / 4);
					FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
							nVideoWidths, nVideoHeights);
					lp.gravity = Gravity.CENTER;
					myGlSurfaceView.setLayoutParams(lp);
					// bitmap = Bitmap.createScaledBitmap(mBmp, 640,
					// 480, true);
					bitmap = Bitmap.createScaledBitmap(mBmp, width,
							(nVideoWidths / width) * (height / 2), true);
					Toast.makeText(PlayActivity.this, "高度：" + height,
							Toast.LENGTH_LONG).show();

					Log.i("info", "竖屏");
					
			
				} 
				//系统信息为横屏时
				else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

					FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
							width, height);
					lp.gravity = Gravity.CENTER;
					myGlSurfaceView.setLayoutParams(lp);
					bitmap = Bitmap
							.createScaledBitmap(mBmp, width, height, true);
					Log.i("info", "横屏");
				} */
				
				
			
				
//				myRender.writeSample(videodata, nVideoWidth, nVideoHeight);
//				videoViewStandard.setVisibility(View.GONE);
//				vidoeView.setImageBitmap(mBmp);
				
				videoViewStandard.setImageBitmap(bitmap);
				videoViewStandard.setVisibility(View.VISIBLE);
				playSurface.setBackgroundColor(0xff000000);
//				Drawable drawable = new BitmapDrawable(bitmap);
//				playSurface.setBackgroundDrawable(drawable);
			}
				break;
			case 2: // JPEG
			{
				// ptzTakeVideo.setVisibility(View.GONE);
				myGlSurfaceView.setVisibility(View.GONE);
				mBmp = BitmapFactory
						.decodeByteArray(videodata, 0, videoDataLen);
				if (mBmp == null) {
					Log.d(LOG_TAG, "bmp can't be decode...");
					bDisplayFinished = true;
					return;
				}

				nVideoWidth = mBmp.getWidth();
				nVideoHeight = mBmp.getHeight();

				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					videoViewStandard.setVisibility(View.GONE);
					vidoeView.setVisibility(View.VISIBLE);
					vidoeView.setImageBitmap(mBmp);

				} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					videoViewStandard.setImageBitmap(mBmp);
					videoViewStandard.setVisibility(View.VISIBLE);
					vidoeView.setVisibility(View.GONE);
				}
				if (isTakepic) {
					isTakepic = false;
					// takePicture(mBmp);
				}

			}
				break;
			case 3: //
			{
				displayResolution();
			}
				break;
			}

			if (msg.what == 1 || msg.what == 2) {

				// showTimeStamp();
				bDisplayFinished = true;

				nPlayCount++;
				if (nPlayCount >= 100) {
					nPlayCount = 0;
				}
			}
		}

	};

	protected void displayResolution() {
		/*
		 * 0->640x480 1->320x240 2->160x120; 3->1280x720 4->640x360 5->1280x960
		 */

		String strCurrResolution = null;

		switch (nResolution) {
		case 0:// vga
			strCurrResolution = "640x480";
			break;
		case 1:// qvga
			strCurrResolution = "320x240";
			break;
		case 2:
			strCurrResolution = "160x120";
			break;
		case 3:// 720p
			strCurrResolution = "1280x720";
			break;
		case 4:
			strCurrResolution = "640x360";
			break;
		case 5:
			strCurrResolution = "1280x960";
			break;
		default:
			return;
		}
	}

	public void initExitPopupWindow2() {
		LayoutInflater li = LayoutInflater.from(this);
		View popv = li.inflate(R.layout.popup_d, null);
		Button button_load = (Button) popv.findViewById(R.id.add_check_load);
		Button button_phone = (Button) popv.findViewById(R.id.add_check_phone);
		popupWindow_about = new PopupWindow(popv,
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		popupWindow_about.setAnimationStyle(R.style.AnimationPreview);
		popupWindow_about.setFocusable(true);
		popupWindow_about.setOutsideTouchable(true);
		popupWindow_about.setBackgroundDrawable(new ColorDrawable(0));
		button_load.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				NativeCaller.PPPPCameraControl(SystemValue.deviceId, 36, 36);
				popupWindow_about.dismiss();
				ptzResolutoin.setText("VGA");
				Log.d("VGA", "VGA");
			}
		});
		button_phone.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				NativeCaller.PPPPCameraControl(SystemValue.deviceId, 37, 37);
				popupWindow_about.dismiss();
				ptzResolutoin.setText("QVGA");
				Log.d("VGA", "QVGA");
			}
		});
		popupWindow_about
				.setOnDismissListener(new PopupWindow.OnDismissListener() {

					@Override
					public void onDismiss() {
						// TODO Auto-generated method stub
						popupWindow_about.dismiss();
					}
				});
		popupWindow_about.setTouchInterceptor(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_OUTSIDE) {
					popupWindow_about.dismiss();
				}
				return false;
			}
		});
	}

	//得到相机参数
	private void getCameraParams() {

		NativeCaller.PPPPGetSystemParams(strDID,
				ContentCommon.MSG_TYPE_GET_CAMERA_PARAMS);
	}

	private Handler msgHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				Log.d("tag", "断线了");
				Toast.makeText(getApplicationContext(),
						R.string.pppp_status_disconnect, Toast.LENGTH_SHORT)
						.show();
				finish();
			}
		}
	};

	private Handler msgStreamCodecHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (nStreamCodecType == ContentCommon.PPPP_STREAM_TYPE_JPEG) {
				// textCodec.setText("JPEG");
			} else {
				// textCodec.setText("H.264");
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getDataFromOther();
		//设置窗口标记
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//是否启用扩展窗口特性，关闭屏幕标题
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.play);
		strName = SystemValue.deviceName;
		strDID = SystemValue.deviceId;
		findView();
		InitParams();
		//声音播放
		AudioBuffer = new CustomBuffer();
		audioPlayer = new AudioPlayer(AudioBuffer);
		// myvideoRecorder = new CustomVideoRecord(this, strDID);
		//设置播放接口
		BridgeService.setPlayInterface(this);
		//获得SurfaceHolder对象
		playHolder = playSurface.getHolder();
		//设置格式化像素
		playHolder.setFormat(PixelFormat.RGB_565);
		//添加回调函数
		playHolder.addCallback(videoCallback);

		//设置触摸监听
		playSurface.setOnTouchListener(this);
		//设置是否可以长按
		playSurface.setLongClickable(true);

		//得到相机参数
		getCameraParams();
		//移除顶部
		dismissTopAnim = AnimationUtils.loadAnimation(this,
				R.anim.ptz_top_anim_dismiss);
		//显示顶部
		showTopAnim = AnimationUtils.loadAnimation(this,
				R.anim.ptz_top_anim_show);
		showAnim = AnimationUtils.loadAnimation(this,
				R.anim.ptz_otherset_anim_show);
		dismissAnim = AnimationUtils.loadAnimation(this,
				R.anim.ptz_otherset_anim_dismiss);

		// prompt user how to control ptz when first enter play
		SharedPreferences sharePreferences = getSharedPreferences("ptzcontrol",
				MODE_PRIVATE);
		isPTZPrompt = sharePreferences.getBoolean("ptzcontrol", false);
		if (!isPTZPrompt) {
			Editor edit = sharePreferences.edit();
			edit.putBoolean("ptzcontrol", true);
			edit.commit();
		}
		initExitPopupWindow2();
		brodCast = new MyBrodCast();
		IntentFilter filter = new IntentFilter();
		filter.addAction("drop");
		PlayActivity.this.registerReceiver(brodCast, filter);
	}

	//初始化参数
	private void InitParams() {
		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		nSurfaceHeight = dm.heightPixels;
		textosd.setText(strName);
	}

	//启动播放声音
	private void StartAudio() {
		synchronized (this) {
			AudioBuffer.ClearAll();
			audioPlayer.AudioPlayStart();
			NativeCaller.PPPPStartAudio(strDID);
		}
	}

	//停止播放声音
	private void StopAudio() {
		synchronized (this) {
			audioPlayer.AudioPlayStop();
			AudioBuffer.ClearAll();
			NativeCaller.PPPPStopAudio(strDID);
		}
	}

	//设置分辨率
	protected void setResolution(int Resolution) {
		Log.d("tag", "setResolution resolution:" + Resolution);
		NativeCaller.PPPPCameraControl(strDID, 0, Resolution);
	}

	private void findView() {
		playSurface = (SurfaceView) findViewById(R.id.playSurface);
		playSurface.setBackgroundColor(0xff000000);
		button_back = (ImageButton) findViewById(R.id.login_top_back);
		myGlSurfaceView = (GLSurfaceView) findViewById(R.id.myhsurfaceview);
		myRender = new MyRender(myGlSurfaceView);
		myGlSurfaceView.setRenderer(myRender);
		//上下左右活动时显示的图片
		imgUp = (ImageView) findViewById(R.id.imgup);
		imgDown = (ImageView) findViewById(R.id.imgdown);
		imgRight = (ImageView) findViewById(R.id.imgright);
		imgLeft = (ImageView) findViewById(R.id.imgleft);
		imgUp.setOnClickListener(this);
		imgDown.setOnClickListener(this);
		imgLeft.setOnClickListener(this);
		imgRight.setOnClickListener(this);
		button_back.setOnClickListener(this);
		//视频视图
		vidoeView = (ImageView) findViewById(R.id.vedioview);
		videoViewStandard = (ImageView) findViewById(R.id.vedioview_standard);
		progressView = (View) findViewById(R.id.progressLayout);
		textosd = (TextView) findViewById(R.id.textosd);
		textTimeoutTextView = (TextView) findViewById(R.id.textTimeout);
		osdView = (View) findViewById(R.id.osdlayout);
		ptzHoriMirror2 = (ImageButton) findViewById(R.id.ptz_hori_mirror);
		ptzVertMirror2 = (ImageButton) findViewById(R.id.ptz_vert_mirror);
		ptzHoriTour2 = (ImageButton) findViewById(R.id.ptz_hori_tour);
		ptzVertTour2 = (ImageButton) findViewById(R.id.ptz_vert_tour);
		ptzAudio = (ImageButton) findViewById(R.id.ptz_audio);
		ImageButton ptzBrightness = (ImageButton) findViewById(R.id.ptz_brightness);
		ImageButton ptzContrast = (ImageButton) findViewById(R.id.ptz_contrast);
		//下方按钮列表中的按钮
		//切换分辨率按钮
		ptzResolutoin = (Button) findViewById(R.id.ptz_resoluti);
		//播放模式图片按钮
		ptzPlayMode = (ImageButton) findViewById(R.id.ptz_playmode);
		//旋转按钮
		ptzRevolve = (ImageButton) findViewById(R.id.ptz_revolve) ;
		//下方按钮列表视图view
		ptzOtherSetAnimView = findViewById(R.id.ptz_othersetview_anim);
		ImageButton ptzDefaultSet = (ImageButton) findViewById(R.id.ptz_default_set);
		ptzHoriMirror2.setOnClickListener(this);
		ptzVertMirror2.setOnClickListener(this);
		ptzHoriTour2.setOnClickListener(this);
		ptzVertTour2.setOnClickListener(this);
		ptzAudio.setOnClickListener(this);
		ptzBrightness.setOnClickListener(this);
		ptzContrast.setOnClickListener(this);
		ptzResolutoin.setOnClickListener(this);
		ptzPlayMode.setOnClickListener(this);
		//为旋转按钮设置事件响应
		ptzRevolve.setOnClickListener(this);
		ptzDefaultSet.setOnClickListener(this);
		topbg = (RelativeLayout) findViewById(R.id.top_bg);
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.top_bg);
		drawable = new BitmapDrawable(bitmap);
		drawable.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		drawable.setDither(true);
		topbg.setBackgroundDrawable(drawable);
		ptzOtherSetAnimView.setBackgroundDrawable(drawable);
	}

	//重写配置改变信息函数
	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);

		mBaseMatrix = new Matrix();
		mSuppMatrix = new Matrix();
		mDisplayMatrix = new Matrix();
		videoViewStandard.setImageMatrix(mDisplayMatrix);
		
		Log.i("1111111111", "2222222222");
		
			
		
	}

	private boolean isDown = false;
	private boolean isSecondDown = false;
	private float x1 = 0;
	private float x2 = 0;
	private float y1 = 0;
	private float y2 = 0;

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		if (!isDown) {
			x1 = event.getX();
			y1 = event.getY();
			isDown = true;
		}
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			mode = DRAG;
			originalScale = getScale();
			break;
		case MotionEvent.ACTION_POINTER_UP:

			break;
		case MotionEvent.ACTION_UP:
			if (Math.abs((x1 - x2)) < 25 && Math.abs((y1 - y2)) < 25) {

				if (resolutionPopWindow != null
						&& resolutionPopWindow.isShowing()) {
					resolutionPopWindow.dismiss();
				}

				if (mPopupWindowProgress != null
						&& mPopupWindowProgress.isShowing()) {
					mPopupWindowProgress.dismiss();
				}
				if (!isSecondDown) {
					if (!bProgress) {
						showTop();
						showBottom();
					}
				}
				isSecondDown = false;
			} else {
			}
			x1 = 0;
			x2 = 0;
			y1 = 0;
			y2 = 0;
			isDown = false;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			isSecondDown = true;
			oldDist = spacing(event);
			if (oldDist > 10f) {
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			break;

		case MotionEvent.ACTION_MOVE:
			x2 = event.getX();
			y2 = event.getY();

			int midx = getWindowManager().getDefaultDisplay().getWidth() / 2;
			int midy = getWindowManager().getDefaultDisplay().getHeight() / 2;
			if (mode == ZOOM) {
				float newDist = spacing(event);
				if (newDist > 0f) {
					float scale = newDist / oldDist;
					Log.d("scale", "scale:" + scale);
					if (scale <= 2.0f && scale >= 0.2f) {
						// zoomTo(originalScale * scale, midx, midy);
					}
				}
			}
		}

		return gt.onTouchEvent(event);
	}

	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2;

	private int mode = NONE;
	private float oldDist;
	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();
	private PointF start = new PointF();
	private PointF mid = new PointF();
	float mMaxZoom = 2.0f;
	float mMinZoom = 0.3125f;
	float originalScale;
	float baseValue;
	protected Matrix mBaseMatrix = new Matrix();
	protected Matrix mSuppMatrix = new Matrix();
	private Matrix mDisplayMatrix = new Matrix();
	private final float[] mMatrixValues = new float[9];

	//实现相机移动函数
	protected void zoomTo(float scale, float centerX, float centerY) {
		Log.d("zoomTo", "zoomTo scale:" + scale);
		if (scale > mMaxZoom) {
			scale = mMaxZoom;
		} else if (scale < mMinZoom) {
			scale = mMinZoom;
		}

		float oldScale = getScale();
		float deltaScale = scale / oldScale;
		Log.d("deltaScale", "deltaScale:" + deltaScale);
		mSuppMatrix.postScale(deltaScale, deltaScale, centerX, centerY);
		videoViewStandard.setScaleType(ImageView.ScaleType.MATRIX);
		videoViewStandard.setImageMatrix(getImageViewMatrix());
	}

	//得到图片视图矩阵
	protected Matrix getImageViewMatrix() {
		mDisplayMatrix.set(mBaseMatrix);
		mDisplayMatrix.postConcat(mSuppMatrix);
		return mDisplayMatrix;
	}

	protected float getScale(Matrix matrix) {
		return getValue(matrix, Matrix.MSCALE_X);
	}

	protected float getScale() {
		return getScale(mSuppMatrix);
	}

	protected float getValue(Matrix matrix, int whichValue) {
		matrix.getValues(mMatrixValues);
		return mMatrixValues[whichValue];
	}

	private float spacing(MotionEvent event) {
		try {
			float x = event.getX(0) - event.getX(1);
			float y = event.getY(0) - event.getY(1);
			return FloatMath.sqrt(x * x + y * y);
		} catch (Exception e) {
		}
		return 0;
	}

	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		Log.d("tag", "onDown");
		return false;
	}

	private final int MINLEN = 80;
	private RelativeLayout topbg;
	private Animation showTopAnim;
	private Animation dismissTopAnim;
	private ImageButton ptzHoriMirror2;
	private ImageButton ptzVertMirror2;
	private ImageButton ptzHoriTour2;
	private ImageButton ptzVertTour2;
	private boolean isPTZPrompt;

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		float x1 = e1.getX();
		float x2 = e2.getX();
		float y1 = e1.getY();
		float y2 = e2.getY();

		float xx = x1 > x2 ? x1 - x2 : x2 - x1;
		float yy = y1 > y2 ? y1 - y2 : y2 - y1;

		if (xx > yy) {
			if ((x1 > x2) && (xx > MINLEN)) {// left
				NativeCaller
						.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_RIGHT);
			} else if ((x1 < x2) && (xx > MINLEN)) {// right
				NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_LEFT);
			}

		} else {
			if ((y1 > y2) && (yy > MINLEN)) {// down
				NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_DOWN);
			} else if ((y1 < y2) && (yy > MINLEN)) {// up
				NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_UP);
			}

		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	public void showSureDialogPlay() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.app);
		builder.setTitle(getResources().getString(R.string.exit_show));
		builder.setMessage(R.string.exit_play_show);
		builder.setPositiveButton(R.string.str_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						PlayActivity.this.finish();
					}
				});
		builder.setNegativeButton(R.string.str_cancel, null);
		builder.show();
	}

	@Override
	public void onClick(View v) {
//		switch (v.getId()) {
//
//		case R.id.login_top_back:
//			bManualExit = true;
//			if (!bProgress) {
//				if (isTakeVideo == true) {
//					showToast(R.string.eixt_show_toast);
//				} else {
//					showSureDialogPlay();
//				}
//			}
//			break;
//		case R.id.imgup:
//			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_UP);
//			Log.d("tag", "up");
//			break;
//		case R.id.imgdown:
//			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_DOWN);
//			Log.d("tag", "down");
//			break;
//		case R.id.imgleft:
//			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_LEFT);
//			Log.d("tag", "left");
//			break;
//		case R.id.imgright:
//			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_RIGHT);
//			Log.d("tag", "right");
//			break;
//		case R.id.ptz_hori_mirror:
//			if (isHorizontalMirror) {
//				ptzHoriMirror2.setBackgroundColor(0x00ffffff);
//				isHorizontalMirror = false;
//				NativeCaller.PPPPCameraControl(strDID, 5,
//						ContentCommon.CMD_PTZ_ORIGINAL);
//				Log.d("tag", "水平镜像还原：" + ContentCommon.CMD_PTZ_ORIGINAL);
//			} else {
//				isHorizontalMirror = true;
//				ptzHoriMirror2.setBackgroundColor(0xff0044aa);
//				NativeCaller.PPPPCameraControl(strDID, 5,
//						ContentCommon.CMD_PTZ_HORIZONAL_MIRROR);
//				Log.d("tag", "水平镜像：" + ContentCommon.CMD_PTZ_HORIZONAL_MIRROR);
//			}
//			break;
//		case R.id.ptz_vert_mirror:
//			if (isVerticalMirror) {
//				isVerticalMirror = false;
//				ptzVertMirror2.setBackgroundColor(0x00ffffff);
//				NativeCaller.PPPPCameraControl(strDID, 5,
//						ContentCommon.CMD_PTZ_ORIGINAL);
//				Log.d("tag", "垂直镜像还原：" + ContentCommon.CMD_PTZ_ORIGINAL);
//			} else {
//				isVerticalMirror = true;
//				ptzVertMirror2.setBackgroundColor(0xff0044aa);
//				NativeCaller.PPPPCameraControl(strDID, 5,
//						ContentCommon.CMD_PTZ_VERTICAL_MIRROR);
//				Log.d("tag", "垂直镜像：" + ContentCommon.CMD_PTZ_VERTICAL_MIRROR);
//			}
//			break;
//
//		case R.id.ptz_hori_tour:
//			if (isLeftRight) {
//				ptzHoriTour2.setBackgroundColor(0x000044aa);
//				isLeftRight = false;
//				NativeCaller.PPPPPTZControl(strDID,
//						ContentCommon.CMD_PTZ_LEFT_RIGHT_STOP);
//				Log.d("tag", "水平巡视停止:" + ContentCommon.CMD_PTZ_LEFT_RIGHT_STOP);
//			} else {
//				ptzHoriTour2.setBackgroundColor(0xff0044aa);
//				isLeftRight = true;
//				NativeCaller.PPPPPTZControl(strDID,
//						ContentCommon.CMD_PTZ_LEFT_RIGHT);
//				Log.d("tag", "水平巡视开始:" + ContentCommon.CMD_PTZ_LEFT_RIGHT);
//			}
//			break;
//		case R.id.ptz_vert_tour:
//			if (isUpDown) {
//				ptzVertTour2.setBackgroundColor(0x000044aa);
//				isUpDown = false;
//				NativeCaller.PPPPPTZControl(strDID,
//						ContentCommon.CMD_PTZ_UP_DOWN_STOP);
//				Log.d("tag", "垂直巡视停止:" + ContentCommon.CMD_PTZ_UP_DOWN_STOP);
//			} else {
//				ptzVertTour2.setBackgroundColor(0xff0044aa);
//				isUpDown = true;
//				NativeCaller.PPPPPTZControl(strDID,
//						ContentCommon.CMD_PTZ_UP_DOWN);
//				Log.d("tag", "垂直巡视开始:" + ContentCommon.CMD_PTZ_UP_DOWN);
//			}
//			break;
//		case R.id.ptz_audio:
//			dismissBrightAndContrastProgress();
//			if (!isMcriophone) {
//				if (bAudioStart) {
//					Log.d("tag", "没有声音");
//					bAudioStart = false;
//					ptzAudio.setImageResource(R.drawable.ptz_audio_off);
//					StopAudio();
//				} else {
//					Log.d("tag", "有声音");
//					bAudioStart = true;
//					ptzAudio.setImageResource(R.drawable.ptz_audio_on);
//					StartAudio();
//				}
//			}
//			break;
//		case R.id.ptz_brightness:
//			if (mPopupWindowProgress != null
//					&& mPopupWindowProgress.isShowing()) {
//				mPopupWindowProgress.dismiss();
//				mPopupWindowProgress = null;
//			}
//			setBrightOrContrast(BRIGHT);
//			break;
//		case R.id.ptz_contrast:
//			if (mPopupWindowProgress != null
//					&& mPopupWindowProgress.isShowing()) {
//				mPopupWindowProgress.dismiss();
//				mPopupWindowProgress = null;
//			}
//			setBrightOrContrast(CONTRAST);
//			break;
//		case R.id.ptz_resoluti:
//			popupWindow_about.showAtLocation(button_back, Gravity.CENTER, 0, 0);
//			break;
//		case R.id.ptz_resolution_jpeg_qvga:
//			dismissBrightAndContrastProgress();
//			resolutionPopWindow.dismiss();
//			nResolution = 1;
//			setResolution(nResolution);
//			Log.d("tag", "jpeg resolution:" + nResolution + " qvga");
//			break;
//		case R.id.ptz_resolution_jpeg_vga:
//			dismissBrightAndContrastProgress();
//			resolutionPopWindow.dismiss();
//			nResolution = 0;
//			setResolution(nResolution);
//			Log.d("tag", "jpeg resolution:" + nResolution + " vga");
//			break;
//		case R.id.ptz_resolution_h264_qvga:
//			dismissBrightAndContrastProgress();
//			resolutionPopWindow.dismiss();
//			nResolution = 1;
//			setResolution(nResolution);
//			Log.d("tag", "h264 resolution:" + nResolution + " qvga");
//			break;
//		case R.id.ptz_resolution_h264_vga:
//			dismissBrightAndContrastProgress();
//			resolutionPopWindow.dismiss();
//			nResolution = 0;
//			setResolution(nResolution);
//			Log.d("tag", "h264 resolution:" + nResolution + " vga");
//			break;
//		case R.id.ptz_resolution_h264_720p:
//			dismissBrightAndContrastProgress();
//			resolutionPopWindow.dismiss();
//			nResolution = 3;
//			setResolution(nResolution);
//			Log.d("tag", "h264 resolution:" + nResolution + " 720p");
//			break;
//		case R.id.ptz_playmode:
//			dismissBrightAndContrastProgress();
//			switch (playmode) {
//			case FULLSCREEN:
//				ptzPlayMode.setImageResource(R.drawable.ptz_playmode_enlarge);
//				ptzPlayMode
//						.setBackgroundResource(R.drawable.ptz_takepic_selector);
//				Log.d("tg", "magnify 1");
//				playmode = STANDARD;
//				break;
//			case MAGNIFY:
//				Log.d("tg", "STANDARD 2");
//				playmode = FULLSCREEN;
//				ptzPlayMode.setImageResource(R.drawable.ptz_playmode_standard);
//				ptzPlayMode
//						.setBackgroundResource(R.drawable.ptz_takepic_selector);
//				break;
//			case STANDARD:
//				Log.d("tg", "FULLSCREEN 3");
//				playmode = MAGNIFY;
//				ptzPlayMode
//						.setImageResource(R.drawable.ptz_playmode_fullscreen);
//				ptzPlayMode
//						.setBackgroundResource(R.drawable.ptz_takepic_selector);
//				break;
//			default:
//				break;
//			}
//
//			break;
//		case R.id.ptz_default_set:
//			dismissBrightAndContrastProgress();
//			defaultVideoParams();
//			break;
//		}
		
		//修改
		int id = v.getId();
		if (id == R.id.login_top_back) {
			bManualExit = true;
			if (!bProgress) {
				if (isTakeVideo == true) {
					showToast(R.string.eixt_show_toast);
				} else {
					showSureDialogPlay();
				}
			}
		} else if (id == R.id.imgup) {
			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_UP);
			Log.d("tag", "up");
		} else if (id == R.id.imgdown) {
			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_DOWN);
			Log.d("tag", "down");
		} else if (id == R.id.imgleft) {
			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_LEFT);
			Log.d("tag", "left");
		} else if (id == R.id.imgright) {
			NativeCaller.PPPPPTZControl(strDID, ContentCommon.CMD_PTZ_RIGHT);
			Log.d("tag", "right");
		} else if (id == R.id.ptz_hori_mirror) {
			if (isHorizontalMirror) {
				ptzHoriMirror2.setBackgroundColor(0x00ffffff);
				isHorizontalMirror = false;
				NativeCaller.PPPPCameraControl(strDID, 5,
						ContentCommon.CMD_PTZ_ORIGINAL);
				Log.d("tag", "水平镜像还原：" + ContentCommon.CMD_PTZ_ORIGINAL);
			} else {
				isHorizontalMirror = true;
				ptzHoriMirror2.setBackgroundColor(0xff0044aa);
				NativeCaller.PPPPCameraControl(strDID, 5,
						ContentCommon.CMD_PTZ_HORIZONAL_MIRROR);
				Log.d("tag", "水平镜像：" + ContentCommon.CMD_PTZ_HORIZONAL_MIRROR);
			}
		} else if (id == R.id.ptz_vert_mirror) {
			if (isVerticalMirror) {
				isVerticalMirror = false;
				ptzVertMirror2.setBackgroundColor(0x00ffffff);
				NativeCaller.PPPPCameraControl(strDID, 5,
						ContentCommon.CMD_PTZ_ORIGINAL);
				Log.d("tag", "垂直镜像还原：" + ContentCommon.CMD_PTZ_ORIGINAL);
			} else {
				isVerticalMirror = true;
				ptzVertMirror2.setBackgroundColor(0xff0044aa);
				NativeCaller.PPPPCameraControl(strDID, 5,
						ContentCommon.CMD_PTZ_VERTICAL_MIRROR);
				Log.d("tag", "垂直镜像：" + ContentCommon.CMD_PTZ_VERTICAL_MIRROR);
			}
		} else if (id == R.id.ptz_hori_tour) {
			if (isLeftRight) {
				ptzHoriTour2.setBackgroundColor(0x000044aa);
				isLeftRight = false;
				NativeCaller.PPPPPTZControl(strDID,
						ContentCommon.CMD_PTZ_LEFT_RIGHT_STOP);
				Log.d("tag", "水平巡视停止:" + ContentCommon.CMD_PTZ_LEFT_RIGHT_STOP);
			} else {
				ptzHoriTour2.setBackgroundColor(0xff0044aa);
				isLeftRight = true;
				NativeCaller.PPPPPTZControl(strDID,
						ContentCommon.CMD_PTZ_LEFT_RIGHT);
				Log.d("tag", "水平巡视开始:" + ContentCommon.CMD_PTZ_LEFT_RIGHT);
			}
		} else if (id == R.id.ptz_vert_tour) {
			if (isUpDown) {
				ptzVertTour2.setBackgroundColor(0x000044aa);
				isUpDown = false;
				NativeCaller.PPPPPTZControl(strDID,
						ContentCommon.CMD_PTZ_UP_DOWN_STOP);
				Log.d("tag", "垂直巡视停止:" + ContentCommon.CMD_PTZ_UP_DOWN_STOP);
			} else {
				ptzVertTour2.setBackgroundColor(0xff0044aa);
				isUpDown = true;
				NativeCaller.PPPPPTZControl(strDID,
						ContentCommon.CMD_PTZ_UP_DOWN);
				Log.d("tag", "垂直巡视开始:" + ContentCommon.CMD_PTZ_UP_DOWN);
			}
		} else if (id == R.id.ptz_audio) {
			dismissBrightAndContrastProgress();
			if (!isMcriophone) {
				if (bAudioStart) {
					Log.d("tag", "没有声音");
					bAudioStart = false;
					ptzAudio.setImageResource(R.drawable.ptz_audio_off);
					StopAudio();
				} else {
					Log.d("tag", "有声音");
					bAudioStart = true;
					ptzAudio.setImageResource(R.drawable.ptz_audio_on);
					StartAudio();
				}
			}
		} else if (id == R.id.ptz_brightness) {
			if (mPopupWindowProgress != null
					&& mPopupWindowProgress.isShowing()) {
				mPopupWindowProgress.dismiss();
				mPopupWindowProgress = null;
			}
			setBrightOrContrast(BRIGHT);
		} else if (id == R.id.ptz_contrast) {
			if (mPopupWindowProgress != null
					&& mPopupWindowProgress.isShowing()) {
				mPopupWindowProgress.dismiss();
				mPopupWindowProgress = null;
			}
			setBrightOrContrast(CONTRAST);
		} else if (id == R.id.ptz_resoluti) {
			popupWindow_about.showAtLocation(button_back, Gravity.CENTER, 0, 0);
		} else if (id == R.id.ptz_resolution_jpeg_qvga) {
			dismissBrightAndContrastProgress();
			resolutionPopWindow.dismiss();
			nResolution = 1;
			setResolution(nResolution);
			Log.d("tag", "jpeg resolution:" + nResolution + " qvga");
		} else if (id == R.id.ptz_resolution_jpeg_vga) {
			dismissBrightAndContrastProgress();
			resolutionPopWindow.dismiss();
			nResolution = 0;
			setResolution(nResolution);
			Log.d("tag", "jpeg resolution:" + nResolution + " vga");
		} else if (id == R.id.ptz_resolution_h264_qvga) {
			dismissBrightAndContrastProgress();
			resolutionPopWindow.dismiss();
			nResolution = 1;
			setResolution(nResolution);
			Log.d("tag", "h264 resolution:" + nResolution + " qvga");
		} else if (id == R.id.ptz_resolution_h264_vga) {
			dismissBrightAndContrastProgress();
			resolutionPopWindow.dismiss();
			nResolution = 0;
			setResolution(nResolution);
			Log.d("tag", "h264 resolution:" + nResolution + " vga");
		} else if (id == R.id.ptz_resolution_h264_720p) {
			dismissBrightAndContrastProgress();
			resolutionPopWindow.dismiss();
			nResolution = 3;
			setResolution(nResolution);
			Log.d("tag", "h264 resolution:" + nResolution + " 720p");
		}
		else if(id == R.id.ptz_revolve){
//			实现屏幕旋转
//			byte[] rgb = new byte[nVideoWidths * nVideoHeights * 2];
//			NativeCaller.YUV4202RGB565(videodata, rgb, nVideoWidths,
//					nVideoHeights);
//			ByteBuffer buffer = ByteBuffer.wrap(rgb);
//			rgb = null;
//			/* ByteBuffer buffer = ByteBuffer.wrap(videodata); */
//			mBmp = Bitmap.createBitmap(nVideoWidths, nVideoHeights,
//					Bitmap.Config.RGB_565);
//			mBmp.copyPixelsFromBuffer(buffer);
//			int width = getWindowManager().getDefaultDisplay().getWidth();
//			int height = getWindowManager().getDefaultDisplay().getHeight();
//			
//			vidoeView.setVisibility(View.GONE);
//			Bitmap bitmap = null;
//			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//			width, height);
//			lp.gravity = Gravity.CENTER;
//			myGlSurfaceView.setLayoutParams(lp);
//			bitmap = Bitmap
//			.createScaledBitmap(mBmp, width, height, true);
			//修改配置文件的值为横屏
			mBaseMatrix = new Matrix();
			mSuppMatrix = new Matrix();
			mDisplayMatrix = new Matrix();
			videoViewStandard.setImageMatrix(mDisplayMatrix);
//			Boolean bl=false ;
//			if(bl){
//				getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT ;
//				bl = true ;
//			}else{
//				getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE ;
//			}
			
			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ){
				getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT ;
			}
			else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ){
				getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE ;
			};
			
			Log.i("info", "横屏111111111"+ptzRevolve.isClickable() );
			
		} 

		else if (id == R.id.ptz_playmode) {
			dismissBrightAndContrastProgress();
			switch (playmode) {
			case FULLSCREEN:
				ptzPlayMode.setImageResource(R.drawable.ptz_playmode_enlarge);
				ptzPlayMode
						.setBackgroundResource(R.drawable.ptz_takepic_selector);
				Log.d("tg", "magnify 1");
				playmode = STANDARD;
				break;
			case MAGNIFY:
				Log.d("tg", "STANDARD 2");
				playmode = FULLSCREEN;
				ptzPlayMode.setImageResource(R.drawable.ptz_playmode_standard);
				ptzPlayMode
						.setBackgroundResource(R.drawable.ptz_takepic_selector);
				break;
			case STANDARD:
				Log.d("tg", "FULLSCREEN 3");
				playmode = MAGNIFY;
				ptzPlayMode
						.setImageResource(R.drawable.ptz_playmode_fullscreen);
				ptzPlayMode
						.setBackgroundResource(R.drawable.ptz_takepic_selector);
				break;
			default:
				break;
			}
		}		
		else if (id == R.id.ptz_default_set) {
			dismissBrightAndContrastProgress();
			defaultVideoParams();
		}
		
		
	}

	private void dismissBrightAndContrastProgress() {
		if (mPopupWindowProgress != null && mPopupWindowProgress.isShowing()) {
			mPopupWindowProgress.dismiss();
			mPopupWindowProgress = null;
		}
	}

	private void showBottom() {
		if (isUpDownPressed) {
			isUpDownPressed = false;
			ptzOtherSetAnimView.startAnimation(dismissAnim);
			ptzOtherSetAnimView.setVisibility(View.GONE);
		} else {
			isUpDownPressed = true;
			ptzOtherSetAnimView.startAnimation(showAnim);
			ptzOtherSetAnimView.setVisibility(View.VISIBLE);
		}
	}

	private void setBrightOrContrast(final int type) {
		Log.i(LOG_TAG, "type:" + type + "  bInitCameraParam:"
				+ bInitCameraParam);
		if (!bInitCameraParam) {
			return;
		}
		int width = getWindowManager().getDefaultDisplay().getWidth();
		LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(
				R.layout.brightprogress, null);
		SeekBar seekBar = (SeekBar) layout.findViewById(R.id.brightseekBar1);
		seekBar.setMax(255);
		switch (type) {
		case BRIGHT:
			seekBar.setProgress(nBrightness);
			break;
		case CONTRAST:
			seekBar.setProgress(nContrast);
			break;
		default:
			break;
		}
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int progress = seekBar.getProgress();
				switch (type) {
				case BRIGHT:// 亮度
					nBrightness = progress;
					NativeCaller.PPPPCameraControl(strDID, BRIGHT, nBrightness);
					break;
				case CONTRAST:// 对比度
					nContrast = progress;
					NativeCaller.PPPPCameraControl(strDID, CONTRAST, nContrast);
					break;
				default:
					break;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

			}

			@Override
			public void onProgressChanged(SeekBar arg0, int progress,
					boolean arg2) {

			}
		});

		mPopupWindowProgress = new PopupWindow(layout, width / 2, 180);
		mPopupWindowProgress.showAtLocation(findViewById(R.id.play),
				Gravity.TOP, 0, 0);

	}

	private MyRender myRender = null;
	private GLSurfaceView myGlSurfaceView = null;
	private ImageView imgUp = null;
	private ImageView imgDown = null;
	private ImageView imgRight = null;
	private ImageView imgLeft = null;

	@Override
	protected void onDestroy() {
		NativeCaller.StopPPPPLivestream(strDID);
		StopAudio();
		if (myRender != null) {
			myRender.destroyShaders();
		}
		if (brodCast != null) {
			unregisterReceiver(brodCast);
		}
		Log.d("tag", "PlayActivity onDestroy");

		super.onDestroy();
	}

	/***
	 * BridgeService callback
	 * 回调桥接服务中的播放接口，摄像机参数通知
	 * **/
	@Override
	public void callBackCameraParamNotify(String did, int resolution,
			int brightness, int contrast, int hue, int saturation, int flip) {
		Log.d("info", "CameraParamNotify...did:" + did + " brightness: "
				+ brightness + " resolution: " + resolution + " contrast: "
				+ contrast + " hue: " + hue + " saturation: " + saturation
				+ " flip: " + flip);
		Log.d("tag", "contrast:" + contrast + " brightness:" + brightness);
		nBrightness = brightness;
		nContrast = contrast;
		nResolution = resolution;
		Log.d("VGA", nResolution + "");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (nResolution == 0) {
					// vga
					ptzResolutoin.setText("VGA");
				} else if (nResolution == 3) {
					// 720
					ptzResolutoin.setText("720P");
				} else if (nResolution == 1) {
					// 720
					ptzResolutoin.setText("QVGA");
				}
			}
		});
		Message msg = new Message();
		msg.what = 3;
		mHandler.sendMessage(msg);

		bInitCameraParam = true;
	}

	/***
	 * BridgeService callback
	 * 回调视频数据
	 * **/
	@Override
	public void callBaceVideoData(byte[] videobuf, int h264Data, int len,
			int width, int height) {
		Log.d("info", "Call VideoData...h264Data: " + h264Data + " len: "
				+ len + " videobuf len: " + videobuf.length + "width=="
				+ nVideoWidth + "height==" + nVideoHeight);
		if (!bDisplayFinished) {
			Log.d("info", "return bDisplayFinished");
			return;
		}

		bDisplayFinished = false;
		videodata = videobuf;
		videoDataLen = len;
		nVideoWidths = width;
		nVideoHeights = height;
		Message msg = new Message();
		if (h264Data == 1) { // H264
			Log.i("info","h264Data....");
			if (isTakepic) {
				isTakepic = false;
				byte[] rgb = new byte[width * height * 2];
				NativeCaller.YUV4202RGB565(videobuf, rgb, width, height);
				ByteBuffer buffer = ByteBuffer.wrap(rgb);
				mBmp = Bitmap
						.createBitmap(width, height, Bitmap.Config.RGB_565);
				mBmp.copyPixelsFromBuffer(buffer);
//				takePicture(mBmp);
			}
			msg.what = 1;
		} else { // MJPEG
			Log.i("info","MJPEG....");
			msg.what = 2;
		}
		mHandler.sendMessage(msg);
	}

	/***
	 * BridgeService callback
	 * 回调消息通知
	 * **/
	@Override
	public void callBackMessageNotify(String did, int msgType, int param) {
		Log.d("tag", "MessageNotify did: " + did + " msgType: " + msgType
				+ " param: " + param);
		if (bManualExit)
			return;

		if (msgType == ContentCommon.PPPP_MSG_TYPE_STREAM) {
			nStreamCodecType = param;
			Message msgMessage = new Message();
			msgStreamCodecHandler.sendMessage(msgMessage);
			return;
		}

		if (msgType != ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS) {
			return;
		}

		if (!did.equals(strDID)) {
			return;
		}

		Message msg = new Message();
		msg.what = 1;

		msgHandler.sendMessage(msg);
	}

	/***
	 * BridgeService callback
	 * 回调声音数据
	 * **/
	@Override
	public void callBackAudioData(byte[] pcm, int len) {
		Log.d(LOG_TAG, "AudioData: len :+ " + len);
		if (!audioPlayer.isAudioPlaying()) {
			return;
		}
		CustomBufferHead head = new CustomBufferHead();
		CustomBufferData data = new CustomBufferData();
		head.length = len;
		head.startcode = AUDIO_BUFFER_START_CODE;
		data.head = head;
		data.data = pcm;
		AudioBuffer.addData(data);
	}

	/***
	 * BridgeService callback
	 * 
	 * **/
	@Override
	public void callBackH264Data(byte[] h264, int type, int size) {
		Log.d("tag", "CallBack_H264Data" + " type:" + type + " size:" + size);
	}

}
