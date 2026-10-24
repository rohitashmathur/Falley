package com.perfgames.ropelab;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** A single read-only result image, shared only through temporary URI permission. */
public final class ShareCardProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private File card(Uri uri) throws FileNotFoundException {
        if(!"/result.png".equals(uri.getPath()))throw new FileNotFoundException("Unknown result");
        File file=new File(getContext().getCacheDir(),"falley-share/result.png");
        if(!file.isFile())throw new FileNotFoundException("Result unavailable");return file;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(card(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri){return "image/png";}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
        try {File f=card(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor c=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"Falley-Daily-Drop.png":OpenableColumns.SIZE.equals(columns[i])?f.length():null;c.addRow(row);return c;
        }catch(FileNotFoundException missing){return null;}
    }
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException("Read only");}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException("Read only");}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("Read only");}
}
