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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    com.google.android.material.textfield.TextInputLayout inputFieldDecoration;
    com.google.android.material.textfield.TextInputEditText inputField;

    TextView nfcResult;
    Button fastRead, writeAuth, setWriteProtection, removeWriteProtection;
    Button test, verifySignature, specialSettings;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputField = findViewById(R.id.etMainInputField);
        inputFieldDecoration = findViewById(R.id.etMainInputFieldDecoration);
        nfcResult = findViewById(R.id.tvMainNfcaResult);
        fastRead = findViewById(R.id.btnMainFastRead);
        writeAuth = findViewById(R.id.btnMainWriteAuth);

        setWriteProtection = findViewById(R.id.btnMainSetWriteProtection);
        removeWriteProtection = findViewById(R.id.btnMainRemoveWriteProtection);
        specialSettings = findViewById(R.id.btnMainSpecialSettings);

        test = findViewById(R.id.btnMainTest);
        verifySignature = findViewById(R.id.btnMainVerifySignature);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        fastRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NtagDataReadingActivity.class);
                startActivity(intent);
            }
        });

        writeAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WriteAuthActivity.class);
                startActivity(intent);
            }
        });

        setWriteProtection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SetWriteProtectionActivity.class);
                startActivity(intent);
            }
        });

        removeWriteProtection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(intent);
            }
        });

        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CommandActivity.class);
                startActivity(intent);
                /*
                byte accessByte = (byte) 0x80;
                byte accessByteUnset = unsetBitInByte(accessByte, 7);
                byte accessByteSet = setBitInByte(accessByteUnset, 7);
                System.out.println("accessByte  : " + accessByte + " " + printByteArrayBinary(new byte[(byte) accessByte]));
                System.out.println("accessByte u: " + accessByteUnset + " " + printByteArrayBinary(new byte[(byte) accessByteUnset]));
                System.out.println("accessByte s: " + accessByteSet + " " + printByteArrayBinary(new byte[(byte) accessByteSet]));

                 */
            }
        });

        verifySignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VerifyTagSignatureActivity.class);
                startActivity(intent);
            }
        });

        specialSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SpecialSettingsActivity.class);
                startActivity(intent);
            }
        });
    }


    // ##### TEST METHODS #####
    // position is 0 based starting from right to left
    private byte setBitInByte(byte input, int pos) {
        return (byte) (input | (1 << pos));
    }

    // position is 0 based starting from right to left
    private byte unsetBitInByte(byte input, int pos) {
        return (byte) (input & ~(1 << pos));
    }

    private static String printByteArrayBinary(byte[] bytes) {
        String output = "";
        for (byte b1 : bytes) {
            String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
            //s1 += " " + Integer.toHexString(b1);
            //s1 += " " + b1;
            //output = output + " " + s1;
            output = s1;
            //System.out.println(s1);
        }
        return output;
    }

    // ##### TEST METHODS END #####

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
                byte[] response;
                try {
                    // get data from InputField
                    String dataString = inputField.getText().toString();

                    // limitation: maximal 8 characters
                    if (dataString.length() > 8) {
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

                    // ### section for writing only part of page
                    if (dataPagesMod == 0) {
                        // don't write a new page
                        nfcaContent = nfcaContent + "write result: SUCCESS" + "\n";
                        writeToUi(nfcResult, nfcaContent);
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(),
                                    "Data written SUCCESSFUL",
                                    Toast.LENGTH_SHORT).show();
                        });
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

                } catch (TagLostException e) {
                    // Log and return
                    nfcaContent = nfcaContent + "ERROR: Tag lost exception";
                    String finalNfcaText = nfcaContent;
                    runOnUiThread(() -> {
                        nfcResult.setText(finalNfcaText);
                        System.out.println(finalNfcaText);
                    });
                    return;
                } catch (IOException e) {
                    nfcaContent = nfcaContent + "IOException: " + e.toString();
                    String finalNfcaText = nfcaContent;
                    runOnUiThread(() -> {
                        nfcResult.setText(finalNfcaText);
                        System.out.println(finalNfcaText);
                    });
                    e.printStackTrace();
                    return;
                }
                nfcaContent = nfcaContent + "write result: SUCCESS" + "\n";
                String finalNfcaRawText = nfcaContent;
                String finalNfcaText = "parsed content:\n" + new String(ntagMemory, StandardCharsets.US_ASCII);
                runOnUiThread(() -> {
                    nfcResult.setText(finalNfcaRawText);
                    nfcResult.setText(finalNfcaText);
                    System.out.println(finalNfcaRawText);
                    Toast.makeText(getApplicationContext(),
                            "Data written SUCCESSSFUL",
                            Toast.LENGTH_SHORT).show();

                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "NFC tag is NOT Nfca compatible",
                            Toast.LENGTH_SHORT).show();
                });
            }
        } catch (IOException e) {
            //Trying to catch any ioexception that may be thrown
            e.printStackTrace();
            String finalNfcaRawText = "ERROR: IOException";
            String finalNfcaText = "ERROR: IOException";
            runOnUiThread(() -> {
                nfcResult.setText(finalNfcaRawText);
                nfcResult.setText(finalNfcaText);
                System.out.println(finalNfcaRawText);
            });
        } catch (Exception e) {
            String finalNfcaRawText = "ERROR: Exception";
            String finalNfcaText = "ERROR: Exception";
            runOnUiThread(() -> {
                nfcResult.setText(finalNfcaRawText);
                nfcResult.setText(finalNfcaText);
                System.out.println(finalNfcaRawText);
            });
            //Trying to catch any exception that may be thrown
            e.printStackTrace();

        } finally {
            try {
                nfcA.close();
            } catch (IOException e) {
                String finalNfcaRawText = "ERROR: IOException";
                String finalNfcaText = "ERROR: IOException";
                runOnUiThread(() -> {
                    nfcResult.setText(finalNfcaRawText);
                    nfcResult.setText(finalNfcaText);
                    System.out.println(finalNfcaRawText);
                });
            }
        }

    }

    private void writeToUi(TextView textView, String message) {
        runOnUiThread(() -> {
            textView.setText(message);
        });
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