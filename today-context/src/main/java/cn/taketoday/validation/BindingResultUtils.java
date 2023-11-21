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

package cn.taketoday.validation;

import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Convenience methods for looking up BindingResults in a model Map.
 *
 * @author Juergen Hoeller
 * @see BindingResult#MODEL_KEY_PREFIX
 * @since 4.0
 */
public abstract class BindingResultUtils {

  /**
   * Find the BindingResult for the given name in the given model.
   *
   * @param model the model to search
   * @param name the name of the target object to find a BindingResult for
   * @return the BindingResult, or {@code null} if none found
   * @throws IllegalStateException if the attribute found is not of type BindingResult
   */
  @Nullable
  public static BindingResult getBindingResult(Map<?, ?> model, String name) {
    Assert.notNull(model, "Model map is required");
    Assert.notNull(name, "Name is required");
    Object attr = model.get(BindingResult.MODEL_KEY_PREFIX + name);
    if (attr != null && !(attr instanceof BindingResult)) {
      throw new IllegalStateException("BindingResult attribute is not of type BindingResult: " + attr);
    }
    return (BindingResult) attr;
  }

  /**
   * Find a required BindingResult for the given name in the given model.
   *
   * @param model the model to search
   * @param name the name of the target object to find a BindingResult for
   * @return the BindingResult (never {@code null})
   * @throws IllegalStateException if no BindingResult found
   */
  public static BindingResult getRequiredBindingResult(Map<?, ?> model, String name) {
    BindingResult bindingResult = getBindingResult(model, name);
    if (bindingResult == null) {
      throw new IllegalStateException("No BindingResult attribute found for name '" + name +
              "'- have you exposed the correct model?");
    }
    return bindingResult;
  }

}
