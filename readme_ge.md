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

