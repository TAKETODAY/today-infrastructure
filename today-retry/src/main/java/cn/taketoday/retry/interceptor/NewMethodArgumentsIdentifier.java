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

package cn.taketoday.retry.interceptor;

/**
 * Strategy interface to distinguish new arguments from ones that have been processed
 * before, e.g. by examining a message flag.
 *
 * @author Dave Syer
 */
public interface NewMethodArgumentsIdentifier {

  /**
   * Inspect the arguments and determine if they have never been processed before. The
   * safest choice when the answer is indeterminate is 'false'.
   *
   * @param args the current method arguments.
   * @return true if the item is known to have never been processed before.
   */
  boolean isNew(Object[] args);

}
