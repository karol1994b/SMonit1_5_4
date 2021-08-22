

package com.example.smonit1_5_4;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


class ECGDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ecg_results";
    private static final int DB_VERSION = 2;

    public ECGDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        updateMyDatabase(db, 0, DB_VERSION);
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateMyDatabase(db, oldVersion, newVersion);
    }

    private void updateMyDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE RESULTS (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "DATA0 INTEGER," + "DATA1 INTEGER," + " DATA2 INTEGER);");

        }
        if (oldVersion < 2) {
            //boolean ifDbDone = true;
            //log.d("TAG", "Utworzono bazę danych");
            //Toast.makeText(getContext(), "Utworzono bazę danych", Toast.LENGTH_SHORT);
            //insertExampleResults(db, 95);
        }
    }

    private static void insertExampleResults(SQLiteDatabase db,
                                             int exampleResult) {
        ContentValues resultValues = new ContentValues();
        resultValues.put("DATA", exampleResult);
        db.insert("RESULTS", null, resultValues);
    }
}

