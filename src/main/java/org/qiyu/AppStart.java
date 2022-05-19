package org.qiyu;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import java.io.IOException;
import org.qiyu.service.SessionHistoryService;

/**
 * 1、填写文件 qiyu.properties
 *
 * 2、填写需要导出会话记录的起始和结束时间参数 start 和 end
 *
 * 3、执行 AppStart.main 方法
 *
 * 4、download 文件夹获取文件
 *
 * @author xianyu
 * @create 5/18/22 6:37 PM
 */
public class AppStart {

  public static void main(String[] args) throws IOException {

    /**
     * 限制时间跨度不能大于两天
     */

    // 请填写开始时间
    String start = "2022-05-01 00:00:00";
    // 请填写结束时间
    String end = "2022-05-03 00:00:00";

    // 下载客户端
    SessionHistoryService sessionHistoryService = new SessionHistoryService();
    sessionHistoryService.downloadSession(formatTime(start), formatTime(end));

  }

  /**
   * yyyy-MM-dd HH:mm:ss
   */
  public static long formatTime(String time) {
    return DateUtil.parse(time, DatePattern.NORM_DATETIME_PATTERN).getTime();
  }

}
