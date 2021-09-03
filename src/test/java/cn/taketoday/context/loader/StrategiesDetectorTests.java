/*
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.loader;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import cn.taketoday.beans.PropertyValueException;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.core.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/7/17 22:17
 */
public class StrategiesDetectorTests {

  @Test
  public void load() {

    final StrategiesDetector loader = new StrategiesDetector();
    final List<PropertyValueResolver> strategy = loader.getStrategies(PropertyValueResolver.class);

    assertThat(strategy)
            .hasSize(1);

    assertThat(loader.getStrategies())
            .containsKey("cn.taketoday.context.loader.PropertyValueResolver")
            .hasSize(2);

    final Collection<String> strategies = loader.getStrategies("cn.taketoday.context.loader.PropertyValueResolver");

    assertThat(strategies)
            .hasSize(4);
  }

  @Test
  public void testYaml() {
    final StrategiesDetector loader = new StrategiesDetector(
            new YamlStrategiesReader(), "classpath:META-INF/today.strategies.yaml");
    loader.loadStrategies();
    final MultiValueMap<String, String> strategies = loader.getStrategies();

    final List<PropertyValueResolver> strategy = loader.getStrategies(PropertyValueResolver.class);

    assertThat(strategy)
            .hasSize(1);

    assertThat(strategies)
            .hasSize(1)
            .containsKey("cn.taketoday.context.loader.PropertyValueResolver");

    final List<String> strings = strategies.get("cn.taketoday.context.loader.PropertyValueResolver");

    assertThat(strings)
            .hasSize(4)
            .contains("cn.taketoday.context.loader.StrategiesDetectorTests$MyPropertyValueResolver");
  }

  public static class MyPropertyValueResolver implements PropertyValueResolver {

    @Override
    public PropertySetter resolveProperty(Field field) throws PropertyValueException {
      return null;
    }
  }

  public static class MyPropertyValueResolver1 implements PropertyValueResolver {

    @Override
    public PropertySetter resolveProperty(Field field) throws PropertyValueException {
      return null;
    }
  }

}
