/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.persistence;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * default {@link TableNameGenerator}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:09
 */
public class DefaultTableNameGenerator implements TableNameGenerator {

  private final TableNameGenerator annotationGenerator = TableNameGenerator.forTableAnnotation();

  @Nullable
  private String prefixToAppend;

  @Nullable
  private String[] suffixArrayToRemove;

  private boolean lowercase = true;

  private boolean camelCaseToUnderscore = true;

  public void setLowercase(boolean lowercase) {
    this.lowercase = lowercase;
  }

  public void setCamelCaseToUnderscore(boolean camelCaseToUnderscore) {
    this.camelCaseToUnderscore = camelCaseToUnderscore;
  }

  public void setPrefixToAppend(@Nullable String prefixToAppend) {
    this.prefixToAppend = prefixToAppend;
  }

  public void setSuffixToRemove(@Nullable String suffixArrayToRemove) {
    if (suffixArrayToRemove == null) {
      this.suffixArrayToRemove = null;
    }
    else {
      this.suffixArrayToRemove = new String[] { suffixArrayToRemove };
    }
  }

  public void setSuffixArrayToRemove(@Nullable String... suffixToRemove) {
    this.suffixArrayToRemove = suffixToRemove;
  }

  @Override
  public String generateTableName(Class<?> entityClass) {
    String name = annotationGenerator.generateTableName(entityClass);
    if (name != null) {
      return name;
    }

    String simpleName = entityClass.getSimpleName();

    // append the prefix like "t_" -> t_user, t_order
    StringBuilder tableName = new StringBuilder();
    if (StringUtils.hasText(prefixToAppend)) {
      tableName.append(prefixToAppend);
    }

    // remove the common suffix like UserModel - Model = User, UserEntity - Entity = User
    if (ObjectUtils.isNotEmpty(suffixArrayToRemove)) {
      for (String suffix : suffixArrayToRemove) {
        if (simpleName.endsWith(suffix)) {
          simpleName = simpleName.substring(0, simpleName.length() - suffix.length());
          break;
        }
      }
    }

    // UserOrder -> user_order
    if (camelCaseToUnderscore) {
      simpleName = StringUtils.camelCaseToUnderscore(simpleName);
    }
    else if (lowercase) {
      // User -> user
      simpleName = simpleName.toLowerCase();
    }

    tableName.append(simpleName);
    return tableName.toString();
  }
}
