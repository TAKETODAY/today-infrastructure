/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.persistence;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/19 13:02
 */
public class MatchableQueryCondition extends DefaultQueryCondition {
  private final ConditionMatcher conditionMatcher;

  public MatchableQueryCondition(
          String columnName, Operator operator,
          @Nullable Object parameterValue, ConditionMatcher conditionMatcher) {
    super(columnName, operator, parameterValue);
    this.conditionMatcher = conditionMatcher;
  }

  @Override
  protected boolean matches() {
    return conditionMatcher.matches(parameterValue);
  }

}
