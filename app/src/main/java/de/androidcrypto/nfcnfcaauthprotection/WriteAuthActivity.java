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

public class WriteAuthActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    com.google.android.material.textfield.TextInputLayout passwordDecoration, packDecoration, inputFieldDecoration;
    com.google.android.material.textfield.TextInputEditText passwordField, packField, inputField;
    TextView nfcResult;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_auth);

        inputField = findViewById(R.id.etWriteAuthInputField);
        inputFieldDecoration = findViewById(R.id.etWriteAuthInputFieldDecoration);
        nfcResult = findViewById(R.id.tvWriteAuthNfcaResult);
        passwordField = findViewById(R.id.etWriteAuthPassword);
        passwordDecoration = findViewById(R.id.etWriteAuthPasswordDecoration);
        packField = findViewById(R.id.etWriteAuthPack);
        packDecoration = findViewById(R.id.etWriteAuthPasswordDecoration);
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

                    // get data from passwordField
                    String passwordString = passwordField.getText().toString();
                    // limitation: exact 4 alphanumerical characters
                    passwordString = removeAllNonAlphaNumeric(passwordString);
                    if (passwordString.length() != 4) {
                        nfcaContent = nfcaContent + "Error: you need to enter exact 4 alphanumerical characters for PASSWORD" + "\n";
                        writeToUi(nfcResult, nfcaContent);
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
                        writeToUi(nfcResult, nfcaContent);
                        return;
                    }
                    byte[] packByte = packString.getBytes(StandardCharsets.UTF_8);
                    int packLength = packByte.length;
                    nfcaContent = nfcaContent + "Pack: " + packString + " hex: " + bytesToHex(packByte) + "\n";
                    // as we write a complete page we need to fill up the bytes 3 + 4 with 0x00
                    byte[] packBytePage = new byte[4];
                    System.arraycopy(packByte, 0, packBytePage, 0, 2);

                    // send the pwdAuth command
                    // this is the default value
                    byte[]  passwordByteDefault = new byte[]{
                            (byte) (255 & 0x0ff),
                            (byte) (255 & 0x0ff),
                            (byte) (255 & 0x0ff),
                            (byte) (255 & 0x0ff)
                    };
                    //passwordByte = passwordByteDefault.clone();
                    boolean responseSuccessful;
                    System.out.println("*** start authentication");
                    responseSuccessful = sendPwdAuthData(nfcA, passwordByte, nfcResult, response);
                    if (!responseSuccessful) return;
                    // todo check response for PACK to proceed


                    // get data from InputField
                    String dataString = inputField.getText().toString();

                    // limitation: maximal 8 characters
                    if (dataString.length() > 8 ) {
                        dataString = dataString.substring(0, 8);
                    }

                    byte[] dataByte = dataString.getBytes(StandardCharsets.UTF_8);
                    int dataLength = dataByte.length;
                    // as the Tag is saving in blocks of 4 bytes we need to know how many pages we do need
                    int dataPages = dataLength / 4;
                    int dataPagesMod = dataLength % 4; // if there is a remainder we need to use a new page to write
                    nfcaContent = nfcaContent + "data length: " + dataLength + "\n";
                    nfcaContent = nfcaContent + "data: " + bytesToHex(dataByte) + "\n";
                    nfcaContent = nfcaContent + "dataPages: " + dataPages + "\n";
                    nfcaContent = nfcaContent + "dataPagesMod: " + dataPagesMod + "\n";

                    // check that the data is fitting on the tag
                    if (dataLength > ntagMemoryBytes) {
                        runOnUiThread(() -> {
                            nfcResult.setText("data in InputField is too long for tag");
                            System.out.println("data in InputField is too long for tag");
                            Toast.makeText(getApplicationContext(),
                                    "data in InputField is too long for tag",
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    nfcaContent = nfcaContent + "writing full pages" + "\n";
                    // writing full pages of 4 bytes each
                    for (int i = 0; i < dataPages; i++) {
                        System.out.println("starting round: " + i);
                        byte[] commandW;
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + i) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (i * 4)],
                                dataByte[1 + (i * 4)],
                                dataByte[2 + (i * 4)],
                                dataByte[3 + (i * 4)]
                        };
                        nfcaContent = nfcaContent + "command: " + bytesToHex(commandW) + "\n";
                        response = nfcA.transceive(commandW);
                        if (response == null) {
                            // either communication to the tag was lost or a NACK was received
                            // Log and return
                            nfcaContent = nfcaContent + "ERROR: null response";
                            String finalNfcaText = nfcaContent;
                            runOnUiThread(() -> {
                                nfcResult.setText(finalNfcaText);
                                System.out.println(finalNfcaText);
                            });
                            return;
                        } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                            // NACK response according to Digital Protocol/T2TOP
                            // Log and return
                            nfcaContent = nfcaContent + "ERROR: NACK response: " + bytesToHex(response);
                            String finalNfcaText = nfcaContent;
                            runOnUiThread(() -> {
                                nfcResult.setText(finalNfcaText);
                                System.out.println(finalNfcaText);
                            });
                            return;
                        } else {
                            // success: response contains ACK or actual data
                            // nfcaContent = nfcaContent + "successful reading " + response.length + " bytes\n";
                            // nfcaContent = nfcaContent + bytesToHex(response) + "\n";
                            // copy the response to the ntagMemory
                            //nfcaContent = nfcaContent + "number of bytes read: : " + response.length + "\n";
                            nfcaContent = nfcaContent + "response:\n" + bytesToHex(response) + "\n";
                            //System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * i), nfcaMaxTranceive4ByteLength);
                        }

                    }

                    // ### section for writing only a part of page
                    if (dataPagesMod == 0) {
                        // don't write a new page
                        nfcaContent = nfcaContent + "write result: SUCCESS" + "\n";
                        writeToUi(nfcResult, nfcaContent);
                        try {
                            nfcA.close();
                        } catch (IOException e) {
                        }
                        return;
                    }
                    byte[] commandW = new byte[0];
                    if (dataPagesMod == 1) {
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + dataPages) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (dataPages * 4)],
                                (byte) 0x00,
                                (byte) 0x00,
                                (byte) 0x00
                        };
                    }
                    if (dataPagesMod == 2) {
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + dataPages) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (dataPages * 4)],
                                dataByte[1 + (dataPages * 4)],
                                (byte) 0x00,
                                (byte) 0x00
                        };
                    }
                    if (dataPagesMod == 3) {
                        commandW = new byte[]{
                                (byte) 0xA2,  // WRITE
                                (byte) ((4 + dataPages) & 0x0ff), // page 4 is the first user memory page
                                dataByte[0 + (dataPages * 4)],
                                dataByte[1 + (dataPages * 4)],
                                dataByte[2 + (dataPages * 4)],
                                (byte) 0x00
                        };
                    }

                    nfcaContent = nfcaContent + "command: " + bytesToHex(commandW) + "\n";
                    response = nfcA.transceive(commandW);
                    if (response == null) {
                        // either communication to the tag was lost or a NACK was received
                        // Log and return
                        nfcaContent = nfcaContent + "ERROR: null response";
                        String finalNfcaText = nfcaContent;
                        runOnUiThread(() -> {
                            nfcResult.setText(finalNfcaText);
                            System.out.println(finalNfcaText);
                        });
                        return;
                    } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                        // NACK response according to Digital Protocol/T2TOP
                        // Log and return
                        nfcaContent = nfcaContent + "ERROR: NACK response: " + bytesToHex(response);
                        String finalNfcaText = nfcaContent;
                        runOnUiThread(() -> {
                            nfcResult.setText(finalNfcaText);
                            System.out.println(finalNfcaText);
                        });
                        return;
                    } else {
                        // success: response contains ACK or actual data
                        // nfcaContent = nfcaContent + "successful reading " + response.length + " bytes\n";
                        // nfcaContent = nfcaContent + bytesToHex(response) + "\n";
                        // copy the response to the ntagMemory
                        //nfcaContent = nfcaContent + "number of bytes read: : " + response.length + "\n";
                        nfcaContent = nfcaContent + "response:\n" + bytesToHex(response) + "\n";
                        //System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * i), nfcaMaxTranceive4ByteLength);
                    }

                } catch(TagLostException e){
                    // Log and return
                    System.out.println("ERROR: Tag lost exception in body of WriteAuth");
                    nfcaContent = nfcaContent + "ERROR: Tag lost exception";
                    String finalNfcaText = nfcaContent;
                    runOnUiThread(() -> {
                        nfcResult.setText(finalNfcaText);
                        System.out.println(finalNfcaText);
                    });
                    return;
                } catch(IOException e){

                    e.printStackTrace();

                }
                nfcaContent = nfcaContent + "write result: SUCCESS" + "\n";
                String finalNfcaRawText = nfcaContent;
                String finalNfcaText = "parsed content:\n" + new String(ntagMemory, StandardCharsets.US_ASCII);

                runOnUiThread(() -> {
                    nfcResult.setText(finalNfcaRawText);
                    nfcResult.setText(finalNfcaText);
                    System.out.println(finalNfcaRawText);
                });
            } else{
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "NFC tag is NOT Nfca compatible",
                            Toast.LENGTH_SHORT).show();
                });
            }
        } catch(
                IOException e)

        {
            //Trying to catch any ioexception that may be thrown
            e.printStackTrace();
        } catch(
                Exception e)

        {
            //Trying to catch any exception that may be thrown
            e.printStackTrace();

        } finally

        {
            try {
                nfcA.close();
            } catch (IOException e) {
            }
        }

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
                writeToUiAppend(textView, "ERROR: NACK response: " + bytesToHex(response));
                return false;
            } else {
                // success: response contains (P)ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + bytesToHex(response));
                System.out.println("pwdAuth " + bytesToHex(passwordByte) + " response: " + bytesToHex(response));
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

    private void writeToUi(TextView textView, String message) {
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

    public static String removeAllNonAlphaNumeric(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
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