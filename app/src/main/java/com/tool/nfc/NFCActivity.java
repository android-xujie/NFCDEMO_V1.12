package com.tool.nfc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("NewApi")
public class NFCActivity extends BaseActivity {
    private double latitude = 0.0;
    private double longitude = 0.0;
    private TextView info;
    private LocationManager locationManager;
	@SuppressLint("NewApi")
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// �?��nfc是否�?��
		checkNfc();
		onNewIntent(getIntent());

		initListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation();
            //gps已打开
        } else {
            toggleGPS();
            new Handler() {
            }.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getLocation();
                }
            }, 2000);

        }

	}

	private void initListener() {
		readButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (checkBlock()) {
					MifareClassicCard mifareClassicCard = new MifareClassicCard(
							mifareClassic);
					int block = Integer.parseInt(blockIdEditText.getText()
							.toString().trim());
					String content = mifareClassicCard.readCarCode(block,
							keyEdittext.getText().toString().trim());
					if ("秘钥错误".equals(content) || "读取失败".equals(content))
						setHintToContentEd(content);
					else
						contentEditText.setText(content);
				}

			}
		});
		wriButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkBlock()) {
					MifareClassicCard mifareClassicCard = new MifareClassicCard(
							mifareClassic);
					int block = Integer.parseInt(blockIdEditText.getText()
							.toString().trim());
					String content = contentEditText.getText().toString()
							.trim();
					String result = mifareClassicCard.wirteCarCode(content,
							block, keyEdittext.getText().toString().trim());
					if ("".equals(result))
						setHintToContentEd("写入成功");
					else
						setHintToContentEd(result);

				}

			}
		});

		modifyButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkBlock()) {
					MifareClassicCard mifareClassicCard = new MifareClassicCard(
							mifareClassic);
					int block = Integer.parseInt(blockIdEditText.getText()
							.toString().trim());
					String content = contentEditText.getText().toString()
							.trim();
					String key = keyEdittext.getText().toString().trim();
					String result = mifareClassicCard.modifyPassword(block,
							content, key);
					if ("".equals(result))
						setHintToContentEd("修改成功");
					else
						setHintToContentEd(result);
				}

			}
		});
	}

	@SuppressLint("NewApi")
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (nfcAdapter != null)
			nfcAdapter.enableForegroundDispatch(this, pendingIntent, FILTERS,
					TECHLISTS);

	}

	@SuppressLint("NewApi")
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	@SuppressLint("NewApi")
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

		} else {
			if (tag != null) {

				// 获取卡类型，根据卡类型可推出卡协�?
				String[] techList = tag.getTechList();
				StringBuffer techString = new StringBuffer();
				for (int i = 0; i < techList.length; i++) {
					techString.append(techList[i]);
					techString.append(";\n");
				}
				typeEditText.setText(techString.toString());
				// 获取卡id
				byte[] id = tag.getId();
				carCodeEditText.setText(ByteArrayToHexString(id));

				mifareClassic = MifareClassic.get(tag);
				if (mifareClassic != null) {
					showView(1);
				} else {
					showView(0);
				}
			}

		}
	}

    private void toggleGPS() {
        Intent gpsIntent = new Intent();
        gpsIntent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
        gpsIntent.addCategory("android.intent.category.ALTERNATIVE");
        gpsIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(this, 0, gpsIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
            Location location1 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location1 != null) {
                latitude = location1.getLatitude(); // 经度
                longitude = location1.getLongitude(); // 纬度
				//[FC,00,01,01,00,|,m,a,c,y,u,n,w,e,n,w,i,n,|,3,2,.,2,5,4,6,4,5,6,|,9,0,.,2,5,4,6,4,5,6,|,1,8,3,6,5,2,6,5,8,7,2,|,1,1,2,2,3,3,4,4,2,2,1,1,|,2C]
                ArrayList<String> objects = new ArrayList<>();
                objects.add("FC");
                objects.add("01");
                objects.add("00");
                objects.add("|");
                objects.add("macyunwenwin");
                objects.add("|");
                objects.add("32.2546456");
                objects.add("|");
                objects.add("90.3546456");
                objects.add("|");
                objects.add("112233441111");
                objects.add("|");
                objects.add("2C");
//                objects.add("m");
//                objects.add("a");
//                objects.add("c");
//                objects.add("y");
//                objects.add("u");
//                objects.add("n");
//                objects.add("w");
//                objects.add("e");
//                objects.add("n");
//                objects.add("w");
//                objects.add("i");
//                objects.add("n");
//                objects.add("|");
//                objects.add("3");
//                objects.add("2");
//                objects.add(".");
//                objects.add("2");
//                objects.add("5");
//                objects.add("4");
//                objects.add("6");
//                objects.add("4");
//                objects.add("5");
//                objects.add("6");
//                objects.add("|");
//                objects.add("9");
//                objects.add("0");
//                objects.add(".");
//                objects.add("2");
//                objects.add("5");
//                objects.add("6");
//                objects.add("m");
//                objects.add("a");
//                objects.add("c");
//                objects.add("y");
//                objects.add("u");
//                objects.add("n");
//                objects.add("w");
//                objects.add("e");
//                objects.add("n");
//                objects.add("w");
//                objects.add("i");
//                objects.add("n");
//                objects.add("|");
//                objects.add("3");
//                objects.add("2");
//                objects.add(".");
//                objects.add("2");
//                objects.add("5");
//                objects.add("4");
//                objects.add("6");
//                objects.add("4");
//                objects.add("5");
//                objects.add("6");
//                objects.add("|");
//                objects.add("9");
//                objects.add("0");
//                objects.add(".");
//                objects.add("2");
//                objects.add("5");
//                objects.add("6");
//                objects.add("|");
//                objects.add("1");
//                objects.add("8");
//                objects.add("3");
//                objects.add("6");
//                objects.add("5");
//                objects.add("2");
//                objects.add("6");
//                objects.add("5");
//                objects.add("8");
//                objects.add("7");
//                objects.add("2");
//                objects.add("|");
//                objects.add("1");
//                objects.add("1");
//                objects.add("1");
//                objects.add("2");
//                objects.add("2");
//                objects.add("2");
//                objects.add("3");
//                objects.add("3");
//                objects.add("3");
//                objects.add("3");
//                objects.add("3");
//                objects.add("4");
//                objects.add("4");
//                objects.add("4");
//                objects.add("4");
//                objects.add("|");
//                objects.add("2C");
//                byte [] bytes=new byte[objects.size()];
                for (int i = 0; i < objects.size(); i++) {
                    byte[] bytes = objects.get(i).getBytes();
                    Log.e("123123123", "toggleGPS: "+bytes, null);
                }
            }
        }
    }

    private void getLocation() {
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        } else {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
		Log.e("123","纬度：" + latitude + "\n" + "经度：" + longitude);
        ArrayList<String> objects = new ArrayList<>();
        objects.add("FC");
        objects.add("1");
        objects.add("0");
        objects.add("|");
        objects.add("macyunwenwin");
        objects.add("|");
        objects.add("32.2546456");
        objects.add("|");
        objects.add("90.3546456");
        objects.add("|");
        objects.add("112233441111");
        objects.add("|");
        objects.add("2C");
        String s="FC010010|macyunwenwin|32.2546456|90.3546456|14706170560|123123123132|2C";
        byte[] bytes1 = s.getBytes();
        TalkClient4Byte(bytes1);
        for (int i = 0; i < objects.size(); i++) {
            byte[] bytes = objects.get(i).getBytes();
//            Log.e("123123123", "toggleGPS: "+bytes.length+"---"+bytes.toString(), null);
            for (int j = 0; j < bytes.length; j++) {
                byte aByte = bytes[j];
//                Log.e("123123111", "getLocation: "+aByte,null );
            }
        }
    }


    private Socket socket;
    private SocketAddress address;

    public void TalkClient4Byte(final byte[] bytes1) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                socket = new Socket();
                address = new InetSocketAddress("60.205.187.77", 337);
                try {
                    socket.connect(address, 1000);
                    talk(bytes1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void talk(byte[] bytes1) {
        try {
            //使用DataInputStream封装输入流
            InputStream os = new DataInputStream(System.in);
            try (DataOutputStream dos // 建立输出流
                         = new DataOutputStream(socket.getOutputStream())) {
                dos.write(bytes1, 0, bytes1.length); // 向输出流写入 bytes
                dos.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }

    LocationListener locationListener = new LocationListener() {
        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {
            Log.e("123", provider);
        }

        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {
            Log.e("123", provider);
        }

        // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                Log.e("Map", "Location changed : Lat: " + location.getLatitude() + " Lng: " + location.getLongitude());
                latitude = location.getLatitude(); // 经度
                longitude = location.getLongitude(); // 纬度
            }
        }
    };

//	public  Map<String, Double> getLocationInfo() {
//		Map<String, Double> locationInfo = null;
////		Context application = MyApplication.getApplication();
//		LocationManager mLocal = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//		if (mLocal.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
//			locationInfo = new HashMap<>();
//			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//					!= PackageManager.PERMISSION_GRANTED
//					&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//					!= PackageManager.PERMISSION_GRANTED) {
//				locationInfo.put("Latitude", .0);
//				locationInfo.put("Longitude", .0);
//				return locationInfo;
//			} else {
//				Location lastKnownLocation = mLocal.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//				if (null != lastKnownLocation) {
//					locationInfo.put("Latitude", lastKnownLocation.getLatitude());
//					locationInfo.put("Longitude", lastKnownLocation.getLongitude());
//				}
//			}
//		} else {
//			locationInfo.put("Latitude", .0);
//			locationInfo.put("Longitude", .0);
//		}
//		return locationInfo;
//	}
}
