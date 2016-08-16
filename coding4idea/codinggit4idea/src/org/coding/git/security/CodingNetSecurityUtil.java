package org.coding.git.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by robin on 16/8/8.
 */
public class CodingNetSecurityUtil {

    public static String getUserPasswordOfSHA1(String passWord){
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA-1");
            digest.update(passWord.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String shaHex = Integer.toHexString(messageDigest[i] & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
