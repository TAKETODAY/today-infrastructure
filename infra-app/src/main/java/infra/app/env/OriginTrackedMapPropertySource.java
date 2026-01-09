/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.env;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.core.env.MapPropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.origin.OriginTrackedValue;

/**
 * {@link OriginLookup} backed by a {@link Map} containing {@link OriginTrackedValue
 * OriginTrackedValues}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see OriginTrackedValue
 * @since 4.0
 */
public final class OriginTrackedMapPropertySource extends MapPropertySource implements OriginLookup<String> {

  private final boolean immutable;

  /**
   * Create a new {@link OriginTrackedMapPropertySource} instance.
   *
   * @param name the property source name
   * @param source the underlying map source
   */
  @SuppressWarnings("rawtypes")
  public OriginTrackedMapPropertySource(String name, Map source) {
    this(name, source, false);
  }

  /**
   * Create a new {@link OriginTrackedMapPropertySource} instance.
   *
   * @param name the property source name
   * @param source the underlying map source
   * @param immutable if the underlying source is immutable and guaranteed not to change
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public OriginTrackedMapPropertySource(String name, Map source, boolean immutable) {
    super(name, source);
    this.immutable = immutable;
  }

  @Nullable
  @Override
  public Object getProperty(String name) {
    Object value = super.getProperty(name);
    if (value instanceof OriginTrackedValue) {
      return ((OriginTrackedValue) value).getValue();
    }
    return value;
  }

  @Nullable
  @Override
  public Origin getOrigin(String name) {
    Object value = super.getProperty(name);
    if (value instanceof OriginTrackedValue) {
      return ((OriginTrackedValue) value).getOrigin();
    }
    return null;
  }

  @Override
  public boolean isImmutable() {
    return this.immutable;
  }

}
