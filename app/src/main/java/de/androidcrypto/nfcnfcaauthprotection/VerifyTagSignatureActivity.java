package de.androidcrypto.nfcnfcaauthprotection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class VerifyTagSignatureActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    EditText tagId, tagSignature, publicKeyNxp, result;
    private NfcAdapter mNfcAdapter;
    byte[] tagIdByte, tagSignatureByte, publicKeyByte;
    boolean signatureVerfied;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_tag_signature);
        tagId = findViewById(R.id.etVerifyTagId);
        tagSignature = findViewById(R.id.etVerifySignature);
        publicKeyNxp = findViewById(R.id.etVerifyPublicKey);
        result = findViewById(R.id.etVerifyResult);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        publicKeyNxp.setText("04494E1A386D3D3C" +
                "FE3DC10E5DE68A49" +
                "9B1C202DB5B13239" +
                "3E89ED19FE5BE8BC61");
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
                    result.setText("");
                });

                nfcA.connect();

                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (ntagVersion.equals("0")) {
                    runOnUiThread(() -> {
                        result.setText("NFC tag is NOT of type NXP NTAG213/215/216");
                        Toast.makeText(getApplicationContext(),
                                "NFC tag is NOT of type NXP NTAG213/215/216",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // tag ID
                tagIdByte = tag.getId();
                runOnUiThread(() -> {
                    tagId.setText(bytesToHex(tagIdByte));
                });

                byte[] response = new byte[0];

                try {
                    String commandString = "3c00"; // read signature
                    byte[] commandByte = hexStringToByteArray(commandString);
                    try {
                        response = nfcA.transceive(commandByte); // response should be 16 bytes = 4 pages
                        if (response == null) {
                            // either communication to the tag was lost or a NACK was received
                            writeToUiAppend(result, "ERROR: null response");
                            return;
                        } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                            // NACK response according to Digital Protocol/T2TOP
                            // Log and return
                            writeToUiAppend(result, "ERROR: NACK response: " + bytesToHex(response));
                            return;
                        } else {
                            // success: response contains (P)ACK or actual data
                            writeToUiAppend(result, "SUCCESS: response: " + bytesToHex(response));
                            //System.out.println("write to page " + page + ": " + bytesToHex(response));
                            tagSignatureByte = response.clone();
                            runOnUiThread(() -> {
                                tagSignature.setText(bytesToHex(tagSignatureByte));
                            });
                        }
                    } catch (TagLostException e) {
                        // Log and return
                        System.out.println("*** TagLostException");
                        runOnUiThread(() -> {
                            result.setText("ERROR: Tag lost exception or command not recognized");
                        });
                        return;
                    } catch (IOException e) {
                        writeToUiAppend(result, "ERROR: IOException " + e.toString());
                        System.out.println("*** IOException");
                        e.printStackTrace();
                        return;
                    }
                } finally {
                    try {
                        nfcA.close();
                    } catch (IOException e) {
                        writeToUiAppend(result, "ERROR: IOException " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            writeToUiAppend(result, "ERROR: IOException " + e.toString());
            e.printStackTrace();
        }

        KeyFactory kf = null;
        PublicKey pubKey = null;
        publicKeyByte = hexStringToByteArray(publicKeyNxp.getText().toString());
        try {
            kf = KeyFactory.getInstance("EC");
            pubKey = (PublicKey) kf.generatePublic(new X509EncodedKeySpec(publicKeyByte));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        // now we are going to verify
        signatureVerfied = ecVerifySignatureP1363(pubKey, tagIdByte, tagSignatureByte);
        writeToUiAppend(result, "SignatureVerified: " + signatureVerfied);

        // manual data from AN12196.pdf
        String tagIdX = "04518DFAA96180";
        String signatureRX = "D1940D17CFEDA4BFF80359AB975F9F6514313E8F90C1D3CAAF5941AD";
        String signatureSX = "744A1CDF9A83F883CAFE0FE95D1939B1B7E47113993324473B785D21";
        String pubXD = "8A9B380AF2EE1B98DC417FECC263F8449C7625CECE82D9B916C992DA";
        String pubYD = "209D68422B81EC20B65A66B5102A61596AF3379200599316A00A1410";
        // secp224r1


    }

    private static Boolean ecVerifySignatureP1363(PublicKey publicKey, byte[] messageByte, byte[] signatureByte)
 {
     Signature publicSignature = null;
     try {
         //publicSignature = Signature.getInstance("SHA256withECDSAinP1363format");
         publicSignature = Signature.getInstance("SHA384withECDSAinP1363format");
         publicSignature.initVerify(publicKey);
         publicSignature.update(messageByte);
         return publicSignature.verify(signatureByte);
     } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
         e.printStackTrace();
         return false;
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

    private static String printByteArrayBinary(byte[] bytes) {
        String output = "";
        for (byte b1 : bytes) {
            String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
            //s1 += " " + Integer.toHexString(b1);
            //s1 += " " + b1;
            output = output + " " + s1;
            //System.out.println(s1);
        }
        return output;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
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

}