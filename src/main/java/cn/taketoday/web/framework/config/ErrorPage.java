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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.framework.config;

import java.util.Objects;

import lombok.Getter;

/**
 * @author TODAY <br>
 * 2019-02-06 14:49
 */
@Getter
public class ErrorPage {

  private final int status;
  private final String path;
  private final Class<? extends Throwable> exception;

  public ErrorPage(String path) {
    this(500, path, null);
  }

  public ErrorPage(int status, String path) {
    this(status, path, null);
  }

  public ErrorPage(Class<? extends Throwable> exception, String path) {
    this(500, path, exception);
  }

  public ErrorPage(int status, String path, Class<? extends Throwable> exception) {
    this.path = path;
    this.status = status;
    this.exception = exception;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final ErrorPage errorPage))
      return false;
    return status == errorPage.status && Objects.equals(path, errorPage.path) && Objects
            .equals(exception, errorPage.exception);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, path, exception);
  }
}
