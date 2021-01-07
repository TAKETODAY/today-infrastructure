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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.jdbc.model;

import cn.taketoday.jdbc.Constant;
import cn.taketoday.jdbc.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @Time 2017 10 11--18:07 <br>
 *       Single Article Class
 */
@Setter
@Getter
@Table("t_article")
@SuppressWarnings("serial")
public class Article extends Post {

  /** category name */
  private String category;
  private String copyRight;

  @Override
  public String getUrl() {
    return getId() + Constant.BLANK;
  }

}
