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

package cn.taketoday.context.properties.sample.immutable;

import java.time.Duration;
import java.util.List;

import cn.taketoday.context.properties.sample.DefaultValue;

/**
 * Simple immutable properties with collections types and defaults.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutableCollectionProperties {

  private final List<String> names;

  private final List<Boolean> flags;

  private final List<Duration> durations;

  public ImmutableCollectionProperties(List<String> names, @DefaultValue({ "true", "false" }) List<Boolean> flags,
          @DefaultValue({ "10s", "1m", "1h" }) List<Duration> durations) {
    this.names = names;
    this.flags = flags;
    this.durations = durations;
  }

}
