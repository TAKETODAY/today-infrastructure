/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.core.namedparam;

import org.jspecify.annotations.Nullable;

import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.support.JdbcDataAccessObjectSupport;

/**
 * Extension of JdbcDaoSupport that exposes a NamedParameterJdbcTemplate as well.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see NamedParameterJdbcTemplate
 * @since 4.0
 */
public class NamedParameterJdbcDaoSupport extends JdbcDataAccessObjectSupport {

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
