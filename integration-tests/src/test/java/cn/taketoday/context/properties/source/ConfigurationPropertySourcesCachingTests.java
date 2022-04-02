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

package cn.taketoday.context.properties.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link ConfigurationPropertySourcesCaching}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesCachingTests {

  private List<ConfigurationPropertySource> sources;

  private ConfigurationPropertySourcesCaching caching;

  @BeforeEach
  void setup() {
    this.sources = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      this.sources.add(mockSource(i % 2 == 0));
    }
    this.caching = new ConfigurationPropertySourcesCaching(this.sources);
  }

  private ConfigurationPropertySource mockSource(boolean cachingSource) {
    if (!cachingSource) {
      return mock(ConfigurationPropertySource.class);
    }
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class,
            withSettings().extraInterfaces(CachingConfigurationPropertySource.class));
    ConfigurationPropertyCaching caching = mock(ConfigurationPropertyCaching.class);
    given(((CachingConfigurationPropertySource) source).getCaching()).willReturn(caching);
    return source;
  }

  @Test
  void enableDelegatesToCachingConfigurationPropertySources() {
    this.caching.enable();
    then(getCaching(0)).should().enable();
    then(getCaching(2)).should().enable();
  }

  @Test
  void enableWhenSourcesIsNullDoesNothing() {
    new ConfigurationPropertySourcesCaching(null).enable();
  }

  @Test
  void disableDelegatesToCachingConfigurationPropertySources() {
    this.caching.disable();
    then(getCaching(0)).should().disable();
    then(getCaching(2)).should().disable();
  }

  @Test
  void disableWhenSourcesIsNullDoesNothing() {
    new ConfigurationPropertySourcesCaching(null).disable();
  }

  @Test
  void setTimeToLiveDelegatesToCachingConfigurationPropertySources() {
    Duration ttl = Duration.ofDays(1);
    this.caching.setTimeToLive(ttl);
    then(getCaching(0)).should().setTimeToLive(ttl);
    then(getCaching(2)).should().setTimeToLive(ttl);
  }

  @Test
  void setTimeToLiveWhenSourcesIsNullDoesNothing() {
    new ConfigurationPropertySourcesCaching(null).setTimeToLive(Duration.ofSeconds(1));
  }

  @Test
  void clearDelegatesToCachingConfigurationPropertySources() {
    this.caching.clear();
    then(getCaching(0)).should().clear();
    then(getCaching(2)).should().clear();
  }

  @Test
  void clearWhenSourcesIsNullDoesNothing() {
    new ConfigurationPropertySourcesCaching(null).enable();
  }

  private ConfigurationPropertyCaching getCaching(int index) {
    return CachingConfigurationPropertySource.find(this.sources.get(index));
  }

}
