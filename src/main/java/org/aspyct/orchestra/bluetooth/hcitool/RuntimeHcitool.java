package org.aspyct.orchestra.bluetooth.hcitool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import org.aspyct.orchestra.api.bluetooth.BluetoothDevice;

public class RuntimeHcitool implements Hcitool {
	private Runtime runtime;

	public RuntimeHcitool(Runtime runtime) {
		super();
		this.runtime = runtime;
	}
	
	/**
	 * 
	 * @param device the bluetooth device to look for
	 * @post device.name is set if the device is found
	 * @post device.lastSeen is updated if the device is found
	 * @return true if the device is found, false otherwise
	 */
	public boolean name(BluetoothDevice device) {
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
				
				if (!name.isEmpty()) {
					device.setName(name);
					device.setLastSeen(Calendar.getInstance().getTime());
					
					return true;
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
		
		return false;
	}
}
