package org.qiyu.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyu
 * @create 5/18/22 7:17 PM
 */
@Slf4j
public final class QiyuConfig {

  private QiyuConfig() {
  }

  private static final String QIYU_PRO_URL = "/qiyu.properties";
  private static String appKey;
  private static String appSecret;

  static {

    InputStream in = QiyuConfig.class.getResourceAsStream(QIYU_PRO_URL);

    Properties properties = new Properties();

    try {

      properties.load(in);

      appKey = properties.getProperty("qiyu.appkey");
      appSecret = properties.getProperty("qiyu.appsecret");

    } catch (IOException e) {
      log.error("qiyu.properties Initialization failure", e);
    }catch (Exception e){
      log.error("",e);
    }

  }

  public static String getAppKey() {
    return appKey;
  }

  public static String getAppSecret() {
    return appSecret;
  }

}
