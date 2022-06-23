package de.androidcrypto.nfcnfcaauthprotection;

import androidx.appcompat.app.AppCompatActivity;

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
import android.provider.Settings;
import android.renderscript.Sampler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SetWriteProtectionActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    com.google.android.material.textfield.TextInputLayout passwordDecoration, packDecoration, startProtectionDecoration;
    com.google.android.material.textfield.TextInputEditText passwordField, packField, startProtection;
    SwitchMaterial enableReadProtectionSwitch;
    TextView nfcResult;
    Button fastRead, sample2, setWriteProtection, removeWriteProtection;
    private NfcAdapter mNfcAdapter;
    boolean readProtectionChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_write_protection);

        passwordField = findViewById(R.id.etWriteProtectionPassword);
        passwordDecoration = findViewById(R.id.etWriteProtectionPasswordDecoration);
        packField = findViewById(R.id.etWriteProtectionPack);
        packDecoration = findViewById(R.id.etWriteProtectionPackDecoration);
        startProtection = findViewById(R.id.etWriteProtectionStartProtection);
        startProtectionDecoration = findViewById(R.id.etWriteProtectionStartProtectionDecoration);
        enableReadProtectionSwitch = findViewById(R.id.swReadProtection);

        nfcResult = findViewById(R.id.tvWriteProtectionNfcaResult);

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

                nfcA.connect();

                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (ntagVersion.equals("0")) {
                    runOnUiThread(() -> {
                        nfcResult.setText("NFC tag is NOT of type NXP NTAG213/215/216");
                        Toast.makeText(getApplicationContext(),
                                "NFC tag is NOT of type NXP NTAG213/215/216",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                int nfcaMaxTranceiveLength = nfcA.getMaxTransceiveLength(); // important for the readFast command
                int ntagPages = NfcIdentifyNtag.getIdentifiedNtagPages();
                int ntagMemoryBytes = NfcIdentifyNtag.getIdentifiedNtagMemoryBytes();
                String tagIdString = getDec(tag.getId());
                String nfcaContent = "raw data of " + NfcIdentifyNtag.getIdentifiedNtagType() + "\n" +
                        "number of pages: " + ntagPages +
                        " total memory: " + ntagMemoryBytes +
                        " bytes\n" +
                        "tag ID: " + bytesToHex(NfcIdentifyNtag.getIdentifiedNtagId()) + "\n" +
                        "tag ID: " + tagIdString + "\n";
                nfcaContent = nfcaContent + "maxTranceiveLength: " + nfcaMaxTranceiveLength + " bytes\n";
                // read the complete memory depending on ntag type
                byte[] ntagMemory = new byte[ntagMemoryBytes];
                // read the content of the tag in several runs
                byte[] response = new byte[0];

                try {

                    // default values in form
                    // password: 1234 = x31 x32 x33 x34
                    // pack: oK = x6f x4B
                    // protectionStartingPage = 4

                    // get data from passwordField
                    String passwordString = passwordField.getText().toString();
                    // limitation: exact 4 alphanumerical characters
                    passwordString = removeAllNonAlphaNumeric(passwordString);
                    if (passwordString.length() != 4) {
                        nfcaContent = nfcaContent + "Error: you need to enter exact 4 alphanumerical characters for PASSWORD" + "\n";
                        writeToUiAppend(nfcResult, nfcaContent);
                        return;
                    }
                    byte[] passwordByte = passwordString.getBytes(StandardCharsets.UTF_8);
                    int passwordLength = passwordByte.length;
                    nfcaContent = nfcaContent + "Password: " + passwordString + " hex: " + bytesToHex(passwordByte) + "\n";

                    // get pack from etWriteProtectionPack
                    String packString = packField.getText().toString();
                    // limitation: exact 2 alphanumerical characters
                    packString = removeAllNonAlphaNumeric(packString);
                    if (packString.length() != 2) {
                        nfcaContent = nfcaContent + "Error: you need to enter exact 2 alphanumerical characters for PACK" + "\n";
                        writeToUiAppend(nfcResult, nfcaContent);
                        return;
                    }
                    byte[] packByte = packString.getBytes(StandardCharsets.UTF_8);
                    int packLength = packByte.length;
                    nfcaContent = nfcaContent + "Pack: " + packString + " hex: " + bytesToHex(packByte) + "\n";
                    // as we write a complete page we need to fill up the bytes 3 + 4 with 0x00
                    byte[] packBytePage = new byte[4];
                    System.arraycopy(packByte, 0, packBytePage, 0, 2);

                    // get starting page for write/read protection from etWriteProtectionStartProtection
                    String startProtectionString = startProtection.getText().toString();
                    // limitation: only numbers through field type, maximum depends on NTAG type
                    int startProtectionPage = Integer.valueOf(startProtectionString);
                    if (startProtectionPage > 230) {
                        nfcaContent = nfcaContent + "Error: the page has to be in range 00 - 230" + "\n";
                        writeToUiAppend(nfcResult, nfcaContent);
                        return;
                    }
                    nfcaContent = nfcaContent + "Protection starting page: " + startProtectionPage + "\n";
                    writeToUi2(nfcResult, nfcaContent);

                    // enable password protection for reading as well
                    readProtectionChecked = enableReadProtectionSwitch.isChecked();

                    // write password to page 43/133/229 (NTAG 213/215/216)  ### WRONG ### page 4 for testing purposes
                    boolean responseSuccessful;
                    //responseSuccessful = writeTagData(nfcA, 04, passwordByte, nfcResult, response);
                    responseSuccessful = writeTagData(nfcA, 229, passwordByte, nfcResult, response);
                    if (!responseSuccessful) return;

                    // write pack to page 44/134/230 (NTAG 213/215/216) ### WRONG ### page 5 for testing purposes
                    //responseSuccessful = writeTagData(nfcA, 05, packBytePage, nfcResult, response);
                    responseSuccessful = writeTagData(nfcA, 230, packBytePage, nfcResult, response);
                    if (!responseSuccessful) return;

                    // write auth0 to page 41/131/227 (NTAG 213/215/216)### WRONG ### page 5 for testing purposes
                    // before writing the complete page we need to read the data from byte 0..2
                    byte[] configurationPages = new byte[16];
                    responseSuccessful = getTagData(nfcA, 227, nfcResult, configurationPages);
                    if (!responseSuccessful) return;
                    byte[] configurationPage0 = new byte[4];
                    System.arraycopy(configurationPages, 0, configurationPage0, 0, 4);
                    writeToUiAppend(nfcResult, "configuration page old: " + bytesToHex(configurationPage0));
                    // change byte 03 for AUTH0 data
                    configurationPage0[3] = (byte) (startProtectionPage & 0x0ff);
                    writeToUiAppend(nfcResult, "configuration page 0 new: " + bytesToHex(configurationPage0));
                    // write the page back to tag
                    responseSuccessful = writeTagData(nfcA, 227, configurationPage0, nfcResult, response);
                    if (!responseSuccessful) return;

                    // write BIT for enabling read protection to page 42/132/228 (NTAG 213/215/216) ### WRONG ### page 5 for testing purposes
                    // before writing the complete page we need to read the data from byte 0..2 = already done for auth0
                    // get value of byte 0 = access
                    byte[] configurationPage1 = new byte[4];
                    System.arraycopy(configurationPages, 4, configurationPage1, 0, 4);
                    byte accessByte = configurationPage1[0];
                    byte[] abOld = new byte[1];
                    abOld[0] = accessByte;
                    writeToUiAppend(nfcResult, "Configuration page 1 old: " + bytesToHex(configurationPage1) + " ACCESS byte: " + printByteArrayBinary(abOld));
                    // https://www.mytecbits.com/tools/encoders/binary-encoder
                    // value 0x40 = 64d = 01000000
                    /*
                    data structure in this byte
                    7 = PROT / Password protection, 0 = only write protection,
                                                    1 = read and write protection
                    6 = CFGLCK / Configuration pages lock, 0 = user configuration open to write access,
                                                           1 = user configuration permanently locked
                    5 = RFUI fixed 0
                    4 = NFC_CNT_EN = NFC counter configuration, 0 = NFC counter disabled
                                                                1 = NFC counter enabled
                    3 = NFC_CNT_PWD_PROT = NFC counter protection, 0 = NFC counter not protected
                                                                   1 = NFC counter protected:
                        If the NFC counter password protection is enabled, the NFC tag will only respond
                        to a READ_CNT command with the NFC counter value after a valid password verification
                    2 = AUTHLIM / Limitation of negative password verification attempts
                            000b = limiting of negative password verification attempts disabled
                            001b-111b ... maximum number of negative password verification attempts
                    1 = AUTHLIM (continued)
                    0 = AUTHLIM (continued)
                    */
                    // setting bit 7 depends on readProtectionEnabled
                    if (readProtectionChecked) {
                        // set bit 7 to 1, pos is 0 based
                        accessByte = setBitInByte(accessByte, 7);
                    } else {
                        // set bit 7 to 0, pos is 0 based
                        accessByte = unsetBitInByte(accessByte, 7);
                    }
                    // rebuild the page data
                    configurationPage1[0] = accessByte;
                    byte[] ab = new byte[1];
                    ab[0] = accessByte;
                    writeToUiAppend(nfcResult, "Configuration page 1 new: " + bytesToHex(configurationPage1) + " ACCESS byte: " + printByteArrayBinary(ab));
                    // save the data
                    //writeToUiAppend(nfcResult, "configuration page 1 new: " + bytesToHex(configurationPage1));
                    // write the page back to tag
                    responseSuccessful = writeTagData(nfcA, 228, configurationPage1, nfcResult, response);
                    if (!responseSuccessful) return;
                    writeToUiAppend(nfcResult, "NFC tag is password protected now");
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(),
                                "NFC tag is password protected now",
                                Toast.LENGTH_SHORT).show();
                    });

                } finally {
                    try {
                        nfcA.close();
                    } catch (IOException e) {
                        writeToUiAppend(nfcResult, "ERROR: IOException " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(nfcResult, "ERROR: Tag lost exception");
        }
        catch (IOException e) {
            writeToUiAppend(nfcResult, "ERROR: IOException " + e.toString());
            e.printStackTrace();
        }
    }

    // position is 0 based starting from right to left
    private byte setBitInByte(byte input, int pos) {
        return (byte) (input | (1 << pos));
    }

    // position is 0 based starting from right to left
    private byte unsetBitInByte(byte input, int pos) {
        return (byte) (input & ~(1 << pos));
    }


    private void writeToUi2(TextView textView, String message) {
        runOnUiThread(() -> {
            textView.setText(message);
        });
    }

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
            String newString = textView.getText().toString() + "\n" + message;
            textView.setText(newString);
        });
    }

    private boolean writeTagData(NfcA nfcA, int page, byte[] dataByte, TextView textView,
                                 byte[] response) {
        boolean result;
        //byte[] response;
        byte[] command = new byte[]{
                (byte) 0xA2,  // WRITE
                (byte) (page & 0x0ff), // page
                dataByte[0],
                dataByte[1],
                dataByte[2],
                dataByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "ERROR: null response");
                return false;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "ERROR: NACK response: " + bytesToHex(response));
                return false;
            } else {
                // success: response contains (P)ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + bytesToHex(response));
                System.out.println("write to page " + page + ": " + bytesToHex(response));
                result = true;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception");
            return false;
        } catch (IOException e) {
            writeToUiAppend(textView, "IOException: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return result; // response contains the response
    }

    private boolean getTagData(NfcA nfcA, int page, TextView textView, byte[] response) {
        boolean result;
        //byte[] response;
        byte[] command = new byte[]{
                (byte) 0x30,  // READ
                (byte) (page & 0x0ff),
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "ERROR: null response");
                return false;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "ERROR: NACK response: " + bytesToHex(response));
                return false;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + bytesToHex(response));
                System.out.println("read from page " + page + ": " + bytesToHex(response));
                result = true;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception");
            return false;
        } catch (IOException e) {
            writeToUiAppend(textView, "IOException: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes)
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static String removeAllNonAlphaNumeric(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    private String getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result + "";
    }

    private static String printByteArrayBinary(byte[] bytes){
        String output = "";
        for (byte b1 : bytes){
            String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
            //s1 += " " + Integer.toHexString(b1);
            //s1 += " " + b1;
            output = output + " " + s1;
            //System.out.println(s1);
        }
        return output;
    }

    private void showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

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
}