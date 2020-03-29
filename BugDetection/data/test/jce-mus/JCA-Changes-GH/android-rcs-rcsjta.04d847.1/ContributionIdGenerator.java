package com.orangelabs.rcs.core.ims.service.im.chat;

import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.DeviceUtils;

public class ContributionIdGenerator {

    private static byte[] secretKey = generateSecretKey();

    public synchronized static String getContributionId(String callId) {
    	try {
            // HMAC-SHA1 operation
            SecretKeySpec sks = new SecretKeySpec(secretKey, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(sks);
            byte[] contributionId = mac.doFinal(callId.getBytes());

            // Convert to Hexa and keep only 128 bits
            StringBuilder hexString = new StringBuilder(32);
            for (int i = 0; i < 16 && i < contributionId.length; i++) {
                String hex = Integer.toHexString(0xFF & contributionId[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String id = hexString.toString();
            return id;
        } catch(Exception e) {
            return null;
        }
    }
}
