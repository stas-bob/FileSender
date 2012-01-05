package de.stas.db.tables;

public class PathTable {
	public static final String TABLE_NAME = "path";
	public static final String ID = "id";
	public static final String PATH = "path";
	public static final String SQL_CREATE = 
											"CREATE TABLE " + TABLE_NAME + " (" +
											ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
											PATH + " TEXT NOT NULL);";
	

	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
}
