package de.androidcrypto.nfcnfcaauthprotection;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NtagDataReadingActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    EditText page00, page01, page02, page03, page04, page05, pageE2, pageE3, pageE4, pageE5, pageE6;
    EditText pageE4Byte0, counterField, signatureField;
    EditText readResult;
    com.google.android.material.textfield.TextInputLayout passwordDecoration, packDecoration;
    com.google.android.material.textfield.TextInputEditText passwordField, packField;
    com.google.android.material.switchmaterial.SwitchMaterial authenticationSwitch;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ntag_data_reading);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        authenticationSwitch = findViewById(R.id.swReadAuth);
        passwordField = findViewById(R.id.etReadAuthPassword);
        passwordDecoration = findViewById(R.id.etReadAuthPasswordDecoration);
        packField = findViewById(R.id.etReadAuthPack);
        packDecoration = findViewById(R.id.etReadAuthPackDecoration);
        page00 = findViewById(R.id.etNtagRead00);
        page01 = findViewById(R.id.etNtagRead01);
        page02 = findViewById(R.id.etNtagRead02);
        page03 = findViewById(R.id.etNtagRead03);
        page04 = findViewById(R.id.etNtagRead04);
        page05 = findViewById(R.id.etNtagRead05);
        pageE2 = findViewById(R.id.etNtagReadE2);
        pageE3 = findViewById(R.id.etNtagReadE3);
        pageE4 = findViewById(R.id.etNtagReadE4);
        pageE4Byte0 = findViewById(R.id.etNtagReadE4Byte0);
        pageE5 = findViewById(R.id.etNtagReadE5);
        pageE6 = findViewById(R.id.etNtagReadE6);
        counterField = findViewById(R.id.etNtagReadCnt);
        signatureField = findViewById(R.id.etNtagReadSignature);
        readResult = findViewById(R.id.etNtagReadResult);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        authenticationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    passwordDecoration.setVisibility(View.VISIBLE);
                    packDecoration.setVisibility(View.VISIBLE);
                } else {
                    passwordDecoration.setVisibility(View.GONE);
                    packDecoration.setVisibility(View.GONE);
                }
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
                try {

                    // do we want to write with or without authentication ?
                    boolean readWithAuthentication = authenticationSwitch.isChecked();
                    if (readWithAuthentication) {
                        // get data from passwordField
                        String passwordString = passwordField.getText().toString();
                        // limitation: exact 4 alphanumerical characters
                        passwordString = Utils.removeAllNonAlphaNumeric(passwordString);
                        if (passwordString.length() != 4) {
                            nfcaContent = nfcaContent + "Error: you need to enter exact 4 alphanumerical characters for PASSWORD" + "\n";
                            writeToUiAppend(readResult, nfcaContent);
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
                            writeToUiAppend(readResult, nfcaContent);
                            return;
                        }
                        byte[] packByte = packString.getBytes(StandardCharsets.UTF_8);
                        int packLength = packByte.length;
                        nfcaContent = nfcaContent + "Pack: " + packString + " hex: " + Utils.bytesToHex(packByte) + "\n";
                        // as we write a complete page we need to fill up the bytes 3 + 4 with 0x00
                        byte[] packBytePage = new byte[4];
                        System.arraycopy(packByte, 0, packBytePage, 0, 2);

                        // send the pwdAuth command
                        byte[] response;
                        // this is the default value
                        byte[] passwordByteDefault = new byte[]{
                                (byte) (255 & 0x0ff),
                                (byte) (255 & 0x0ff),
                                (byte) (255 & 0x0ff),
                                (byte) (255 & 0x0ff)
                        };
                        //passwordByte = passwordByteDefault.clone();
                        writeToUiAppend(readResult, "*** start authentication");
                        response = sendPwdAuthData(nfcA, passwordByte, readResult);
                        if (response == null) {
                            writeToUiAppend(readResult, "authentication FAILURE. Maybe wrong password or the tag is not write protected");
                            writeToUiToast("authentication FAILURE. Maybe wrong password or the tag is not write protected");
                            return;
                        }
                        byte[] packResponse = response.clone();
                        if (Arrays.equals(packResponse, packByte)) {
                            writeToUiAppend(readResult, "The entered PACK is correct");
                        } else {
                            writeToUiAppend(readResult, "entered PACK: " + Utils.bytesToHex(packByte));
                            writeToUiAppend(readResult, "Respons PACK: " + Utils.bytesToHex(packResponse));
                            writeToUiAppend(readResult, "The entered PACK is NOT correct, abort");
                            writeToUiToast("The entered PACK is NOT correct, abort");
                            return;
                        }

                    } // writeWithAuthentication

                    boolean responseSuccessful;
                    responseSuccessful = getTagData(nfcA, 00, page00, page01, page02, page03, readResult);
                    if (!responseSuccessful) return;
                    responseSuccessful = getTagData(nfcA, 04, page04, page05, null, null, readResult);
                    if (!responseSuccessful) return;
                    // show ascii characters in page 04 and 05 if available
                    try {
                        byte[] page04Org = Utils.hexStringToByteArray(page04.getText().toString());
                        byte[] page05Org = Utils.hexStringToByteArray(page05.getText().toString());
                        String page04NewHex = Utils.bytesToHex(page04Org) + " (" +
                                new String(page04Org, StandardCharsets.US_ASCII) +
                                ")";
                        String page05NewHex = Utils.bytesToHex(page05Org) + " (" +
                                new String(page05Org, StandardCharsets.US_ASCII) +
                                ")";
                        runOnUiThread(() -> {
                            page04.setText(page04NewHex);
                            page05.setText(page05NewHex);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    responseSuccessful = getTagData(nfcA, 226, pageE2, pageE3, pageE4, pageE5, readResult);
                    if (!responseSuccessful) return;
                    byte[] responsePage226 = getTagDataResponse(nfcA, 226);
                    byte[] responsePage228Byte0 = new byte[1];
                    System.arraycopy(responsePage226, 8, responsePage228Byte0, 0, 1);
                    // analyse the bits in pageE4 Byte 0
                    //byte pageE4Byte0Byte = hexStringToByteArray(pageE4.getText().toString())[3];

                    //String s1;
                    //s1 = printByteArray(hexStringToByteArray(pageE4.getText().toString()));
                    String s1 = Utils.printByteArrayBinary(responsePage228Byte0);
                    //byte pageE4Byte0Byte = (byte) 129;
                    //String s1 = String.format("%8s", Integer.toBinaryString(pageE4Byte0Byte & 0xFF)).replace(' ', '0');
                    //System.out.println(s1); // 10000001

                    runOnUiThread(() -> {
                        pageE4Byte0.setText(s1);
                        //nfcContentParsed.setText(finalNfcaText);
                        System.out.println(s1);
                    });
                    //System.out.println( s );

                    responseSuccessful = getTagData(nfcA, 230, pageE6, null, null, null, readResult);
                    if (!responseSuccessful) return;

                    // highlight the auth0 field
                    runOnUiThread(() -> {
                        SpannableString spannableStr = new SpannableString(pageE3.getText().toString());
                        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(Color.GREEN);
                        spannableStr.setSpan(backgroundColorSpan, 6, 8, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        pageE3.setText(spannableStr);
                    });

                    // highlight the ACCESS field
                    runOnUiThread(() -> {
                        SpannableString spannableStr = new SpannableString(pageE4.getText().toString());
                        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(Color.CYAN);
                        spannableStr.setSpan(backgroundColorSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        pageE4.setText(spannableStr);
                    });

                    // counter
                    byte[] nfcCounter = getTagCounterResponse(nfcA);
                    String nfcTagCounterString = "";
                    if (nfcCounter != null) {
                        nfcTagCounterString = Utils.getDec(nfcCounter);
                    } else {
                        nfcTagCounterString = "counter not enabled";
                    }

                    String finalNfcTagCounterString = nfcTagCounterString;
                    runOnUiThread(() -> {
                        counterField.setText(finalNfcTagCounterString);
                    });

                    // signature
                    // see https://www.nxp.com/docs/en/application-note/AN12196.pdf
                    // pages 50/51
                    // NTAG public key:
                    // 048A9B380AF2EE1B98DC417FECC263F8449C7625CECE82D9B916C992DA209D68 422B81EC20B65A66B5102A61596AF3379200599316A00A1410

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
                    resultText.setText("ERROR: NACK response: " + Utils.bytesToHex(response));
                    pageData1.setText("response: " + Utils.bytesToHex(response));
                    if (pageData2 != null) {
                        pageData2.setText("response: " + Utils.bytesToHex(response));
                    }
                    if (pageData3 != null) {
                        pageData3.setText("response: " + Utils.bytesToHex(response));
                    }
                    if (pageData4 != null) {
                        pageData4.setText("response: " + Utils.bytesToHex(response));
                    }
                });
                return false;
            } else {
                // success: response contains ACK or actual data
                runOnUiThread(() -> {
                    resultText.setText("SUCCESS: response: " + Utils.bytesToHex(response));
                    // split the response
                    byte[] res1 = new byte[4];
                    byte[] res2 = new byte[4];
                    byte[] res3 = new byte[4];
                    byte[] res4 = new byte[4];
                    System.arraycopy(response, 0, res1, 0, 4);
                    System.arraycopy(response, 4, res2, 0, 4);
                    System.arraycopy(response, 8, res3, 0, 4);
                    System.arraycopy(response, 12, res4, 0, 4);
                    pageData1.setText(Utils.bytesToHex(res1));
                    System.out.println("page " + page + ": " + Utils.bytesToHex(res1));
                    if (pageData2 != null) {
                        pageData2.setText(Utils.bytesToHex(res2));
                        System.out.println("page " + (page + 1) + ": " + Utils.bytesToHex(res2));
                    }
                    if (pageData3 != null) {
                        pageData3.setText(Utils.bytesToHex(res3));
                        System.out.println("page " + (page + 2) + ": " + Utils.bytesToHex(res3));
                    }
                    if (pageData4 != null) {
                        pageData4.setText(Utils.bytesToHex(res4));
                        System.out.println("page " + (page + 3) + ": " + Utils.bytesToHex(res4));
                    }
                });
                System.out.println("page " + page + ": " + Utils.bytesToHex(response));
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
            runOnUiThread(() -> {
                readResult.setText("ERROR: Tag lost exception");
            });
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private byte[] getTagCounterResponse(NfcA nfcA) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0x39,  // READ_CNT
                (byte) 0x02,
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
                System.out.println("READ_CNT response: " + Utils.bytesToHex(response));
            }
        } catch (TagLostException e) {
            // Log and return
            runOnUiThread(() -> {
                readResult.setText("ERROR: Tag lost exception");
            });
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private byte[] sendPwdAuthData(NfcA nfcA, byte[] passwordByte, TextView textView) {
        byte[] response; // the response is the PACK returned by the tag when successful authentication
        byte[] command = new byte[]{
                (byte) 0x1B,  // PWD_AUTH
                passwordByte[0],
                passwordByte[1],
                passwordByte[2],
                passwordByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "ERROR: null response");
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "ERROR: NACK response: " + Utils.bytesToHex(response));
                return null;
            } else {
                // success: response contains (P)ACK or actual data
                writeToUiAppend(textView, "SUCCESS: response: " + Utils.bytesToHex(response));
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception OR Tag is not protected");
            return null;
        } catch (IOException e) {
            writeToUiAppend(textView, "IOException: " + e.toString());
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private byte[] getTagSignatureResponse(NfcA nfcA) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) (0x3C),  // READ_SIG
                (byte) (0x00)
        };
        try {
            System.out.println("*** before getTagSignatureResponse transceive");
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            System.out.println("*** after getTagSignatureResponse transceive");
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                System.out.println("*** response == null");
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                System.out.println("*** Bad NACK response: " + Utils.bytesToHex(response));
                return null;
            } else {
                // success: response contains ACK or actual data
                System.out.println("*** READ_SIG response: " + Utils.bytesToHex(response));
            }
        } catch (TagLostException e) {
            // Log and return
            System.out.println("*** TagLostException");
            runOnUiThread(() -> {
                readResult.setText("ERROR: Tag lost exception");
            });
            return null;
        } catch (IOException e) {
            System.out.println("*** IOException");
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private void showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mRead = menu.findItem(R.id.action_read);
        mRead.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(NtagDataReadingActivity.this, NtagDataReadingActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWrite = menu.findItem(R.id.action_write);
        mWrite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(NtagDataReadingActivity.this, WriteActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteProtection = menu.findItem(R.id.action_write_protection);
        mWriteProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(NtagDataReadingActivity.this, SetWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mRemoveProtection = menu.findItem(R.id.action_remove_protection);
        mRemoveProtection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(NtagDataReadingActivity.this, RemoveWriteProtectionActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mSpecialSettings = menu.findItem(R.id.action_special_settings);
        mSpecialSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(NtagDataReadingActivity.this, SpecialSettingsActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mWriteNdef = menu.findItem(R.id.action_write_ndef_message);
        mWriteNdef.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(NtagDataReadingActivity.this, WriteNdefMessageActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mEnableMirrorNdefMessage = menu.findItem(R.id.action_enable_ndef_message_mirror);
        mEnableMirrorNdefMessage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(NtagDataReadingActivity.this, EnableMirrorForNdefActivity.class);
                startActivity(i);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
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