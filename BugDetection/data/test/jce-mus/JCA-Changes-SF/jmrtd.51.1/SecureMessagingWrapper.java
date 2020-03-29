package sos.mrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import sos.smartcards.Apdu;
import sos.smartcards.ISO7816;
import sos.util.Hex;

public class SecureMessagingWrapper
{
   private static final IvParameterSpec ZERO_IV_PARAM_SPEC =
           new IvParameterSpec(new byte[8]);

   private SecretKey ksEnc, ksMac;
   private Cipher cipher;
   private Mac mac;
   private long ssc;

   private void readDO8E(DataInputStream in, byte[] rapdu)  {
      int length = in.readUnsignedByte();
      if (length != 8) {
         throw new IllegalStateException("DO'8E wrong length");
      }
      byte[] cc1 = new byte[8];
      in.readFully(cc1);
      mac.init(ksMac);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      DataOutputStream dataOut = new DataOutputStream(out);
      ssc++;
      dataOut.writeLong(ssc);
      byte[] paddedData = Util.pad(rapdu, 0, rapdu.length - 2 - 8 - 2);
      dataOut.write(paddedData, 0, paddedData.length);
      dataOut.flush();
      byte[] cc2 = mac.doFinal(out.toByteArray());
      if (!Arrays.equals(cc1, cc2)) {
         throw new IllegalStateException("Incorrect MAC!");
      }
   }
}