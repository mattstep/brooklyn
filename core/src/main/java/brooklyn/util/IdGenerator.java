package brooklyn.util;

import java.util.Random;

/**
 * 
 */
public class IdGenerator {
    private static Random random = new Random();
    private static String javaIdCharsStart = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static String javaIdCharsSubseq = javaIdCharsStart+"1234567890";
    private static String base64IdChars = javaIdCharsSubseq+"+=";

    /** @see makeRandomJavaId
     * <p>
     * Java semantics selected as a good default; 
     * the 2 extra base-64 chars are confusing as well as sometimes 
     * disallowed where ID's are used (eg in scripts);
     * a leading number could also be confusing and the range gain very slight anyway */
    public static String makeRandomId(int l) {
        return makeRandomJavaId(l);
    }
    
    /** makes a random id string (letters and numbers) of the given length;
     * starts with letter (upper or lower) so can be used as java-id;
     * tests ensure random distribution, so random ID of length 5 
     * is about 2^29 possibilities 
     * <p>
     * implementation is efficient, uses char array, and 
     * makes one call to random per 5 chars; makeRandomId(5)
     * takes about 4 times as long as a simple Math.random call,
     * or about 50 times more than a simple x++ instruction;
     * in other words, it's appropriate for contexts where random id's are needed,
     * but use efficiently (ie cache it per object), and 
     * prefer to use a counter where feasible
     **/
    public static String makeRandomJavaId(int l) {
            // copied from Monterey util's com.cloudsoftcorp.util.StringUtils.

            //this version is 30-50% faster than the old double-based one, 
            //which computed a random every 3 turns --
            //takes about 600 ns to do id of len 10, compared to 10000 ns for old version [on 1.6ghz machine]
            if (l<=0) return "";
            char[] id = new char[l];
            int d = random.nextInt( (26+26) * (26+26+10) * (26+26+10) * (26+26+10) * (26+26+10));
            int i = 0;    
            id[i] = javaIdCharsStart.charAt(d % (26+26));
            d /= (26+26);
            if (++i<l) do {
                    id[i] = javaIdCharsSubseq.charAt(d%(26+26+10));
                    if (++i>=l) break;
                    if (i%5==0) {
                            d = random.nextInt( (26+26+10) * (26+26+10) * (26+26+10) * (26+26+10) * (26+26+10));
                    } else {
                            d /= (26+26+10);
                    }
            } while (true);
            //Message.message("random id is " + id);
            return new String(id);
    }

    public static double randomDouble() {
        return random.nextDouble();
    }
    public static long randomLong() {
        return random.nextLong();
    }
    public static boolean randomBoolean() {
        return random.nextBoolean();
    }
    public static int randomInt() {
        return random.nextInt();
    }
    /** returns in [0,upbound) */
    public static int randomInt(int upbound) {
        return random.nextInt(upbound);
    }
    /** returns the array passed in */
    public static byte[] randomBytes(byte[] buf) {
        random.nextBytes(buf);
        return buf;
    }
    public static byte[] randomBytes(int length) {
        byte[] buf = new byte[length];
        return randomBytes(buf);
    }

    public static String makeRandomBase64Id(int length) {
        StringBuffer s = new StringBuffer();
        while (length>0) {
            appendBase64IdFromValueOfLength(randomLong(), length>10 ? 10 : length, s);
            length -= 10;
        }
        return s.toString();
    }
    public static String getBase64IdFromValue(long value) {
        return getBase64IdFromValue(value, 10);
    }
    public static String getBase64IdFromValue(long value, int length) {
        StringBuffer s = new StringBuffer();
        appendBase64IdFromValueOfLength(value, length, s);
        return s.toString();
    }
    public static void appendBase64IdFromValueOfLength(long value, int length, StringBuffer sb) {
        if (length>11)
            throw new IllegalArgumentException("can't get a Base64 string longer than 11 chars from a long");
        long idx = value;
        for (int i=0; i<length; i++) {
            byte x = (byte)(idx & 63);
            sb.append(base64IdChars.charAt(x));
            idx = idx >> 6;
        }
    }
}
