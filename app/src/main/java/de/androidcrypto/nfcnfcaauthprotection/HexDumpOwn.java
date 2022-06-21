package de.androidcrypto.nfcnfcaauthprotection;

public class HexDumpOwn {

    static int numberHexInt = 16; // anzahl der hexbytes pro zeile, minimum 6
    static boolean printHeaderBool = true;
    static boolean printDecimalAddressBool = false;
    //boolean printHexAddressBool = true;
    static boolean printHexAddressBool = true;
    static boolean printAscii = true; // false keine ascii-zeichen
    static boolean printDotBool = true;

    public static String prettyPrint(byte[] input) {
        String output = "";
        String outputLineString = ""; // eine ausgabezeile
        int filesizeLong = input.length;

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
                    outputLineString = outputLineString + byteToHexString(input[i]);
                    asciiZeileString = asciiZeileString + returnPrintableChar(input[i], printDotBool);
                    adresseInt++;
                    i++;
                }
            }
            i--; // korrektur des zählers für eine korrekte bearbeitung
            if (printAscii == true) {
                System.out.println(formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3)))
                        + (char) 124 + formatMitLeerzeichenRechts(asciiZeileString, (2 + numberHexInt)));
            output = output + "\n" + formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3)))
                    + (char) 124 + formatMitLeerzeichenRechts(asciiZeileString, (2 + numberHexInt));
            } else {
                System.out.println(formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3))));
                output = output + "\n" + formatMitLeerzeichenRechts(outputLineString, (laengeVorspannInt + (numberHexInt * 3)));
            }
        }
        return output;
    }

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
