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

- enable counter / aktiviere den Zaehler: Mit der Aktivierung des Zaehlers wird der Tag-interne 
Zaehler bei jedem Auslesevorgang um 1 erhoeht. Der Zaehler kann zwar wieder deaktiviert werden 
(siehe naechste Funktion), aber eine Zuruecksetzung ist nicht vorgesehen. Der Zaehlerstand kann mit 
einem speziellen Befehl ausgelesen werden - wie es im Menuepunkt "read from Tag / Tag lesen" 
gezeigt wird.

- disable counter /deaktiviere den Zaehler: nach der Deaktivierung des Zaehlers fuehrt ein Ausleseversuch  
zu einem Fehler.

Bezueglich der naechsten Funktionen erklaere ich vorab die Funktionsweise des "mirroring" bzw. der 
Spiegelung. Sowohl die eindeutige "Serien-" Nummer (UID) als auch der Zaehlerstand koennen von vielen 
Android-Smartphones nicht ausgelesen werden. Unter Apple's IOS koennen NFC Tag nur ueber die NDEF-
Technologie gelesen werden und nicht "Low level" mittels NFCA. Damit diese Informationen z.B. aus 
Sicherheitsgruenden lesbar sind hat der Tag-Hersteller NXP die Spiegelung oder Einblendung in den 
Benutzerspeicherplatz vorgesehen. Das bedeutet im Klartext: die Werte des UID und/oder des Zaehlers 
werden virtuell ueber die frueheren Daten gelegt und beim auslesen stattdessen ausgelesen. 

Fuer eigene Versuche empfehle ich, parallel die App "NfcNfcaTagHexDump" auf das Smartphone zu spielen,  
denn damit wird der komplette Speicherinhalt des Tags uebersichtlich angezeigt.

Zum besseren Vergleich - so sieht ein unbeschriebener NTAG216 im Hexdump aus:

```plaintext
Header:
00000000:04 40 89 45 82 35 5B 81 |...E.5..  
00000008:6D 48 00 00 E1 10 6D 00 |mH....m. 
User memory:
00000010:03 00 FE 00 00 00 00 00 |........ 
00000018:00 00 00 00 00 00 00 00 |........
...
Footer:
00000388:00 00 00 BD 04 00 00 FF |........  
00000390:00 05 00 00 00 00 00 00 |........  
```
 
Nachdem mittels "write tag" die Zeichenfolge "8 letter" geschrieben wurde sieht der Tag so aus:

```plaintext
User memory:
00000010:38 20 6C 65 74 74 65 72 |8.letter  
00000018:00 00 00 00 00 00 00 00 |........
```

Spielen wir nun mit den verschiedenen Mirror/Spiegelungs-Funktionen

- activate UID mirror / aktiviere die Spiegelung des UID: nach der Aktivierung dieser Funktion wird 
der 7-stellige UID in Hex-Enkodierung ab Seite 05 abgelegt. Parallel zur Aktivierung dieser Funktion 
ist es auch notwendig, die erste Seite der Spiegelung anzugeben - fuer unser Beispiel ist es Seite 5.   
Der UID ist stets 8 Stellen lang, daher werden 3 Seiten je 4 Byte und eine Seite mit 3 Byte (insgesamt 
14 Byte) fuer die Spiegelung benoetigt. Schauen Sie sich die Werte im HexDump an:

```plaintext
Header:
00000000:04 40 89 45 82 35 5B 81 |...E.5..  
00000008:6D 48 00 00 E1 10 6D 00 |mH....m. 
User memory:
00000010:38 20 6C 65 30 34 34 30 |8.le0440 
00000018:38 39 38 32 33 35 35 42 |8982355B 
00000020:38 31 00 00 00 00 00 00 |81......  
...
Footer:
00000388:00 00 00 BD 44 00 05 FF |....D...  
00000390:00 05 00 00 00 00 00 00 |........ 
```

Page 00: 04 40 89 45 : die ersten 3 Byte 04 40 89 sind die ersten 3 Stellen der UIDs, die weiteren  
Stellen befinden sich in den folgenden Seiten. 

Page 04: 38 20 6c 65: Das sind die Hex-Ascii Werte fuer den Text "8 let"

Page 05: bei deaktivierter UID-Spiegelung steht hier 74 74 65 72 = "tter". Haben Sie die UID-
Spiegelung aktiviert erscheint stattdessen: 30 34 34 30 - diese Werte entsprechen dem Ascii-Text 
"0440" - schauen Sie auf Seite 00 und finden fuer die ersten beiden Bytes der UID "04 40".

- activate counter mirror / aktiviere die Spiegelung des Zaehlers: nach Aktivierung des Zaehlers 
wird der Dezimalwert des Zaehlers ab Adresse Seite 05 als 6 Byte lange Hexadezimal Zahl angezeigt.  
In meinem Beispiel entspricht "5Ah" der Zahl 90 = es gab 90 Lesungen vom Tag.

```plaintext
Header:
00000000:04 40 89 45 82 35 5B 81 |...E.5..  
00000008:6D 48 00 00 E1 10 6D 00 |mH....m. 
User memory:
00000010:38 20 6C 65 30 30 30 30 |8.le0000 
00000018:35 41 00 00 00 00 00 00 |5A...... 
00000020:00 00 00 00 00 00 00 00 |........  
...
Footer:
00000388:00 00 00 BD 44 00 05 FF |....D...  
00000390:10 05 00 00 00 00 00 00 |........  
```

- activate UID + counter mirror / aktiviere die Spiegelung der UID und des Zaehlers: zuerst wird ab 
Adresse Seite 05 die UID eingeblendet, gefolgt von einem "x" Zeichen als Trenner und nachfolgend der 
6 stellige Zaehler (der nun "5Ch" = Zaehlerstand 92 enthaelt):

```plaintext
Header:
00000000:04 40 89 45 82 35 5B 81 |...E.5..  
00000008:6D 48 00 00 E1 10 6D 00 |mH....m. 
User memory:
00000010:38 20 6C 65 30 34 34 30 |8.le0440 
00000018:38 39 38 32 33 35 35 42 |8982355B 
00000020:38 31 78 30 30 30 30 35 |81x00005  
00000028:43 00 00 00 00 00 00 00 |C.......  
...
Footer:
00000388:00 00 00 BD 44 00 05 FF |....D...  
00000390:10 05 00 00 00 00 00 00 |........  
```

- deactivate all mirror / deaktiviere alle Spiegelungen: sobald die Spiegelungen deaktivert sind 
zeigt der Speicher wieder seinen "urspruenglichen" Inhalt an.

Kommen wir nun zu einem praktischen Beispiel fuer die Spiegelung. Sie moechten auf dem Tag eine 
NDEF-formatierte Nachricht speichern, welche eine Zugangs-Internetadresse ist. Mit dem Aufruf der   
Seite moechten Sie auch die Tag-UID sowie den aktuellen Zaehlerstand uebergeben. Rufen Sie hierzu 
diesen Menuepunkt auf: 

- write NDEF message / schreibe NDEF Nachricht: Die Basisadresse in unserem Beispiel lautet  

http://androidcrypto.bplaced.net/test.html?d=

gefolgt von einem String "UUUUUUUUUUUUUUxCCCCCC" - die komplette URL lautet also: 

http://androidcrypto.bplaced.net/test.html?d=UUUUUUUUUUUUUUxCCCCCC

Sie werden es erraten haben - die "UU.." stehen als Platzhalter fuer die UID und die "CC.." fuer 
den Zaehler.

Beschreiben Sie den Tag und erhalten dieses Ergebnis im HexDump:

```plaintext
00000010:03 40 D1 01 3C 55 03 61 |.....U.a  
00000018:6E 64 72 6F 69 64 63 72 |ndroidcr  
00000020:79 70 74 6F 2E 62 70 6C |ypto.bpl  
00000028:61 63 65 64 2E 6E 65 74 |aced.net  
00000030:2F 74 65 73 74 2E 68 74 |.test.ht  
00000038:6D 6C 3F 64 3D 55 55 55 |ml.d.UUU 
00000040:55 55 55 55 55 55 55 55 |UUUUUUUU  
00000048:55 55 55 78 43 43 43 43 |UUUxCCCC  
00000050:43 43 FE 00 00 00 00 00 |CC......  
...
00000388:00 00 00 BD D4 00 0F FF |........  
00000390:00 05 00 00 00 00 00 00 |........  
```

Kehren Sie nun mit der "Home"-Taste auf Ihren Smartphone Startbildschirm zurueck und halten den 
Tag an die Rueckseite - das Smartphone erkennt die URL in der NDEF-Nachricht und versucht ein 
geeignetes Programm zu finden - ein (oder mehrere) Browser werden bei Ihnen bestimmt angeboten. 
Hinweis: die Internetadresse fuehrt ins Leere).

Aktivieren Sie nun die passende Spiegelung fuer diese Nachricht:

- enable mirror for NDEF message / aktivieren Sie die Spiegelung fuer die NDEF-Nachricht: da die 
URL-Adresse etwas laenger ist als die 4 Bytes in unseren "special settings" Beispielen ist die   
Startadresse fuer die Spiegelung die Seite 0Fh und das Byte 1 darin. Nach der Aktivierung finden 
Sie diesen Inhalt auf dem Tag:

```plaintext
00000010:03 40 D1 01 3C 55 03 61 |.....U.a  
00000018:6E 64 72 6F 69 64 63 72 |ndroidcr  
00000020:79 70 74 6F 2E 62 70 6C |ypto.bpl  
00000028:61 63 65 64 2E 6E 65 74 |aced.net  
00000030:2F 74 65 73 74 2E 68 74 |.test.ht  
00000038:6D 6C 3F 64 3D 30 34 34 |ml.d.044  
00000040:30 38 39 38 32 33 35 35 |08982355  
00000048:42 38 31 78 30 30 30 30 |B81x0000  
00000050:36 35 FE 00 00 00 00 00 |65......
...
00000388:00 00 00 BD D4 00 0F FF |........  
00000390:10 05 00 00 00 00 00 00 |........
```

Damit wird der Browser folgende Internetadresse aufrufen:

http://androidcrypto.bplaced.net/test.html?d=04408982355B81x000065

Ein hinter der Adresse liegendes Script kann nun auswerten, das der Tag mit der Seriennummer (UID)
04408982355B81 nun zum 101 Mal (65h) ausgelesen wurde und Kontakt zum Server aufnimmt.

- clear tag to factory settings / setzte den Tag auf Werkseinstellungen zurueck: Dieser Menuepunkt  
beschreibt den kompletten Speicher eines NTAG216 mit den Werten, wie sie bei einem fabrikneuen 
Tag vorhanden sind. Diese Aktion kann bis zu einer Minute dauern und daher ist es sehr wichtig, 
Ihr Smartphone auf den Tag zu legen und nicht mehr zu bewegen, um eine "Tag lost exception" zu 
vermeiden.

Diese Aktion kann aber nur bei einem Tag durchgefuehrt werden, welcher keinen Schreibschutz aktiviert  
hat. Deaktivieren Sie also vorher eine eventuell eingerichtete "write protection". 



