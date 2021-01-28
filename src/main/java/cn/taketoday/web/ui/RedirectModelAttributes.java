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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.ui;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.utils.ConvertUtils;

/**
 * @author TODAY <br>
 *         2018-12-10 16:31
 * @since 2.3.3
 */
@SuppressWarnings("serial")
public class RedirectModelAttributes extends HashMap<String, Object> implements RedirectModel {

  public RedirectModelAttributes() {
    super(8, 1.0f);
  }

  @Override
  public Object attribute(String name) {
    return get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T attribute(String name, Class<T> targetClass) {
    return (T) ConvertUtils.convert(get(name), targetClass);
  }

  @Override
  public RedirectModel attribute(String attributeName, Object attributeValue) {
    put(attributeName, attributeValue);
    return this;
  }

  @Override
  public Map<String, Object> asMap() {
    return this;
  }

  @Override
  public Model removeAttribute(String name) {
    remove(name);
    return this;
  }

  @Override
  public RedirectModel attributes(Map<String, Object> attributes) {
    putAll(attributes);
    return this;
  }

  @Override
  public Enumeration<String> attributes() {
    return Collections.enumeration(keySet());
  }

}
