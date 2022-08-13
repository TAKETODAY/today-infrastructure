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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/13 21:56
 */
class ParameterResolvingStrategiesTests {
  ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();

  @Test
  void indexOf() {
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(-1);
    strategies.add(new MapMethodProcessor());

    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(-1);
    strategies.add(new CookieParameterResolver());

    assertThat(strategies.indexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(-1);
    strategies.add(new ErrorsMethodArgumentResolver());

    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(0);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(1);
    assertThat(strategies.indexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(2);

    assertThat(strategies.get(CookieParameterResolver.class)).isNotNull();
    assertThat(strategies.get(ErrorsMethodArgumentResolver.class)).isNotNull();
    assertThat(strategies.get(MapMethodProcessor.class)).isInstanceOf(MapMethodProcessor.class);
    assertThat(strategies.get(AbstractNamedValueResolvingStrategy.class)).isNull();

  }

  @Test
  void lastIndexOf() {
    assertThat(strategies.lastIndexOf(MapMethodProcessor.class)).isEqualTo(-1);
    strategies.add(new MapMethodProcessor());

    assertThat(strategies.lastIndexOf(CookieParameterResolver.class)).isEqualTo(-1);
    strategies.add(new CookieParameterResolver());

    assertThat(strategies.lastIndexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(-1);
    strategies.add(new ErrorsMethodArgumentResolver());

    assertThat(strategies.lastIndexOf(MapMethodProcessor.class)).isEqualTo(2);
    assertThat(strategies.lastIndexOf(MapMethodProcessor.class)).isEqualTo(2);
    assertThat(strategies.lastIndexOf(CookieParameterResolver.class)).isEqualTo(1);
    assertThat(strategies.lastIndexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(0);
    assertThat(strategies.lastIndexOf(ParameterResolvingStrategy.class)).isEqualTo(-1);

  }

  @Test
  void replace() {
    strategies.add(new MapMethodProcessor());
    strategies.add(new CookieParameterResolver());
    strategies.add(new ErrorsMethodArgumentResolver());
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(0);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(1);

    // replace true
    assertThat(strategies.replace(MapMethodProcessor.class, new CookieParameterResolver())).isTrue();
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(-1);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(0);

    // replace false
    assertThat(strategies.replace(ParameterResolvingStrategy.class, new CookieParameterResolver())).isFalse();
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(-1);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(0);

  }

  @Test
  void contains() {
    strategies.add(new MapMethodProcessor());
    strategies.add(new CookieParameterResolver());

    assertThat(strategies.contains(MapMethodProcessor.class)).isTrue();
    assertThat(strategies.contains(CookieParameterResolver.class)).isTrue();
    assertThat(strategies.contains(ParameterResolvingStrategy.class)).isFalse();

    assertThat(strategies.removeIf(strategy -> strategy instanceof CookieParameterResolver)).isTrue();
    assertThat(strategies.contains(CookieParameterResolver.class)).isFalse();
    assertThat(strategies.removeIf(strategy -> strategy instanceof CookieParameterResolver)).isFalse();

    assertThat(strategies.size()).isEqualTo(1);
    assertThat(strategies.set(0, new CookieParameterResolver())).isInstanceOf(MapMethodProcessor.class);

    assertThatThrownBy(() -> strategies.set(1, new CookieParameterResolver()))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }
}