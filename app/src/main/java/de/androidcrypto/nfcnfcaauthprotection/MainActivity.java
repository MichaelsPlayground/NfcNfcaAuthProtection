package de.androidcrypto.nfcnfcaauthprotection;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mRead = menu.findItem(R.id.action_read);
        mRead.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, NtagDataReadingActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWrite = menu.findItem(R.id.action_write);
        mWrite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, WriteActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteAuthentication = menu.findItem(R.id.action_write_authentication);
        mWriteAuthentication.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, WriteAuthActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteProtection = menu.findItem(R.id.action_write_protection);
        mWriteProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, SetWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mRemoveProtection = menu.findItem(R.id.action_remove_protection);
        mRemoveProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mSpecialSettings = menu.findItem(R.id.action_special_settings);
        mSpecialSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, SpecialSettingsActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteNdef = menu.findItem(R.id.action_write_ndef_message);
        mWriteNdef.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, WriteNdefMessageActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mEnableMirrorNdefMessage = menu.findItem(R.id.action_enable_ndef_message_mirror);
        mEnableMirrorNdefMessage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, EnableMirrorForNdefActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mVerifyTagSignature = menu.findItem(R.id.action_verify_tag_signature);
        mVerifyTagSignature.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, VerifyTagSignatureActivity.class);
                startActivity(i);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}