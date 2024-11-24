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

package infra.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;

import infra.jdbc.core.RowMapper;
import infra.jdbc.core.namedparam.Customer;

public class CustomerMapper implements RowMapper<Customer> {

  private static final String[] COLUMN_NAMES = new String[] { "id", "forename" };

  @Override
  public Customer mapRow(ResultSet rs, int rownum) throws SQLException {
    Customer cust = new Customer();
    cust.setId(rs.getInt(COLUMN_NAMES[0]));
    cust.setForename(rs.getString(COLUMN_NAMES[1]));
    return cust;
  }

}
