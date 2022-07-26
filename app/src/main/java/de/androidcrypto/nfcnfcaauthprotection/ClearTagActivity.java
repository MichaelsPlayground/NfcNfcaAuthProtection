package de.androidcrypto.nfcnfcaauthprotection;

import android.content.Context;
import android.content.Intent;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;

public class ClearTagActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback  {

    EditText responseField;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_tag);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        responseField = findViewById(R.id.etClearTextResponse);

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
                if (!ntagVersion.equals("216")) {
                    writeToUiAppend(responseField, "NFC tag is NOT of type NXP NTAG216");
                    writeToUiToast("NFC tag is NOT of type NXP NTAG216");
                    return;
                }

                // factory values for ntag216 only
                String page03hDefault = "E1106D00"; // ndef compatibility container
                String page04hDefault = "0300FE00"; // first page of user memory
                String page05hDefault = "00000000"; // up to page E1
                String pageE2hDefault = "000000BD"; // dynamic lock bytes
                String pageE3hDefault = "040000FF"; //
                String pageE4hDefault = "00050000"; //
                String pageE5hDefault = ""; // not used as the password cannot be written
                String pageE6hDefault = ""; // not used as the pack cannot be written

                writeToUiAppend(responseField, "*** Start clearing the tag");

                int nfcaMaxTranceiveLength = nfcA.getMaxTransceiveLength(); // important for the readFast command
                int ntagPages = NfcIdentifyNtag.getIdentifiedNtagPages();
                int ntagMemoryBytes = NfcIdentifyNtag.getIdentifiedNtagMemoryBytes();
                String tagIdString = Utils.getDec(tag.getId());
                String nfcaContent = "raw data of " + NfcIdentifyNtag.getIdentifiedNtagType() + "\n" +
                        "number of pages: " + ntagPages +
                        " total memory: " + ntagMemoryBytes +
                        " bytes\n" +
                        "tag ID: " + Utils.bytesToHex(NfcIdentifyNtag.getIdentifiedNtagId()) + "\n" +
                        "tag ID: " + tagIdString + "\n";
                nfcaContent = nfcaContent + "maxTranceiveLength: " + nfcaMaxTranceiveLength + " bytes\n";
                // read the complete memory depending on ntag type
                byte[] ntagMemory = new byte[ntagMemoryBytes];
                // read the content of the tag in several runs
                byte[] response = new byte[0];

                try {
                    // page 03
                    response = writeTagDataResponse(nfcA, 03, Utils.hexStringToByteArray(page03hDefault));
                    if (response == null) {
                        writeToUiAppend(responseField, "Writing of page 03h: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(responseField, "Writing of page 03h: SUCCESS - code: " + Utils.bytesToHex(response));
                    }
                    // page 04
                    response = writeTagDataResponse(nfcA, 04, Utils.hexStringToByteArray(page04hDefault));
                    if (response == null) {
                        writeToUiAppend(responseField, "Writing of page 04h: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(responseField, "Writing of page 04h: SUCCESS - code: " + Utils.bytesToHex(response));
                    }
                    // page 05 to page e1 (225)
                    for (int i = 5; i < 226; i++) {
                        response = writeTagDataResponse(nfcA, i, Utils.hexStringToByteArray(page05hDefault));
                        if (response == null) {
                            writeToUiAppend(responseField, "Writing of page " + i + " : FAILURE");
                            return;
                        } else {
                            writeToUiAppend(responseField, "Writing of page " + i + " : SUCCESS - code: " + Utils.bytesToHex(response));
                        }
                    }
                    // page e2 (226)
                    response = writeTagDataResponse(nfcA, 226, Utils.hexStringToByteArray(pageE2hDefault));
                    if (response == null) {
                        writeToUiAppend(responseField, "Writing of page E2h: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(responseField, "Writing of page E2h: SUCCESS - code: " + Utils.bytesToHex(response));
                    }
                    // page e3 (227)
                    response = writeTagDataResponse(nfcA, 227, Utils.hexStringToByteArray(pageE3hDefault));
                    if (response == null) {
                        writeToUiAppend(responseField, "Writing of page E3h: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(responseField, "Writing of page E3h: SUCCESS - code: " + Utils.bytesToHex(response));
                    }
                    // page e4 (228)
                    response = writeTagDataResponse(nfcA, 228, Utils.hexStringToByteArray(pageE4hDefault));
                    if (response == null) {
                        writeToUiAppend(responseField, "Writing of page E4h: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(responseField, "Writing of page E4h: SUCCESS - code: " + Utils.bytesToHex(response));
                    }
                    writeToUiAppend(responseField, "The tag was set to factory settings");
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

    private byte[] getTagDataResponse(NfcA nfcA, int page) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0x30,  // READ
                (byte) (page & 0x0ff), // page 0
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(responseField, "Error on reading page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(responseField, "Error (NACK) on reading page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(responseField, "SUCCESS on reading page " + page + " response: " + Utils.bytesToHex(response));
                System.out.println("reading page " + page + ": " + Utils.bytesToHex(response));
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(responseField, "ERROR: Tag lost exception on reading");
            return null;
        } catch (IOException e) {
            writeToUiAppend(responseField, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private byte[] writeTagDataResponse(NfcA nfcA, int page, byte[] dataByte) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0xA2,  // WRITE
                (byte) (page & 0x0ff),
                dataByte[0],
                dataByte[1],
                dataByte[2],
                dataByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(responseField, "Error on writing page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(responseField, "Error (NACK) on writing page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(responseField, "SUCCESS on writing page " + page + " response: " + Utils.bytesToHex(response));
                System.out.println("response page " + page + ": " + Utils.bytesToHex(response));
                return response;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(responseField, "ERROR: Tag lost exception");
            return null;
        } catch (IOException e) {
            writeToUiAppend(responseField, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
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
                Intent i = new Intent(ClearTagActivity.this, NtagDataReadingActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWrite = menu.findItem(R.id.action_write);
        mWrite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(ClearTagActivity.this, WriteActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteProtection = menu.findItem(R.id.action_write_protection);
        mWriteProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(ClearTagActivity.this, SetWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mRemoveProtection = menu.findItem(R.id.action_remove_protection);
        mRemoveProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(ClearTagActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mSpecialSettings = menu.findItem(R.id.action_special_settings);
        mSpecialSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(ClearTagActivity.this, SpecialSettingsActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteNdef = menu.findItem(R.id.action_write_ndef_message);
        mWriteNdef.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(ClearTagActivity.this, WriteNdefMessageActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mEnableMirrorNdefMessage = menu.findItem(R.id.action_enable_ndef_message_mirror);
        mEnableMirrorNdefMessage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(ClearTagActivity.this, EnableMirrorForNdefActivity.class);
                startActivity(i);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}