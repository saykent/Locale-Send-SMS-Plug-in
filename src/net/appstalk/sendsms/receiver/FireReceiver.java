/*
 * Copyright 2011 two forty four a.m. LLC <http://www.twofortyfouram.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package net.appstalk.sendsms.receiver;

import java.util.Calendar;

import net.appstalk.sendsms.Constants;
import net.appstalk.sendsms.bundle.BundleScrubber;
import net.appstalk.sendsms.bundle.PluginBundleManager;
import net.appstalk.sendsms.ui.EditActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 */
public final class FireReceiver extends BroadcastReceiver
{

    /**
     * @param context {@inheritDoc}.
     * @param intent the incoming {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING} Intent. This should contain the
     *            {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by {@link EditActivity} and later broadcast
     *            by Locale.
     */
    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        /*
         * Always be sure to be strict on input parameters! A malicious third-party app could always send an empty or otherwise
         * malformed Intent. And since Locale applies settings in the background, the plug-in definitely shouldn't crash in the
         * background.
         */

        /*
         * Locale guarantees that the Intent action will be ACTION_FIRE_SETTING
         */
        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction()))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        /*
         * A hack to prevent a private serializable classloader attack
         */
        BundleScrubber.scrub(intent);
        BundleScrubber.scrub(intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        /*
         * Final verification of the plug-in Bundle before firing the setting.
         */
        if (PluginBundleManager.isBundleValid(bundle))
        {
            String destAddress, message, resendCheck;
            boolean skipSending = false;
            
            destAddress = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_ADDRESS);
            message = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
            resendCheck = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_RESEND);

            // Get current time.
            Calendar cal = Calendar.getInstance();
            long currentTimeMill = cal.getTimeInMillis();

            SentSMSDbAdapter dbHelper = new SentSMSDbAdapter(context);
            dbHelper.open();

            // Retrieve All SMS saved to manage DB.
            Cursor cursor = dbHelper.fetchAllSMSDb();
            if (cursor != null)
            {
                int columnIndex;
                long longSaved;
                boolean dbHasData = true;
                
                if(cursor.moveToFirst() == false)
                {
                    dbHasData = false;
                }
                while(dbHasData)
                {
                    // Check time saved.
                    columnIndex = cursor.getColumnIndex(SentSMSDbAdapter.KEY_SENTTIME);
                    longSaved = cursor.getLong(columnIndex);
                    if(currentTimeMill - longSaved > 1000*60*30)
                    {
                        //Delete the record from DB.
                        columnIndex = cursor.getColumnIndex(SentSMSDbAdapter.KEY_ROWID);
                        longSaved = cursor.getLong(columnIndex);
                        dbHelper.deleteSMSDb(longSaved);
                    }
                    else
                    {
                        columnIndex = cursor.getColumnIndex(SentSMSDbAdapter.KEY_RECIPIENT);
                        String stringSaved = cursor.getString(columnIndex);
                        if(stringSaved.equals(destAddress))
                        {
                            columnIndex = cursor.getColumnIndex(SentSMSDbAdapter.KEY_MESSAGE);
                            stringSaved = cursor.getString(columnIndex);
                            if(stringSaved.equals(message))
                            {
                                /*
                                 * If the user doesn't want to send the same SMS in 30 minute, 
                                 * DO NOT send it. 
                                */
                                if (resendCheck.equals(PluginBundleManager.RESEND_CHECKED))
                                {
                                    skipSending = true;
                                    //Toast.makeText(context, "Message sending skipped!!" , Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    //Just delete the record. New record will be created later.
                                    columnIndex = cursor.getColumnIndex(SentSMSDbAdapter.KEY_ROWID);
                                    longSaved = cursor.getLong(columnIndex);
                                    dbHelper.deleteSMSDb(longSaved);
                                }
                            }
                        }
                    }
                    if (cursor.moveToNext() == false)
                    {
                        break;
                    }
                }
            }
            cursor.close();

            //Toast.makeText(context, destAddress + "\n" +  message , Toast.LENGTH_LONG).show();
            
            if (skipSending == false)
            {
                try {
                    sendSmsMessage( destAddress, message);
                    /*
                     * Save sent message content with time.
                    */
                    dbHelper.createSMSDb(destAddress, message, currentTimeMill);
                } catch (Exception e) {
                    Toast.makeText(context, "Failed to send SMS",
                        Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void sendSmsMessage(String address,String message) throws Exception
    {
        SmsManager smsMgr = SmsManager.getDefault();
        smsMgr.sendTextMessage(address, null, message, null, null);
    }
}