/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package test.jdbc.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-03-27 10:15
 */
@Setter
@Getter
@SuppressWarnings("serial")
public class Post implements Serializable {

  /** release time stamp */
  private long id;
  private String url;
  private String image;
  private String title;

  private int pv;

  private boolean keepNavigation;

  //    @Transient
  @JSONField(serialize = false)
  private PostStatus status;

  private String summary;
  /** html content **/
  private String content;

  /** markdown Content */
  private String markdown;

  private long lastModify;

  /** 需要输入密码才能访问该页面 */
  private String password;

  public boolean isKeepNavigation() {
    return keepNavigation;
  }

  public boolean needPassword() {
    return password != null;
  }

  @Override
  public String toString() {
    return String.format(
            "Post [id=%s, url=%s, image=%s, title=%s, pv=%s, keepNavigation=%s, status=%s, summary=%s, content=%s, markdown=%s, lastModify=%s, password=%s]",
            id, url, image, title, pv, keepNavigation, status, summary, content, markdown, lastModify, password);
  }

}
