package org.aspyct.orchestra.bluetooth.hcitool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.aspyct.orchestra.api.bluetooth.BluetoothDevice;
import org.aspyct.orchestra.api.bluetooth.BluetoothListener;
import org.aspyct.orchestra.api.bluetooth.BluetoothMonitor;

public class HcitoolBluetoothMonitor implements BluetoothMonitor, Runnable {
	private List<BluetoothListener> listeners;
	
	private Set<BluetoothDevice> watchedDevices;
	private Set<BluetoothDevice> liveDevices;
	
	private long longScanInterval;
	private long shortScanInterval;
	
	private Timer scanTimer;

	public HcitoolBluetoothMonitor() {
		listeners = new ArrayList<BluetoothListener>();
		
		watchedDevices = new TreeSet<BluetoothDevice>();
		liveDevices = new TreeSet<BluetoothDevice>();
		
		setLongScanInterval(5 * 60 * 1000);
		setShortScanInterval(1 * 1000);
		
		scanTimer = new Timer();
	}
	
	public void watchDevice(BluetoothDevice device) {
		synchronized (watchedDevices) {
			if (!watchedDevices.contains(device)) {
				watchedDevices.add(device);
			}
		}
	}

	public void stopWatching(BluetoothDevice device) {
		synchronized (watchedDevices) {
			watchedDevices.remove(device);
		}
		
		synchronized (liveDevices) {
			liveDevices.remove(device);
		}
	}

	public List<BluetoothDevice> listDevices() {
		synchronized (liveDevices) {
			return new ArrayList<BluetoothDevice>(liveDevices);
		}
	}

	public boolean isAnyDevicePresent() {
		synchronized (liveDevices) {
			return liveDevices.size() > 0;
		}
	}

	public boolean isPresent(BluetoothDevice device) {
		synchronized (liveDevices) {
			return liveDevices.contains(device);
		}
	}

	public void addBluetoothListener(BluetoothListener listener) {
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}

	public void removeBluetoothListener(BluetoothListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void decreaseBluetoothScanRate() {
		rescheduleTimer(shortScanInterval);
	}

	public void increaseBluetoothScanRate() {
		rescheduleTimer(longScanInterval);
	}
	
	private void rescheduleTimer(long interval) {
		scanTimer.cancel();
		
		scanTimer = new Timer();
		scanTimer.scheduleAtFixedRate(timerTask, interval, interval);
	}

	public long getLongScanInterval() {
		return longScanInterval;
	}

	public void setLongScanInterval(long longScanInterval) {
		this.longScanInterval = longScanInterval;
	}

	public long getShortScanInterval() {
		return shortScanInterval;
	}

	public void setShortScanInterval(long shortScanInterval) {
		this.shortScanInterval = shortScanInterval;
	}

	public void run() {
		scanTimer.scheduleAtFixedRate(timerTask, 0, longScanInterval);
	}
	
	private TimerTask timerTask = new TimerTask() {
		@Override
		public void run() {
			System.out.println("Running now");
			
			List<BluetoothDevice> watch;
			synchronized (watchedDevices) {
				watch = new ArrayList<BluetoothDevice>(watchedDevices);
			}
			
			Runtime runtime = Runtime.getRuntime();
			for (BluetoothDevice device: watch) {
				System.out.println("Looking for " + device.getMacAddr());
				try {
					Process hcitool = runtime.exec(new String[] {
							"hcitool",
							"name",
							device.getMacAddr()
					});
					
					int code = hcitool.waitFor();
					
					if (code == 0) {
						BufferedReader br = new BufferedReader(new InputStreamReader(hcitool.getInputStream()));
						String name = br.readLine().trim();
						
						if (name.isEmpty()) {
							System.out.println("Not found: " + device.getMacAddr());
							synchronized (liveDevices) {
								liveDevices.remove(device);
							}
						}
						else {
							System.out.println("Found: " + device.getMacAddr() + " as \"" + name + "\"");
							synchronized (liveDevices) {
								liveDevices.add(device);
							}
						}
					}
					else {
						System.err.println("Command exited with status " + code);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public static void main(String[] args) {
		HcitoolBluetoothMonitor monitor = new HcitoolBluetoothMonitor();
		monitor.setShortScanInterval(30 * 1000);
		
		for (String addr: args) {
			BluetoothDevice device = new BluetoothDevice();
			device.setMacAddr(addr);
			monitor.watchDevice(device);
		}
		
		monitor.decreaseBluetoothScanRate();
	}
}
