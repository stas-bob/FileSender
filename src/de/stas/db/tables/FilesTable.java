package de.stas.db.tables;

public class FilesTable {
	public static final String TABLE_NAME = "files";
	public static final String FILE_NAME = "file_name";
	public static final String SQL_CREATE = 
											"CREATE TABLE " + TABLE_NAME + " (" + 
											FILE_NAME + " TEXT NOT NULL," +
											PathTable.ID + " INTEGER NOT NULL," +
											"PRIMARY KEY(" + FILE_NAME + ", " + PathTable.ID + ")," +
											"FOREIGN KEY(" + PathTable.ID + ") REFERENCES " + PathTable.TABLE_NAME + "(" + PathTable.ID + ")" +
											");";
	
	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
}
