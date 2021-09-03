/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.view;

/**
 * RuntimeResultHandler
 * <p>
 *
 * Handle runtime
 *
 * @author TODAY <br>
 * 2019-12-25 23:09
 */
public interface RuntimeReturnValueHandler extends ReturnValueHandler {

  /**
   * If this {@link ReturnValueHandler} supports the target handler's result
   * <p>
   * This method can test this {@link ReturnValueHandler} supports the target handler
   * in application runtime
   *
   * @param returnValue
   *         Target handler's return-value or result
   *
   * @return If this {@link ReturnValueHandler} supports the target handler's result
   */
  boolean supportsReturnValue(Object returnValue);
}
