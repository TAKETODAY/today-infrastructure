/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Hints of an item to provide the list of values and/or the name of the provider
 * responsible to identify suitable values. If the type of the related item is a
 * {@link java.util.Map} it can have both key and value hints.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Hints {

  private final List<ValueHint> keyHints = new ArrayList<>();

  private final List<ValueProvider> keyProviders = new ArrayList<>();

  private final List<ValueHint> valueHints = new ArrayList<>();

  private final List<ValueProvider> valueProviders = new ArrayList<>();

  /**
   * The list of well-defined keys, if any. Only applicable if the type of the related
   * item is a {@link java.util.Map}. If no extra {@link ValueProvider provider} is
   * specified, these values are to be considered a closed-set of the available keys for
   * the map.
   *
   * @return the key hints
   */
  public List<ValueHint> getKeyHints() {
    return this.keyHints;
  }

  /**
   * The value providers that are applicable to the keys of this item. Only applicable
   * if the type of the related item is a {@link java.util.Map}. Only one
   * {@link ValueProvider} is enabled for a key: the first in the list that is supported
   * should be used.
   *
   * @return the key providers
   */
  public List<ValueProvider> getKeyProviders() {
    return this.keyProviders;
  }

  /**
   * The list of well-defined values, if any. If no extra {@link ValueProvider provider}
   * is specified, these values are to be considered a closed-set of the available
   * values for this item.
   *
   * @return the value hints
   */
  public List<ValueHint> getValueHints() {
    return this.valueHints;
  }

  /**
   * The value providers that are applicable to this item. Only one
   * {@link ValueProvider} is enabled for an item: the first in the list that is
   * supported should be used.
   *
   * @return the value providers
   */
  public List<ValueProvider> getValueProviders() {
    return this.valueProviders;
  }

}
