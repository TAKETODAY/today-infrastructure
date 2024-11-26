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

package infra.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that retrieves the next value
 * of a given PostgreSQL sequence.
 *
 * <p>Thanks to Tomislav Urban for the suggestion!
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class PostgresSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

  /**
   * Default constructor for bean property style usage.
   *
   * @see #setDataSource
   * @see #setIncrementerName
   */
  public PostgresSequenceMaxValueIncrementer() {
  }

  /**
   * Convenience constructor.
   *
   * @param dataSource the DataSource to use
   * @param incrementerName the name of the sequence/table to use
   */
  public PostgresSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
    super(dataSource, incrementerName);
  }

  @Override
  protected String getSequenceQuery() {
    return "select nextval('" + getIncrementerName() + "')";
  }

}
