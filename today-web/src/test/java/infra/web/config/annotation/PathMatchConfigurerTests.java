/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.config.annotation;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 21:44
 */
class PathMatchConfigurerTests {

  @Test
  void setUseCaseSensitiveMatchStoresValue() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();

    PathMatchConfigurer result = configurer.setUseCaseSensitiveMatch(true);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.isUseCaseSensitiveMatch()).isTrue();
  }

  @Test
  void setUseCaseSensitiveMatchWithNullValue() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();

    PathMatchConfigurer result = configurer.setUseCaseSensitiveMatch(null);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.isUseCaseSensitiveMatch()).isNull();
  }

  @Test
  void setUseTrailingSlashMatchStoresValue() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();

    PathMatchConfigurer result = configurer.setUseTrailingSlashMatch(false);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.isUseTrailingSlashMatch()).isFalse();
  }

  @Test
  void setUseTrailingSlashMatchWithNullValue() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();

    PathMatchConfigurer result = configurer.setUseTrailingSlashMatch(null);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.isUseTrailingSlashMatch()).isNull();
  }

  @Test
  void addPathPrefixAddsSinglePrefix() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();
    Predicate<Class<?>> predicate = clazz -> true;

    PathMatchConfigurer result = configurer.addPathPrefix("/api", predicate);

    assertThat(result).isSameAs(configurer);
    Map<String, Predicate<Class<?>>> prefixes = configurer.getPathPrefixes();
    assertThat(prefixes).containsKey("/api");
    assertThat(prefixes.get("/api")).isSameAs(predicate);
  }

  @Test
  void addPathPrefixAddsMultiplePrefixes() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();
    Predicate<Class<?>> predicate1 = clazz -> true;
    Predicate<Class<?>> predicate2 = clazz -> false;

    configurer.addPathPrefix("/api", predicate1);
    configurer.addPathPrefix("/admin", predicate2);

    Map<String, Predicate<Class<?>>> prefixes = configurer.getPathPrefixes();
    assertThat(prefixes).containsKeys("/api", "/admin");
    assertThat(prefixes.get("/api")).isSameAs(predicate1);
    assertThat(prefixes.get("/admin")).isSameAs(predicate2);
  }

  @Test
  void addPathPrefixMaintainsOrder() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();
    Predicate<Class<?>> predicate1 = clazz -> true;
    Predicate<Class<?>> predicate2 = clazz -> false;
    Predicate<Class<?>> predicate3 = clazz -> true;

    configurer.addPathPrefix("/first", predicate1);
    configurer.addPathPrefix("/second", predicate2);
    configurer.addPathPrefix("/third", predicate3);

    Map<String, Predicate<Class<?>>> prefixes = configurer.getPathPrefixes();
    assertThat(prefixes.keySet()).containsExactly("/first", "/second", "/third");
  }

  @Test
  void getPathPrefixesReturnsNullWhenNoPrefixesAdded() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();

    Map<String, Predicate<Class<?>>> prefixes = configurer.getPathPrefixes();

    assertThat(prefixes).isNull();
  }

  @Test
  void defaultValuesAreNull() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();

    assertThat(configurer.isUseTrailingSlashMatch()).isNull();
    assertThat(configurer.isUseCaseSensitiveMatch()).isNull();
    assertThat(configurer.getPathPrefixes()).isNull();
  }

  @Test
  void chainMethodsReturnSameInstance() {
    PathMatchConfigurer configurer = new PathMatchConfigurer();
    Predicate<Class<?>> predicate = clazz -> true;

    PathMatchConfigurer result1 = configurer.setUseCaseSensitiveMatch(true);
    PathMatchConfigurer result2 = result1.setUseTrailingSlashMatch(false);
    PathMatchConfigurer result3 = result2.addPathPrefix("/api", predicate);

    assertThat(result1).isSameAs(configurer);
    assertThat(result2).isSameAs(configurer);
    assertThat(result3).isSameAs(configurer);

    assertThat(configurer.isUseCaseSensitiveMatch()).isTrue();
    assertThat(configurer.isUseTrailingSlashMatch()).isFalse();
    assertThat(configurer.getPathPrefixes()).containsKey("/api");
  }

}