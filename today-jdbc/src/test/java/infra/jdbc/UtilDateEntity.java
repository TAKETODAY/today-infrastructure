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

package infra.jdbc;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: lars Date: 8/30/11 Time: 10:59 AM To change
 * this template use File | Settings | File Templates.
 */
public class UtilDateEntity {

  public int id;
  public Date d1;
  private Date d2;
  private Date d3;

  public Date getD2() {
    return d2;
  }

  public void setD2(Date d2) {
    this.d2 = d2;
  }

  public Date getD3() {
    return d3;
  }

  public void setD3(Date d3) {
    this.d3 = d3;
  }
}
