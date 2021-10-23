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

package cn.taketoday.context.loader;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import cn.taketoday.beans.PropertyException;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.core.YamlStrategiesReader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/7/17 22:17
 */
class StrategiesDetectorTests {

  @Test
  void load() {

    final StrategiesDetector loader = new StrategiesDetector();
    loader.setStrategiesLocation(TodayStrategies.STRATEGIES_LOCATION);
    final List<PropertyValueResolver> strategy = loader.getStrategies(PropertyValueResolver.class);

    assertThat(strategy)
            .hasSize(1);

    assertThat(loader.getStrategies())
            .containsKey("cn.taketoday.context.loader.PropertyValueResolver")
            .hasSize(3);

    final Collection<String> strategies = loader.getStrategies("cn.taketoday.context.loader.PropertyValueResolver");

    assertThat(strategies)
            .hasSize(4);
  }

  @Test
  void testYaml() {
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

    @Nullable
    @Override
    public PropertySetter resolveProperty(PropertyResolvingContext context, Field field) throws PropertyException {
      return null;
    }
  }

  public static class MyPropertyValueResolver1 implements PropertyValueResolver {

    @Nullable
    @Override
    public PropertySetter resolveProperty(PropertyResolvingContext context, Field field) throws PropertyException {
      return null;
    }
  }

}
