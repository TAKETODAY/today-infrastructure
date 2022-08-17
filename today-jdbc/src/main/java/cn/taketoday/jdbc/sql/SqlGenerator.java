/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

import java.util.ArrayList;
import java.util.Set;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.jdbc.sql.dialect.Dialect;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 21:10
 */
public class SqlGenerator {
  private final PropertyFilter propertyFilter = PropertyFilter.filteredNames(Set.of("class"));

  private final Dialect dialect;

  private final ColumnNameDiscover columnNameDiscover = ColumnNameDiscover.forColumnAnnotation()
          .and(ColumnNameDiscover.camelCaseToUnderscore());

  public SqlGenerator(Dialect dialect) {
    this.dialect = dialect;
  }

  public String generateInsert(EntityHolder entityHolder) {
    return dialect.insert(entityHolder);
  }

}
