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

package infra.samples;

import java.io.PrintWriter;

import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.RestControllerAdvice;
import infra.web.async.AsyncRequestTimeoutException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 20:59
 */
@RestControllerAdvice
public class ApplicationControllerAdvice {

  @ExceptionHandler(Throwable.class)
  public void throwable(Throwable throwable, PrintWriter writer) {
    throwable.printStackTrace(writer);
  }

  @ExceptionHandler(AsyncRequestTimeoutException.class)
  public String asyncTimeout(AsyncRequestTimeoutException timeoutException) {
    return "任务超时";
  }

}
