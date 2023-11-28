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

package cn.taketoday.context.properties.source;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.context.testfixture.origin.MockOrigin;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.OriginTrackedValue;

/**
 * Mock {@link ConfigurationPropertySource} implementation used for testing.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class MockConfigurationPropertySource implements IterableConfigurationPropertySource {

  private final Map<ConfigurationPropertyName, OriginTrackedValue> map = new LinkedHashMap<>();

  public MockConfigurationPropertySource() {
  }

  public MockConfigurationPropertySource(String configurationPropertyName, Object value) {
    this(configurationPropertyName, value, null);
  }

  public MockConfigurationPropertySource(String configurationPropertyName, Object value, String origin) {
    put(ConfigurationPropertyName.of(configurationPropertyName),
            OriginTrackedValue.of(value, MockOrigin.of(origin)));
  }

  public MockConfigurationPropertySource(Map<String, String> configs) {
    configs.forEach(this::put);
  }

  public void put(String name, String value) {
    put(ConfigurationPropertyName.of(name), value);
  }

  public void put(ConfigurationPropertyName name, String value) {
    put(name, OriginTrackedValue.of(value));
  }

  private void put(ConfigurationPropertyName name, OriginTrackedValue value) {
    this.map.put(name, value);
  }

  public ConfigurationPropertySource nonIterable() {
    return new NonIterable();
  }

  @Override
  public Iterator<ConfigurationPropertyName> iterator() {
    return this.map.keySet().iterator();
  }

  @Override
  public Stream<ConfigurationPropertyName> stream() {
    return this.map.keySet().stream();
  }

  @Override
  public Object getUnderlyingSource() {
    return this.map;
  }

  @Override
  public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
    OriginTrackedValue result = this.map.get(name);
    if (result == null) {
      result = findValue(name);
    }
    return of(name, result);
  }

  @Nullable
  static ConfigurationProperty of(ConfigurationPropertyName name, @Nullable OriginTrackedValue value) {
    if (value == null) {
      return null;
    }
    return new ConfigurationProperty(name, value.getValue(), value.getOrigin());
  }

  private OriginTrackedValue findValue(ConfigurationPropertyName name) {
    return this.map.get(name);
  }

  private class NonIterable implements ConfigurationPropertySource {

    @Override
    public Object getUnderlyingSource() {
      return MockConfigurationPropertySource.this.map;
    }

    @Override
    public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
      return MockConfigurationPropertySource.this.getConfigurationProperty(name);
    }

  }

}
