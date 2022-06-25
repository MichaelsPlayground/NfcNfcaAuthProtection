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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RemoveWriteProtectionActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    com.google.android.material.textfield.TextInputLayout passwordDecoration, packDecoration, startProtectionDecoration;
    com.google.android.material.textfield.TextInputEditText passwordField, packField, startProtection;

    TextView nfcResult;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_write_protection);

        passwordField = findViewById(R.id.etRemoveProtectionPassword);
        passwordDecoration = findViewById(R.id.etRemoveProtectionPasswordDecoration);
        packField = findViewById(R.id.etRemoveProtectionPack);
        packDecoration = findViewById(R.id.etRemoveProtectionPackDecoration);
        startProtection = findViewById(R.id.etRemoveProtectionStartProtection);
        startProtectionDecoration = findViewById(R.id.etRemoveProtectionStartProtectionDecoration);
        nfcResult = findViewById(R.id.tvRemoveProtectionNfcaResult);

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
                /*
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "NFC tag is Nfca compatible",
                            Toast.LENGTH_SHORT).show();
                });
                */
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
                /*
                if (ntagVersion.equals("0")) {
                    runOnUiThread(() -> {
                        nfcResult.setText("NFC tag is NOT of type NXP NTAG213/215/216");
                        Toast.makeText(getApplicationContext(),
                                "NFC tag is NOT of type NXP NTAG213/215/216",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                */
                if (!ntagVersion.equals("216")) {
                    runOnUiThread(() -> {
                        nfcResult.setText("NFC tag is NOT of type NXP NTAG216");
                        Toast.makeText(getApplicationContext(),
                                "NFC tag is NOT of type NXP NTAG216",
                                Toast.LENGTH_SHORT).show();
                    });
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

                    // default values in form
                    // password: 1234 = x31 x32 x33 x34
                    // pack: oK = x6f x4B
                    // protectionStartingPage = 4

                    // default values for unprotected tag
                    // password: 1234 = xFF xFF xFF xFF
                    // pack: oK = xFF xFF
                    // protectionStartingPage = 255

                    // get data from passwordField
                    String passwordString = passwordField.getText().toString();
                    // limitation: exact 4 alphanumerical characters
                    passwordString = Utils.removeAllNonAlphaNumeric(passwordString);
                    if (passwordString.length() != 4) {
                        nfcaContent = nfcaContent + "Error: you need to enter exact 4 alphanumerical characters for PASSWORD" + "\n";
                        writeToUiAppend(nfcResult, nfcaContent);
                        return;
                    }
                    byte[] passwordByte = passwordString.getBytes(StandardCharsets.UTF_8);
                    int passwordLength = passwordByte.length;
                    nfcaContent = nfcaContent + "Password: " + passwordString + " hex: " + Utils.bytesToHex(passwordByte) + "\n";

                    // get pack from etWriteProtectionPack
                    String packString = packField.getText().toString();
                    // limitation: exact 2 alphanumerical characters
                    packString = Utils.removeAllNonAlphaNumeric(packString);
                    if (packString.length() != 2) {
                        nfcaContent = nfcaContent + "Error: you need to enter exact 2 alphanumerical characters for PACK" + "\n";
                        writeToUiAppend(nfcResult, nfcaContent);
                        return;
                    }
                    byte[] packByte = packString.getBytes(StandardCharsets.UTF_8);
                    int packLength = packByte.length;
                    nfcaContent = nfcaContent + "Pack: " + packString + " hex: " + Utils.bytesToHex(packByte) + "\n";
                    // as we write a complete page we need to fill up the bytes 3 + 4 with 0x00
                    byte[] packBytePage = new byte[4];
                    System.arraycopy(packByte, 0, packBytePage, 0, 2);

                    // get starting page for write/read protection from etWriteProtectionStartProtection
                    String startProtectionString = startProtection.getText().toString();
                    // limitation: only numbers through field type, maximum depends on NTAG type
                    int startProtectionPage = Integer.valueOf(startProtectionString);
                    /* the value is fixed so no need to check for ranges
                    if (startProtectionPage > 230) {
                        nfcaContent = nfcaContent + "Error: the page has to be in range 00 - 230" + "\n";
                        writeToUiAppend(nfcResult, nfcaContent);
                        return;
                    }*/
                    nfcaContent = nfcaContent + "Protection starting page: " + startProtectionPage + "\n";
                    writeToUi2(nfcResult, nfcaContent);

                    // send the pwdAuth command

                    //passwordByte = passwordByteDefault.clone();
                    boolean responseSuccessful;
                    System.out.println("*** start authentication");
                    //responseSuccessful = sendPwdAuthData(nfcA, passwordByte, nfcResult, response);
                    //if (!responseSuccessful) return;
                    response = sendPwdAuthDataResponse(nfcA, passwordByte);
                    if (response == null) {
                        writeToUiAppend(nfcResult, "ERROR while verifying password, aborted");
                        return;
                    }
                    // check that response equals to entered PACK
                    writeToUiAppend(nfcResult, "response from PWD_AUTH: " + Utils.bytesToHex(response));
                    boolean packResponseAccepted = false;
                    packResponseAccepted = Arrays.equals(response, packByte);
                    if (packResponseAccepted) {
                        writeToUiAppend(nfcResult, "Password authentication successful, PACK is matching");
                    } else {
                        writeToUiAppend(nfcResult, "Password authentication FAILURE, protection NOT removed");
                        return;
                    }

                    // write password to page 43/133/229 (NTAG 213/215/216)  ### WRONG ### page 4 for testing purposes
                    // this is the default value
                    byte[] passwordByteDefault = new byte[]{
                            (byte) (255 & 0x0ff),
                            (byte) (255 & 0x0ff),
                            (byte) (255 & 0x0ff),
                            (byte) (255 & 0x0ff)
                    };
                    //boolean responseSuccessful;
                    //responseSuccessful = writeTagData(nfcA, 04, passwordByte, nfcResult, response);
                    responseSuccessful = writeTagData(nfcA, 229, passwordByteDefault, nfcResult, response);
                    if (!responseSuccessful) return;

                    // write pack to page 44/134/230 (NTAG 213/215/216) ### WRONG ### page 5 for testing purposes
                    //responseSuccessful = writeTagData(nfcA, 05, packBytePage, nfcResult, response);
                    // this is the default value
                    byte[] packByteDefault = new byte[]{
                            (byte) (255 & 0x0ff),
                            (byte) (255 & 0x0ff),
                            (byte) (0 & 0x0ff),
                            (byte) (0 & 0x0ff)
                    };
                    responseSuccessful = writeTagData(nfcA, 230, packByteDefault, nfcResult, response);
                    if (!responseSuccessful) return;

                    // write auth0 to page 41/131/227 (NTAG 213/215/216)### WRONG ### page 5 for testing purposes
                    // before writing the complete page we need to read the data from byte 0..2
                    byte[] configurationPages = new byte[16];
                    responseSuccessful = getTagData(nfcA, 227, nfcResult, configurationPages);
                    if (!responseSuccessful) return;
                    byte[] configurationPage1 = new byte[4];
                    System.arraycopy(configurationPages, 0, configurationPage1, 0, 4);
                    writeToUiAppend(nfcResult, "configuration page old: " + Utils.bytesToHex(configurationPage1));
                    // change byte 03 for AUTH0 data, this should be fixed = default FF = 255
                    configurationPage1[3] = (byte) (startProtectionPage & 0x0ff);
                    writeToUiAppend(nfcResult, "configuration page new: " + Utils.bytesToHex(configurationPage1));
                    // write the page back to tag
                    responseSuccessful = writeTagData(nfcA, 227, configurationPage1, nfcResult, response);
                    if (!responseSuccessful) return;
                    writeToUiAppend(nfcResult, "Password protection removed");
                    runOnUiThread(() -> {
                       Toast.makeText(getApplicationContext(),
                                "Password protection removed",
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
        } catch (IOException e) {
            writeToUiAppend(nfcResult, "ERROR: IOException " + e.toString());
            e.printStackTrace();
        }
    }

    private byte[] sendPwdAuthDataResponse(NfcA nfcA, byte[] passwordByte) {
        byte[] response = new byte[0];
        byte[] command = new byte[]{
                (byte) 0x1B,  // PWD_AUTH
                passwordByte[0],
                passwordByte[1],
                passwordByte[2],
                passwordByte[3]
        };
        try {
            System.out.println("*** sendPwdAuthData before tranceive");
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            System.out.println("*** sendPwdAuthData after tranceive");
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                return null;
            } else {
                // success: response contains (P)ACK or actual data
            }
        } catch (TagLostException e) {
            // Log and return
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return response; // response contains the response
    }

    private boolean sendPwdAuthData(NfcA nfcA, byte[] passwordByte, TextView textView,
                                    byte[] response) {
        boolean result;
        //byte[] response;
        byte[] command = new byte[]{
                (byte) 0x1B,  // PWD_AUTH
                passwordByte[0],
                passwordByte[1],
                passwordByte[2],
                passwordByte[3]
        };
        try {
            System.out.println("*** sendPwdAuthData before tranceive");
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            System.out.println("*** sendPwdAuthData after tranceive");
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "ERROR: null response");
                return false;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "ERROR: NACK response: " + Utils.bytesToHex(response));
                return false;
            } else {
                // success: response contains (P)ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + Utils.bytesToHex(response));
                System.out.println("pwdAuth " + Utils.bytesToHex(passwordByte) + " response: " + Utils.bytesToHex(response));
                result = true;
            }
        } catch (TagLostException e) {
            // Log and return
            System.out.println("ERROR: Tag lost exception OR Tag is not protected");
            writeToUiAppend(textView, "ERROR: Tag lost exception OR Tag is not protected");
            return false;
        } catch (IOException e) {
            writeToUiAppend(textView, "IOException: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return result; // response contains the response
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
                writeToUiAppend(textView, "ERROR: NACK response: " + Utils.bytesToHex(response));
                return false;
            } else {
                // success: response contains (P)ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + Utils.bytesToHex(response));
                System.out.println("write to page " + page + ": " + Utils.bytesToHex(response));
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

    private byte[] getTagDataResponse(NfcA nfcA, int page) {
        boolean result;
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0x30,  // READ
                (byte) (page & 0x0ff), // page 0
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                return null;
            } else {
                // success: response contains ACK or actual data
                System.out.println("page " + page + ": " + Utils.bytesToHex(response));
                result = true;
            }
        } catch (TagLostException e) {
            // Log and return
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return response;
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
                writeToUiAppend(textView, "ERROR: NACK response: " + Utils.bytesToHex(response));
                return false;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + Utils.bytesToHex(response));
                System.out.println("read from page " + page + ": " + Utils.bytesToHex(response));
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