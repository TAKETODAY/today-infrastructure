/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.view;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.Map;

import infra.ui.Model;
import infra.ui.ModelMap;
import infra.validation.BindingResult;

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
