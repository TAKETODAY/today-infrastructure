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

package cn.taketoday.validation;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for building message codes from validation error codes.
 * Used by DataBinder to build the codes list for ObjectErrors and FieldErrors.
 *
 * <p>The resulting message codes correspond to the codes of a
 * MessageSourceResolvable (as implemented by ObjectError and FieldError).
 *
 * @author Juergen Hoeller
 * @see DataBinder#setMessageCodesResolver
 * @see ObjectError
 * @see FieldError
 * @see cn.taketoday.context.MessageSourceResolvable#getCodes()
 * @since 4.0
 */
public interface MessageCodesResolver {

  /**
   * Build message codes for the given error code and object name.
   * Used for building the codes list of an ObjectError.
   *
   * @param errorCode the error code used for rejecting the object
   * @param objectName the name of the object
   * @return the message codes to use
   */
  String[] resolveMessageCodes(String errorCode, String objectName);

  /**
   * Build message codes for the given error code and field specification.
   * Used for building the codes list of an FieldError.
   *
   * @param errorCode the error code used for rejecting the value
   * @param objectName the name of the object
   * @param field the field name
   * @param fieldType the field type (may be {@code null} if not determinable)
   * @return the message codes to use
   */
  String[] resolveMessageCodes(String errorCode, String objectName, String field, @Nullable Class<?> fieldType);

}
