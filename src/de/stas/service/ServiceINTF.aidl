package de.stas.service;

import de.stas.service.ClientINTF;

interface ServiceINTF {
	boolean register(String appName, ClientINTF clientIntf);
	void unregister(String appName);
	int getTimeTillNextScan();
	void scanNow();
	void deleteRemoteFiles();
	void getRemoteFreeSpace();
	void interrupt();
	boolean isScanning();
}