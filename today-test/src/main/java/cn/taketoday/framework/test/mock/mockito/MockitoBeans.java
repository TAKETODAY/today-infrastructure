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

package cn.taketoday.framework.test.mock.mockito;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Beans created using
 *
 * @author Andy Wilkinson
 */
class MockitoBeans implements Iterable<Object> {

  private final List<Object> beans = new ArrayList<>();

  void add(Object bean) {
    this.beans.add(bean);
  }

  @Override
  public Iterator<Object> iterator() {
    return this.beans.iterator();
  }

}
