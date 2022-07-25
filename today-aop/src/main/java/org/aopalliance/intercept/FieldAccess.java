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

package org.aopalliance.intercept;

import java.lang.reflect.Field;

/**
 * This interface represents a field access in the program.
 *
 * <p>
 * A field access is a joinpoint and can be intercepted by a field interceptor.
 *
 * @see FieldInterceptor
 */
public interface FieldAccess extends Joinpoint {

  /** The read access type (see {@link #getAccessType()}). */
  int READ = 0;
  /** The write access type (see {@link #getAccessType()}). */
  int WRITE = 1;

  /**
   * Gets the field being accessed.
   *
   * <p>
   * This method is a frienly implementation of the
   * {@link Joinpoint#getStaticPart()} method (same result).
   *
   * @return the field being accessed.
   */
  Field getField();

  /**
   * Gets the value that must be set to the field.
   *
   * <p>
   * This value can be intercepted and changed by a field interceptor.
   */
  Object getValueToSet();

  /**
   * Returns the access type.
   *
   * @return FieldAccess.READ || FieldAccess.WRITE
   */
  int getAccessType();

}
