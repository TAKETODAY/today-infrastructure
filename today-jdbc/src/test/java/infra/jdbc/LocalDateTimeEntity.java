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

import java.time.LocalDateTime;

public class LocalDateTimeEntity {

  public int id;

  public LocalDateTime joda1;

  private LocalDateTime joda2;

  public LocalDateTime getJoda2() {
    return joda2;
  }

  public void setJoda2(LocalDateTime joda2) {
    this.joda2 = joda2;
  }
}
