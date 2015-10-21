package org.igarape.copcast.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.igarape.copcast.db.IncidentVideoContract.IncidentVideoEntry;
/**
 * Created by martelli on 10/20/15.
 */
public class IncidentVideoDbHelper extends SQLiteOpenHelper {

    private static final String TEXT_TYPE = " TEXT";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + IncidentVideoEntry.TABLE_NAME + " (" +
                    IncidentVideoEntry._ID + " SERIAL PRIMARY KEY," +
                    IncidentVideoEntry.COLUMN_VIDEO_NAME + TEXT_TYPE +" )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + IncidentVideoEntry.TABLE_NAME;


    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "IncidentVideoName.db";


    public IncidentVideoDbHelper(Context context) {
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
