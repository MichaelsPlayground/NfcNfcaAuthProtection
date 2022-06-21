package de.androidcrypto.nfcnfcaauthprotection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NtagDataReadingActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    EditText page00, page01, page02, page03, page04, page05, pageE2, pageE3, pageE4, pageE5, pageE6;
    EditText readResult;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ntag_data_reading);
        page00 = findViewById(R.id.etNtagRead00);
        page01 = findViewById(R.id.etNtagRead01);
        page02 = findViewById(R.id.etNtagRead02);
        page03 = findViewById(R.id.etNtagRead03);
        page04 = findViewById(R.id.etNtagRead04);
        page05 = findViewById(R.id.etNtagRead05);
        pageE2 = findViewById(R.id.etNtagReadE2);
        pageE3 = findViewById(R.id.etNtagReadE3);
        pageE4 = findViewById(R.id.etNtagReadE4);
        pageE5 = findViewById(R.id.etNtagReadE5);
        pageE6 = findViewById(R.id.etNtagReadE6);
        readResult = findViewById(R.id.etNtagReadResult);

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

            try {
                nfcA.connect();


                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (ntagVersion.equals("0")) {
                    runOnUiThread(() -> {
                        readResult.setText("NFC tag is NOT of type NXP NTAG213/215/216");
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
                try {
                    boolean responseSuccessful;
                    responseSuccessful = getTagData(nfcA, 00, page00, page01, page02, page03, readResult);
                    if (!responseSuccessful) return;
                    responseSuccessful = getTagData(nfcA, 04, page04, page05, null, null, readResult);
                    if (!responseSuccessful) return;
                    responseSuccessful = getTagData(nfcA, 226, pageE2, pageE3, pageE4, pageE5, readResult);
                    if (!responseSuccessful) return;
                    responseSuccessful = getTagData(nfcA, 230, pageE6, null, null, null, readResult);
                    if (!responseSuccessful) return;

                    // highlight the auth0 field
                    runOnUiThread(() -> {
                        SpannableString spannableStr = new SpannableString(pageE3.getText().toString());
                        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(Color.GREEN);
                        spannableStr.setSpan(backgroundColorSpan, 6, 8, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        pageE3.setText(spannableStr);
                    });

                    String finalNfcaRawText = nfcaContent;
                    String finalNfcaText = "parsed content:\n" + new String(ntagMemory, StandardCharsets.US_ASCII);
                    runOnUiThread(() -> {
                        readResult.setText(finalNfcaRawText);
                        //nfcContentParsed.setText(finalNfcaText);
                        System.out.println(finalNfcaRawText);
                    });

                } catch (Exception e) {
                    //Trying to catch any exception that may be thrown
                    e.printStackTrace();

                }

                try {
                    nfcA.close();
                } catch (IOException e) {
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean getTagData(NfcA nfcA, int page, EditText pageData1, EditText pageData2, EditText pageData3, EditText pageData4, EditText resultText) {
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
                runOnUiThread(() -> {
                    resultText.setText("ERROR: null response");
                    pageData1.setText("no data");
                    if (pageData2 != null) {
                        pageData2.setText("no data");
                    }
                    if (pageData3 != null) {
                        pageData3.setText("no data");
                    }
                    if (pageData4 != null) {
                        pageData4.setText("no data");
                    }
                });
                return false;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                runOnUiThread(() -> {
                    resultText.setText("ERROR: NACK response: " + bytesToHex(response));
                    pageData1.setText("response: " + bytesToHex(response));
                    if (pageData2 != null) {
                        pageData2.setText("response: " + bytesToHex(response));
                    }
                    if (pageData3 != null) {
                        pageData3.setText("response: " + bytesToHex(response));
                    }
                    if (pageData4 != null) {
                        pageData4.setText("response: " + bytesToHex(response));
                    }
                });
                return false;
            } else {
                // success: response contains ACK or actual data
                runOnUiThread(() -> {
                    resultText.setText("SUCCESS: response: " + bytesToHex(response));
                    // split the response
                    byte[] res1 = new byte[4];
                    byte[] res2 = new byte[4];
                    byte[] res3 = new byte[4];
                    byte[] res4 = new byte[4];
                    System.arraycopy(response, 0, res1, 0, 4);
                    System.arraycopy(response, 4, res2, 0, 4);
                    System.arraycopy(response, 8, res3, 0, 4);
                    System.arraycopy(response, 12, res4, 0, 4);
                    pageData1.setText(bytesToHex(res1));
                    System.out.println("page " + page + ": " + bytesToHex(res1));
                    if (pageData2 != null) {
                        pageData2.setText(bytesToHex(res2));
                        System.out.println("page " + (page + 1) + ": " + bytesToHex(res2));
                    }
                    if (pageData3 != null) {
                        pageData3.setText(bytesToHex(res3));
                        System.out.println("page " + (page + 2) + ": " + bytesToHex(res3));
                    }
                    if (pageData4 != null) {
                        pageData4.setText(bytesToHex(res4));
                        System.out.println("page " + (page + 3) + ": " + bytesToHex(res4));
                    }
                });
                System.out.println("page " + page + ": " + bytesToHex(response));
                result = true;
            }
        } catch (TagLostException e) {
            // Log and return
            runOnUiThread(() -> {
                readResult.setText("ERROR: Tag lost exception");
            });
            return false;
        } catch (IOException e) {
            runOnUiThread(() -> {
                resultText.setText("IOException: " + e.toString());
            });
            e.printStackTrace();
            return false;
        }
        return result;
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
/*
data original
I/System.out: page   0: 049e5042
I/System.out: page   1: 82355b80
I/System.out: page   2: 6c480000
I/System.out: page   3: e1106d00
I/System.out: page   4: 31323334
I/System.out: page   5: 74746572
I/System.out: page 226: 000000bd
I/System.out: page 227: 040000ff
I/System.out: page 228: 00050000
I/System.out: page 229: 00000000
I/System.out: page 230: 00000000

I/System.out: page 227: 040000 ff
                        Auth0: XX
 */