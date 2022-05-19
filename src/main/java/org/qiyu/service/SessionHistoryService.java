package org.qiyu.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.qiyu.config.QiyuConfig;
import org.qiyu.constants.ApiConstant;
import org.qiyu.domain.BaseResult;
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

  public static final int SUCCESS = 200;

  public static final int WAIT = 14403;

  public static final int INTERVAL = 5 * 1000;

  public static final String FILE_DIR_NAME = "5284272";

  /**
   * 下载会话记录
   */
  public void downloadSession(String start, String end) throws IOException {

    BaseResult baseResult = addDownloadSessionTask(formatTime(start), formatTime(end));

    if (baseResult.getCode() != SUCCESS) {
      log.error("添加下载任务失败");
      return;
    }

    BaseResult sessionTaskState = null;
    boolean complete = false;
    while (!complete) {

      sessionTaskState = checkDownloadSessionTaskState(baseResult.getMessage());

      if (sessionTaskState.getCode() == WAIT) {

        log.info("下载中...");

        ThreadUtil.sleep(INTERVAL);

      } else if (sessionTaskState.getCode() == SUCCESS) {

        log.info("session 导出任务完成：{}", sessionTaskState.getMessage());

        complete = true;

      } else {

        log.error("检查任务完成失败：code: {}, message: {}", sessionTaskState.getCode(),
            sessionTaskState.getMessage());

        throw new RuntimeException(sessionTaskState.getMessage());

      }

    }

    String downloadFileUrl = sessionTaskState.getMessage();

    // 下载导出的文件
    String destFile = getDestFile("session_package.zip");
    this.downloadFile(downloadFileUrl, destFile);

    // 解压 zip 文件到 unzip 文件夹
    String unzipPath = extractFile(destFile);

    // 消息文件解压后的地址
    String messagePath =
        unzipPath + File.separator + FILE_DIR_NAME + File.separator + "message.txt";

    String messageDest = getDestFile("message_convert.xlsx");

    // 将 message.txt 转换成 message_convert.xlsx
    convertToExcel(messagePath, messageDest);

  }

  /**
   * 添加下载任务 - 批量导出会话记录
   *
   * @param start 开始时间 时间戳毫秒单位
   * @param end 结束时间 时间戳毫秒单位
   */
  private BaseResult addDownloadSessionTask(long start, long end) throws IOException {
    Map<String, Long> bodyMap = Maps.newHashMap();
    bodyMap.put("start", start);
    bodyMap.put("end", end);
    String content = toJsonString(bodyMap);
    String realApiUrl = fillUrl(ApiConstant.SESSION_URL, content);
    // 正常结果：{"code":200,"message":"key"}
    String postResult = postResult = post(realApiUrl, content);
    return new Gson().fromJson(postResult, BaseResult.class);
  }


  /**
   * 检测会话记录是否导出完成
   *
   * @param key downloadSession 接口返回的 message 内容
   * @return 下载地址
   */
  private BaseResult checkDownloadSessionTaskState(String key) throws IOException {

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

    BaseResult baseResult = new Gson().fromJson(postResult, BaseResult.class);

    return baseResult;

  }

  /**
   * 下载远程文件
   */
  private void downloadFile(String fileUrl, String destUrl) {

    if (FileUtil.exist(destUrl)) {
      FileUtil.del(destUrl);
    }

    HttpUtil.downloadFile(fileUrl, destUrl);

    log.info("session 文件下载完成：{}", destUrl);

  }


  /**
   * 发送 post 请求
   */
  private String post(String url, String jsonString) throws IOException {

    log.info("=== request url === : \n{}", url);

    OkHttpClient client = new OkHttpClient();
    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
    RequestBody requestBody = FormBody.create(mediaType, jsonString);
    Request request = new Request.Builder()
        .url(url)
        .post(requestBody)
        .build();
    Response execute = client.newCall(request).execute();

    String resultBody = execute.body().string();

    log.info("=== response === : \n{}", resultBody);

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
   * 解压压缩包密码
   */
  private String getZipPassword() {
    return StrUtil.sub(QiyuConfig.getAppKey(), 0, 12);
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

    String from = map.get("from");
    if (StrUtil.isNotBlank(from)) {
      from = from.equals("0") ? "客服" : "访客";
    }
    row1.put("from", from);

    String time = map.get("time");
    if (StrUtil.isNotBlank(from)) {
      try {
        time = DateUtil
            .format(DateUtil.date(Long.parseLong(time)), DatePattern.NORM_DATETIME_PATTERN);
      } catch (NumberFormatException e) {
        log.error("时间转换失败", e);
      }
    }
    row1.put("time", time);

    String mType = map.get("mType");
    if (StrUtil.isNotBlank(mType)) {
      mType = MESSAGE_TYPE.get(mType) == null ? mType : MESSAGE_TYPE.get(mType);
    }
    row1.put("mType", mType);

    String autoReply = map.get("autoReply");
    if (StrUtil.isNotBlank(autoReply)) {
      autoReply = autoReply.equals("0") ? "手动" : "自动";
    }
    row1.put("autoReply", autoReply);
    row1.put("msg", map.get("msg"));

    String status = map.get("status");
    if (StrUtil.isNotBlank(status)) {
      status = status.equals("1") ? "正常" : "已撤回";
    }
    row1.put("status", status);

    String isRichMedia = map.get("isRichMedia");
    if (StrUtil.isNotBlank(isRichMedia)) {
      isRichMedia = MEDIA_TYPE.get(isRichMedia) == null ? isRichMedia : MEDIA_TYPE.get(isRichMedia);
    }

    row1.put("isRichMedia", isRichMedia);

    return row1;
  }

  public static final Map<String, String> MEDIA_TYPE = MapUtil
      .builder(new HashMap<String, String>())
      .put("2", "图片消息")
      .put("3", "语音消息")
      .put("4", "文件消息")
      .put("4", "文件消息")
      .put("5", "视频消息")
      .put("115", "富文本消息")
      .put("0", "其他")
      .build();

  public static final Map<String, String> MESSAGE_TYPE = MapUtil
      .builder(new HashMap<String, String>())
      .put("0", "系统消息")
      .put("1", "文本消息")
      .put("2", "图片消息")
      .put("3", "语音消息")
      .put("4", "文件消息")
      .put("5", "视频消息")
      .put("6", "系统提示消息")
      .put("100", "自定义消息")
      .put("110", "机器人答案")
      .put("111", "机器人答案反馈")
      .put("112", "超时关闭前提醒")
      .put("113", "超时关闭提醒")
      .put("114", "工单流程消息")
      .put("115", "富文本消息")
      .put("116", "敏感词屏蔽消息")
      .put("117", "客服拒绝转接")
      .put("118", "客服提交工单消息")
      .put("119", "客服发送邀请评价")
      .put("120", "访客参评")
      .put("121", "主企业客服转接会话到子企业信息")
      .put("122", "客服邀请评价详情")
      .put("123", "emoji消息")
      .put("124", "客服转接到主企业").build();

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
   * 下载目录
   */
  private String getDestFile(String fileName) {
    String property = System.getProperty("user.dir");
    return property + File.separator + "download" + File.separator + fileName;
  }

  /**
   * yyyy-MM-dd HH:mm:ss
   */
  private long formatTime(String time) {
    return DateUtil.parse(time, DatePattern.NORM_DATETIME_PATTERN).getTime();
  }

  /**
   * 提取 zip 文件
   */
  private String extractFile(String filepath) throws ZipException {
    ZipFile zipFile = new ZipFile(filepath, getZipPassword().toCharArray());
    String unzip = getDestFile("unzip");
    zipFile.extractAll(unzip);
    log.info("session 文件解压完成：{}", unzip);
    return unzip;
  }

  /**
   * 转换 excel
   */
  private void convertToExcel(String messageFilePath, String messageDest) {
    List<String> fileTxtRowList = FileUtil.readLines(new File(messageFilePath), "utf8");
    ArrayList<Map<String, String>> rows = CollUtil.newArrayList();
    for (String str : fileTxtRowList) {
      Map<String, String> map = toJsonMap(str);
      if (map == null) {
        continue;
      }

      // 字段转换
      Map<String, String> rowMap = createRowMap(map);
      if (QiyuConfig.getIncludeEnabled()) {
        // 过滤客服ID
        String staffId = rowMap.get("staffId");
        List<String> staffIdInclude = QiyuConfig.getStaffIdInclude();
        if (staffIdInclude.contains(staffId)) {
          rows.add(rowMap);
        }
      } else {
        rows.add(rowMap);
      }
    }
    writerToExcel(messageDest, rows);

    log.info("excel 转换完成：{}", messageDest);

  }

//  public static void main(String[] args) throws ZipException {
//    ZipFile zipFile = new ZipFile("/Users/xianyu/mycp-workspace/qiyu/download/session_package.zip", "c85d39fa1f83".toCharArray());
//    zipFile.extractAll( "/Users/xianyu/mycp-workspace/qiyu/download/unzip");
//    log.info("文件解压完成：{}", "");
//  }

  public static void main(String[] args) {
    FileUtil
        .readLines(new File("/Users/xianyu/mycp-workspace/qiyu/download/unzip/5284272/message.txt"),
            "utf8");
  }

}
