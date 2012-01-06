package de.stas.service;

interface ClientINTF {
	void newMessages();
	void newLine(String line);
	void error(String errMsg);
	void progress(String i);
}