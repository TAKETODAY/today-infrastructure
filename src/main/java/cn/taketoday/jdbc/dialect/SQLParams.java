/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.dialect;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * @author TODAY 2021/10/10 13:16
 * @since 4.0
 */
@Data
public class SQLParams {

  private Class<?> modelClass;

  private Object model;

  private String selectColumns;
  private String tableName;
  private String pkName;
  private StringBuilder conditionSQL;

  private Map<String, Object> updateColumns;

  private List<String> excludedColumns;

  private PageRow pageRow;
  private String orderBy;
  private boolean isSQLLimit;

  private String customSQL;

}
