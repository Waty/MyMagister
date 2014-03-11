package com.wart.magister;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class TeaEncryption {

    public static final int BLOCKSIZE = 16384;
    public static String LastError = "";
    private static byte[] buffer = new byte[BLOCKSIZE];
    private final int CYCLES = 32;
    private int[] _key = new int[4];

    public TeaEncryption(String passphrase) {
        setPassphrase(passphrase);
    }

    public static boolean isSizeValid(int size) {
        return (size & 7) == 0;
    }

    public static int requiredSize(int datasize) {
        int var1 = datasize >>> 3;
        byte var2;
        if (datasize % 8 == 0) {
            var2 = 0;
        } else {
            var2 = 1;
        }

        return var2 + var1 << 3;
    }

    private void setPassphrase(String passphrase) throws SecurityException {
        if (passphrase == null || passphrase.length() < 1) {
            LastError = "Invalid passphrase.";
            throw new SecurityException(LastError);
        }

        int charcount = 0;
        _key[0] = 0;
        _key[1] = 0;
        _key[2] = 0;
        _key[3] = 0;
        byte[] pass = new byte[0];

        try {
            pass = passphrase.toLowerCase(Locale.ENGLISH).getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        for (int i = 0; i < passphrase.length(); i++) {
            byte c = pass[i];
            if ((Character.isLetterOrDigit(c) || 95 == c || CYCLES == c) && (CYCLES != c || i <= 0 || CYCLES != pass[i - 1])) {
                int ofs = (charcount & 3) << 3;
                int msk = 255 << ofs;
                byte y = (byte) ((msk & _key[(charcount & 15) >>> 2]) >>> ofs);
                byte var10 = (byte) (c ^ (y & 255) << 1 ^ (y & 255) >>> 1);
                int[] var11 = _key;
                int var12 = (charcount & 0xF) >>> 2;
                var11[var12] &= msk ^ -1;
                int var13 = msk & (var10 & 255) << ofs | _key[(charcount & 15) >>> 2];
                _key[(charcount & 0xF) >>> 2] = var13;
                ++charcount;
            }
        }

        if (charcount < 1) LastError = "Invalid passphrase.";
    }

    public void crypt(final byte[] array, final int n) {
        int n2 = 13;
        if ((n & 0x7) == 0x0) {
            for (int i = 0; i < n; i += 8) {
                final int n3 = array[i + 3] << 24 | (0xFF & array[i + 2]) << 16 | (0xFF & array[i + 1]) << 8 | 0xFF & array[i];
                int n4 = array[i + 7] << 24 | (0xFF & array[i + 6]) << 16 | (0xFF & array[i + 5]) << 8 | 0xFF & array[i + 4];
                int n5 = n3 ^ n2;
                int n6 = 0;
                int n7 = CYCLES;
                while (true) {
                    final int n8 = n7 - 1;
                    if (n7 <= 0) {
                        break;
                    }
                    n5 += n4 + (n4 << 4 ^ n4 >>> 5) ^ n6 + _key[n6 & 0x3];
                    n6 -= 1640531527;
                    n4 += n5 + (n5 << 4 ^ n5 >>> 5) ^ n6 + _key[0x3 & n6 >>> 11];
                    n7 = n8;
                }
                array[i + 3] = (byte) (n5 >>> 24);
                array[i + 2] = (byte) (0xFF & n5 >>> 16);
                array[i + 1] = (byte) (0xFF & n5 >>> 8);
                array[i] = (byte) (n5 & 0xFF);
                array[i + 7] = (byte) (n4 >>> 24);
                array[i + 6] = (byte) (0xFF & n4 >>> 16);
                array[i + 5] = (byte) (0xFF & n4 >>> 8);
                array[i + 4] = (byte) (n4 & 0xFF);
                n2 = (n2 + 27) % 31456;
            }
        }
    }

    public void decrypt(byte[] var1, int var2) {
        int var3 = 13;
        if ((var2 & 7) == 0) {
            for (int var4 = 0; var4 < var2; var4 += 8) {
                int var5 = var1[var4 + 3] << 24 | (255 & var1[var4 + 2]) << 16 | (255 & var1[var4 + 1]) << 8 | 255 & var1[var4];
                int var6 = var1[var4 + 7] << 24 | (255 & var1[var4 + 6]) << 16 | (255 & var1[var4 + 5]) << 8 | 255 & var1[var4 + 4];
                int var7 = -957401312;
                int var8 = CYCLES;

                while (true) {
                    int var9 = var8 - 1;
                    if (var8 <= 0) {
                        int var10 = var5 ^ var3;
                        var1[var4 + 3] = (byte) (var10 >>> 24);
                        var1[var4 + 2] = (byte) (255 & var10 >>> 16);
                        var1[var4 + 1] = (byte) (255 & var10 >>> 8);
                        var1[var4] = (byte) (var10 & 255);
                        var1[var4 + 7] = (byte) (var6 >>> 24);
                        var1[var4 + 6] = (byte) (255 & var6 >>> 16);
                        var1[var4 + 5] = (byte) (255 & var6 >>> 8);
                        var1[var4 + 4] = (byte) (var6 & 255);
                        var3 = (var3 + 27) % 31456;
                        break;
                    }

                    var6 -= var5 + (var5 << 4 ^ var5 >>> 5) ^ var7 + _key[3 & var7 >>> 11];
                    var7 += 1640531527;
                    var5 -= var6 + (var6 << 4 ^ var6 >>> 5) ^ var7 + _key[var7 & 3];
                    var8 = var9;
                }
            }
        }

    }

    public byte[] decryptData(byte[] var1) throws SecurityException {
        int var2 = var1.length - 4;
        if (var2 < 0 || !isSizeValid(var2)) {
            LastError = "De data kon niet ontsleuteld worden.";
            throw new SecurityException(LastError);
        }

        int var3 = 255 & var1[0] | (255 & var1[1]) << 8 | (255 & var1[2]) << 16 | (255 & var1[3]) << 24;
        if (var2 != requiredSize(var3)) {
            LastError = "De data kon niet ontsleuteld worden.";
            throw new SecurityException(LastError);
        }

        byte[] var4 = new byte[var3];
        int var5 = var2 / 16384;

        for (int var6 = 0; var6 < var5; ++var6) {
            System.arraycopy(var1, 4 + var6 * 16384, buffer, 0, 16384);
            decrypt(buffer, 16384);
            System.arraycopy(buffer, 0, var4, var6 * 16384, 16384);
        }

        int var7 = var2 - var5 * 16384;
        if (var7 > 0) {
            System.arraycopy(var1, var1.length - var7, buffer, 0, var7);
            decrypt(buffer, var7);
            System.arraycopy(buffer, 0, var4, var4.length - var3 % 16384, var3 % 16384);
        }

        return var4;
    }

    public byte[] encryptData(byte[] var1) {
        int var2 = var1.length;
        byte[] var3 = new byte[4 + requiredSize(var2)];

        for (int var4 = 0; var4 < var3.length; ++var4) {
            var3[var4] = 0;
        }

        var3[0] = (byte) (var2 & 255);
        var3[1] = (byte) (255 & var2 >>> 8);
        var3[2] = (byte) (255 & var2 >>> 16);
        var3[3] = (byte) (var2 >>> 24);

        int var5;
        for (var5 = 0; -4 + var3.length - var5 > 16384; var5 += 16384) {
            System.arraycopy(var1, var5, buffer, 0, 16384);
            crypt(buffer, 16384);
            System.arraycopy(buffer, 0, var3, var5 + 4, 16384);
        }

        int var6 = var2 % 16384;
        if (var6 > 0) {
            int var7 = requiredSize(var6);
            System.arraycopy(var1, var5, buffer, 0, var6);

            for (int var8 = var6; var8 < var7; ++var8) {
                buffer[var8] = 0;
            }

            crypt(buffer, var7);
            System.arraycopy(buffer, 0, var3, var5 + 4, var7);
        }

        return var3;
    }
}
