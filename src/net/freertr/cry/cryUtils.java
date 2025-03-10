package net.freertr.cry;

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.List;
import net.freertr.pipe.pipeSide;
import net.freertr.util.bits;

/**
 * crypto utils
 *
 * @author matecsaba
 */
public class cryUtils {

    private cryUtils() {
    }

    /**
     * convert big unsigned integer to buffer
     *
     * @param b integer
     * @return buffer
     */
    public static byte[] bigUint2buf(BigInteger b) {
        byte[] dat = b.toByteArray();
        if (dat[0] != 0) {
            return dat;
        }
        byte[] buf = new byte[dat.length - 1];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = dat[i + 1];
        }
        return buf;
    }

    /**
     * convert buffer to big unsigned integer
     *
     * @param dat buffer
     * @return big integer
     */
    public static BigInteger buf2bigUint(byte[] dat) {
        byte[] buf = new byte[dat.length + 1];
        buf[0] = 0;
        bits.byteCopy(dat, 0, buf, 1, dat.length);
        return new BigInteger(buf);
    }

    /**
     * generate hash from text
     *
     * @param hsh hash to update
     * @param src lines to add
     * @param trm line terminator
     * @return false on success, true on error
     */
    public static boolean hashText(cryHashGeneric hsh, List<String> src, pipeSide.modTyp trm) {
        if (src == null) {
            return true;
        }
        byte[] buf = pipeSide.getEnding(trm);
        for (int i = 0; i < src.size(); i++) {
            hsh.update(src.get(i).getBytes());
            hsh.update(buf);
        }
        return false;
    }

    /**
     * generate hash from file
     *
     * @param hsh hash to update
     * @param src file to add
     * @return false on success, true on error
     */
    public static boolean hashFile(cryHashGeneric hsh, File src) {
        long pos = 0;
        long siz = -1;
        RandomAccessFile fr;
        try {
            fr = new RandomAccessFile(src, "r");
        } catch (Exception e) {
            return true;
        }
        try {
            siz = fr.length();
        } catch (Exception e) {
        }
        for (; pos < siz;) {
            final int max = 8192;
            long rndl = siz - pos;
            if (rndl > max) {
                rndl = max;
            }
            pos += rndl;
            int rndi = (int) rndl;
            byte[] buf = new byte[rndi];
            try {
                fr.read(buf, 0, rndi);
            } catch (Exception e) {
                siz = -1;
                break;
            }
            hsh.update(buf);
        }
        try {
            fr.close();
        } catch (Exception e) {
            return true;
        }
        return siz < 0;
    }

    /**
     * finish hash to hex
     *
     * @param hsh hash to finish
     * @return hex result
     */
    public static String hash2hex(cryHashGeneric hsh) {
        byte[] buf = hsh.finish();
        String s = "";
        for (int i = 0; i < buf.length; i++) {
            s += bits.toHexB(buf[i]);
        }
        return s.toLowerCase();
    }

}
