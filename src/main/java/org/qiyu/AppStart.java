package org.qiyu;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AppStart {

  public static void main(String[] args) {

    // 请填写开始时间和结束时间
    String start = "2022-05-01 00:00:00.000";
    String end = "2022-05-31 23:59:59.999";

    down(start, end);
  }

  public static void down(String start, String end) {

    List<String[]> dateRangeList = getDateRangeList(start, end);

    for (int i = 0; i < dateRangeList.size(); i++) {

      try {

        String[] dateRangeArray = dateRangeList.get(i);

        log.info("开始下载 {} ~ {} 数据", dateRangeArray[0], dateRangeArray[1]);

        SessionHistoryService sessionHistoryService = new SessionHistoryService();

        sessionHistoryService.downloadSession(dateRangeArray[0], dateRangeArray[1], i);

        log.info("结束下载 {} ~ {} 数据", dateRangeArray[0], dateRangeArray[1]);

      } catch (Exception e) {
        log.error("下载出错", e);
      }

    }

  }


  /**
   * 日期拆分
   */
  public static List<String[]> getDateRangeList(String start, String end) {

    List<String[]> dateRangeList = Lists.newArrayList();

    DateTime startDate = DateUtil.parse(start, DatePattern.NORM_DATETIME_MS_PATTERN);
    DateTime endDate = DateUtil.parse(end, DatePattern.NORM_DATETIME_MS_PATTERN);

    if (startDate.getTime() >= endDate.getTime()) {
      throw new RuntimeException("时间配置错误");
    }

    Date itemStart = startDate;
    Date itemEnd = DateUtil.endOfDay(DateUtil.offsetDay(itemStart, 2));

    do {

      // 超过结束时间
      if (itemEnd.getTime() > endDate.getTime()) {
        // 使用结束时间
        itemEnd = endDate;
        String[] dateRange = new String[2];
        dateRange[0] = DateUtil.format(itemStart, DatePattern.NORM_DATETIME_MS_PATTERN);
        dateRange[1] = DateUtil.format(itemEnd, DatePattern.NORM_DATETIME_MS_PATTERN);
        dateRangeList.add(dateRange);
        return dateRangeList;
      } else {
        String[] dateRange = new String[2];
        dateRange[0] = DateUtil.format(itemStart, DatePattern.NORM_DATETIME_MS_PATTERN);
        dateRange[1] = DateUtil.format(itemEnd, DatePattern.NORM_DATETIME_MS_PATTERN);
        dateRangeList.add(dateRange);
      }

      itemStart = DateUtil.beginOfDay(DateUtil.offsetDay(itemEnd, 1));
      itemEnd = DateUtil.endOfDay(DateUtil.offsetDay(itemStart, 2));

    } while (true);

  }

}
