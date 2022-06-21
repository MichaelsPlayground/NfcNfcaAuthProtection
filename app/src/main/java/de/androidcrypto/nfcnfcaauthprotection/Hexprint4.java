package de.androidcrypto.nfcnfcaauthprotection;


// das programm liest eine datei ein und gibt die hexadezimalwerte aus

// eingabe: dateiname filenameReadString = ".."
// eingabe: anzahl der hexzeichen pro zeile numberHexInt = 16
// minimum der anzahl der hexzeichen: 6
// optional ausgabe eines headers printHeaderBool = true
// optional ausgabe der dezimaladresse printDecimalAddressBool = true
// optional ausgabe der hexadezimaladresse printHexAddressBool = true
// optional ausgabe eines punktes statt leerzeichen bei ascii-spalte
//          printDotBool = true
// optional speicherung der ausgabe in eine datei printToFileBool = true
// notwendig bei speicherung der ausgabe in eine datei: filenameWriteString = ".."

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

public class Hexprint4 {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void main(String[] args) throws Exception {
        System.out.println("Hexprint V03");

        // variablen zur nutzung
        //String filenameReadString = "textpane2rtf.rtf"; //ecdsa_der_sample_signature.bin
        //String filenameReadString = "ecdsa_der_sample_signature.bin";
        //String filenameReadString = "ecdsa_p1363_sample_signature.bin";
        //String filenameReadString = "edsacmessage.txt";
        String filenameReadString = "rsamesssage.pss.signature.bin";

        String filenameWriteString = "textpane2rtf.txt";
        int numberHexInt = 16; // anzahl der hexbytes pro zeile, minimum 6
        boolean printHeaderBool = true;
        boolean printDecimalAddressBool = false;
        //boolean printHexAddressBool = true;
        boolean printHexAddressBool = false;
        boolean printAscii = false; // false keine ascii-zeichen
        boolean printDotBool = true;
        boolean printToFileBool = true;

        // programminterne variablen
        long filesizeLong = 0; // göße der datei in bytes
        String outputLineString = ""; // eine ausgabezeile
        /*
        if (FileExistsCheck(filenameReadString) == false) {
            System.out.println("Die Datei " + filenameReadString + " existiert nicht. Das Programm wird beendet.");
            System.exit(0);
        }*/
        filesizeLong = Files.size(new File(filenameReadString).toPath());
        System.out.println("File: " + filenameReadString + " Größe in Bytes:" + filesizeLong);
        // speicherung der ausgaben in eine datei
        BufferedWriter writer = null;
        if (printToFileBool == true) {
            writer = new BufferedWriter(new FileWriter(filenameWriteString));
            writer.write("Datei:" + filenameReadString + " Größe in Bytes:" + filesizeLong);
            writer.write("\n");
        }

        // inhalt der datei komplett lesen
        File fileRead = new File(filenameReadString);
        FileInputStream fis = new FileInputStream(fileRead);
        byte[] fileByte = new byte[(int) fileRead.length()];
        fis.read(fileByte);
        fis.close();

        // ausgabefunktion
        System.out.println();
        int adresseInt = 0;
        String asciiZeileString = "";
        outputLineString = "";
        int laengeVorspannInt = 0; // dezimal + 9, hex + 9, dezimal+hex + 18
        if (printHeaderBool == true) {
            if (printDecimalAddressBool == true) {
                outputLineString = outputLineString + "Dezimal  ";
                laengeVorspannInt = laengeVorspannInt + 9;
            }
            if (printHexAddressBool == true) {
                outputLineString = outputLineString + "Hex      ";
                laengeVorspannInt = laengeVorspannInt + 9;
            }
            outputLineString = outputLineString + formatMitLeerzeichenRechts("Hexadezimalwerte", (numberHexInt * 3));
            if (printAscii == true) {
                outputLineString = outputLineString + (char) 124 + "ASCII";
            }
            System.out.println(outputLineString);
            if (printToFileBool == true) {
                writer.write(outputLineString);
                writer.write("\n");
            }
        }
        // nutzdaten
        for (int i = 0; i < filesizeLong; i++) {
            outputLineString = "";
            // ausgabe der adresse als dezimalzahl
            if (printDecimalAddressBool == true) {
                outputLineString = outputLineString + formatMitNullenLinks(String.valueOf(adresseInt), 8) + ":";
            }
            // ausgabe der adresse als hexwert
            if (printHexAddressBool == true) {
                outputLineString = outputLineString + formatMitNullenLinks(Integer.toHexString(adresseInt), 8) + ":";
            }
            asciiZeileString = "";
            for (int j = 0; j < numberHexInt; j++) {
                // überprüfung ob die maximale zeichenzahl überschritten wird
                if (i < filesizeLong) {
                    outputLineString = outputLineString + byteToHexString(fileByte[i]);
                    asciiZeileString = asciiZeileString + returnPrintableChar(fileByte[i], printDotBool);
                    adresseInt++;
                    i++;
                }
            }
            i--; // korrektur des zählers für eine korrekte bearbeitung
            if (printAscii == true) {
                System.out.println(formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3)))
                        + (char) 124 + formatMitLeerzeichenRechts(asciiZeileString, (2 + numberHexInt)));
                if (printToFileBool == true) {
                    writer.write(formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3)))
                            + (char) 124 + formatMitLeerzeichenRechts(asciiZeileString, (2 + numberHexInt)));
                    writer.write("\n");
                } } else {
                System.out.println(formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3))));
                if (printToFileBool == true) {
                    writer.write(formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3))));
                    writer.write("\n");
                }
            }

        }
        if (printToFileBool == true) {
            writer.close();
        }
        System.out.println("Hexprint V03 beendet");
    }
/*
    private static boolean FileExistsCheck(String dateinameString) {
        return Files.exists(Paths.get(dateinameString), new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
    }
*/
    public static String formatMitLeerzeichenRechts(String value, int len) {
        while (value.length() < len) {
            value += " ";
        }
        return value;
    }

    public static String formatMitNullenLinks(String value, int len) {
        while (value.length() < len) {
            value = "0" + value;
        }
        return value;
    }

    public static String byteToHexString(byte inputByte) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X ", inputByte));
        return sb.toString();
    }

    public static char returnPrintableChar(byte inputByte, Boolean printDotBool) {
        // gibt nur die ascii-zeichen aus folgenden bereichen zurück
        // 48- 57 = 0-9
        // 65- 90 = A-Z
        // 97-122 = a-z
        // wenn printDotBool = true dann ausgabe eines punktes statt leerzeichen
        char rueckgabeChar = 0;
        if (printDotBool == true) {
            rueckgabeChar = 46;
        }
        if ((inputByte >= 48) && (inputByte <= 57)) {
            rueckgabeChar = (char) inputByte;
        }
        if ((inputByte >= 65) && (inputByte <= 90)) {
            rueckgabeChar = (char) inputByte;
        }
        if ((inputByte >= 97) && (inputByte <= 122)) {
            rueckgabeChar = (char) inputByte;
        }
        return rueckgabeChar;
    }
}
