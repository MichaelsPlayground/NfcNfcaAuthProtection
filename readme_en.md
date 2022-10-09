# NFC NFCA Auth Protection

This app enables very complex writing and reading processes with an NFC tag of the NTAG216 type.

Some read or write operations work with hardcoded addresses, **hence works
this app only with NFC tag NTAG216 from the manufacturer NXP**.

The app only reads or writes data when the app and the menu item are loaded.

**WARNING: Improper or careless use can irreversibly damage the tag,
therefore only use the app with test tags or tags that are not in productive use.**

Use the menu at the top right to operate the app and access the following activities:

To understand the individual data fields and activities, it is very helpful to have the official
Download and open the manufacturer's data sheet. This link takes you to
Data sheet: https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf

**read from tag / read tag**:

An NFC tag is usually not protected against reading and writing. If such a
However, if protection is activated, the access password must be entered. The input mask appears
as soon as the "use authentication for reading" switch is switched on.

You enter the correct 4 digit alphanumeric password and a 2 digit pack value
("password acknowledge" / "known password") and then read out the content.

You can find further information on this under the menu item "write protection".

After reading the tag, the app outputs the following data:

Page 00: contains part of the tag's serial number

Page 01: contains part of the tag's serial number

Page 02: contains part of the tag's serial number and "lock bytes"

Page 03: contains the "Compatibility container" so that any NDEF messages on the
day can be read.

Page 04: the first 4 bytes of free user memory

Page 05: the next 4 bytes of free user memory

Page E2: contains the "dynamic lock bytes"

Page E3: contains the "configuration page 1". The last byte (byte 3) has a green background and contains
the page address but the password protection works. The site is without password protection
"FF" = 255 saved. A possible "mirroring" is noted in byte 0 and in byte 2
the page is saved, but the mirrored data starts. For more information, see
the menu item "special settings".

Page E4: contains the "configuration page 2". Byte 0 stores which access rights
have been awarded. Further information is available under the menu item "write protection".

Page E4 Byte 0 (Access): this is the bitwise representation of byte 0 (leftmost in Page 04).

Page E5: contains the password (when reading, "00000000" is always output)

Page E6: contains the pack (reading always returns "00000000")

Below the pages a possibly activated meter reading is displayed (if the meter is deactivated
"counter not enabled" is displayed).

The NTAG216 contains a digital signature based on the "Elliptic Curve" cryptosystem. here
only the signature is issued, the correctness is checked in a separate app
("NfcNfcaVerifyNtag21xSignature").

The last field contains a log file with data about the tag and the read processes.

**write on tag / describe tag**:

A very simple function is offered here - a few characters are written to the user memory
written from page 04h. The special feature lies in the possibility of using the data on one day
to send with activated write protection. For this purpose, the switch "use authentication for writing"
switched on and entered the password along with the pack. For more information, see
the menu item "write protection".

A log file is output below the input field.

**set up write protection**

With this menu item you set up a write and read protection for the day.
The password consists of exactly 4 letters and numbers ("alphanumeric"). Additionally assigned
a 2-digit "PACK", which is the "correct password acknowledge" (= "answer at
correct password"), a.

The meaning of the PACK is quite simple: after an authentication attempt, the tag responds
with the PACK (i.e. the password is correct) or a "NAK" (="no acknowledge" / "not
right"). This is an extra level of security for your program - if you use the PACK
evaluate.

In addition, it is necessary to specify the page from which the write (and possibly read)
Access is protected.

The following switch determines whether the memory area is "only" write-protected or
additionally receives a read protection.

Very important for all users of NDEF messages on the tag: as soon as the stick has a
has read protection (i.e. a password is required to read the NDEF message

is), can
the day can no longer be processed via the usual routines - it always requires a
special reading program.

Also important: there is no reset option if you forgot your password
has been; the day is then worthless.

For information only (since not implemented in the program): in addition to password protection
the maximum number of incorrect entries for the password can also be programmed.
Further information can be found in the data sheet on page 30 under point "8.8 Password verification
protection" and "8.8.2 Limiting negative verification attempts".

**remove protection**

This app removes a protection set up with the previous menu item. This is correct
Enter password and PACK. After successful authentication, the first "protected"
Page set to FFh (= 255) - since this page is far above the maximum number of pages, the
whole day no longer protected. In addition, the password field with FFh FFh FFh FFh and der
PACK assigned 00h 00h.

**special settings / special tasks**

The functions in this menu enable 2 interesting special tasks - activating the tag
internal read counter as well as the mirroring (better fade-in) of the tag ID and/or the counter in
the free user memory of the tag.

- enable counter / activate the counter: With the activation of the counter, the tag-internal
  Counter increased by 1 with each reading process. The counter can be deactivated again
  (see next function), but a reset is not provided. The meter reading can
  be read out with a special command - as it is in the menu item "read from tag / read tag"
  will be shown.

- disable counter / deactivate the counter: after the deactivation of the counter, a reading attempt is made
  to a mistake.

With regard to the next functions, I will first explain how "mirroring" or the
Reflection. Both the unique "serial" number (UID) and the meter reading can be used by many
Android smartphones cannot be read. On Apple's IOS, NFC tags can only be used via the NDEF
technology and not "low level" using NFCA. So that this information e.g
For security reasons, the tag manufacturer NXP has the mirroring or display in the
User storage provided. That means in plain language: the values of the UID and/or the counter
are placed virtually over the earlier data and are read instead when read out.

For your own experiments, I recommend running the "NfcNfcaTagHexDump" app on your smartphone at the same time,
because the complete memory content of the tag is clearly displayed.

For a better comparison - this is what an unwritten NTAG216 looks like in the hexdump:

```plaintext
headers:
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

After the character string "8 letter" has been written using "write tag", the tag looks like this:

```plaintext
User memory:
00000010:38 20 6C 65 74 74 65 72 |8.letter
00000018:00 00 00 00 00 00 00 00 |........
```

Now let's play with the different mirror functions

- activate UID mirror / activate mirroring of UID: after activating this function will
  the 7-digit UID is stored in hex encoding from page 05. Parallel to the activation of this function
  it is also necessary to specify the first side of the mirror - for our example it is side 5.
  The UID is always 8 digits long, so 3 pages of 4 bytes each and one page of 3 bytes (total
  14 bytes) required for mirroring. Look at the values in the HexDump:

```plaintext
headers:
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

Page 00: 04 40 89 45 : the first 3 bytes 04 40 89 are the first 3 digits of the UIDs, the others
Jobs are on the following pages.

Page 04: 38 20 6c 65: These are the hex ascii values for the text "8 let"

Page 05: if UID mirroring is deactivated, 74 74 65 72 = "tter" appears here. Do you have the UID
Mirroring enabled appears instead: 30 34 34 30 - these values correspond to the Ascii text
"0440" - look at page 00 and find "04 40" for the first two bytes of the UID.

- activate counter mirror: after activating the counter
  the decimal value of the counter is written as a 6-byte Hexadecimal number displayed.
  In my example, "5Ah" corresponds to the number 90 = there were 90 readings from the day.

```plaintext
headers:
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

- activate UID + counter mirror / activate mirroring of UID and counter: first becomes off
  Address Page 05, the UID is displayed, followed by an "x" character as a separator and then the
  6-digit counter (which now contains "5Ch" = counter reading 92):

```plaintext
headers:
00000000:04 40 89 45 82 35 5B 81 |...E.5..
00000008:6D 48 00 00 E1 10 6D 00 |mH....m.
User memory:
00000010:38 20 6C 65 30 34 34 30 |8.le0440
00000018:38 39 38 32 33 35 35 42 |8982355B
00000020:38 31 78 30 30 30 30 35 |81x00005
00000028:43 00 00 00 00 00 00 00 |C......
...
Footer:
00000388:00 00 00 BD 44 00 05 FF |....D...
00000390:10 05 00 00 00 00 00 00 |........
```

- deactivate all mirror / deactivate all mirrors: once the mirrors are deactivated
  the memory shows its "original" content again.

Now let's move on to a practical example of mirroring. They want one on the day
Store NDEF formatted message which is a gateway internet address. By calling the
page you also want to transfer the tag UID and the current counter reading. Call for this
this menu item:

- write NDEF message / write NDEF message: The base address in our example is

http://androidcrypto.bplaced.net/test.html?d=

followed by a string "UUUUUUUUUUUUUUxCCCCCC" - so the complete URL is:

http://androidcrypto.bplaced.net/test.html?d=UUUUUUUUUUUUUUxCCCCCC

You will have guessed it - the "UU.." are placeholders for the UID and the "CC.." for
the counter.

Describe the day and get this result in the HexDump:

```plaintext
00000010:03 40 D1 01 3C 55 03 61 |.....E.a
00000018:6E 64 72 6F 69 64 63 72 |ndroidcr
00000020:79 70 74 6F 2E 62 70 6C |ypto.bpl
00000028:61 63 65 64 2E 6E 65 74|aced.net
00000030:2F 74 65 73 74 2E 68 74 |.test.ht
00000038:6D 6C 3F 64 3D 55 55 55 |ml.d.UUU
00000040:55 55 55 55 55 55 55 55 |UUUUUUUU
00000048:55 55 55 78 43 43 43 43 |UUUxCCCC
00000050:43 43 FE 00 00 00 00 00 |CC......
...
00000388:00 00 00 BD D4 00 0F FF |........
00000390:00 05 00 00 00 00 00 00 |........
```

Now return to your smartphone start screen with the "Home" button and hold the button
Tag to the back - the smartphone recognizes the URL in the NDEF message and tries
to find a suitable program - one (or more) browsers will certainly be offered to you.
Note: the internet address leads to nowhere).

Now activate the appropriate mirroring for this message:

- enable mirror for NDEF message / enable mirror for NDEF message: since the
  URL address is slightly longer than the 4 bytes in our "special settings" examples is the
  Start address for mirroring page 0Fh and byte 1 in it. Find after activation
  this content on the tag:

```plaintext
00000010:03 40 D1 01 3C 55 03 61 |.....E.a
00000018:6E 64 72 6F 69 64 63 72 |ndroidcr
00000020:79 70 74 6F 2E 62 70 6C |ypto.bpl
00000028:61 63 65 64 2E 6E 65 74|aced.net
00000030:2F 74 65 73 74 2E 68 74 |.test.ht
00000038:6D 6C 3F 64 3D 30 34 34 |ml.d.044
00000040:30 38 39 38 32 33 35 35 |08982355
00000048:42 38 31 78 30 30 30 30 |B81x0000
00000050:36 35 FE 00 00 00 00 00 |65......
...
00000388:00 00 00 BD D4 00 0F FF |........
00000390:10 05 00 00 00 00 00 00 |........
```

The browser will then call up the following Internet address:

http://androidcrypto.bplaced.net/test.html?d=04408982355B81x000065

A script behind the address can now evaluate that the tag with the serial number (UID)
04408982355B81 has now been read out 101 times (65h) and is making contact with the server.

- clear tag to factory settings / reset the tag to factory settings: This menu item
  describes the complete memory of an NTAG216 with the values as in a brand new one
  tag are available. This action can take up to a minute and it is therefore very important
  Putting your smartphone on the tag and not moving it anymore to get a "tag lost exception".
  avoid.

However, this action can only be carried out on a tag that does not activate write protection
has. You should therefore first deactivate any "write protection" that may have been set up.