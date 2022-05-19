package org.qiyu.constants;

/**
 * @author xianyu
 * @create 5/18/22 6:39 PM
 */
public interface ApiConstant {

  /**
   * 添加任务 - 该接口用于告诉七鱼,企业将开始一个导出数据的任务。 参数： start和end一同决定了要导出的数据的时间范围，其最大时间间隔不能超过2天
   */
  String SESSION_URL =
      "https://qiyukf.com/openapi/export/session?appKey={}&time={}&checksum={}";

  /**
   * 校验任务是否完成
   */
  String SESSION_CHECK_URL =
      "https://qiyukf.com/openapi/export/session/check?appKey={}&time={}&checksum={}";

  /**
   * 根据会话ID获取会话字段
   */
  String SESSION_ONE_URL =
      "https://qiyukf.com/openapi/export/session/one?appKey={}&time={}&checksum={}";

  /**
   * 根据会话ID获取会话消息
   */
  String SESSION_ONE_MESSGE_URL =
      "https://qiyukf.com/openapi/export/session/one/message?appKey={}&time={}&checksum={}";

}
