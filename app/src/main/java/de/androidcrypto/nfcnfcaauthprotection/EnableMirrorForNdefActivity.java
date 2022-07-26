package de.androidcrypto.nfcnfcaauthprotection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

import java.io.IOException;

public class EnableMirrorForNdefActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback  {

    EditText responseField;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_mirror_for_ndef);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        responseField = findViewById(R.id.etetEnableMirrorForNdefResponse);

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

                writeToUiAppend(responseField, "*** Start creating the NDEF message and record");

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

                    response = writeEnableUidCounterMirrorNdef(nfcA);
                    if (response == null) {
                        writeToUiAppend(responseField, "Enabling the Uid + counter mirror: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(responseField, "Enabling the Uid + counter mirror: SUCCESS - code: " + Utils.bytesToHex(response));
                    }
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

    private byte[] writeEnableUidCounterMirrorNdef(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the uid mirror is in pages 41/131/227 (0x29 / 0x83 / 0xE3)
         * depending on the tag type
         *
         * byte 0 of this pages holds the MIRROR byte
         * byte 2 of this pages holds the MIRROR_PAGE byte
         *
         * Mirror byte has these flags
         * bits 6+7 define which mirror shall be used:
         *   00b = no ASCII mirror
         *   01b = Uid ASCII mirror
         *   10b = NFC counter ASCII mirror
         *   11b = Uid and NFC counter ASCII mirror
         * bits 4+5 define the byte position within the page defined in MIRROR_PAGE byte
         *
         * MIRROR_PAGE byte defines the start of mirror.
         *
         * It is import that the end of mirror is within the user memory. These lengths apply:
         * Uid mirror: 14 bytes
         * NFC counter mirror: 6 bytes
         * Uid + NFC counter mirror: 21 bytes (14 bytes for Uid and 1 byte separation + 6 bytes counter value
         * Separator is x (0x78)
         *
         * This function writes the MIRROR_PAGE and MIRROR_BYTE to the place where the WRITE NDEF MESSAGE needs it
         *
         */

        writeToUiAppend(responseField, "* Start enabling the Counter mirror *");
        // read page 227 = Configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, 227); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(responseField, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            byte mirrorByteNew;
            // setting bit 7
            mirrorByteNew = Utils.setBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);
            // fix: start the mirror from byte 1 of the designated page, so bits are set as follows
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 4);
            writeToUiAppend(responseField, "MIRROR content new: " + Utils.printByteBinary(mirrorByteNew));
            // set the page where the mirror is starting, we use a fixed page here:
            int setMirrorPage = 15; // 0x0F
            byte mirrorPageNew = (byte) (setMirrorPage & 0x0ff);
            // rebuild the page data
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 227, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(responseField, "write page to tag: " + Utils.bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(responseField, "SUCCESS: writing with response: " + Utils.bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(responseField, "FAILURE: no writing on the tag");
            }
        }
        return null;
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
                Intent i = new Intent(EnableMirrorForNdefActivity.this, NtagDataReadingActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWrite = menu.findItem(R.id.action_write);
        mWrite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(EnableMirrorForNdefActivity.this, WriteActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteProtection = menu.findItem(R.id.action_write_protection);
        mWriteProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(EnableMirrorForNdefActivity.this, SetWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mRemoveProtection = menu.findItem(R.id.action_remove_protection);
        mRemoveProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(EnableMirrorForNdefActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mSpecialSettings = menu.findItem(R.id.action_special_settings);
        mSpecialSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(EnableMirrorForNdefActivity.this, SpecialSettingsActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteNdef = menu.findItem(R.id.action_write_ndef_message);
        mWriteNdef.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(EnableMirrorForNdefActivity.this, WriteNdefMessageActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mEnableMirrorNdefMessage = menu.findItem(R.id.action_enable_ndef_message_mirror);
        mEnableMirrorNdefMessage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(EnableMirrorForNdefActivity.this, EnableMirrorForNdefActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mClearTag = menu.findItem(R.id.action_clear_tag);
        mClearTag.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(EnableMirrorForNdefActivity.this, ClearTagActivity.class);
                startActivity(i);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}