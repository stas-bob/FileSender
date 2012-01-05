package de.stas.db;


import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import de.stas.db.content.Path;
import de.stas.db.tables.FilesTable;
import de.stas.db.tables.PathTable;




public class DBWrapper {
	private SQLiteDatabase db;
	private MyDB sheme;
	
	public DBWrapper(Context context) {
		sheme = new MyDB(context);
		resume();
	}
	
	public void close() {
		if (db != null && db.isOpen())
			db.close();
	}
	
	public void resume() {
		if (db == null || !db.isOpen()) {
			db = sheme.getWritableDatabase();
			db.execSQL("PRAGMA foreign_keys = ON;");
		}
	}

	public Path getPath(String path) {
		String sql = "select " + PathTable.ID  + 
				" from " + PathTable.TABLE_NAME + 
				" where " + PathTable.PATH + " = '" + path + "'"; 
		Cursor c = null;
		try {
			c = db.rawQuery(sql, null);
			while (c.moveToNext()) {
				int id = c.getInt(0);
				Path p = new Path();
				p.setId(id);
				p.setPath(path);
				return p;
			}
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
	
	public Path savePath(String path) throws Exception {
		Path p = getPath(path);
		if (p != null) {
			return null;
		} else {
			String insert = "insert into " + PathTable.TABLE_NAME + " values (null, ?);";
			SQLiteStatement stmt = db.compileStatement(insert);
			stmt.bindString(1, path);
			p = new Path();
			p.setPath(path);
			long id = stmt.executeInsert();
			p.setId((int)id);
			return p;
		}
	}

	public List<Path> getPaths() throws Exception {
		String sql = "select " + PathTable.ID + ", " + PathTable.PATH + " from " + PathTable.TABLE_NAME;
		Cursor c = null;
		try {
			c = db.rawQuery(sql, null);
			List<Path> list = new LinkedList<Path>();
			while (c.moveToNext()) {
				Path p = new Path();
				p.setId(c.getInt(0));
				p.setPath(c.getString(1));
				list.add(p);
			}
			return list;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public File[] filterNewFiles(File[] files) throws Exception {
		String sql = "select " + FilesTable.FILE_NAME + " from " + FilesTable.TABLE_NAME;
		Cursor c = null;
		try {
			c = db.rawQuery(sql, null);
			List<File> list = new LinkedList<File>();
			loop:for (File file : files) {
				while (c.moveToNext()) {
					String fileInDB = c.getString(0); 
					if (fileInDB.equals(file.getName())) {
						c.moveToPosition(-1);
						continue loop;
					}
				}
				c.moveToPosition(-1);
				if (file.isFile())
					list.add(file);
			}
			return list.toArray(new File[0]);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public void removeDirtyFilePaths() throws Exception {
		String sql = "select " + FilesTable.FILE_NAME + "," + PathTable.PATH + ", " + PathTable.TABLE_NAME + "." + PathTable.ID + " from " + FilesTable.TABLE_NAME + " join " + PathTable.TABLE_NAME +
				" on " + FilesTable.TABLE_NAME + "." + PathTable.ID + " = " + PathTable.TABLE_NAME + "." + PathTable.ID;
		Cursor c = null;
		try {
			c = db.rawQuery(sql, null);
			while (c.moveToNext()) {
				String fileName = c.getString(0);
				String filePath = c.getString(1);
				int pathId = c.getInt(2);
				String absoluteFileName = filePath + "/" + fileName;
				File file = new File(absoluteFileName);
				if (!file.exists()) {
					db.execSQL("delete from " + FilesTable.TABLE_NAME + " where " + FilesTable.FILE_NAME + " = '" + fileName + "' and " + PathTable.ID + " = " + pathId + ";");
				}
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public void addNewFile(File file, Path path) throws Exception {
		String sql = "insert into " + FilesTable.TABLE_NAME + " values('" + file.getName() + "','" + path.getId() + "');";
		db.execSQL(sql);
	}

	public void deletePath(String text) {
		Path p = getPath(text);
		db.beginTransaction();
		try {
			db.execSQL("delete from " + FilesTable.TABLE_NAME + " where " + PathTable.ID + " = " + p.getId() + ";");
			db.execSQL("delete from " + PathTable.TABLE_NAME + " where " + PathTable.ID + " = " + p.getId() + ";");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
}
