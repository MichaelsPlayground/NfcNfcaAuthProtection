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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class SpecialSettingsActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    Button enableCounter, disableCounter;
    Button enableUidMirror, disableAllMirror;
    Button enableCounterMirror, enableUidCounterMirror;
    EditText task, commandReponse;
    private NfcAdapter mNfcAdapter;

    final String ENABLE_COUNTER_TASK = "enable counter";
    final String DISABLE_COUNTER_TASK = "disable counter";
    final String ENABLE_UID_MIRROR_TASK = "enable Uid mirror";
    final String DISABLE_ALL_MIRROR_TASK = "disable all mirror";
    final String ENABLE_COUNTER_MIRROR_TASK = "enable counter mirror";
    final String ENABLE_UID_COUNTER_MIRROR_TASK = "enable Uid + counter mirror";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_special_settings);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        enableCounter = findViewById(R.id.btnSpecialSettingEnableCounter);
        disableCounter = findViewById(R.id.btnSpecialSettingDisableCounter);
        enableUidMirror = findViewById(R.id.btnSpecialSettingEnableUidMirror);
        disableAllMirror = findViewById(R.id.btnSpecialSettingDisableAllMirror);
        enableCounterMirror = findViewById(R.id.btnSpecialSettingEnableCounterMirror);
        enableUidCounterMirror = findViewById(R.id.btnSpecialSettingEnableUidCounterMirror);
        task = findViewById(R.id.etSpecialSettingsTask);
        commandReponse = findViewById(R.id.etSpecialSettingsResponse);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        enableCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(ENABLE_COUNTER_TASK);
            }
        });

        disableCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(DISABLE_COUNTER_TASK);
            }
        });

        enableUidMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(ENABLE_UID_MIRROR_TASK);
            }
        });

        disableAllMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(DISABLE_ALL_MIRROR_TASK);
            }
        });

        enableCounterMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(ENABLE_COUNTER_MIRROR_TASK);
            }
        });

        enableUidCounterMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.setText(ENABLE_UID_COUNTER_MIRROR_TASK);
            }
        });
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
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "NFC tag is Nfca compatible",
                            Toast.LENGTH_SHORT).show();
                });

                // Make a Sound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);
                }

                runOnUiThread(() -> {
                    commandReponse.setText("");
                });

                nfcA.connect();

                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (!ntagVersion.equals("216")) {
                    writeToUiAppend(commandReponse, "NFC tag is NOT of type NXP NTAG216");
                    writeToUiToast("NFC tag is NOT of type NXP NTAG216");
                    return;
                }

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

                    // get command from task
                    String taskString = task.getText().toString();

                    switch (taskString) {
                        case ENABLE_COUNTER_TASK: {
                                response = writeEnableCounter(nfcA);
                                if (response == null) {
                                    writeToUiAppend(commandReponse, "Enabling the counter: FAILURE");
                                    return;
                                } else {
                                    writeToUiAppend(commandReponse, "Enabling the counter: SUCCESS - code: " + Utils.bytesToHex(response));
                                }
                            break;
                        }
                        case DISABLE_COUNTER_TASK: {
                            response = writeDisableCounter(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Disabling the counter: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Disabling the counter: SUCCESS - code: " + Utils.bytesToHex(response));
                            }
                            break;
                        }
                        case ENABLE_UID_MIRROR_TASK: {
                            response = writeEnableUidMirror(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Enabling the Uid mirror: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Enabling the Uid mirror: SUCCESS - code: " + Utils.bytesToHex(response));
                            }
                            break;
                        }
                        case DISABLE_ALL_MIRROR_TASK: {
                            response = writeDisableAllMirror(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Disabling all mirror: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Disabling all mirror: SUCCESS - code: " + Utils.bytesToHex(response));
                            }
                            break;
                        }
                        case ENABLE_COUNTER_MIRROR_TASK: {
                            response = writeEnableCounterMirror(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Enabling the counter mirror: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Enabling the counter mirror: SUCCESS - code: " + Utils.bytesToHex(response));
                            }
                            break;
                        }
                        case ENABLE_UID_COUNTER_MIRROR_TASK: {
                            response = writeEnableUidCounterMirror(nfcA);
                            if (response == null) {
                                writeToUiAppend(commandReponse, "Enabling the Uid + counter mirror: FAILURE");
                                return;
                            } else {
                                writeToUiAppend(commandReponse, "Enabling the Uid + counter mirror: SUCCESS - code: " + Utils.bytesToHex(response));
                            }
                            break;
                        }
                        default: {
                            // to task
                            writeToUiAppend(commandReponse, "choose a task by pressing the button");
                            return;
                        }
                    }

                } finally {
                    try {
                        nfcA.close();
                    } catch (IOException e) {
                        writeToUiAppend(commandReponse, "ERROR: IOException " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            writeToUiAppend(commandReponse, "ERROR: IOException " + e.toString());
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

    private byte[] writeEnableCounter(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the counter is in pages 42/132/228 (0x2A / 0x84 / 0xE4)
         * depending on the tag type
         *
         * byte 0 of this pages holds the ACCESS byte
         * bit 4 is the counter enabling flag, 0 = disabled, 1 = enabled
         *
         * bit 3 is the counter password protection 0 = NFC counter not protected, 1 = enabled
         * If the NFC counter password protection is enabled, the NFC tag will only respond to a
         * READ_CNT command with the NFC counter value after a valid password verification
         * This bit is NOT set in this command
         */

        writeToUiAppend(commandReponse, "* Start enabling the counter *");
        // first read the page, set bit to 1 and save the page back to the tag
        // read page 228 = Configuration page 1
        byte[] readPageResponse = getTagDataResponse(nfcA, 228); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = ACCESS byte
            byte accessByte = readPageResponse[0];
            writeToUiAppend(commandReponse, "ACCESS content old: " + Utils.printByteBinary(accessByte));
            // setting bit 4
            byte accessByteNew;
            accessByteNew = Utils.setBitInByte(accessByte, 4);
            writeToUiAppend(commandReponse, "ACCESS content new: " + Utils.printByteBinary(accessByteNew));
            // rebuild the page data
            readPageResponse[0] = accessByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 228, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + Utils.bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + Utils.bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    private byte[] writeDisableCounter(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the counter is in pages 42/132/228 (0x2A / 0x84 / 0xE4)
         * depending on the tag type
         *
         * byte 0 of this pages holds the ACCESS byte
         * bit 4 is the counter enabling flag, 0 = disabled, 1 = enabled
         *
         * bit 3 is the counter password protection 0 = NFC counter not protected, 1 = enabled
         * If the NFC counter password protection is enabled, the NFC tag will only respond to a
         * READ_CNT command with the NFC counter value after a valid password verification
         * This bit is NOT set in this command
         */

        writeToUiAppend(commandReponse, "* Start disabling the counter *");
        // first read the page, set bit to 0 and save the page back to the tag
        // read page 228 = Configuration page 1
        byte[] readPageResponse = getTagDataResponse(nfcA, 228); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = ACCESS byte
            byte accessByte = readPageResponse[0];
            writeToUiAppend(commandReponse, "ACCESS content old: " + Utils.printByteBinary(accessByte));
            // setting bit 4
            byte accessByteNew;
            accessByteNew = Utils.unsetBitInByte(accessByte, 4);
            writeToUiAppend(commandReponse, "ACCESS content new: " + Utils.printByteBinary(accessByteNew));
            // rebuild the page data
            readPageResponse[0] = accessByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 228, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + Utils.bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + Utils.bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    private byte[] writeEnableUidMirror(NfcA nfcA) {
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
         */

        writeToUiAppend(commandReponse, "* Start enabling the Uid mirror *");
        // read page 227 = Configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, 227); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(commandReponse, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            // unsetting bit 7
            byte mirrorByteNew;
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);
            // fix: start the mirror from byte 0 of the designated page, so both bits are set to 0
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
            writeToUiAppend(commandReponse, "MIRROR content new: " + Utils.printByteBinary(mirrorByteNew));
            // set the page where the mirror is starting, we use a fixed page here:
            int setMirrorPage = 5;
            byte mirrorPageNew = (byte) (setMirrorPage & 0x0ff);
            // rebuild the page data
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 227, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + Utils.bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + Utils.bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    private byte[] writeDisableAllMirror(NfcA nfcA) {
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
         */

        writeToUiAppend(commandReponse, "* Start disabling the Uid mirror *");
        // read page 227 = Configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, 227); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(commandReponse, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            // unsetting bit 6+7
            byte mirrorByteNew;
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 6);
            // fix: start the mirror from byte 0 of the designated page, so both bits are set to 0
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
            writeToUiAppend(commandReponse, "MIRROR content new: " + Utils.printByteBinary(mirrorByteNew));
            // set the page where the mirror is starting, we use a fixed page here:
            int setMirrorPage = 0; // = disable
            byte mirrorPageNew = (byte) (setMirrorPage & 0x0ff);
            // rebuild the page data
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 227, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + Utils.bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + Utils.bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    private byte[] writeEnableCounterMirror(NfcA nfcA) {
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
         */

        writeToUiAppend(commandReponse, "* Start enabling the Counter mirror *");
        // read page 227 = Configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, 227); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(commandReponse, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            // setting bit 7
            byte mirrorByteNew;
            mirrorByteNew = Utils.setBitInByte(mirrorByte, 7);
            // unsetting bit 6
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 6);
            // fix: start the mirror from byte 0 of the designated page, so both bits are set to 0
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
            writeToUiAppend(commandReponse, "MIRROR content new: " + Utils.printByteBinary(mirrorByteNew));
            // set the page where the mirror is starting, we use a fixed page here:
            int setMirrorPage = 5;
            byte mirrorPageNew = (byte) (setMirrorPage & 0x0ff);
            // rebuild the page data
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 227, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + Utils.bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + Utils.bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }

    private byte[] writeEnableUidCounterMirror(NfcA nfcA) {
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
         */

        writeToUiAppend(commandReponse, "* Start enabling the Counter mirror *");
        // read page 227 = Configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, 227); // this is for NTAG216 only
        if (readPageResponse != null) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(commandReponse, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            // setting bit 7
            byte mirrorByteNew;
            mirrorByteNew = Utils.setBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);
            // fix: start the mirror from byte 0 of the designated page, so both bits are set to 0
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
            writeToUiAppend(commandReponse, "MIRROR content new: " + Utils.printByteBinary(mirrorByteNew));
            // set the page where the mirror is starting, we use a fixed page here:
            int setMirrorPage = 5;
            byte mirrorPageNew = (byte) (setMirrorPage & 0x0ff);
            // rebuild the page data
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 227, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(commandReponse, "write page to tag: " + Utils.bytesToHex(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(commandReponse, "SUCCESS: writing with response: " + Utils.bytesToHex(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(commandReponse, "FAILURE: no writing on the tag");
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
                writeToUiAppend(commandReponse, "Error on reading page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(commandReponse, "Error (NACK) on reading page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(commandReponse, "SUCCESS on reading page " + page + " response: " + Utils.bytesToHex(response));
                System.out.println("reading page " + page + ": " + Utils.bytesToHex(response));
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(commandReponse, "ERROR: Tag lost exception on reading");
            return null;
        } catch (IOException e) {
            writeToUiAppend(commandReponse, "ERROR: IOEexception: " + e);
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
                writeToUiAppend(commandReponse, "Error on writing page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(commandReponse, "Error (NACK) on writing page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(commandReponse, "SUCCESS on writing page " + page + " response: " + Utils.bytesToHex(response));
                System.out.println("response page " + page + ": " + Utils.bytesToHex(response));
                return response;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(commandReponse, "ERROR: Tag lost exception");
            return null;
        } catch (IOException e) {
            writeToUiAppend(commandReponse, "ERROR: IOEexception: " + e);
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
                Intent i = new Intent(SpecialSettingsActivity.this, NtagDataReadingActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWrite = menu.findItem(R.id.action_write);
        mWrite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(SpecialSettingsActivity.this, WriteActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteProtection = menu.findItem(R.id.action_write_protection);
        mWriteProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(SpecialSettingsActivity.this, SetWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mRemoveProtection = menu.findItem(R.id.action_remove_protection);
        mRemoveProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(SpecialSettingsActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mSpecialSettings = menu.findItem(R.id.action_special_settings);
        mSpecialSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(SpecialSettingsActivity.this, SpecialSettingsActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteNdef = menu.findItem(R.id.action_write_ndef_message);
        mWriteNdef.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(SpecialSettingsActivity.this, WriteNdefMessageActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mEnableMirrorNdefMessage = menu.findItem(R.id.action_enable_ndef_message_mirror);
        mEnableMirrorNdefMessage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(SpecialSettingsActivity.this, EnableMirrorForNdefActivity.class);
                startActivity(i);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

}