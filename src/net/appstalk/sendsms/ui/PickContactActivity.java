package net.appstalk.sendsms.ui;

import net.appstalk.sendsms.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class PickContactActivity extends Activity{
    private static final int DIALOG_SINGLE_CHOICE = 1;
    
    private String[] contactsProjection = new String[] {
            ContactsContract.Contacts._ID, // 0
            ContactsContract.Contacts.DISPLAY_NAME, // 1
            ContactsContract.Contacts.HAS_PHONE_NUMBER // 2
    };
    public final static int COLUMN_INDEX_CONTACTS_ID = 0;
    public final static int COLUMN_INDEX_CONTACTS_NAME = 1;
    public final static int COLUMN_INDEX_CONTACTS_HAS_PHONE_NUMBER = 2;

    private ListView mContactList;
    private Cursor mCursor;
    private Cursor mPhoneCursor;
    private String mNumberSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.contact_list);
        
        // Obtain handles to UI objects
        mContactList = (ListView) findViewById(R.id.contactList);
        
        mContactList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                int idColumnIndex = mCursor.getColumnIndex(ContactsContract.Contacts._ID);
                mCursor.moveToPosition(position);
                final long contactId = mCursor.getLong(idColumnIndex);
                /*
                 * Get a Cusor for Alert Dialog.
                 * setSingleChoiceItems requires 'ContactsContract.CommonDataKinds.Phone._ID'.
                 */
                mPhoneCursor = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone._ID},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null,
                        ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY + " DESC");
                int count = mPhoneCursor.getCount();
                if(count != 0)
                {
                    showDialog(DIALOG_SINGLE_CHOICE);
                }
            }
            
        });
        
        mCursor = getContacts();
        // Populate the contact list
        populateContactList(mCursor);
    }
    
        
    @Override
    protected Dialog onCreateDialog(int id) {
        
        switch(id) {
        case DIALOG_SINGLE_CHOICE:
            String adTitle, adPositive, adNegative;
            int idColumnIndex;
            
            // Set default phone number.
            mPhoneCursor.moveToPosition(0);
            idColumnIndex = mPhoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            mNumberSelected = mPhoneCursor.getString(idColumnIndex);                    

            // Build Alert Dialog.
            AlertDialog.Builder ad = new AlertDialog.Builder(PickContactActivity.this);
            adTitle = getString(R.string.alert_dialog_title);
            adPositive = getString(R.string.alert_dialog_positive);
            adNegative = getString(R.string.alert_dialog_negative);
            
            ad.setTitle(adTitle);
            ad.setSingleChoiceItems(mPhoneCursor, 0,  ContactsContract.CommonDataKinds.Phone.NUMBER, 
                    new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPhoneCursor.moveToPosition(which);
                    int idx = mPhoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    mNumberSelected = mPhoneCursor.getString(idx);                    
                }
                
            });
            ad.setPositiveButton(adPositive, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent result = new Intent();
                    result.putExtra(EditActivity.EXTRA_NUMBER, mNumberSelected);
                    setResult(RESULT_OK, result);
                    finish();
                    mPhoneCursor.close();
                }
                
            });
            
            ad.setNegativeButton(adNegative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            // Set OnDismissListener to call removeDialog. 
            // It will make onCreateDialog be called whenever the dialog appears.
            Dialog dlg = ad.create();
            dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                
                @Override
                public void onDismiss(DialogInterface dialog) {
                    removeDialog(DIALOG_SINGLE_CHOICE);
                    
                }
            });
            return dlg;
        }
        return null;
    }

    /*
     * Populate the contact list based on account currently selected in the account spinner.
     */
    private void populateContactList(Cursor cursor)
    {
        // Build adapter with contact entries.
        
        String[] fields = new String[] {
                ContactsContract.Contacts.DISPLAY_NAME
        };
        PickContactAdapter adapter = new PickContactAdapter(this, R.layout.contact_entry, cursor, fields, 
                                                              new int[] {R.id.contactEntryText});
        mContactList.setAdapter(adapter);
    }
    
    /**
     * Obtain contact list for the currently selected account.
     * @return A cursor for accessing the contact list.
     */
    private Cursor getContacts()
    {
        // Run query. Contacts that have at least one phone number will be returned.
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String selection = "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                + ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1) AND ("
                + ContactsContract.Contacts.DISPLAY_NAME + " != ''))";
        String[] selectionArgs = null;
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
        
        return managedQuery(uri, contactsProjection, selection, selectionArgs, sortOrder);
        
    }

    
}
