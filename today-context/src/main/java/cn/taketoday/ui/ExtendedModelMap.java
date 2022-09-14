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

package cn.taketoday.ui;

import java.util.Collection;
import java.util.Map;

import cn.taketoday.lang.Nullable;

/**
 * Subclass of {@link ModelMap} that implements the {@link Model} interface.
 *
 * <p>This is an implementation class exposed to handler methods by Spring MVC, typically via
 * a declaration of the {@link cn.taketoday.ui.Model} interface. There is no need to
 * build it within user code; a plain {@link ModelMap} or even a just
 * a regular {@link Map} with String keys will be good enough to return a user model.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ExtendedModelMap extends ModelMap implements Model {

  @Override
  public ExtendedModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
    super.addAttribute(attributeName, attributeValue);
    return this;
  }

  @Override
  public ExtendedModelMap addAttribute(Object attributeValue) {
    super.addAttribute(attributeValue);
    return this;
  }

  @Override
  public ExtendedModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
    super.addAllAttributes(attributeValues);
    return this;
  }

  @Override
  public ExtendedModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
    super.addAllAttributes(attributes);
    return this;
  }

  @Override
  public ExtendedModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
    super.mergeAttributes(attributes);
    return this;
  }

  @Override
  public Map<String, Object> asMap() {
    return this;
  }

}
