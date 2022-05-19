package org.qiyu.domain;

import java.io.Serializable;
import lombok.Data;

/**
 * @author xianyu
 * @create 5/19/22 10:57 AM
 */
@Data
public class BaseResult implements Serializable {

  private Long code;

  private String message;

}
