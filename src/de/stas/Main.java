package de.stas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.stas.db.content.Path;
import de.stas.service.ClientINTF;
import de.stas.service.ServiceINTF;

public class Main extends BaseActivity implements ServiceConnection, OnItemClickListener {
	private ListView pathsListView, msgsListView;
	private ServiceINTF service;
	private TextView timer;
	private TextView progressSpeedTextView;
	private TextView progressAllTextView;
	private ProgressBar progressAll;
	private ProgressBar progressDetail;
	private Button cancelButton;
	private Callback callback;
	
	@Override
	public void onSaveInstanceState(Bundle b) {
		b.putInt("progress_all", progressAll.getProgress());
		b.putInt("progress_detail", progressDetail.getProgress());
		b.putParcelableArrayList("msgs", ((MsgsArrayAdapter)msgsListView.getAdapter()).getMsgs());
		super.onSaveInstanceState(b);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle b) {
		progressAll.setProgress(b.getInt("progress_all"));
		progressDetail.setProgress(b.getInt("progress_detail"));
		((MsgsArrayAdapter)msgsListView.getAdapter()).addAll((Collection<? extends Message>) b.getParcelableArrayList("msgs"));
		msgsListView.setSelection(((MsgsArrayAdapter)msgsListView.getAdapter()).getCount());
	}
	
	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.main);
		cancelButton = (Button)findViewById(R.id.cancel_button);
		progressAll = (ProgressBar)findViewById(R.id.progress_all);
		progressAllTextView = (TextView)findViewById(R.id.progress_all_textView);
		progressDetail = (ProgressBar)findViewById(R.id.progress_detail);
		progressSpeedTextView = (TextView)findViewById(R.id.progress_detail_speed_textView);
		timer = (TextView)findViewById(R.id.timer_textView);
		pathsListView = (ListView)findViewById(R.id.path_listView);
		msgsListView = (ListView)findViewById(R.id.cur_act_listView);
		pathsListView.setEmptyView((TextView)findViewById(R.id.path_emptyView_textView));
		msgsListView.setEmptyView((TextView)findViewById(R.id.cur_act_emptyView_textView));
		pathsListView.setOnItemClickListener(this);
		try {
			List<Path> paths = dbwrapper.getPaths();
			ArrayList<Message> msgs = new ArrayList<Message>();
			pathsListView.setAdapter(new PathsArrayAdapter(this, R.id.path_textView, paths));
			msgsListView.setAdapter(new MsgsArrayAdapter(this, R.id.msg_textView, msgs));
		} catch (Exception e) {
			String className = e.getClass().getName();
			warningDialog.setTitle(className.substring(className.lastIndexOf('.') + 1));
			warningDialog.setText(e.getMessage());
			showDialog(WARNING_DIALOG);
			e.printStackTrace();
		}
		cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				progressSpeedTextView.setText("canceling...");
				try {
					service.interrupt();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
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
	
	class MsgsArrayAdapter extends ArrayAdapter<Message> {
		private Context context;
		private ArrayList<Message> msgs;
		
		public MsgsArrayAdapter(Context context, int id, ArrayList<Message> objects) {
			super(context, id, objects);
			this.context = context;
			msgs = objects;
		}
		
		public ArrayList<Message> getMsgs() {
			return msgs;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Message msg = getItem(position);
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.msg_list, null);
				holder = new ViewHolder(convertView.findViewById(R.id.msg_textView));
				convertView.setTag(holder);
			}
			holder = (ViewHolder) convertView.getTag();
			TextView tv = holder.getMsg();
			tv.setText(msg.getLine());
			if (msg.isError()) {
				tv.setTextColor(Color.RED);
			} else {
				tv.setTextColor(Color.WHITE);
			}
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
		MenuItem settings = menu.add("settings");
		settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startActivity(new Intent(Main.this, Settings.class));
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
				String className = e.getClass().getName();
				warningDialog.setTitle(className.substring(className.lastIndexOf('.') + 1));
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
					if (service.isScanning()) {
						showDetails();
					} else {
						hideDetails();
					}
					if(!service.register(getComponentName().getClassName(), callback)) {
						throw new RuntimeException("could not bind service");
					}
					while(!interrupted) {
						timer.post(new Runnable() {
							@Override
							public void run() {
								try {
									int sec = service.getTimeTillNextScan();
									if (sec == 0) {
										timer.setText("scanning");
									} else {
										int min = (sec / 60)%60;
										timer.setText("Next scan in " + sec / 3600 + "h " + min + "m " + sec % 60 + "s");
									}
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
							String className = e.getClass().getName();
							warningDialog.setTitle(className.substring(className.lastIndexOf('.') + 1));
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
			confirmDialog.setTitle("Delete that path?");
			confirmDialog.setText("All saved files will be removed");
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
						progressSpeedTextView.setText("");
						progressAll.setProgress(0);
						progressDetail.setProgress(0);
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
						((MsgsArrayAdapter)msgsListView.getAdapter()).add(new Message(line, false));
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
						warningDialog.setTitle("Error received");
						warningDialog.setText(errMsg);
						showDialog(WARNING_DIALOG);
						((MsgsArrayAdapter)msgsListView.getAdapter()).add(new Message(errMsg, true));
						changed();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void progressAll(final String current, final int i) throws RemoteException {
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progressAll.setProgress(i);
						progressAllTextView.setText(current);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void progressDetail(final String speed, final int i) throws RemoteException {
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progressDetail.setProgress(i);
						progressSpeedTextView.setText(speed);
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

		@Override
		public void done() throws RemoteException {
			hideDetails();
		}

		@Override
		public void started() throws RemoteException {
			showDetails();
		}
	}
	
	public void showDetails() {
		try {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					cancelButton.setVisibility(View.VISIBLE);
					((View)progressDetail.getParent()).setVisibility(View.VISIBLE);
					progressAll.setVisibility(View.VISIBLE);
					progressAllTextView.setVisibility(View.VISIBLE);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void hideDetails() {
		try {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					cancelButton.setVisibility(View.GONE);
					((View)progressDetail.getParent()).setVisibility(View.GONE);
					progressAll.setVisibility(View.GONE);
					progressAllTextView.setVisibility(View.GONE);
					progressDetail.setProgress(0);
					progressAll.setProgress(0);
					progressAllTextView.setText("");
					progressSpeedTextView.setText("");
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
