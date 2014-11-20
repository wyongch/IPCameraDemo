package com.ipcamer.demo;

import java.util.Map;
import java.util.TimerTask;

import vstc2.nativecaller.NativeCaller;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ipcamer.demo.BridgeService.AddCameraInterface;
import com.ipcamer.demo.BridgeService.IpcamClientInterface;

public class AddCameraActivity extends Activity implements OnClickListener,
		AddCameraInterface, OnItemSelectedListener, IpcamClientInterface {
	private EditText userEdit = null;	//用户名
	private EditText pwdEdit = null;	//密码
	private EditText didEdit = null;	//设备id
	private TextView textView_top_show = null;	//显示相机状态
	private Button done;						//连接按钮
	private static final int SEARCH_TIME = 3000;	//搜索时间
	private int option = ContentCommon.INVALID_OPTION;	//无效选项
	private int CameraType = ContentCommon.CAMERA_TYPE_MJPEG; //摄像头类型
	private Button btnSearchCamera;			//搜索相机按钮
	private SearchListAdapter listAdapter = null;	
	private ProgressDialog progressdlg = null;  //进度框 ，不允许用户操作
	private boolean isSearched;			//是否搜索
	private MyBroadCast receiver;	//自定义类，继承自广播接收器BroadcastReceiver
	private WifiManager manager = null;   //wifi管理器
	private ProgressBar progressBar = null;		//进度条
	private static final String STR_DID = "did";
	private static final String STR_MSG_PARAM = "msgparam";
	private MyWifiThread myWifiThread = null;		//自定义wifi进程，继承自Thread
	private boolean blagg = false;
	private Intent intentbrod = null;		//定义结束广播意图
	private WifiInfo info = null;
	boolean bthread = true;
	private Button button_play = null;		//预览模式按钮
	private Button button_setting = null;	//系统设置按钮
	private int tag = 0;

	//定义一个在特定时间执行的任务
	class MyTimerTask extends TimerTask {

		public void run() {
			//发送一个值为100000的消息
			updateListHandler.sendEmptyMessage(100000);
		}
	};

	class MyWifiThread extends Thread {
		@Override
		public void run() {
			while (blagg == true) {
				super.run();

				updateListHandler.sendEmptyMessage(100000);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private class MyBroadCast extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			AddCameraActivity.this.finish();
			Log.d("ip", "AddCameraActivity.this.finish()");
		}

	}

	//实现线程接口
	class StartPPPPThread implements Runnable {
		@Override
		public void run() {
			try {
				
				Thread.sleep(100);
				StartCameraPPPP();
			} catch (Exception e) {

			}
		}
	}

	//启动网络摄像头
	private void StartCameraPPPP() {
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
		//修改网络搜索
		Log.i("11111", "1222222111");
		int nRes = NativeCaller.PPPPNetworkDetect();
		Log.i("11111", "111555511");
		//本地调用StartPPPP方法，得到返回结果
		int result = NativeCaller.StartPPPP(SystemValue.deviceId, SystemValue.deviceName,
				SystemValue.devicePass);
		Log.i("11111", "11666666611");
		Log.i("ip", "result:"+result);
	} 

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.add_camera);
		Intent in = getIntent();
		//创建搜索进度框
		progressdlg = new ProgressDialog(this);
		progressdlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressdlg.setMessage(getString(R.string.searching_tip));
		//创建搜索列表适配器
		listAdapter = new SearchListAdapter(this);
		//自定义findView函数
		findView();
		//得到wifi管理器
		manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		//调用自定义初始化参数函数
		InitParams();

		//
		BridgeService.setAddCameraInterface(this);
		//新建一个广播接收者
		receiver = new MyBroadCast();
		//new一个意图过滤器
		IntentFilter filter = new IntentFilter();
		//通过addAction方法匹配隐式事件
		filter.addAction("finish");
		//注册广播接收者
		registerReceiver(receiver, filter);
		//new一个结束广播意图
		intentbrod = new Intent("drop");
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		blagg = true;
	}

	//自定义初始化参数 ，即设置连接监听和搜索监听
	private void InitParams() {

		done.setOnClickListener(this);
		btnSearchCamera.setOnClickListener(this);
	}

	//重载AddCameraActivity中的停止函数
	@Override
	protected void onStop() {
		if (myWifiThread != null) {
			blagg = false;
		}
		//移除搜索进度框
		progressdlg.dismiss();
		//停止搜索局域网中的相机
		NativeCaller.StopSearch();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		NativeCaller.Free();
		Intent intent = new Intent();
		//setClass相当于得到BridgeService的名字
		intent.setClass(this, BridgeService.class);
		//停止BridgeService服务
		stopService(intent);
		tag = 0;
	}
	//定义线程可执行的接口
	Runnable updateThread = new Runnable() {

		public void run() {
			NativeCaller.StopSearch();
			//移除搜索进度框
			progressdlg.dismiss();
			Message msg = updateListHandler.obtainMessage();
			msg.what = 1;
			updateListHandler.sendMessage(msg);
		}
	};
	// 15576341699
	//定义handler对象用于更新消息
	Handler updateListHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				listAdapter.notifyDataSetChanged();
				if (listAdapter.getCount() > 0) {
					//为当前上下文构造一个对话框
					AlertDialog.Builder dialog = new AlertDialog.Builder(
							AddCameraActivity.this);
					//设置对话框的标题
					dialog.setTitle(getResources().getString(
							R.string.add_search_result));
					//为刷新按钮设置确定事件响应
					dialog.setPositiveButton(
							getResources().getString(R.string.refresh),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									startSearch();
								}
							});
					//为搜索结果对话框设置取消按钮监听
					dialog.setNegativeButton(
							getResources().getString(R.string.str_cancel), null);
					//为搜索结果添加事件监听
					dialog.setAdapter(listAdapter,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int arg2) {
									Map<String, Object> mapItem = (Map<String, Object>) listAdapter
											.getItemContent(arg2);
									if (mapItem == null) {
										return;
									}

									String strName = (String) mapItem
											.get(ContentCommon.STR_CAMERA_NAME);
									String strDID = (String) mapItem
											.get(ContentCommon.STR_CAMERA_ID);
									String strUser = ContentCommon.DEFAULT_USER_NAME;
									String strPwd = ContentCommon.DEFAULT_USER_PWD;
									userEdit.setText(strUser);
									pwdEdit.setText(strPwd);
									didEdit.setText(strDID);

								}
							});

					dialog.show();
				} else {
					Toast.makeText(AddCameraActivity.this,
							getResources().getString(R.string.add_search_no),
							Toast.LENGTH_LONG).show();
					isSearched = false;// 
				}
			}
		}
	};
	
	//将长整型数据格式化成ip字符串
	public static String int2ip(long ipInt) {
		//定义字符串构造器，并且设置附加值
		StringBuilder sb = new StringBuilder();
		sb.append(ipInt & 0xFF).append(".");
		sb.append((ipInt >> 8) & 0xFF).append(".");
		sb.append((ipInt >> 16) & 0xFF).append(".");
		sb.append((ipInt >> 24) & 0xFF);
		return sb.toString();
	}

	private void startSearch() {
		listAdapter.ClearAll();
		//设置搜索进度框的内容
		progressdlg.setMessage(getString(R.string.searching_tip));
		//显示进度框
		progressdlg.show();
		//new一个搜索线程并启动
		new Thread(new SearchThread()).start();
		updateListHandler.postDelayed(updateThread, SEARCH_TIME);
	}

	//实现线程接口Runnable的抽象方法
	private class SearchThread implements Runnable {
		@Override
		public void run() {
			Log.d("tag", "startSearch");
			//本地调用开始搜索方法
			NativeCaller.StartSearch();
		}
	}

	//找到当前Activity中的view控件
	private void findView() {
		progressBar = (ProgressBar) findViewById(R.id.main_model_progressBar1);
		textView_top_show = (TextView) findViewById(R.id.login_textView1);
		button_play = (Button) findViewById(R.id.play);
		button_setting = (Button) findViewById(R.id.setting);
		done = (Button) findViewById(R.id.done);
		done.setText("连   接");
		userEdit = (EditText) findViewById(R.id.editUser);
		pwdEdit = (EditText) findViewById(R.id.editPwd);
		didEdit = (EditText) findViewById(R.id.editDID);
		btnSearchCamera = (Button) findViewById(R.id.btn_searchCamera);
		button_play.setOnClickListener(this);
		button_setting.setOnClickListener(this);
	}

	//为控件添加事件响应
	@Override
	public void onClick(View v) {
		//将switch  case修改成if else 备份
//		switch (v.getId()) {
//		case R.id.play:
//			Intent intent = new Intent(AddCameraActivity.this,
//					PlayActivity.class);
//			startActivity(intent);
//			break;
//		case R.id.setting:
//			if (tag == 1) {
//				Intent intent1 = new Intent(AddCameraActivity.this,
//						SettingWifiActivity.class);
//				intent1.putExtra(ContentCommon.STR_CAMERA_ID,
//						SystemValue.deviceId);
//				intent1.putExtra(ContentCommon.STR_CAMERA_NAME,
//						SystemValue.deviceName);
//				intent1.putExtra(ContentCommon.STR_CAMERA_PWD, SystemValue.devicePass);
//				startActivity(intent1);
//				overridePendingTransition(R.anim.in_from_right,
//						R.anim.out_to_left);
//			} else {
//				Toast.makeText(AddCameraActivity.this,
//						getResources().getString(R.string.main_setting_prompt),
//						0).show();
//			}
//			break;
//		case R.id.done:
//			if (tag == 1) {
//				Toast.makeText(AddCameraActivity.this, "ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½Ñ¾ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½×´Ì¬...", 0)
//						.show();
//			} else if (tag == 2) {
//				Toast.makeText(AddCameraActivity.this, "ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½Ó£ï¿½ï¿½ï¿½ï¿½Ôºï¿½...", 0).show();
//			} else {
//				//链接网络摄像头
//				done();
//			}
//
//			break;
//		case R.id.btn_searchCamera:
//			searchCamera();
//			break;
//
//		default:
//			break;
//		}
		
		//修改成if else
		int id = v.getId();
		//预览按钮事件响应
		if (id == R.id.play) {
			Intent intent = new Intent(AddCameraActivity.this,
					PlayActivity.class);
			startActivity(intent);
		} 
		//系统设置按钮事件响应
		else if (id == R.id.setting) {
			if (tag == 1) {
				Intent intent1 = new Intent(AddCameraActivity.this,
						SettingWifiActivity.class);
				intent1.putExtra(ContentCommon.STR_CAMERA_ID,
						SystemValue.deviceId);
				intent1.putExtra(ContentCommon.STR_CAMERA_NAME,
						SystemValue.deviceName);
				intent1.putExtra(ContentCommon.STR_CAMERA_PWD, SystemValue.devicePass);
				startActivity(intent1);
				overridePendingTransition(R.anim.in_from_right,
						R.anim.out_to_left);
			} else {
				Toast.makeText(AddCameraActivity.this,
						getResources().getString(R.string.main_setting_prompt),
						0).show();
			}
		} 
		//连接按钮事件响应
		else if (id == R.id.done) {
			if (tag == 1) {
				Toast.makeText(AddCameraActivity.this, "ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½Ñ¾ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½×´Ì¬...", 0)
						.show();
			} else if (tag == 2) {
				Toast.makeText(AddCameraActivity.this, "ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½Ó£ï¿½ï¿½ï¿½ï¿½Ôºï¿½...", 0).show();
			} else {
				//链接网络摄像头
				done();
			}
		} 
		//局域网搜索按钮事件响应
		else if (id == R.id.btn_searchCamera) {
			//调用自定义搜索函数
			searchCamera();
		} else {
		}
		
		
	}
	
	
	
	

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		 if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){

//　            tv.setText("横屏");

       Toast.makeText(this, "横屏", Toast.LENGTH_LONG).show();

		 }else{

//　            tv.setText("竖屏");
		Toast.makeText(this, "竖屏", Toast.LENGTH_LONG).show();
       

		 }
	}


	//搜索摄像机
	private void searchCamera() {
		if (!isSearched) {
			isSearched = true;
			//执行开始搜索
			startSearch();
		} else {
			//为当Activity构建对话框
			AlertDialog.Builder dialog = new AlertDialog.Builder(
					AddCameraActivity.this);
			//设置对话框标题
			dialog.setTitle(getResources()
					.getString(R.string.add_search_result));
			//设置刷新按钮
			dialog.setPositiveButton(
					getResources().getString(R.string.refresh),
					new DialogInterface.OnClickListener() {
						//刷新按钮事件响应
						@Override
						public void onClick(DialogInterface dialog, int which) {
							startSearch();

						}
					});
			//设置取消按钮
			dialog.setNegativeButton(
					getResources().getString(R.string.str_cancel), null);
			//设置搜索结果适配器
			dialog.setAdapter(listAdapter,
					new DialogInterface.OnClickListener() {
					//每一个搜索结果添加事件响应
						@Override
						public void onClick(DialogInterface dialog, int arg2) {
							Map<String, Object> mapItem = (Map<String, Object>) listAdapter
									.getItemContent(arg2);
							if (mapItem == null) {
								return;
							}
							//得到搜索结果中摄像机的name，did，user，pwd
							String strName = (String) mapItem
									.get(ContentCommon.STR_CAMERA_NAME);
							String strDID = (String) mapItem
									.get(ContentCommon.STR_CAMERA_ID);
							String strUser = ContentCommon.DEFAULT_USER_NAME;
							String strPwd = ContentCommon.DEFAULT_USER_PWD;
							//设置编辑框的内容为选定的参数
							userEdit.setText(strUser);
							pwdEdit.setText(strPwd);
							didEdit.setText(strDID);

						}
					});
			dialog.show();
		}
	}

	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//返回按钮事件响应
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			AddCameraActivity.this.finish();
			return false;
		}
		return false;
	}

	//执行链接网络摄像头
	private void done() {
		Intent in = new Intent();
		//得到输入的用户名，密码，设备id
		String strUser = userEdit.getText().toString();
		String strPwd = pwdEdit.getText().toString();
		String strDID = didEdit.getText().toString();

		//判断did长度不为0
		if (strDID.length() == 0) {
			Toast.makeText(AddCameraActivity.this,
					getResources().getString(R.string.input_camera_id), 0)
					.show();
			return;
		}
		//判断user长度不为0
		if (strUser.length() == 0) {
			Toast.makeText(AddCameraActivity.this,
					getResources().getString(R.string.input_camera_user), 0)
					.show();
			return;
		}
		// in.setAction(ContentCommon.STR_CAMERA_INFO_RECEIVER);
		//如果为无效选项，则设置为添加摄像机选项
		if (option == ContentCommon.INVALID_OPTION) {
			//公共内容 添加摄像机赋值给option
			option = ContentCommon.ADD_CAMERA;
		}
		//设置参数
		in.putExtra(ContentCommon.CAMERA_OPTION, option);
		in.putExtra(ContentCommon.STR_CAMERA_ID, strDID);
		in.putExtra(ContentCommon.STR_CAMERA_USER, strUser);
		in.putExtra(ContentCommon.STR_CAMERA_PWD, strPwd);
		in.putExtra(ContentCommon.STR_CAMERA_TYPE, CameraType);
		progressBar.setVisibility(View.VISIBLE);
		// sendBroadcast(in);
		SystemValue.deviceName = strUser;
		SystemValue.deviceId = strDID;
		SystemValue.devicePass = strPwd;
		//调用设置网络摄像头客户端接口
		BridgeService.setIpcamClientInterface(this);
		//本地调用初始化
		NativeCaller.Init();
		new Thread(new StartPPPPThread()).start();
		// overridePendingTransition(R.anim.in_from_right,
		// R.anim.out_to_left);// ï¿½ï¿½ï¿½ë¶¯ï¿½ï¿½
		// finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			Bundle bundle = data.getExtras();
			String scanResult = bundle.getString("result");
			didEdit.setText(scanResult);
		}
	}

	/**
	 * BridgeService callback
	 * **/
	@Override
	public void callBackSearchResultData(int cameraType, String strMac,
			String strName, String strDeviceID, String strIpAddr, int port) {
		if (!listAdapter.AddCamera(strMac, strName, strDeviceID)) {
			return;
		}
	}

	public String getInfoSSID() {

		info = manager.getConnectionInfo();
		String ssid = info.getSSID();
		return ssid;
	}

	public int getInfoIp() {

		info = manager.getConnectionInfo();
		int ip = info.getIpAddress();
		return ip;
	}

	private Handler PPPPMsgHandler = new Handler() {
		public void handleMessage(Message msg) {

			Bundle bd = msg.getData();
			int msgParam = bd.getInt(STR_MSG_PARAM);
			int msgType = msg.what;
			Log.i("aaa", "===="+msgType+"--msgParam:"+msgParam);
			String did = bd.getString(STR_DID);
			switch (msgType) {
			case ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS:
				int resid;
				switch (msgParam) {
				//正在连接
				case ContentCommon.PPPP_STATUS_CONNECTING:  //0
					resid = R.string.pppp_status_connecting;
					progressBar.setVisibility(View.VISIBLE);
					tag = 2;
					break;
				//连接失败
				case ContentCommon.PPPP_STATUS_CONNECT_FAILED://3
					resid = R.string.pppp_status_connect_failed;
					progressBar.setVisibility(View.GONE);
					tag = 0;
					break;
				//断线
				case ContentCommon.PPPP_STATUS_DISCONNECT://4
					resid = R.string.pppp_status_disconnect;
					progressBar.setVisibility(View.GONE);
					tag = 0;
					break;
				//启动
				case ContentCommon.PPPP_STATUS_INITIALING://1
					resid = R.string.pppp_status_initialing;
					progressBar.setVisibility(View.VISIBLE);
					tag = 2;
					break;
				//无效ID
				case ContentCommon.PPPP_STATUS_INVALID_ID://5
					resid = R.string.pppp_status_invalid_id;
					progressBar.setVisibility(View.GONE);
					tag = 0;
					break;
				//在线
				case ContentCommon.PPPP_STATUS_ON_LINE://2
					resid = R.string.pppp_status_online;
					progressBar.setVisibility(View.GONE);
					tag = 1;
					break;
				//摄像机不在线
				case ContentCommon.PPPP_STATUS_DEVICE_NOT_ON_LINE://6
					resid = R.string.device_not_on_line;
					progressBar.setVisibility(View.GONE);
					tag = 0;
					break;
				//连接超时
				case ContentCommon.PPPP_STATUS_CONNECT_TIMEOUT://7
					resid = R.string.pppp_status_connect_timeout;
					progressBar.setVisibility(View.GONE);
					tag = 0;
					break;
				//密码错误
				case ContentCommon.PPPP_STATUS_CONNECT_ERRER://8
					resid =R.string.pppp_status_pwd_error;
					progressBar.setVisibility(View.GONE);
					tag = 0;
					break;
				default:
					//未知状态
					resid = R.string.pppp_status_unknown;
				}
				textView_top_show.setText(getResources().getString(resid));
				if (msgParam == ContentCommon.PPPP_STATUS_ON_LINE) {
					NativeCaller.PPPPGetSystemParams(did,
							ContentCommon.MSG_TYPE_GET_PARAMS);
				}
				if (msgParam == ContentCommon.PPPP_STATUS_INVALID_ID
						|| msgParam == ContentCommon.PPPP_STATUS_CONNECT_FAILED
						|| msgParam == ContentCommon.PPPP_STATUS_DEVICE_NOT_ON_LINE
						|| msgParam == ContentCommon.PPPP_STATUS_CONNECT_TIMEOUT
						|| msgParam == ContentCommon.PPPP_STATUS_CONNECT_ERRER) {
					NativeCaller.StopPPPP(did);
				}
				break;
			case ContentCommon.PPPP_MSG_TYPE_PPPP_MODE:
				break;

			}

		}
	};

	//桥接服务BridgeService 通知数据
	@Override
	public void BSMsgNotifyData(String did, int type, int param) {
		Log.d("ip", "type:" + type + " param:" + param);
		Bundle bd = new Bundle();
		Message msg = PPPPMsgHandler.obtainMessage();
		msg.what = type;
		bd.putInt(STR_MSG_PARAM, param);
		bd.putString(STR_DID, did);
		msg.setData(bd);
		PPPPMsgHandler.sendMessage(msg);
		if (type == ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS) {
			intentbrod.putExtra("ifdrop", param);
			sendBroadcast(intentbrod);
		}

	}
	

	@Override
	public void BSSnapshotNotify(String did, byte[] bImage, int len) {
		// TODO Auto-generated method stub
		Log.i("ip", "BSSnapshotNotify---len"+len);
	}

	@Override
	public void callBackUserParams(String did, String user1, String pwd1,
			String user2, String pwd2, String user3, String pwd3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void CameraStatus(String did, int status) {

	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

}
