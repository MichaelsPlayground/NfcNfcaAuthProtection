# NFC NFCA Auth Protection

Diese App ermöglicht sehr komplexe Schreib- und Lesevorgaenge mit einem NFC Tag vom Typ NTAG216.

Einige Lese- oder Schreiboperationen arbeiten mit fest kodierten Adressen, **daher funktioniert 
diese App nur mit NFC Tag NTAG216 des Herstellers NXP**.

Die App liest oder schreibt Daten nur, wenn die App und der Menuepunkt geladen sind.

**WARNUNG: Durch unsachgemaessen oder unvorsichtigen Gebrauch koennen Sie den Tag irreversibel beschaedigen, 
daher benutzen Sie die App nur mit Test-Tags oder Tags, die nicht im produktiven Einsatz sind.**

Über das Menue rechts oben bedienen Sie die App und gelangen zu den folgenden Aktivitaeten:

Zum Verstaendnis der einzelnen Datenfelder und Aktivitaeten ist es sehr hilfreich, das offizielle 
Datenblatt des Herstellers herunter zu laden und zu oeffnen. Sie gelangen ueber diesen Link zum 
Datenblatt: https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf

**read from Tag / Tag lesen**: 

Ueblicherweise ist ein NFC Tag nicht gegen das Lesen und Schreiben geschuetzt. Sollte ein derartiger 
Schutz jedoch aktiviert sein, muss das Zugangspasswort eingegeben werden. Die Eingabemaske erscheint 
so bald der Switch "use authentication for reading" eingeschaltet wird.

Sie geben das richtige 4 stellige alphanumerische Passwort und einen 2 stelligen Pack-Wert 
("password acknowledge" / "bekanntes passwort") ein und lesen dann den Inhalt aus.

Weitere Informationen dazu erhalten Sie unter dem Menuepunkt "write protection".

Die App gibt nach dem Lesen des Tags folgende Daten aus:

Page 00: enthaelt einen Teil der Seriennummer des Tags

Page 01: enthaelt einen Teil der Seriennummer des Tags

Page 02: enthaelt einen Teil der Seriennummer des Tags sowie "lock bytes"

Page 03: enthaelt den "Compatibility container", damit eventuelle NDEF Nachrichten auf dem 
Tag gelesen werden können.

Page 04: die ersten 4 Bytes des freien Benutzerspeichers

Page 05: die naechsten 4 Bytes des freien Benutzerspeichers

Page E2: enthaelt die "dynamic lock bytes"

Page E3: enthaelt die "configuration page 1". Das letzte Byte (Byte 3) ist grün unterlegt und enthaelt 
die Seitenadresse aber der ein Passwortschutz funktioniert. Ohne Passwortschutz ist die Seite 
"FF" = 255 gespeichert. Im Byte 0 ist eine eventuelle "Spiegelung" vorgemerkt und in Byte 2 wird 
die Seite gespeichert, aber der die gespiegelten Daten beginnen. Weitere Informationen erhalten Sie unter 
dem Menuepunkt "special settings".

Page E4: enthaelt die "configuration page 2". Hier wird im Byte 0 gespeichert, welche Zugriffsrechte 
vergeben worden sind. Weitere Informationen erhalten Sie unter dem Menuepunkt "write protection".

Page E4 Byte 0 (Access): das ist die bitweise Darstellung des Byte 0 (ganz links in Page 04). 

Page E5: enthaelt das Passwort (beim Lesen wird immer "00000000" ausgegeben)

Page E6: enthaelt das Pack (beim Lesen wird immer "00000000" ausgegeben)

Unterhalb der Seiten wird ein ggfls. aktivierter Zaehlerstand angezeigt (falls der Zaehler deaktiviert 
ist erscheint "counter not enabled" / Zaehler nicht aktiviert).

Der NTAG216 enthaelt eine digitale Signatur auf Basis des "Elliptische Kurven" Kryptosystems. Hier 
wird nur die Signatur ausgegeben, die Prüfung auf Richtigkeit erfolgt in einer separaten App 
("NfcNfcaVerifyNtag21xSignature").

Das letzte Feld enthaelt ein Log-File mit Daten zum Tag und der Lesevorgaenge.

**write on Tag / Tag beschreiben**: 

Hier wird eine ganz einfache Funktion angeboten - es werden ein paar Zeichen in den Benutzerspeicher 
ab Seite 04h geschrieben. Die Besonderheit liegt in der Moeglichkeit, die Daten auf einen Tag mit 
aktiviertem Schreibschutz zu senden. Hierzu wird der Switch "use authentication for writing" 
eingeschaltet und das Passwort nebst Pack eingegeben. Weitere Informationen dazu erhalten Sie unter 
dem Menuepunkt "write protection".

Unterhalb des Eingabefeldes wird ein Logfile ausgegeben.

**write protection / Schreibschutz einrichten**

Mit diesem Menuepunkt richten Sie einen Schreib- und zusaetzlich eine Leseschutz fuer den Tag ein. 
Das Passwort besteht aus exakt 4 Buchstaben und Ziffern ("alphanumerisch"). Zusaetzlich vergeben 
Sie ein 2-stelliges "PACK", das ist das "correct password acknowledge" (= "Antwort bei 
korrektem Passwort"), ein. 

Die Bedeutung ist des PACK recht einfach: Nach einem Authentisierungsversuch antwortet der Tag 
mit dem PACK (d.h. das Passwort ist korrekt) oder einem "NAK" (="no acknowledge" / "nicht 
richtig"). Das ist eine zusaetzliche Sicherheitsstufe fuer Ihr Programm - wenn Sie den PACK 
auswerten.

Zusaetzlich ist es notwendig, die Seite anzugeben, ab der der Schreib- (und eventuell Lese-) 
Zugriff geschuetzt ist.

Der folgende Switch bestimmt, ob der Speicherbereich "nur" schreibgeschuetzt ist oder 
zusaetzlich auch einen Leseschutz erhaelt.

Ganz wichtig fuer alle Nutzer von NDEF-Nachrichten auf dem Tag: sobald der Stick einen 
Leseschutz hat (d.h. fuer das Lesen der NDEF-Nachricht ein Passwort notwendig ist), kann 
der Tag nicht mehr ueber die ueblichen Routinen verarbeitet werden - er benoetigt stets ein 
spezielles Leseprogramm.

Ebenfalls wichtig: es gibt keine Zuruecksetzungsmoeglichkeit wenn das Passwort vergessen 
worden ist; der Tag ist dann wertlos. 

Nur zur Information (da im Programm nicht implementiert): zusaetzlich zum Passwortschutz 
kann auch die maximale Zahl an Falscheingaben fuer das Passwort programmiert werden. 
Weitere Informationen sind im Datenblatt auf Seite 30 unter Punkt "8.8 Password verification 
protection" sowie "8.8.2 Limiting negative verification attempts" zu finden.

**remove protection / Schreibschutz entfernen**

Diese App entfernt einen mit dem vorigen Menuepunkt eingerichteten Schutz. Hierzu ist das korrekte 
Passwort sowie PACK einzugeben. Nach erfolgreicher Authentifizierung wird die erste "geschuetzte" 
Seite auf FFh (= 255) gesetzt - da diese Seite weit oberhalb der maximalen Seitenzahl liegt ist der 
gesamte Tag damit nicht mehr geschuetzt. Zusaetzlich wird das Passwortfeld mit FFh FFh FFh FFh und der 
PACK mit 00h 00h belegt.

**special settings / Spezialaufgaben**

Die Funktionen in diesem Menue ermoeglichen 2 interessante Spezialaufgaben - die Aktivierung des Tag- 
internen Lesezaehlers sowie die Spiegelung (besser Einblendung) der Tag-ID und/oder des Zaehlers in  
den freien Benutzerspeicher des Tags.






