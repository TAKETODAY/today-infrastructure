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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cn.taketoday.context.ConfigurationException;
import cn.taketoday.context.io.Resource;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.DefaultMultiValueMap;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * Strategies file reader
 *
 * @author TODAY 2021/7/17 22:36
 * @since 4.0
 */
public abstract class StrategiesReader {
  public static final Logger log = LoggerFactory.getLogger(StrategiesReader.class);

  /**
   * read a key multi-value map
   *
   * @param strategiesLocation
   *         file location
   *
   * @return a key multi-value map
   */
  public MultiValueMap<String, String> read(String strategiesLocation) {
    MultiValueMap<String, String> strategies = new DefaultMultiValueMap<>();
    read(strategiesLocation, strategies);
    return strategies;
  }

  /**
   * read a key multi-value map
   *
   * @param strategiesLocation
   *         file location supports multiple files:
   *         <p> classpath*:META-INF/today.strategies,classpath*:META-INF/my.strategies
   *
   * @throws ConfigurationException
   *         strategiesLocation load failed
   */
  public void read(String strategiesLocation, MultiValueMap<String, String> strategies) {
    Assert.notNull(strategiesLocation, "file-location must not be null");
    try {
      final List<String> strategiesLocations = StringUtils.splitAsList(strategiesLocation);
      for (final String location : strategiesLocations) {
        log.info("Detecting strategies location '{}'", location);
        final Resource[] resources = ResourceUtils.getResources(location);
        for (final Resource resource : resources) {
          read(resource, strategies);
        }
      }
    }
    catch (IOException e) {
      log.error("'{}' load failed", strategiesLocation, e);
      throw new ConfigurationException("'" + strategiesLocation + "' load failed", e);
    }
  }

  /**
   *
   */
  protected void read(Resource resource, MultiValueMap<String, String> strategies) throws IOException {
    log.info("Reading strategies file '{}'", resource.getLocation());
    try (InputStream inputStream = resource.getInputStream()) {
      readInternal(inputStream, strategies);
    }
    catch (IOException e) {
      fallback(e, resource, strategies);
    }
  }

  protected void fallback(IOException e, Resource resource, MultiValueMap<String, String> strategies)
          throws IOException {
    log.error("'{}' load failed", resource.getLocation(), e);
  }

  /**
   * for read different Strategies
   */
  protected abstract void readInternal(
          InputStream inputStream, MultiValueMap<String, String> strategies) throws IOException;

}
