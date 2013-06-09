package org.aspyct.orchestra.bluetooth.hcitool;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspyct.orchestra.api.bluetooth.BluetoothDevice;
import org.junit.Before;
import org.junit.Test;

public class HcitoolBluetoothMonitorTest {
	private HcitoolBluetoothMonitor monitor;
	
	private Hcitool hcitool;
	private BluetoothDevice device;

	@Before
	public void setUp() throws Exception {
		monitor = new HcitoolBluetoothMonitor();
		
		hcitool = mock(Hcitool.class);
		monitor.setHcitool(hcitool);
		
		device = new BluetoothDevice("hello there");
	}

	@Test
	public void monitorShouldTrackPresentDevices() {
		deviceIsPresent(device);
		
		monitor.watchDevice(device);
		monitor.scanForDevices();
		
		verify(hcitool).name(device);
		assertTrue(monitor.isPresent(device));
	}
	
	@Test
	public void monitorShouldNotConsiderDevicePresentIfNotScannable() {
		monitor.watchDevice(device);
		monitor.scanForDevices();
		
		verify(hcitool).name(device);
		assertFalse(monitor.isPresent(device));
	}
	
	@Test
	public void monitorShouldDiscardDevicesWhenTheyDisappear() {
		monitor.watchDevice(device);
		
		deviceIsPresent(device);
		monitor.scanForDevices();
		
		deviceIsNotPresent(device);
		monitor.scanForDevices();
		
		assertFalse(monitor.isPresent(device));
	}
	
	@Test
	public void monitorDoesNotWatchArbitraryDevices() {
		monitor.scanForDevices();
		
		verify(hcitool, never()).name(device);
	}

	private void deviceIsPresent(BluetoothDevice device) {
		when(hcitool.name(device)).thenReturn(true);
	}
	
	private void deviceIsNotPresent(BluetoothDevice device) {
		when(hcitool.name(device)).thenReturn(false);
	}
}
