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

package cn.taketoday.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.function.Consumer;

import cn.taketoday.core.env.CompositePropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.testfixture.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/2 21:17
 */
@ExtendWith(MockitoExtension.class)
class DefaultPropertiesPropertySourceTests {

  @Mock
  private Consumer<DefaultPropertiesPropertySource> action;

  @Captor
  private ArgumentCaptor<DefaultPropertiesPropertySource> captor;

  @Test
  void nameIsDefaultProperties() {
    assertThat(DefaultPropertiesPropertySource.NAME).isEqualTo("defaultProperties");
  }

  @Test
  void createCreatesSource() {
    DefaultPropertiesPropertySource propertySource = new DefaultPropertiesPropertySource(
            Collections.singletonMap("spring", "boot"));
    assertThat(propertySource.getName()).isEqualTo("defaultProperties");
    assertThat(propertySource.getProperty("spring")).isEqualTo("boot");
  }

  @Test
  void hasMatchingNameWhenNameMatchesReturnsTrue() {
    MockPropertySource propertySource = new MockPropertySource("defaultProperties");
    assertThat(DefaultPropertiesPropertySource.hasMatchingName(propertySource)).isTrue();
  }

  @Test
  void hasMatchingNameWhenNameDoesNotMatchReturnsFalse() {
    MockPropertySource propertySource = new MockPropertySource("normalProperties");
    assertThat(DefaultPropertiesPropertySource.hasMatchingName(propertySource)).isFalse();
  }

  @Test
  void hasMatchingNameWhenPropertySourceIsNullReturnsFalse() {
    assertThat(DefaultPropertiesPropertySource.hasMatchingName(null)).isFalse();
  }

  @Test
  void ifNotEmptyWhenNullDoesNotCallAction() {
    DefaultPropertiesPropertySource.ifNotEmpty(null, this.action);
    then(this.action).shouldHaveNoInteractions();
  }

  @Test
  void ifNotEmptyWhenEmptyDoesNotCallAction() {
    DefaultPropertiesPropertySource.ifNotEmpty(Collections.emptyMap(), this.action);
    then(this.action).shouldHaveNoInteractions();
  }

  @Test
  void ifNotEmptyHasValueCallsAction() {
    DefaultPropertiesPropertySource.ifNotEmpty(Collections.singletonMap("spring", "boot"), this.action);
    then(this.action).should().accept(this.captor.capture());
    assertThat(this.captor.getValue().getProperty("spring")).isEqualTo("boot");
  }

  @Test
  void moveToEndWhenNotPresentDoesNothing() {
    MockEnvironment environment = new MockEnvironment();
    DefaultPropertiesPropertySource.moveToEnd(environment);
  }

  @Test
  void addOrMergeWhenExistingNotFoundShouldAdd() {
    MockEnvironment environment = new MockEnvironment();
    PropertySources propertySources = environment.getPropertySources();
    DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap("spring", "boot"), propertySources);
    assertThat(propertySources.contains(DefaultPropertiesPropertySource.NAME)).isTrue();
    assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("spring")).isEqualTo("boot");
  }

  @Test
  void addOrMergeWhenExistingFoundShouldMerge() {
    MockEnvironment environment = new MockEnvironment();
    PropertySources propertySources = environment.getPropertySources();
    propertySources.addLast(new DefaultPropertiesPropertySource(Collections.singletonMap("spring", "boot")));
    DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap("hello", "world"), propertySources);
    assertThat(propertySources.contains(DefaultPropertiesPropertySource.NAME)).isTrue();
    assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("spring")).isEqualTo("boot");
    assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("hello")).isEqualTo("world");
  }

  @Test
  void addOrMergeWhenExistingNotMapPropertySourceShouldNotMerge() {
    MockEnvironment environment = new MockEnvironment();
    PropertySources propertySources = environment.getPropertySources();
    CompositePropertySource composite = new CompositePropertySource(DefaultPropertiesPropertySource.NAME);
    composite.addPropertySource(new DefaultPropertiesPropertySource(Collections.singletonMap("spring", "boot")));
    propertySources.addFirst(composite);
    DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap("hello", "world"), propertySources);
    assertThat(propertySources.contains(DefaultPropertiesPropertySource.NAME)).isTrue();
    assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("spring")).isNull();
    assertThat(propertySources.get(DefaultPropertiesPropertySource.NAME).getProperty("hello")).isEqualTo("world");
  }

  @Test
  void moveToEndWhenPresentMovesToEnd() {
    MockEnvironment environment = new MockEnvironment();
    PropertySources propertySources = environment.getPropertySources();
    propertySources.addLast(new DefaultPropertiesPropertySource(Collections.singletonMap("spring", "boot")));
    propertySources.addLast(new MockPropertySource("test"));
    DefaultPropertiesPropertySource.moveToEnd(environment);
    String[] names = propertySources.stream().map(PropertySource::getName).toArray(String[]::new);
    assertThat(names).containsExactly(MockPropertySource.MOCK_PROPERTIES_PROPERTY_SOURCE_NAME, "test",
            DefaultPropertiesPropertySource.NAME);
  }

}