package net.appstalk.sendsms.ui;

import java.io.ByteArrayInputStream;

import net.appstalk.sendsms.R;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

public class PickContactAdapter extends SimpleCursorAdapter {
    private Cursor mCursor;
    private Context mContext;

    public PickContactAdapter(Context context, int layout, Cursor c,
            String[] from, int[] to) {
        super(context, layout, c, from, to);
        mCursor = c;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row =  super.getView(position, convertView, parent);
        ImageView imageView = (ImageView)row.findViewById(R.id.contactEntryBadge);

        mCursor.moveToPosition(position);
        int idx = mCursor.getColumnIndex(ContactsContract.Contacts._ID);
        //int idx = PickContactActivity.COLUMN_INDEX_CONTACTS_ID;
        long contactId = mCursor.getLong(idx);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        
        Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
        Cursor photoCursor = mContext.getContentResolver().query(photoUri,
             new String[] {Contacts.Photo.PHOTO}, null, null, null);

        // Get default image to fill no image contact.
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_contact_picture);
        int xSize = bitmap.getWidth();
        int ySize = bitmap.getHeight();

        if (photoCursor != null) 
        {
            try {
                if (photoCursor.moveToFirst()) {
                    byte[] data = photoCursor.getBlob(0);
                    if (data != null) {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, opts);
                        // Scale the bitmap to fit the size of default image.
                        float scaleHeight = (float)ySize/opts.outHeight;
                        float scaleWidth = (float)xSize/opts.outWidth;
                        Matrix matrix = new Matrix();
                        matrix.postScale(scaleWidth, scaleHeight);
                        bitmap = Bitmap.createBitmap(bitmap, 0 , 0, opts.outWidth, opts.outHeight, matrix, true);
                        imageView.setImageBitmap(bitmap);
                    }
                }
                else
                {
                    imageView.setImageBitmap(bitmap);
                }
            } finally {
                photoCursor.close();
            }
        }
        
        return row;
    }



}
