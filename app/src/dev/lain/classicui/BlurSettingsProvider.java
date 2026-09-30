package dev.lain.classicui;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/** Read-only bridge: SystemUI can read the radius without root or write permissions. */
public final class BlurSettingsProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
        MatrixCursor result=new MatrixCursor(new String[]{"blur"});
        result.addRow(new Object[]{getContext().getSharedPreferences("ui",0).getInt("blur",160)});return result;
    }
    public String getType(Uri uri){return "vnd.android.cursor.item/vnd.classicui.blur";}
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
