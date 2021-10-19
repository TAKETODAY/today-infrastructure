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
package cn.taketoday.core.io;

import java.io.IOException;

/**
 * @author TODAY <br>
 * 2019-05-25 22:21
 */
@FunctionalInterface
public interface ResourceFilter {

  /**
   * Tests whether or not the specified abstract {@link Resource} name should be
   * included in a name list.
   *
   * @param resource The Resource to be tested
   * @return <code>true</code> if and only if <code>pathname</code> should be
   * included
   */
  boolean accept(Resource resource) throws IOException;
}
