package org.qiyu.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.qiyu.config.QiyuConfig;
import org.qiyu.constants.ApiConstant;
import org.qiyu.util.QiyuPushCheckSum;

/**
 * 七鱼 - 在线系统 - 获取会话记录：
 *
 * http://qiyukf.com/docs/guide/server/6-%E5%9C%A8%E7%BA%BF%E7%B3%BB%E7%BB%9F.html#%E8%8E%B7%E5%8F%96%E4%BC%9A%E8%AF%9D%E8%AE%B0%E5%BD%95
 *
 * @author xianyu
 * @create 5/18/22 10:45 AM
 */
@Slf4j
public class SessionHistoryService {

  /**
   * 批量导出会话记录
   *
   * @param start 开始时间 时间戳毫秒单位
   * @param end 结束时间 时间戳毫秒单位
   */
  public String downloadSession(long start, long end) throws IOException {

    Map<String, Long> bodyMap = Maps.newHashMap();
    bodyMap.put("start", start);
    bodyMap.put("end", end);

    String content = toJsonString(bodyMap);

    String realApiUrl = fillUrl(ApiConstant.SESSION_URL, content);

    // 正常结果：{"code":200,"message":"key"}
    String postResult = post(realApiUrl, content);

    return postResult;

  }


  /**
   * 检测会话记录是否导出完成
   *
   * @param key downloadSession 接口返回的 message 内容
   * @return 下载地址
   */
  private String sessionCheck(String key) throws IOException {

    Map<String, String> bodyMap = Maps.newHashMap();
    bodyMap.put("key", key);

    String content = toJsonString(bodyMap);

    String realApiUrl = fillUrl(ApiConstant.SESSION_CHECK_URL, content);

    /**
     * {"code":200,"message":"httpUrl"}
     *
     * {"code": 非200,"message":"相关描述"}
     *
     * {"code":14403,"message":"Wait..."}
     */

    String postResult = post(realApiUrl, content);

    return null;

  }


  /**
   * 发送 post 请求
   */
  private String post(String url, String jsonString) throws IOException {

    log.info("=== request url === : {}", url);

    OkHttpClient client = new OkHttpClient();
    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
    RequestBody requestBody = FormBody.create(mediaType, jsonString);
    Request request = new Request.Builder()
        .url(url)
        .post(requestBody)
        .build();
    Response execute = client.newCall(request).execute();

    String resultBody = execute.body().string();

    log.info("=== response === : {}", resultBody);

    if (execute.isSuccessful()) {
      return resultBody;
    } else {
      throw new RuntimeException("请求失败，状态码：" + execute.code());
    }
  }

  /**
   * obj to json str
   */
  private String toJsonString(Object obj) {
    return new GsonBuilder().create().toJson(obj);
  }

  /**
   * 填充 url
   */
  private String fillUrl(String url, String requestBody) {
    String time = String.valueOf(System.currentTimeMillis() / 1000);
    String checksum = getChecksum(requestBody, time);
    return StrUtil.format(url, QiyuConfig.getAppKey(), time, checksum);
  }

  /**
   * 获取签名值 checksum
   */
  private String getChecksum(String body, String time) {
    String bodyMd5 = DigestUtil.md5Hex(body);
    return QiyuPushCheckSum.encode(QiyuConfig.getAppSecret(), bodyMd5, time);
  }

  /**
   * 行数据处理
   */
  private Map<String, String> createRowMap(Map<String, String> map) {
    Map<String, String> row1 = new LinkedHashMap<>();
    row1.put("id", map.get("id"));
    row1.put("sessionId", map.get("sessionId"));
    row1.put("staffId", map.get("staffId"));
    row1.put("staffName", map.get("staffName"));
    row1.put("userId", map.get("userId"));
    row1.put("userName", map.get("userName"));
    row1.put("from", map.get("from"));
    row1.put("time", map.get("time"));
    row1.put("mType", map.get("mType"));
    row1.put("autoReply", map.get("autoReply"));
    row1.put("msg", map.get("msg"));
    row1.put("status", map.get("status"));
    row1.put("isRichMedia", map.get("isRichMedia"));
    return row1;
  }

  /**
   * 写出 excel
   */
  private void writerToExcel(String pathname, ArrayList<Map<String, String>> list) {
    // 通过工具类创建writer
    ExcelWriter writer = ExcelUtil.getWriter(pathname);
    //  一次性写出内容，强制输出标题
    writer.write(list, true);
    // 关闭writer，释放内存
    writer.close();
  }

  /**
   * 行数据转换为 json 数据
   */
  private Map<String, String> toJsonMap(String rowStr) {
    try {
      Map<String, String> map = new GsonBuilder()
          .setLongSerializationPolicy(LongSerializationPolicy.STRING).create()
          .fromJson(rowStr, new TypeToken<Map<String, String>>() {
          }.getType());
      return map;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 转换 excel
   */
  private void convertToExcel() {

    String messageFilePath = "/Users/xianyu/Desktop/message.txt";
    String excelFilePath = "/Users/xianyu/Desktop/message_new_" + DateUtil.format(DateUtil.date(),
        DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";

    List<String> fileTxtRowList = FileUtil.readLines(new File(messageFilePath), "utf8");

    ArrayList<Map<String, String>> rows = CollUtil.newArrayList();
    for (String str : fileTxtRowList) {
      Map<String, String> map = toJsonMap(str);
      if (map == null) {
        continue;
      }
      Map<String, String> rowMap = createRowMap(map);
      rows.add(rowMap);
    }

    writerToExcel(excelFilePath, rows);

  }

}
