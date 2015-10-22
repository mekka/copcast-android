package org.igarape.copcast.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.igarape.copcast.db.JsonDataContract.JsonDataEntry;
/**
 * Created by martelli on 10/20/15.
 */
public class JsonDataDbHelper extends SQLiteOpenHelper {

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + JsonDataEntry.TABLE_NAME + " (" +
                    JsonDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    JsonDataEntry.COLUMN_TYPE + " TEXT," +
                    JsonDataEntry.COLUMN_DATA + " TEXT," +
                    JsonDataEntry.COLUMN_USER + " TEXT," +
                    JsonDataEntry.COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + JsonDataEntry.TABLE_NAME;


    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "CopcastJsonData.db";


    public JsonDataDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
