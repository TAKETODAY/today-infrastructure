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

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA. User: lars Date: 5/21/11 Time: 10:15 PM To change
 * this template use File | Settings | File Templates.
 */
public class Entity {

  public long id;
  public String text;
  public Date time;
  public Timestamp ts;
  public Integer aNumber;
  public Long aLongNumber;

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public Timestamp getTs() {
    return ts;
  }

  public void setTs(Timestamp ts) {
    this.ts = ts;
  }

  public Integer getaNumber() {
    return aNumber;
  }

  public void setaNumber(Integer aNumber) {
    this.aNumber = aNumber;
  }
}
