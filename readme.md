# NFC NFCA Auth Protection

**Important notice** This app is designed for the use of NTAG216 tags. Please do NOT use NTAG213 or NTAG215 
at the moment as some writing addresses do not match the tag and will fail.

Actual project status:

MainActivity: is writing an 8 character long string to pages 4 and 5 on the tag. If the tag is write 
protected on these pages the operation will fail with an IOException.

**FastReadActivity:** is reading some pages of the tag and displays them separately:
- page 00 serial number
- page 01 serial number (continued)
- page 02 serial number (continued), internal, static lock bytes
- page 03 Capability Container for NDEF messafes
- page 04 user data, 1st page
- page 05 user data, 2nd page
- page E2 (227) Configuration page 1 (see below)
- page E3 (228) Configuration page 2 (see below)
- page E4 (229) password
- page E5 (230) pack

The "fast read" is the special mode of NTAG21x tags as we can read a lot of more pages in one 
call that the "old fashioned" "read" command.

**FastReadAuthActivity:** the basic features are from FastReadActivity plus the authentication 
on password protected tags. You need to provide the same password as it was set during write 
protection operation plus the "PACK" value (for more information see SetWriteProtectionActivity).

**WriteActivity:**

**WritAuthActivity:**

**SetWriteProtectionActivity:**

**RemoveWriteProtectionActivity:**

**SpecialSettingsActivity:**

**WriteNdefMessageActivity:**

**EnableMirrorForNdefActivity:**





The Configuration page 1 does have the following fields:
- byte 0: Mirror
- byte 1: RFUI
- byte 2: Mirror_Page
- byte 3: AUTH0

The Configuration page 2 does have the following fields:
- byte 0: Access
- byte 1: RFUI
- byte 2: RFUI
- byte 3: RFUI

* RFUI means not in use at the moment

In config page 1 the AUTH0 byte is defining the page from which the password protection should start. 
Pages before this address are not protected.

In config page 2 the access byte defines if the memory is write protected (Bit = 0) or read and write 
protected (Bit = 1).


AndroidManifest.xml:
```plaintext
    <uses-permission android:name="android.permission.NFC" />
    <uses-permission android:name="android.permission.VIBRATE" />
```



```plaintext
NXP public key from easy_ecc_test.c
    //OSG Authentication - PubKey from Application note AN11350
    uint8_t PubKey[] = { 0x04, 0x49, 0x4E, 0x1A, 0x38, 0x6D, 0x3D, 0x3C,
        0xFE, 0x3D, 0xC1, 0x0E, 0x5D, 0xE6, 0x8A, 0x49,
        0x9B, 0x1C, 0x20, 0x2D, 0xB5, 0xB1, 0x32, 0x39,
        0x3E, 0x89, 0xED, 0x19, 0xFE, 0x5B, 0xE8, 0xBC, 0x61 };
```

The app icon is generated with help from **Launcher icon generator** 
(https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html), 
(options trim image and resize to 110%, color #2196F3).
