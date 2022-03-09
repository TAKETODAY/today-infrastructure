/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.framework.adapter;

import java.io.Serial;

/**
 * Exception thrown when an attempt is made to use an unsupported
 * Advisor or Advice type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 19:14
 * @see org.aopalliance.aop.Advice
 * @see cn.taketoday.aop.Advisor
 */
public class UnknownAdviceTypeException extends IllegalArgumentException {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new UnknownAdviceTypeException for the given advice object.
   * Will create a message text that says that the object is neither a
   * subinterface of Advice nor an Advisor.
   *
   * @param advice the advice object of unknown type
   */
  public UnknownAdviceTypeException(Object advice) {
    super("Advice object [" + advice + "] is neither a supported sub-interface of " +
            "[org.aopalliance.aop.Advice] nor an [cn.taketoday.aop.Advisor]");
  }

  /**
   * Create a new UnknownAdviceTypeException with the given message.
   *
   * @param message the message text
   */
  public UnknownAdviceTypeException(String message) {
    super(message);
  }

}
