package de.stas;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.stas.db.content.Path;
import de.stas.service.ClientINTF;
import de.stas.service.ServiceINTF;

public class Main extends BaseActivity implements ServiceConnection, OnItemClickListener {
	private ListView pathsListView, msgsListView;
	private ServiceINTF service;
	private TextView timer;
	private TextView progress;
	private Callback callback;
	
	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.main);
		timer = (TextView)findViewById(R.id.timer_textView);
		pathsListView = (ListView)findViewById(R.id.path_listView);
		msgsListView = (ListView)findViewById(R.id.cur_act_listView);
		pathsListView.setEmptyView((TextView)findViewById(R.id.path_emptyView_textView));
		msgsListView.setEmptyView((TextView)findViewById(R.id.cur_act_emptyView_textView));
		progress = (TextView)findViewById(R.id.progress_textView);
		pathsListView.setOnItemClickListener(this);
		try {
			List<Path> paths = dbwrapper.getPaths();
			List<String> msgs = new ArrayList<String>();
			pathsListView.setAdapter(new PathsArrayAdapter(this, R.id.path_textView, paths));
			msgsListView.setAdapter(new MsgsArrayAdapter(this, R.id.msg_textView, msgs));
		} catch (Exception e) {
			warningDialog.setText(e.getMessage());
			e.printStackTrace();
		}
		callback = new Callback();
	}
	
	class PathsArrayAdapter extends ArrayAdapter<Path> {
		private Context context;
		
		public PathsArrayAdapter(Context context, int id, List<Path> objects) {
			super(context, id, objects);
			this.context = context;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Path path = getItem(position);
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.path_list, null);
				holder = new ViewHolder(convertView.findViewById(R.id.path_textView));
				convertView.setTag(holder);
			}
			holder = (ViewHolder) convertView.getTag();
			TextView tv = holder.getPath();
			tv.setText(path.getPath());
			return convertView;
		}
		private class ViewHolder {
			private TextView path;
			
			public ViewHolder(View filename) {
				this.path = (TextView)filename;
			}
			
			public TextView getPath() {
				return path;
			}
		}
	}
	
	class MsgsArrayAdapter extends ArrayAdapter<String> {
		private Context context;
		public MsgsArrayAdapter(Context context, int id, List<String> objects) {
			super(context, id, objects);
			this.context = context;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			String msg = getItem(position);
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.msg_list, null);
				holder = new ViewHolder(convertView.findViewById(R.id.msg_textView));
				convertView.setTag(holder);
			}
			holder = (ViewHolder) convertView.getTag();
			TextView tv = holder.getMsg();
			tv.setText(msg);
			return convertView;
		}
		private class ViewHolder {
			private TextView msg;
			
			public ViewHolder(View filename) {
				this.msg = (TextView)filename;
			}
			
			public TextView getMsg() {
				return msg;
			}
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem addPath = menu.add("Add path");
		addPath.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startActivityForResult(new Intent(Main.this, Directory.class), 0);
				return false;
			}
		});
		MenuItem sendNow = menu.add("scan now");
		sendNow.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				try {
					service.scanNow();
				} catch(Exception e) {}
				return false;
			}
		});
		MenuItem delete = menu.add("delete remote files");
		delete.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				try {
					service.deleteRemoteFiles();
				} catch(Exception e) {}
				return false;
			}
		});
		MenuItem freeDiscSpace = menu.add("free disc space");
		freeDiscSpace.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				try {
					service.getRemoteFreeSpace();
				} catch(Exception e) {}
				return false;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED)
			return;
	
		switch (requestCode) {
		case 0:
			String path = data.getExtras().getString("path");
			try {
				dbwrapper.resume();
				Path p = dbwrapper.savePath(path);
				if (p != null) {
					((PathsArrayAdapter)pathsListView.getAdapter()).add(p);
					((PathsArrayAdapter)pathsListView.getAdapter()).notifyDataSetChanged();
				}
			} catch (Exception e) {
				warningDialog.setText(e.getMessage());
				showDialog(WARNING_DIALOG);
				e.printStackTrace();
			}
			break;
		}
	}

	@Override
	public void onServiceConnected(ComponentName arg0, IBinder arg1) {
		service = ServiceINTF.Stub.asInterface(arg1);
		
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		service = null;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Intent i = new Intent();
		i.setAction("de.stas.service.SendService");
		startService(i);
		bindService(i, this, BIND_AUTO_CREATE);
		new Thread() {
			private volatile boolean interrupted;
			
			public void inter() {
				interrupted = true;
			}
			@Override
			public void run() {
				while (service == null);
				try {
					if(!service.register(getComponentName().getClassName(), callback)) {
						throw new RuntimeException("could not bind service");
					}
					while(!interrupted) {
						timer.post(new Runnable() {
							@Override
							public void run() {
								try {
									int sec = service.getTimeTillNextScan();
									int min = (sec / 60)%60;
									timer.setText("Next scan in " + sec / 3600 + "h " + min + "m " + sec % 60 + "s");
								} catch (RemoteException e) {
									inter();
								}
							}
						});
						synchronized(this) {
							try {
								wait(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} catch (android.os.DeadObjectException ee) {
					
				} catch (final RemoteException e) {
					timer.post(new Runnable() {
						@Override
						public void run() {
							warningDialog.setText(e.getMessage());
							showDialog(WARNING_DIALOG);
							e.printStackTrace();
						}
					});
				}
			}
		}.start();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		try {
			if (service != null) {
				service.unregister(getComponentName().getClassName());
				unbindService(this);
			}
		} catch(Exception e) {}
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
		if (arg0 == pathsListView) {
			final TextView tv = (TextView)arg1.findViewById(R.id.path_textView);
			confirmDialog.setText("delete that path?");
			confirmDialog.setOkRunnable(new Runnable() {

				@Override
				public void run() {
					dbwrapper.deletePath(tv.getText().toString());
					Path p = ((PathsArrayAdapter)pathsListView.getAdapter()).getItem(arg2);
					((PathsArrayAdapter)pathsListView.getAdapter()).remove(p);
					((PathsArrayAdapter)pathsListView.getAdapter()).notifyDataSetChanged();
				}
				
			});
			showDialog(CONFIRM_DIALOG);
		}
	}
	
	private class Callback extends ClientINTF.Stub {

		@Override
		public void newMessages() throws RemoteException {
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((MsgsArrayAdapter)msgsListView.getAdapter()).clear();
						progress.setText("");
						changed();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void newLine(final String line) throws RemoteException {
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((MsgsArrayAdapter)msgsListView.getAdapter()).add(line);
						changed();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void error(final String errMsg) throws RemoteException {
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						warningDialog.setText(errMsg);
						showDialog(WARNING_DIALOG);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void progress(final String str) throws RemoteException {
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progress.setText(str.substring(str.indexOf(' ') + 1) + " %");
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void changed() {
			((MsgsArrayAdapter)msgsListView.getAdapter()).notifyDataSetChanged();
			msgsListView.setSelection(((MsgsArrayAdapter)msgsListView.getAdapter()).getCount());
		}
		
	}
	
}
