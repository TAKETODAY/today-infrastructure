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

package cn.taketoday.web.view;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2021/4/1 15:56
 * @since 3.0
 */
public class ModelAttributes extends AttributeAccessorSupport implements Model, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  public boolean containsAttribute(String name) {
    return super.hasAttribute(name);
  }

  @Override
  public Model addAttribute(@Nullable Object attributeValue) {
    if (attributeValue != null) {
      if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
        return this;
      }
      setAttribute(Conventions.getVariableName(attributeValue), attributeValue);
    }
    return this;
  }

  @Override
  public Model addAllAttributes(@Nullable Collection<?> attributeValues) {
    if (attributeValues != null) {
      for (Object attributeValue : attributeValues) {
        addAttribute(attributeValue);
      }
    }
    return this;
  }

  @Override
  public Model addAllAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      getAttributes().putAll(attributes);
    }
    return this;
  }

  @Override
  public Model mergeAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      Map<String, Object> map = getAttributes();
      for (Map.Entry<String, ?> entry : attributes.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (!map.containsKey(key)) {
          map.put(key, value);
        }
      }
    }
    return this;
  }

  @Override
  protected LinkedHashMap<String, Object> createAttributes() {
    return new LinkedHashMap<>();
  }

  @Override
  public Map<String, Object> asMap() {
    return getAttributes();
  }

}
