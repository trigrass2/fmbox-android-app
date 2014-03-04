package org.ionic.bluetooth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.cordova.xxx.CustomPlugin;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

public class BlueTooth {
	
	private static final String TAG = "BlueTooth";
	private static BluetoothAdapter adapter;
	private static BlockingQueue<Message> connQueue = new LinkedBlockingQueue<Message>(64);
	private static Activity activity;

	public static void send(String msg) {
		Message m = new Message();
		m.what = 1;
		m.obj = msg;
		try {
			connQueue.put(m);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

	public static String BoundStateStr(int i) {
		switch (i) {
		case BluetoothDevice.BOND_BONDING:
			return "BOND_BONDING";
		case BluetoothDevice.BOND_BONDED:
			return "BOND_BONDED";
		case BluetoothDevice.BOND_NONE:
			return "BOND_NONE";
		}
		return "";
	}
		
	// web call scan	
	// web message
	// "BlueToothDisconnect"
	// "BlueToothConnectFailed"
	// "BlueToothConnceted"
	
	private static String sockLock = "";
	private static BluetoothSocket sock;
		
	private static Thread connThread = new Thread(new Runnable() {
		
    	private OutputStream out;
    	
		public void send(String msg) {
			Log.d(TAG, "send:" + msg);
			msg += "\n";
    		try {
				out.write(msg.getBytes());
			} catch (Exception e) {
			}
    	}
		
		private String readLine(InputStream in) throws IOException {
			byte[] arr = new byte[4096];
			int i;
			for (i = 0; i < arr.length-2; i++) {
				byte[] b = new byte[1];
				in.read(b);
				arr[i] = b[0];
				if (b[0] == 13)
					break;
			}
			arr[i] = 0;
			arr[i+1] = 0;
			return new String(arr, "UTF-8");
		}
		
		private void recvThread(final InputStream in) {
			Thread t = new Thread(new Runnable() {
				public void run() {
					Log.d(TAG, "recv starts");
					while (true) {
			    		try {
			    			String msg = readLine(in);
			    			Log.d(TAG, "recv: " + msg);
							CustomPlugin.send(msg);
						} catch (Exception e) {
							Log.d(TAG, "read failed");
							return;
						}
					}
				}
			});
			t.start();
		}
		
		public void run() {
			while (true) {
				try {
					handleMessage(connQueue.take());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		private boolean conn(BluetoothDevice dev) {
			BluetoothSocket s = null;
			adapter.cancelDiscovery();
			Log.d(TAG, "connHandler: conn: create socket");
			
			ParcelUuid[] uuids = dev.getUuids();
			for (int i = 0; i < uuids.length; i++) {
				Log.d(TAG, "connHandler: conn:   uuid: " + uuids[i].getUuid());
			}
			
			try {
				//dev.fetchUuidsWithSdp();
				Method m = dev.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
				s = (BluetoothSocket) m.invoke(dev, 1);
				 
				//s = dev.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
			} catch (Exception e1) {
				e1.printStackTrace();
				Log.d(TAG, "connHandler: create socket failed");
				return false;
			}

			try {
				Log.d(TAG, "connHandler: connect");
				s.connect();
				Log.d(TAG, "connHandler: connect ok");
				recvThread(s.getInputStream());
				out = s.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(TAG, "connHandler: connect failed");
				return false;
			}

			synchronized (sockLock) {
				sock = s;
			}
			return true;
		}
    	
        public void handleMessage(Message msg) throws InterruptedException {
        	switch (msg.what) {
        	case 0:
        		for (int i = 0; i < 10; i++) {
        			if (conn((BluetoothDevice)msg.obj)) {
        				CustomPlugin.send("BlueToothConnceted");
        				return;
        			}
        			Log.d(TAG, "conn: try " + i);
        			Thread.sleep(1000);
        		}
				CustomPlugin.send("BlueToothConnectFailed");       		
        		break;
        	case 1:
        		send((String)msg.obj);
        		break;
        	}
        };

	});
	
	private static int deviceMatch(BluetoothDevice dev) {
		if (!dev.getName().equals("HC-06"))
			return 0;
		if (dev.getBondState() != BluetoothDevice.BOND_BONDED)
			return 1;
		return 2;
	}
	
	public static BroadcastReceiver receiverScan = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			Log.d(TAG, action);

			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				Log.d(TAG,
						"found: " + device.getName() + " "
								+ device.getAddress() + " "
								+ BoundStateStr(device.getBondState()));
				int match = deviceMatch(device);
				if (match == 2) {
					adapter.cancelDiscovery();
					Message m = new Message();
					m.what = 0;
					m.obj = device;
					try {
						connQueue.put(m);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (match == 1) {
					Log.d(TAG, "  NeedPair");
					CustomPlugin.send("BlueToothNeedPair");
				}
			}
			if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
			}
		}
	};
	
	public static void scan() {
		Log.d(TAG, "scan");

		synchronized (sockLock) {
			if (sock != null && sock.isConnected()) {
				Log.d(TAG, "already connected");
				return;
			}
		}

		adapter.cancelDiscovery();
		
		for (BluetoothDevice dev : adapter.getBondedDevices()) {
			if (deviceMatch(dev) == 2) {
				Log.d(TAG, "found already bounded");
				Message m = new Message();
				m.what = 0;
				m.obj = dev;
				try {
					connQueue.put(m);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		activity.registerReceiver(receiverScan, filter);

		adapter.startDiscovery();
	}

	public synchronized static void init(Activity a) {
		if (adapter != null)
			return; 
		
	    activity = a;
		Log.d(TAG, "init");

		adapter = BluetoothAdapter.getDefaultAdapter();
	    
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    activity.startActivityForResult(enableBtIntent, 1);
		connThread.start();
	}
	
}
