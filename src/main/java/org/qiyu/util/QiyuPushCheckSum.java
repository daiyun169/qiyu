package org.qiyu.util;

import java.security.MessageDigest;

/**
 * @author xianyu
 * @create 5/18/22 10:50 AM
 */
public class QiyuPushCheckSum {

  private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  public static String encode(String appSecret, String nonce, String time) {
    String content = appSecret + nonce + time;
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("sha1");
      messageDigest.update(content.getBytes());
      return getFormattedText(messageDigest.digest());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String getFormattedText(byte[] bytes) {
    int len = bytes.length;
    StringBuilder buf = new StringBuilder(len * 2);
    for (int j = 0; j < len; j++) {
      buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
      buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
    }
    return buf.toString();
  }

}
