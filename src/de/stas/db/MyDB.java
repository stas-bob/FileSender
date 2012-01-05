package de.stas.db;

import de.stas.db.tables.PathTable;
import de.stas.db.tables.FilesTable;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDB extends SQLiteOpenHelper {

	private static final String DB_NAME = "mydb.db";
	private static final int DB_VERSION = 2;
	
	public MyDB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(PathTable.SQL_CREATE);
		db.execSQL(FilesTable.SQL_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(FilesTable.SQL_DROP);
		db.execSQL(PathTable.SQL_DROP);
		onCreate(db);
	}
}