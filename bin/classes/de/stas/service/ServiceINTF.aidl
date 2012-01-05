package de.stas.service;

interface ServiceINTF {
	boolean register(String appName);
	void unregister();
	int getTimeTillNextScan();
	void scanNow();
	void deleteRemoteFiles();
	void getRemoteFreeSpace();
}