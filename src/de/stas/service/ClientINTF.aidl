package de.stas.service;

interface ClientINTF {
	void newMessages();
	void newLine(String line);
	void error(String errMsg);
	void progressAll(String current, int i);
	void progressDetail(String speed, int i);
}