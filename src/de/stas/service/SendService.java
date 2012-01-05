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
import java.util.List;

import de.stas.db.DBWrapper;
import de.stas.db.content.Path;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class SendService extends Service {
	private String client;
	private DBWrapper dbWrapper;
	private SocketAddress sa;
	private SendThread st;
	private static final int SAVE_FILES = 1;
	private static final int RM_FILES = 2;
	private static final int BEGIN_DATA = 3;
	private static final int BEGIN_RM = 4;
	private static final int STRING = 2;
	private static final int ERROR = 3;
	private static final int INTEGER = 0;
	private static final int GET_DISK_SPACE = 5;
	private static final String SERVER = "192.168.178.1";

	
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
	
	public void answerClient(String msg, boolean error) {
		if (client != null) {
			Intent i = new Intent(client);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (error) {
				i.putExtra("error", msg);
			} else {
				i.putExtra("string", msg);
			}
			startActivity(i);
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
		answerClient(msg, false);
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
				throw new Exception(str);
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
			
			public boolean register(String appName) throws RemoteException {
				client = appName;
				System.out.println(client + " registered");
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
					new Thread() {
						@Override
						public void run() {
							sa = new InetSocketAddress(SERVER, 8081);
							try {
								answerClient("new", false);
								Socket s = new Socket();
								s.connect(sa);
								sendSynced(s, getIntBytes(RM_FILES), INTEGER);
								answerClient(new String(receive(s)), false);
								sendSynced(s, getIntBytes(BEGIN_RM), INTEGER);
								answerClient(new String(receive(s)), false);
							} catch (Exception e) {
								answerClient(e.getMessage(), true);
								e.printStackTrace();
							}
						}
						
					}.start();
			}

			@Override
			public void unregister() throws RemoteException {
				System.out.println(client + " unregistered");
				client = null;
			}

			@Override
			public void getRemoteFreeSpace() throws RemoteException {
				new Thread() {
					@Override
					public void run() {
						sa = new InetSocketAddress(SERVER, 8081);
						try {
							answerClient("new", false);
							Socket s = new Socket();
							s.connect(sa);
							sendSynced(s, getIntBytes(GET_DISK_SPACE), INTEGER);
							answerClient(new String(receive(s)) + " mb", false);
						} catch (Exception e) {
							answerClient(e.getMessage(), true);
							e.printStackTrace();
						}
					}
				}.start();
			}

		};
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
			sa = new InetSocketAddress(SERVER, 8081);
			while (true) {
				Socket s = new Socket();
				secondsToWait = 5*60*60;
				try {
					while (secondsToWait > 0) {
						secondsToWait--;
						synchronized (this) {
							wait(1000);
						}
					}
					answerClient("new", false);
					dbWrapper.removeDirtyFilePaths();
					s.connect(sa);
					sendSynced(s, getIntBytes(SAVE_FILES), INTEGER);
					List<Path> paths = dbWrapper.getPaths();
					sendSynced(s, getIntBytes(paths.size()), INTEGER);

					for (Path path : paths) {
						int scanProgress = 0;
						File[] files = new File(path.getPath()).listFiles();
						files = dbWrapper.filterNewFiles(files);
			            sendSynced(s, getIntBytes(files.length), INTEGER);
						for (File file : files) {
							answerClient("proceeding file " + file.getName(), false);
				            byte[] bytes;
				            try {
				            	bytes = getBytesFromFile(file);
				            } catch(Exception e) {
				            	answerClient(e.getMessage(), true);
				            	sendSynced(s, e.getMessage().getBytes(), ERROR);
				            	dbWrapper.addNewFile(file, path);
				            	scanProgress++;
				            	continue;
				            }
				            sendSynced(s, file.getName().getBytes(), STRING);
			
				            sendSynced(s, getIntBytes(bytes.length), INTEGER);
				            sendSynced(s, getIntBytes(BEGIN_DATA), INTEGER);
				            sendData(s.getOutputStream(), bytes);
				            answerClient(new String(receive(s)), false);
				            dbWrapper.addNewFile(file, path);
				            scanProgress++;
				            answerClient("scanProgress: Folder: " + path.getPath() + " " + 100*scanProgress/files.length, false);
						}
					}
				} catch (Exception e) {
					answerClient(e.getMessage(), true);
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

		private void sendData(OutputStream out, byte[] bytes) {
			try {
				int bufferSize = 100;
				int bytesWritten = 0;
				while (bytesWritten < bytes.length) {
					int remainingBytesCount = bytes.length - bytesWritten;
					int actualBufferSize = 0;					
					if (remainingBytesCount <= bufferSize) {
						actualBufferSize = remainingBytesCount;
					} else {
						actualBufferSize = bufferSize;
					}	
					out.write(bytes, bytesWritten, actualBufferSize);
					bytesWritten += actualBufferSize;			
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
}

