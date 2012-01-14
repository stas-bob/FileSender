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
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import de.stas.Main;
import de.stas.R;
import de.stas.ServerException;
import de.stas.db.DBWrapper;
import de.stas.db.content.Path;

public class SendService extends Service {
	private HashMap<String, ClientINTF> clients;
	private DBWrapper dbWrapper;
	private SendThread st;
	private DecimalFormat df = new DecimalFormat("0.00");
	private static final int SAVE_FILES = 1;
	private static final int RM_FILES = 2;
	private static final int BEGIN_DATA = 3;
	private static final int BEGIN_RM = 4;
	private static final int STRING = 2;
	private static final int ERROR = 3;
	private static final int INTEGER = 0;
	private static final int GET_DISK_SPACE = 5;
	private static final int SKIP = 14;
	private RemoteViews notificationView;
	private Notification notification;
	private NotificationManager notificationManager;
	private boolean interrupted;
	private boolean scanning;

	
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
	
	public void sendErrorToClient(String msg) throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			clients.get(appName).error(msg);
		}
	}
	public void sendNewLineToClient(String msg) throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			clients.get(appName).newLine(msg);
		}
	}
	public void sendNewMessagesToClient() throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			clients.get(appName).newMessages();
		}
	}
	public void sendProgressAllToClient(String current, int i) throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			clients.get(appName).progressAll(current, i);
		}
	}
	public void sendProgressDetailToClient(String speed, int i) throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			clients.get(appName).progressDetail(speed, i);
		}
	}
	public void sendCanceledToClient() throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			clients.get(appName).done();
		}
	}
	public void sendStartedToClient() throws RemoteException {
		Set<String> appNames = clients.keySet();
		for (String appName : appNames) {
			clients.get(appName).started();
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
		
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_launcher, "FileSender", System.currentTimeMillis());
		notificationView = new RemoteViews(getPackageName(), R.layout.progress_notification);
		notification.contentView = notificationView;
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.contentIntent = contentIntent;

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
		sendNewLineToClient(msg);
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
	
	private int receiveInt(Socket s) throws Exception {
		int type = readInt(s.getInputStream());
		if ((type & INTEGER) == INTEGER) {
			int data = readInt(s.getInputStream());
			return data;
		}
		throw new Exception("This is not an INTEGER");
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
							sendNewLineToClient(new String(receive(s)));
							sendSynced(s, getIntBytes(BEGIN_RM), INTEGER);
							sendNewLineToClient(new String(receive(s)));
						} catch (Exception e) {
							try {
								if (e.getMessage() == null || e.getMessage().length() == 0) {
									sendErrorToClient(e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1));
								} else {
									sendErrorToClient(e.getMessage());
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
						Socket s = new Socket();
						try {
							int timeout= Integer.parseInt(getPrefs().getString("timeout_list", null)) * 1000;
							s.setSoTimeout(timeout);
							String address = getPrefs().getString("ip/dns", null);
							s.connect(createSA(address), timeout);
							String password = getPrefs().getString("password", null);
							sendSynced(s, password.getBytes(), STRING);
							sendSynced(s, getIntBytes(GET_DISK_SPACE), INTEGER);
							sendNewLineToClient(new String(receive(s)) + " mb");
						} catch (Exception e) {
							try {
								if (e.getMessage() == null || e.getMessage().length() == 0) {
									sendErrorToClient(e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1));
									sendNotSynced(s, e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1).getBytes(), ERROR);
								} else {
									sendErrorToClient(e.getMessage());
									sendNotSynced(s, e.getMessage().getBytes(), ERROR);
								}
							} catch (Exception e1) {
								e1.printStackTrace();
							}
							e.printStackTrace();
						}
						runningFreeSpace = false;
					}
				}.start();
			}

			@Override
			public void interrupt() throws RemoteException {
				interrupted = true;
			}

			@Override
			public boolean isScanning() throws RemoteException {
				return scanning;
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
					scanning = true;
					sendStartedToClient();
					sendNewMessagesToClient();
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
						sendProgressAllToClient(path.getPath(), 0);
						int scanProgress = 0;
						File[] files = new File(path.getPath()).listFiles();
						files = dbWrapper.filterNewFiles(files);
			            sendSynced(s, getIntBytes(files.length), INTEGER);
						for (File file : files) {
							sendProgressAllToClient(path.getPath(), (int)(100*scanProgress/(double)files.length));
							sendNewLineToClient("proceeding file " + file.getName());
				           
				            sendSynced(s, file.getName().getBytes(), STRING);

				            if (file.length() >= 5*1024*1024) {
					            	sendErrorToClient(file.getName() + " too big (> 5MB).\nWill be skipped.");
					            	sendNotSynced(s, (file.getName() + " too big. Will be skipped.").getBytes(), SKIP);
				            } else {
				            	sendNewLineToClient("file size: " + file.length());
					            sendSynced(s, getIntBytes((int)file.length()), INTEGER);
					            sendSynced(s, getIntBytes(BEGIN_DATA), INTEGER);
					            receive(s);
					            sendData(s, file);
					            sendNewLineToClient(new String(receive(s)));
				            }
				            dbWrapper.addNewFile(file, path);
				            scanProgress++;
				            notificationView.setProgressBar(R.id.progress_all_notification_progressBar, 100,(int)(100*scanProgress/(double)files.length), false);
				            notificationView.setTextViewText(R.id.title_notification_textView, "scanning: " + (int)(100*scanProgress/(double)files.length) + "%");
				            notificationManager.notify(0, notification);
						}
					}
					sendCanceledToClient();
				} catch (InterruptedException ie) {
					try {
						sendCanceledToClient();
						interrupted = false;
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					ie.printStackTrace();
				} catch (ServerException se) {
					try {
						sendErrorToClient(se.getMessage());
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					se.printStackTrace();
				} catch (Exception e) {
					try {
						if (e.getMessage() == null || e.getMessage().length() == 0) {
							sendErrorToClient(e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1));
							sendNotSynced(s, e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1).getBytes(), ERROR);
						} else {
							sendErrorToClient(e.getMessage());
							sendNotSynced(s, e.getMessage().getBytes(), ERROR);
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				} finally {
					scanning = false;
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		private class SpeedThread extends Thread {
			private int fileLength;
			private volatile int bytesRemotelyWritten;
			

			public SpeedThread(int length) {
				this.fileLength = length;
			}

			public void setBytesRemotelyWritten(int bytesCount) {
				bytesRemotelyWritten = bytesCount;
			}
			
			@Override
			public void run() {
				int lastBytesRemotelyWritten = 0;
				while (!isInterrupted()) {
					synchronized (this) {
						try {
							wait(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							interrupt();
						}
					}
					double diff = bytesRemotelyWritten - lastBytesRemotelyWritten;
					diff /= 1024.0;
					diff *= 1;
					lastBytesRemotelyWritten = bytesRemotelyWritten;
					boolean mb = false;
					if (diff > 999) {
						diff /= 1024.0;
						mb = true;
					}
					try {
						sendProgressDetailToClient(df.format(diff) + (mb ? " mb/s" : " kb/s"), (int)((100 * bytesRemotelyWritten) / (double)fileLength));
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		};
		private void sendData(Socket s, File file) throws Exception {
			FileInputStream fis = null;
			OutputStream out = s.getOutputStream();
			
			SpeedThread speedThread = new SpeedThread((int)file.length());
			speedThread.start();
			try {
				int bufferSize = 16024;
				int bytesWritten = 0;
				fis = new FileInputStream(file);
				int received = 0;
				while (bytesWritten < file.length()) {
					if (interrupted) {
						throw new InterruptedException();
					}
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
					received = receiveInt(s);
					speedThread.setBytesRemotelyWritten(received);
				}
				while (received < file.length()) {
					received = receiveInt(s);
					speedThread.setBytesRemotelyWritten(received);
				}
			} finally {
				speedThread.interrupt();
				fis.close();
			}
		}
	}
}

