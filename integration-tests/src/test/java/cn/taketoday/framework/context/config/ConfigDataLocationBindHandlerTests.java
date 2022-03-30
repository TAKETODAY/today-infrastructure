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

import java.util.List;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataLocationBindHandler}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ConfigDataLocationBindHandlerTests {

  private static final Bindable<ConfigDataLocation[]> ARRAY = Bindable.of(ConfigDataLocation[].class);

  private static final Bindable<ValueObject> VALUE_OBJECT = Bindable.of(ValueObject.class);

  private final ConfigDataLocationBindHandler handler = new ConfigDataLocationBindHandler();

  @Test
  void bindToArrayFromCommaStringPropertySetsOrigin() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("locations", "a,b,c");
    Binder binder = new Binder(source);
    ConfigDataLocation[] bound = binder.bind("locations", ARRAY, this.handler).get();
    String expectedLocation = "\"locations\" from property source \"source\"";
    assertThat(bound[0]).hasToString("a");
    assertThat(bound[0].getOrigin()).hasToString(expectedLocation);
    assertThat(bound[1]).hasToString("b");
    assertThat(bound[1].getOrigin()).hasToString(expectedLocation);
    assertThat(bound[2]).hasToString("c");
    assertThat(bound[2].getOrigin()).hasToString(expectedLocation);
  }

  @Test
  void bindToArrayFromCommaStringPropertyIgnoresEmptyElements() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("locations", ",a,,b,c,");
    Binder binder = new Binder(source);
    ConfigDataLocation[] bound = binder.bind("locations", ARRAY, this.handler).get();
    String expectedLocation = "\"locations\" from property source \"source\"";
    assertThat(bound[0]).hasToString("a");
    assertThat(bound[0].getOrigin()).hasToString(expectedLocation);
    assertThat(bound[1]).hasToString("b");
    assertThat(bound[1].getOrigin()).hasToString(expectedLocation);
    assertThat(bound[2]).hasToString("c");
    assertThat(bound[2].getOrigin()).hasToString(expectedLocation);
  }

  @Test
  void bindToArrayFromIndexedPropertiesSetsOrigin() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("locations[0]", "a");
    source.put("locations[1]", "b");
    source.put("locations[2]", "c");
    Binder binder = new Binder(source);
    ConfigDataLocation[] bound = binder.bind("locations", ARRAY, this.handler).get();
    assertThat(bound[0]).hasToString("a");
    assertThat(bound[0].getOrigin()).hasToString("\"locations[0]\" from property source \"source\"");
    assertThat(bound[1]).hasToString("b");
    assertThat(bound[1].getOrigin()).hasToString("\"locations[1]\" from property source \"source\"");
    assertThat(bound[2]).hasToString("c");
    assertThat(bound[2].getOrigin()).hasToString("\"locations[2]\" from property source \"source\"");
  }

  @Test
  void bindToValueObjectFromCommaStringPropertySetsOrigin() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("test.locations", "a,b,c");
    Binder binder = new Binder(source);
    ValueObject bound = binder.bind("test", VALUE_OBJECT, this.handler).get();
    String expectedLocation = "\"test.locations\" from property source \"source\"";
    assertThat(bound.getLocation(0)).hasToString("a");
    assertThat(bound.getLocation(0).getOrigin()).hasToString(expectedLocation);
    assertThat(bound.getLocation(1)).hasToString("b");
    assertThat(bound.getLocation(1).getOrigin()).hasToString(expectedLocation);
    assertThat(bound.getLocation(2)).hasToString("c");
    assertThat(bound.getLocation(2).getOrigin()).hasToString(expectedLocation);
  }

  @Test
  void bindToValueObjectFromCommaStringPropertyIgnoresEmptyElements() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("test.locations", ",a,b,,c,");
    Binder binder = new Binder(source);
    ValueObject bound = binder.bind("test", VALUE_OBJECT, this.handler).get();
    String expectedLocation = "\"test.locations\" from property source \"source\"";
    assertThat(bound.getLocation(0)).hasToString("a");
    assertThat(bound.getLocation(0).getOrigin()).hasToString(expectedLocation);
    assertThat(bound.getLocation(1)).hasToString("b");
    assertThat(bound.getLocation(1).getOrigin()).hasToString(expectedLocation);
    assertThat(bound.getLocation(2)).hasToString("c");
    assertThat(bound.getLocation(2).getOrigin()).hasToString(expectedLocation);
  }

  @Test
  void bindToValueObjectFromIndexedPropertiesSetsOrigin() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("test.locations[0]", "a");
    source.put("test.locations[1]", "b");
    source.put("test.locations[2]", "c");
    Binder binder = new Binder(source);
    ValueObject bound = binder.bind("test", VALUE_OBJECT, this.handler).get();
    assertThat(bound.getLocation(0)).hasToString("a");
    assertThat(bound.getLocation(0).getOrigin())
            .hasToString("\"test.locations[0]\" from property source \"source\"");
    assertThat(bound.getLocation(1)).hasToString("b");
    assertThat(bound.getLocation(1).getOrigin())
            .hasToString("\"test.locations[1]\" from property source \"source\"");
    assertThat(bound.getLocation(2)).hasToString("c");
    assertThat(bound.getLocation(2).getOrigin())
            .hasToString("\"test.locations[2]\" from property source \"source\"");
  }

  static class ValueObject {

    private final List<ConfigDataLocation> locations;

    ValueObject(List<ConfigDataLocation> locations) {
      this.locations = locations;
    }

    ConfigDataLocation getLocation(int index) {
      return this.locations.get(index);
    }

  }

}
