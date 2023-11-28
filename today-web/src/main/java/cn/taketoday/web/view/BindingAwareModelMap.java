/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.ui.Model;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.validation.BindingResult;

/**
 * Subclass of {@link ModelMap} that automatically removes a {@link BindingResult}
 * object if the corresponding target attribute gets replaced through regular
 * {@link Map} operations.
 *
 * <p>This is the class exposed to handler methods by Web MVC, typically consumed
 * through a declaration of the {@link Model} interface. There is no need to
 * build it within user code; a plain {@link ModelMap} or even a just a regular
 * {@link Map} with String keys will be good enough to return a user model.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BindingResult
 * @since 4.0 2022/4/28 9:04
 */
public class BindingAwareModelMap extends ModelMap {

  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  public Object put(String key, @Nullable Object value) {
    removeBindingResultIfNecessary(key, value);
    return super.put(key, value);
  }

  @Override
  public void putAll(Map<? extends String, ?> map) {
    for (Map.Entry<? extends String, ?> entry : map.entrySet()) {
      removeBindingResultIfNecessary(entry.getKey(), entry.getValue());
    }
    super.putAll(map);
  }

  private void removeBindingResultIfNecessary(Object key, @Nullable Object value) {
    if (key instanceof String attributeName) {
      if (!attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
        String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + attributeName;
        if (get(bindingResultKey) instanceof BindingResult bindingResult) {
          if (bindingResult.getTarget() != value) {
            remove(bindingResultKey);
          }
        }
      }
    }
  }

}
