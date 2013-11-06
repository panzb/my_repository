package com.quanta.weathertime;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class WeatherContentProvider extends ContentProvider{    
    private static final String TAG = "WeatherContentProvider";
	private static final boolean LOGD = false;

    private static final String DATABASE_NAME = "citys.db";
    private static final int DATABASE_VERSION = 11;
    public static final String AUTHORITY = "com.quanta.weathertime";
    public static final Uri CONTENT_URI =
            Uri.parse("content://"+ AUTHORITY + "/citys");
    public static final Uri CONTENT_SHOWCITYS_URI = 
            Uri.parse("content://"+ AUTHORITY + "/weatherwidgets");

    private SQLiteOpenHelper mOpenHelper;


    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
          super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			 db.execSQL("CREATE TABLE citys (" +
	                    "_id INTEGER PRIMARY KEY," +
	                    "cityIndex INTEGER," +
	                    "showName TEXT," +
	                    "cityId TEXT," +
	                    "cityName TEXT," +
	                    "cityDetail TEXT," +
	                    "cityLocalTime TEXT," +
	                    "geoPosition TEXT," +
	                    "currentCondition TEXT," +
	                    "forecast TEXT" +
	                    ");");
			 db.execSQL("CREATE TABLE weatherwidgets (" +
	                    "_id INTEGER PRIMARY KEY," +
	                    "AppwidgetId INTEGER," +
	                    "showCitys TEXT" +
	                    ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (LOGD) {
			    Log.d(TAG, "Upgrading database from version " + 
			            oldVersion + " to " + newVersion + 
			            ", which will destroy all old data");
			}
			
			int version = oldVersion;
			
			if (version < 10) {
			    boolean hasGeoPosition = false;
			    
			    Cursor result = db.query("citys", null, null, null, null, null, null);
	            for (int i = 0; i < result.getColumnCount(); i++) {
	                if ("geoPosition".equals(result.getColumnName(i))) {
	                    hasGeoPosition = true;
	                }
	            }
	            
				db.beginTransaction();
				try {
					try {
						if (!hasGeoPosition) {
							db.execSQL("ALTER TABLE citys ADD COLUMN geoPosition TEXT");
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
					db.setTransactionSuccessful();
					version = 10;
				} finally {
					db.endTransaction();
				}
			}
			
			if (version < 11) {
				db.beginTransaction();
				try {
					try {
						db.execSQL("ALTER TABLE citys ADD COLUMN currentCondition TEXT");
						db.execSQL("ALTER TABLE citys ADD COLUMN forecast TEXT");
					} catch (Exception e) {
						// TODO: handle exception
					}
					db.setTransactionSuccessful();
					version = 11;
				} finally{
					db.endTransaction();
				}
			}
			
			if (version != DATABASE_VERSION) {  
				db.execSQL("DROP TABLE IF EXISTS citys");
				db.execSQL("DROP TABLE IF EXISTS weatherwidgets");
				onCreate(db);
			}
		}
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// TODO Auto-generated method stub
		SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = db.insert(args.table, null, initialValues);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        //sendNotify(uri);
		return uri;
	}



	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
	}

	 static final String PARAMETER_NOTIFY = "notify";
	private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

}
