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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.transaction.interceptor;

/**
 * Tag subclass of {@link RollbackRuleAttribute} that has the opposite behavior
 * to the {@code RollbackRuleAttribute} superclass.
 *
 * @author Rod Johnson
 * @since 4.0
 */
@SuppressWarnings("serial")
public class NoRollbackRuleAttribute extends RollbackRuleAttribute {

  /**
   * Create a new instance of the {@code NoRollbackRuleAttribute} class
   * for the supplied {@link Throwable} class.
   *
   * @param clazz the {@code Throwable} class
   * @see RollbackRuleAttribute#RollbackRuleAttribute(Class)
   */
  public NoRollbackRuleAttribute(Class<?> clazz) {
    super(clazz);
  }

  /**
   * Create a new instance of the {@code NoRollbackRuleAttribute} class
   * for the supplied {@code exceptionName}.
   *
   * @param exceptionName the exception name pattern
   * @see RollbackRuleAttribute#RollbackRuleAttribute(String)
   */
  public NoRollbackRuleAttribute(String exceptionName) {
    super(exceptionName);
  }

  @Override
  public String toString() {
    return "No" + super.toString();
  }

}
