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

package net.appstalk.sendsms.ui;

import net.appstalk.sendsms.R;
import net.appstalk.sendsms.bundle.BundleScrubber;
import net.appstalk.sendsms.bundle.PluginBundleManager;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.twofortyfouram.locale.BreadCrumber;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 */
public final class EditActivity extends Activity
{
    public final static int PICK_CONTACT = 1;
    public final static String EXTRA_NUMBER = "net.appstalk.smssend.extra_number";

    /**
     * Help URL, used for the {@link com.twofortyfouram.locale.platform.R.id#twofortyfouram_locale_menu_help} menu item.
     */
    // TODO: Place a real help URL here
    private static final String HELP_URL = "http://www.appstalk.net/2011/10/send-sms-plug-in/"; //$NON-NLS-1$

    /**
     * Flag boolean that can only be set to true via the "Don't Save"
     * {@link com.twofortyfouram.locale.platform.R.id#twofortyfouram_locale_menu_dontsave} menu item in
     * {@link #onMenuItemSelected(int, MenuItem)}.
     * <p>
     * If true, then this {@code Activity} should return {@link Activity#RESULT_CANCELED} in {@link #finish()}.
     * <p>
     * If false, then this {@code Activity} should generally return {@link Activity#RESULT_OK} with extras
     * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} and {@link com.twofortyfouram.locale.Intent#EXTRA_STRING_BLURB}.
     * <p>
     * There is no need to save/restore this field's state when the {@code Activity} is paused.
     */
    private boolean mIsCancelled = false;
    
    //private TextView m

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /*
         * A hack to prevent a private serializable classloader attack
         */
        BundleScrubber.scrub(getIntent());
        BundleScrubber.scrub(getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));

        setContentView(R.layout.main);

        setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(), getString(R.string.plugin_name)));

        final FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
        frame.addView(getLayoutInflater().cloneInContext(new ContextThemeWrapper(this, R.style.Theme_Locale_Light)).inflate(R.layout.frame, frame, false));

        CheckBox resendCheckBox = (CheckBox) findViewById(R.id.resendCheck);
        
        Button contactButton = (Button) findViewById(R.id.contact_button);
        contactButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditActivity.this, PickContactActivity.class);
                startActivityForResult(intent, PICK_CONTACT);
            }
            
        });
        /*
         * if savedInstanceState is null, then then this is a new Activity instance and a check for EXTRA_BUNDLE is needed
         */
        if (null == savedInstanceState)
        {
            final Bundle forwardedBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

            if (PluginBundleManager.isBundleValid(forwardedBundle))
            {
                ((EditText) findViewById(R.id.addrEditText)).setText(forwardedBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_ADDRESS));
                ((EditText) findViewById(R.id.msgEditText)).setText(forwardedBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE));
                String resendChecked = forwardedBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_RESEND); 
                if (resendChecked.equals(PluginBundleManager.RESEND_CHECKED))
                {
                    resendCheckBox.setChecked(true);
                }
                else
                {
                    resendCheckBox.setChecked(false);
                }
            }
        }
        /*
         * if savedInstanceState != null, there is no need to restore any Activity state directly via onSaveInstanceState()), as
         * the TextView object handles that automatically
         */
    }

    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        
        switch(requestCode) {
        case PICK_CONTACT:
            if(resultCode == RESULT_OK)
            {
                String result = data.getStringExtra(EXTRA_NUMBER);
                ((EditText) findViewById(R.id.addrEditText)).setText(result);
            }
            break;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void finish()
    {
        if (mIsCancelled)
        {
            setResult(RESULT_CANCELED);
        }
        else
        {
            final String destAddress = ((EditText) findViewById(R.id.addrEditText)).getText().toString();
            final String message = ((EditText) findViewById(R.id.msgEditText)).getText().toString();
            CheckBox resendCheckBox = (CheckBox) findViewById(R.id.resendCheck);
            /*
             * If the message is of 0 length, then there isn't a setting to save.
             */
            if (0 == message.length() && 0 == destAddress.length())
            {
                setResult(RESULT_CANCELED);
            }
            else
            {
                /*
                 * This is the return Intent, into which we'll put all the required extras
                 */
                final Intent returnIntent = new Intent();

                /*
                 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note that anything
                 * placed in this Bundle must be available to Locale's class loader. So storing String, int, and other standard
                 * objects will work just fine. However Parcelable objects must also be Serializable. And Serializable objects
                 * must be standard Java objects (e.g. a private subclass to this plug-in cannot be stored in the Bundle, as
                 * Locale's classloader will not recognize it).
                 */
                final Bundle returnBundle = new Bundle();
                returnBundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_ADDRESS, destAddress);
                returnBundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE, message);
                if (resendCheckBox.isChecked())
                {
                    returnBundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_RESEND, 
                                               PluginBundleManager.RESEND_CHECKED);
                }
                else
                {
                    returnBundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_RESEND, 
                                               PluginBundleManager.RESEND_UNCHECKED);
                }


                returnIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, returnBundle);

                /*
                 * This is the blurb concisely describing what your setting's state is. This is simply used for display in the UI.
                 */
                if (message.length() > getResources().getInteger(R.integer.twofortyfouram_locale_maximum_blurb_length))
                {
                    returnIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, message.substring(0, getResources().getInteger(R.integer.twofortyfouram_locale_maximum_blurb_length)));
                }
                else
                {
                    returnIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, message);
                }

                setResult(RESULT_OK, returnIntent);
            }
        }

        super.finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        /*
         * inflate the default menu layout from XML
         */
        getMenuInflater().inflate(R.menu.twofortyfouram_locale_help_save_dontsave, menu);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item)
    {

        /*
         * Royal pain in the butt to support the home button in SDK 11's ActionBar
         */
        if (Build.VERSION.SDK_INT >= 11)
        {
            try
            {
                if (item.getItemId() == android.R.id.class.getField("home").getInt(null)) //$NON-NLS-1$
                {
                    // app icon in Action Bar clicked; go home
                    final Intent intent = new Intent(getPackageManager().getLaunchIntentForPackage(getCallingPackage()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
            }
            catch (final NoSuchFieldException e)
            {
                // this should never happen on SDK 11 or greater
                throw new RuntimeException(e);
            }
            catch (final IllegalAccessException e)
            {
                // this should never happen on SDK 11 or greater
                throw new RuntimeException(e);
            }
        }

        switch (item.getItemId())
        {
            case R.id.twofortyfouram_locale_menu_help:
            {
                try
                {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(HELP_URL)));
                }
                catch (final Exception e)
                {
                    Toast.makeText(getApplicationContext(), com.twofortyfouram.locale.platform.R.string.twofortyfouram_locale_application_not_available, Toast.LENGTH_LONG).show();
                }

                return true;
            }
            case R.id.twofortyfouram_locale_menu_dontsave:
            {
                mIsCancelled = true;
                finish();
                return true;
            }
            case R.id.twofortyfouram_locale_menu_save:
            {
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }
}