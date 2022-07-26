package de.androidcrypto.nfcnfcaauthprotection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

public class WriteNdefMessageActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    EditText linkUrl, completeLinkUrl, responseField;
    private NfcAdapter mNfcAdapter;

    final static String UID_COUNTER_MIRROR_PLACEHOLDER = "UUUUUUUUUUUUUUxCCCCCC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_ndef_message);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        linkUrl = findViewById(R.id.etWriteNdefLinkUrl);
        completeLinkUrl = findViewById(R.id.etWriteNdefCompleteLinkUrl);
        responseField = findViewById(R.id.etWriteNdefResponse);

        /**
         * Important note: DO NOT change the pre given string in
         * "@+id/etWriteNdefLinkUrl"
         * as the Uid + counter mirror needs to have a fixed starting point
         * For the given string "http://androidcrypto.bplaced.net/test.html?d="
         * the mirror will be written to page 0x0F and byte 1
         */

        // static
        completeLinkUrl.setText(linkUrl.getText().toString() + UID_COUNTER_MIRROR_PLACEHOLDER);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");

        NfcA nfcA = null;

        try {
            nfcA = NfcA.get(tag);

            if (nfcA != null) {
                writeToUiToast("NFC tag is Nfca compatible");

                // Make a Sound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);
                }

                runOnUiThread(() -> {
                    responseField.setText("");
                });

                nfcA.connect();

                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (ntagVersion.equals("0")) {
                    writeToUiAppend(responseField, "NFC tag is NOT of type NXP NTAG213/215/216");
                    writeToUiToast("NFC tag is NOT of type NXP NTAG213/215/216");
                    return;
                }

                writeToUiAppend(responseField, "*** Start creating the NDEF message and record");

                byte[] response = new byte[0];
                // get the link url
                String linkUrlString = linkUrl.getText().toString();
                // basic check for length
                if (linkUrlString.length() < 8) {
                    writeToUiAppend(responseField, "The URL is too short, aborted");
                    return;
                }

                String completeUrlString = completeLinkUrl.getText().toString();
                writeToUiAppend(responseField, "This link is written on the tag:\n" + completeUrlString);

                try {
                    // we are generating a complete NDEF message and record with the NDEF class but writing with NfcA
                    // see https://stackoverflow.com/questions/42105626/writing-ndef-data-to-ntag216-tag-using-low-level-nfc-communication-methods
                    // source by user Michael Roland
                    NdefMessage ndefMessage;
                    NdefRecord ndefRecord;
                    ndefRecord = NdefRecord.createUri(completeUrlString);
                    ndefMessage = new NdefMessage(ndefRecord);
                    byte[] ndefMessageByte = ndefMessage.toByteArray();
                    writeToUiAppend(responseField, "This is the NDEF message: " + Utils.bytesToHex(ndefMessageByte));
                    // wrap into TLV structure
                    byte[] tlvEncodedData = null;
                    if (ndefMessageByte.length < 255) {
                        tlvEncodedData = new byte[ndefMessageByte.length + 3];
                        tlvEncodedData[0] = (byte)0x03;  // NDEF TLV tag
                        tlvEncodedData[1] = (byte)(ndefMessageByte.length & 0x0FF);  // NDEF TLV length (1 byte)
                        System.arraycopy(ndefMessageByte, 0, tlvEncodedData, 2, ndefMessageByte.length);
                        tlvEncodedData[2 + ndefMessageByte.length] = (byte)0xFE;  // Terminator TLV tag
                    } else {
                        tlvEncodedData = new byte[ndefMessageByte.length + 5];
                        tlvEncodedData[0] = (byte)0x03;  // NDEF TLV tag
                        tlvEncodedData[1] = (byte)0xFF;  // NDEF TLV length (3 byte, marker)
                        tlvEncodedData[2] = (byte)((ndefMessageByte.length >>> 8) & 0x0FF);  // NDEF TLV length (3 byte, hi)
                        tlvEncodedData[3] = (byte)(ndefMessageByte.length & 0x0FF);          // NDEF TLV length (3 byte, lo)
                        System.arraycopy(ndefMessageByte, 0, tlvEncodedData, 4, ndefMessageByte.length);
                        tlvEncodedData[4 + ndefMessageByte.length] = (byte)0xFE;  // Terminator TLV tag
                    }
                    // fill up with zeros to block boundary:
                    tlvEncodedData = Arrays.copyOf(tlvEncodedData, (tlvEncodedData.length / 4 + 1) * 4);
                    for (int i = 0; i < tlvEncodedData.length; i += 4) {
                        byte[] command = new byte[] {
                                (byte)0xA2, // WRITE
                                (byte)((4 + i / 4) & 0x0FF), // block address
                                0, 0, 0, 0
                        };
                        System.arraycopy(tlvEncodedData, i, command, 2, 4);
                        response = nfcA.transceive(command);
                        if (response == null) {
                            // either communication to the tag was lost or a NACK was received
                            writeToUiAppend(responseField, "ERROR: null response");
                            return;
                        } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                            // NACK response according to Digital Protocol/T2TOP
                            // Log and return
                            writeToUiAppend(responseField, "ERROR: NACK response: " + Utils.bytesToHex(response));
                            return;
                        } else {
                            // success: response contains (P)ACK or actual data
                            writeToUiAppend(responseField, "Writing to page: " + (4 + i) + "SUCCESS: response: " + Utils.bytesToHex(response));
                        }
                    }
                    writeToUiAppend(responseField, "*** End creating the NDEF message and record");
                } catch (TagLostException e) {
                    writeToUiAppend(responseField, "ERROR: TagLostException");
                } finally {
                    try {
                        nfcA.close();
                    } catch (IOException e) {
                        writeToUiAppend(responseField, "ERROR: IOException " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            writeToUiAppend(responseField, "ERROR: IOException " + e.toString());
            e.printStackTrace();
        }
    }

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
            String newString = message + "\n" + textView.getText().toString();
            textView.setText(newString);
        });
    }

    private void writeToUiToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mRead = menu.findItem(R.id.action_read);
        mRead.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteNdefMessageActivity.this, NtagDataReadingActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWrite = menu.findItem(R.id.action_write);
        mWrite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteNdefMessageActivity.this, WriteActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteProtection = menu.findItem(R.id.action_write_protection);
        mWriteProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteNdefMessageActivity.this, SetWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mRemoveProtection = menu.findItem(R.id.action_remove_protection);
        mRemoveProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteNdefMessageActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mSpecialSettings = menu.findItem(R.id.action_special_settings);
        mSpecialSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteNdefMessageActivity.this, SpecialSettingsActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteNdef = menu.findItem(R.id.action_write_ndef_message);
        mWriteNdef.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteNdefMessageActivity.this, WriteNdefMessageActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mEnableMirrorNdefMessage = menu.findItem(R.id.action_enable_ndef_message_mirror);
        mEnableMirrorNdefMessage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(WriteNdefMessageActivity.this, EnableMirrorForNdefActivity.class);
                startActivity(i);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

}