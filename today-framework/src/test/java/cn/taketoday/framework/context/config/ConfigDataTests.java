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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.framework.context.config.ConfigData.Option;
import cn.taketoday.framework.context.config.ConfigData.Options;
import cn.taketoday.framework.context.config.ConfigData.PropertySourceOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigData}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataTests {

  @Test
  void createWhenPropertySourcesIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigData(null))
            .withMessage("PropertySources must not be null");
  }

  @Test
  void createWhenOptionsIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigData(Collections.emptyList(), (Option[]) null))
            .withMessage("Options must not be null");
  }

  @Test
  void getPropertySourcesReturnsCopyOfSources() {
    MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
    List<MapPropertySource> sources = new ArrayList<>(Collections.singleton(source));
    ConfigData configData = new ConfigData(sources);
    sources.clear();
    assertThat(configData.getPropertySources()).containsExactly(source);
  }

  @Test
  void getOptionsWhenOptionsSetAtConstructionAlwaysReturnsSameOptions() {
    MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
    ConfigData configData = new ConfigData(Collections.singleton(source), Option.IGNORE_IMPORTS);
    assertThat(configData.getOptions(source).asSet()).containsExactly(Option.IGNORE_IMPORTS);
  }

  @Test
  void getOptionsReturnsOptionsFromPropertySourceOptions() {
    MapPropertySource source1 = new MapPropertySource("test", Collections.emptyMap());
    MapPropertySource source2 = new MapPropertySource("test", Collections.emptyMap());
    Options options1 = Options.of(Option.IGNORE_IMPORTS);
    Options options2 = Options.of(Option.IGNORE_PROFILES);
    PropertySourceOptions propertySourceOptions = (source) -> (source != source1) ? options2 : options1;
    ConfigData configData = new ConfigData(Arrays.asList(source1, source2), propertySourceOptions);
    assertThat(configData.getOptions(source1)).isEqualTo(options1);
    assertThat(configData.getOptions(source2)).isEqualTo(options2);
  }

  @Test
  void getOptionsWhenPropertySourceOptionsReturnsNullReturnsNone() {
    MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
    PropertySourceOptions propertySourceOptions = (propertySource) -> null;
    ConfigData configData = new ConfigData(Collections.singleton(source), propertySourceOptions);
    assertThat(configData.getOptions(source)).isEqualTo(Options.NONE);
  }

  @Test
  void optionsOfCreatesOptions() {
    Options options = Options.of(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
    assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
  }

  @Test
  void optionsOfUsesCopyOfOptions() {
    Option[] array = { Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES };
    Options options = Options.of(array);
    array[0] = Option.PROFILE_SPECIFIC;
    assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
  }

  @Test
  void optionsNoneReturnsEmptyOptions() {
    assertThat(Options.NONE.asSet()).isEmpty();
  }

  @Test
  void optionsWithoutReturnsNewOptions() {
    Options options = Options.of(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
    Options without = options.without(Option.IGNORE_PROFILES);
    assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
    assertThat(without.asSet()).containsExactly(Option.IGNORE_IMPORTS);
  }

  @Test
  void optionsWithReturnsNewOptions() {
    Options options = Options.of(Option.IGNORE_IMPORTS);
    Options with = options.with(Option.IGNORE_PROFILES);
    assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS);
    assertThat(with.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
  }

  @Test
  void propertySourceOptionsAlwaysReturnsSameOptionsEachTime() {
    PropertySourceOptions options = PropertySourceOptions.always(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
    assertThat(options.get(mock(PropertySource.class)).asSet()).containsExactly(Option.IGNORE_IMPORTS,
            Option.IGNORE_PROFILES);
  }

}
