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

package cn.taketoday.web.servlet;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.view.Model;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/5 23:55
 */
public abstract class AbstractEnumerableModel implements Model {

  @Override
  public Model addAllAttributes(@Nullable Collection<?> attributeValues) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Model addAllAttributes(@Nullable Map<String, ?> attributes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Model addAttribute(@Nullable Object attributeValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Model addAllAttributes(@Nullable Model attributes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Model mergeAttributes(@Nullable Map<String, ?> attributes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    return getAttributeNames().length == 0;
  }

  @Override
  public Map<String, Object> asMap() {
    LinkedHashMap<String, Object> ret = new LinkedHashMap<>();
    Enumeration<String> attributeNames = getAttributes();
    while (attributeNames.hasMoreElements()) {
      String name = attributeNames.nextElement();
      ret.put(name, getAttribute(name));
    }
    return ret;
  }

  protected abstract Enumeration<String> getAttributes();

  @Override
  public void clear() {
    Enumeration<String> attributeNames = getAttributes();
    while (attributeNames.hasMoreElements()) {
      String name = attributeNames.nextElement();
      removeAttribute(name);
    }
  }

  @Override
  public String[] getAttributeNames() {
    return CollectionUtils.toArray(getAttributes(), Constant.EMPTY_STRING_ARRAY);
  }

  @Override
  public Iterator<String> attributeNames() {
    return getAttributes().asIterator();
  }

}
