package de.stas.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import de.stas.R;
import de.stas.ServerException;
import de.stas.db.DBWrapper;
import de.stas.db.content.Path;

public class SendService extends Service {
	private HashMap<String, ClientINTF> clients;
	private DBWrapper dbWrapper;
	private SendThread st;
	private static final int SAVE_FILES = 1;
	private static final int RM_FILES = 2;
	private static final int BEGIN_DATA = 3;
	private static final int BEGIN_RM = 4;
	private static final int STRING = 2;
	private static final int ERROR = 3;
	private static final int INTEGER = 0;
	private static final int GET_DISK_SPACE = 5;
	private static final int NEW_LINE = 6;
	private static final int NEW = 7;
	private static final int PROGRESS = 8;
	private static final int SKIP = 14;

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("destroy");
		System.exit(0);
	}
	
	public void answerClient(String msg, int type) throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			switch (type) {
			case ERROR: clients.get(appName).error(msg); break;
			case NEW_LINE: clients.get(appName).newLine(msg); break;
			case NEW: clients.get(appName).newMessages(); break;
			case PROGRESS: clients.get(appName).progress(msg); break;
			}
		}
	}
	
	public String readString(InputStream s) throws IOException {
		String str = "";
		int i = 0;
		while ((i = s.read()) != 0) {
			str += (char)i;
		}
		return str;
	}
	
	
	public byte[] getIntBytes(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.asIntBuffer().put(i);
        return buffer.array();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		clients = new HashMap<String, ClientINTF>();
		dbWrapper = new DBWrapper(getApplicationContext());
		st = new SendThread();
		st.start();
	}
	
	public int readInt(InputStream is) throws IOException {
		byte[] integer = new byte[4];
		int b;
		for (int i = 0; i < 4 && (b = is.read()) != -1; i++) {
			integer[i] = (byte)b;
		}
		ByteBuffer buf = ByteBuffer.wrap(integer);
		return buf.asIntBuffer().get();
	}
	
	public void sendSynced(Socket s, byte[] ptr, int type) throws Exception {
		s.getOutputStream().write(toLittleEndian(type));
		if ((type & 0x2) == STRING) {
			s.getOutputStream().write(toLittleEndian(ptr.length));
			s.getOutputStream().write(ptr);
		} else {
			ByteBuffer buf = ByteBuffer.wrap(ptr);
			int i = buf.getInt();
			s.getOutputStream().write(toLittleEndian(i));
		}
		String msg = new String(receive(s));
		answerClient(msg, NEW_LINE);
	}
	
	public void sendNotSynced(Socket s, byte[] ptr, int type) throws Exception {
		s.getOutputStream().write(toLittleEndian(type));
		if ((type & 0x2) == STRING) {
			s.getOutputStream().write(toLittleEndian(ptr.length));
			s.getOutputStream().write(ptr);
		} else {
			ByteBuffer buf = ByteBuffer.wrap(ptr);
			int i = buf.getInt();
			s.getOutputStream().write(toLittleEndian(i));
		}
	}
	
	public byte[] toLittleEndian(int i) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(i);
		return buf.array();
	}
	
	public byte[] receive(Socket s) throws Exception {
		int type = readInt(s.getInputStream());
		if ((type & STRING) == STRING) {
			int length = readInt(s.getInputStream());
			String str = "";
			int c;
			for (int i = 0; i < length; i++) {
				c = s.getInputStream().read();
				str += (char)c;
			}
			if ((type & ERROR) == ERROR) {
				throw new ServerException(str);
			} else {
				return str.getBytes();
			}
		} else {
			int data = readInt(s.getInputStream());
			return (data + "").getBytes();
		}
	}
	
	public byte[] getBytesFromFile(File file) throws Exception {
	    InputStream is = new FileInputStream(file);

	    long length = file.length();

	    if (length > 2*1000*1000) {
	    	throw new Exception("File " + file.getName() + " is too big");
	    }
	    byte[] bytes = new byte[(int)length];

	    int offset = 0;
	    int bytesRead = 0;
	    while (offset < bytes.length && (bytesRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
	        offset += bytesRead;
	    }

	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file " + file.getName());
	    }

	    is.close();
	    return bytes;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return new ServiceINTF.Stub() {
			private boolean runningDelete;
			private boolean runningFreeSpace;
			
			synchronized public boolean register(String appName, ClientINTF clientIntf) throws RemoteException {
				clients.put(appName, clientIntf);
				System.out.println(appName + " registered");
				return true;
			}
			
			public int getTimeTillNextScan() throws RemoteException {
				return st.getSecondsToWait();
			}

			@Override
			public void scanNow() throws RemoteException {
				st.setSecondsToWait(0);
			}

			@Override
			public void deleteRemoteFiles() throws RemoteException {
				synchronized (this) {
					if(runningDelete) {
						return;
					} else {
						runningDelete = true;
					}
				}
				new Thread() {
					@Override
					public void run() {
						try {
							Socket s = new Socket();
							int timeout = Integer.parseInt(getPrefs().getString("timeout_list", null)) * 1000;
							s.setSoTimeout(timeout);
							String address = getPrefs().getString("ip/dns", null);
							s.connect(createSA(address), timeout);
							String password = getPrefs().getString("password", null);
							sendSynced(s, password.getBytes(), STRING);
							sendSynced(s, getIntBytes(RM_FILES), INTEGER);
							answerClient(new String(receive(s)), NEW_LINE);
							sendSynced(s, getIntBytes(BEGIN_RM), INTEGER);
							answerClient(new String(receive(s)), NEW_LINE);
						} catch (Exception e) {
							try {
								if (e.getMessage() == null || e.getMessage().length() == 0) {
									answerClient(e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1), ERROR);
								} else {
									answerClient(e.getMessage(), ERROR);
								}
							} catch (RemoteException e1) {
								e1.printStackTrace();
							}
							e.printStackTrace();
						}
						runningDelete = false;
					}
					
				}.start();
			}

			@Override
			synchronized public void unregister(String appName) throws RemoteException {
				if (clients.size() == 0) {
					System.out.println(appName + " unregisters but is not registered!");
				} else {
					System.out.println(appName + " unregistered");
					clients.remove(appName);
				}
			}

			@Override
			public void getRemoteFreeSpace() throws RemoteException {
				synchronized (this) {
					if(runningFreeSpace) {
						return;
					} else {
						runningFreeSpace = true;
					}
				}
				new Thread() {
					@Override
					public void run() {
						try {
							Socket s = new Socket();
							int timeout= Integer.parseInt(getPrefs().getString("timeout_list", null)) * 1000;
							s.setSoTimeout(timeout);
							String address = getPrefs().getString("ip/dns", null);
							s.connect(createSA(address), timeout);
							String password = getPrefs().getString("password", null);
							sendSynced(s, password.getBytes(), STRING);
							sendSynced(s, getIntBytes(GET_DISK_SPACE), INTEGER);
							answerClient(new String(receive(s)) + " mb", NEW_LINE);
						} catch (Exception e) {
							try {
								if (e.getMessage() == null || e.getMessage().length() == 0) {
									answerClient(e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1), ERROR);
								} else {
									answerClient(e.getMessage(), ERROR);
								}
							} catch (RemoteException e1) {
								e1.printStackTrace();
							}
							e.printStackTrace();
						}
						runningFreeSpace = false;
					}
				}.start();
			}
		};
	}

	public SharedPreferences getPrefs() {
		return getSharedPreferences(getPackageName() + "_preferences", MODE_MULTI_PROCESS);
	}
	
	public SocketAddress createSA(String address) throws Exception {
		Pattern patt = Pattern.compile("^(.+):(\\d?\\d?\\d?\\d?\\d)$");
		Matcher m = patt.matcher(address);
		if (!m.matches()) throw new Exception("ip adress invalid");
		SocketAddress sa = new InetSocketAddress(m.group(1), Integer.parseInt(m.group(2)));
		return sa;
	}
	
	class SendThread extends Thread {
		private volatile int secondsToWait;
		
		public int getSecondsToWait() {
			return secondsToWait;
		}
		
		public void setSecondsToWait(int sec) {
			secondsToWait = sec;
		}
		
		
		@Override
		public void run() {
			while (true) {
				Socket s = new Socket();
				try {
					secondsToWait = Integer.parseInt(getPrefs().getString("scan_period", null));
					while (secondsToWait > 0) {
						secondsToWait--;
						synchronized (this) {
							wait(1000);
						}
					}
					answerClient("new", NEW);
					dbWrapper.removeDirtyFilePaths();
					String address = getPrefs().getString("ip/dns", null);
					SocketAddress sa = createSA(address);
					int timeout = Integer.parseInt(getPrefs().getString("timeout_list", null)) * 1000;
					s.setSoTimeout(timeout);
					s.connect(sa, timeout);
					String password = getPrefs().getString("password", null);
					sendSynced(s, password.getBytes(), STRING);
					sendSynced(s, getIntBytes(SAVE_FILES), INTEGER);
					List<Path> paths = dbWrapper.getPaths();
					sendSynced(s, getIntBytes(paths.size()), INTEGER);

					for (Path path : paths) {
						int scanProgress = 0;
						File[] files = new File(path.getPath()).listFiles();
						files = dbWrapper.filterNewFiles(files);
			            sendSynced(s, getIntBytes(files.length), INTEGER);
						for (File file : files) {
							answerClient("proceeding file " + file.getName(), NEW_LINE);
				           
				            sendSynced(s, file.getName().getBytes(), STRING);

				            if (file.length() >= 10*1024*1024) {
					            	answerClient(file.getName() + " too big (> 10MB).\nWill be skipped.", ERROR);
					            	sendNotSynced(s, (file.getName() + " too big. Will be skipped.").getBytes(), SKIP);
				            } else {
				            	answerClient("file size: " + file.length(), NEW_LINE);
					            sendSynced(s, getIntBytes((int)file.length()), INTEGER);
					            sendSynced(s, getIntBytes(BEGIN_DATA), INTEGER);
					            sendData(s, file);
					            answerClient(new String(receive(s)), NEW_LINE);
				            }
				            dbWrapper.addNewFile(file, path);
				            scanProgress++;
				            answerClient("scanProgress: Folder: " + path.getPath() + " " + 100*scanProgress/files.length, PROGRESS);
						}
					}
				} catch (ServerException se) {
					try {
						answerClient(se.getMessage(), ERROR);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					try {
						if (e.getMessage() == null || e.getMessage().length() == 0) {
							answerClient(e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1), ERROR);
							sendNotSynced(s, e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1).getBytes(), ERROR);
						} else {
							answerClient(e.getMessage(), ERROR);
							sendNotSynced(s, e.getMessage().getBytes(), ERROR);
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				} finally {
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private void sendData(Socket s, File file) throws NumberFormatException, Exception {
			FileInputStream fis = null;
			OutputStream out = s.getOutputStream();
			InputStream in = s.getInputStream();
			try {
				int bufferSize = 1024;
				int bytesWritten = 0;
				fis = new FileInputStream(file);
				while (bytesWritten < file.length()) {
					int remainingBytesCount = (int)file.length() - bytesWritten;
					int actualBufferSize = 0;					
					if (remainingBytesCount <= bufferSize) {
						actualBufferSize = remainingBytesCount;
					} else {
						actualBufferSize = bufferSize;
					}	
					byte[] bytes = new byte[actualBufferSize];
					fis.read(bytes);
					out.write(bytes);
					bytesWritten += actualBufferSize;			
					try {
						String data = new String(receive(s));
						answerClient(100*Integer.parseInt(data)/file.length() + "", PROGRESS);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			} finally {
				fis.close();
			}
			
		}
	}
}

