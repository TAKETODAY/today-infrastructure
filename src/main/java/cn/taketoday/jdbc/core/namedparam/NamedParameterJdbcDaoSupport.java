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

package cn.taketoday.jdbc.core.namedparam;

import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.core.support.JdbcDaoSupport;
import cn.taketoday.lang.Nullable;

/**
 * Extension of JdbcDaoSupport that exposes a NamedParameterJdbcTemplate as well.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see NamedParameterJdbcTemplate
 * @since 2.0
 */
public class NamedParameterJdbcDaoSupport extends JdbcDaoSupport {

  @Nullable
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  /**
   * Create a NamedParameterJdbcTemplate based on the configured JdbcTemplate.
   */
  @Override
  protected void initTemplateConfig() {
    JdbcTemplate jdbcTemplate = getJdbcTemplate();
    if (jdbcTemplate != null) {
      this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }
  }

  /**
   * Return a NamedParameterJdbcTemplate wrapping the configured JdbcTemplate.
   */
  @Nullable
  public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
    return this.namedParameterJdbcTemplate;
  }

}
