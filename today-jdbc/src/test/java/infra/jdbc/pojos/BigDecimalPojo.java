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

package infra.jdbc.pojos;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA. User: lars Date: 11/15/11 Time: 10:18 AM To change
 * this template use File | Settings | File Templates.
 */
public class BigDecimalPojo {
  public int id;

  public BigDecimal val1;

  public BigDecimal val2;

  public void setId(int id) {
    this.id = id;
  }

  public void setVal1(BigDecimal val1) {
    this.val1 = val1;
  }

  public void setVal2(BigDecimal val2) {
    this.val2 = val2;
  }

  public int getId() {
    return id;
  }

  public BigDecimal getVal1() {
    return val1;
  }

  public BigDecimal getVal2() {
    return val2;
  }
}
