package be.mapariensis.kanjiryoku.util;

public class ByteUtils {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toHexString(b & 0xff));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        hex = hex.replace("-", "");
        if (hex.length() % 2 != 0)
            throw new NumberFormatException(
                    "String should have an even number of characters");
        byte[] res = new byte[hex.length() / 2];
        for (int i = 0; i < res.length; i++) {
            res[i] = fromHex(hex.substring(2 * i, 2 * i + 2));
        }
        return res;
    }

    private static byte fromHex(String hex) {
        char[] chars = hex.replace("-", "").toCharArray();
        int hi = Character.digit(chars[0], 16);
        int lo = Character.digit(chars[1], 16);
        if (hi == -1 || lo == -1)
            throw new NumberFormatException("Not a hex digit: " + hex);
        return (byte) (16 * hi + lo);
    }
}
