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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY 2021/10/2 23:38
 * @see BeanDefinitionLoadingStrategies
 * @since 4.0
 */
public class ScanningBeanDefinitionReader {
  private static final Logger log = LoggerFactory.getLogger(ScanningBeanDefinitionReader.class);

  private final BeanDefinitionRegistry registry;
  private final DefinitionLoadingContext loadingContext;
  private final BeanDefinitionLoadingStrategies scanningStrategies;
  private final ClassPathScanningComponentProvider componentProvider;

  public ScanningBeanDefinitionReader(DefinitionLoadingContext loadingContext) {
    this.registry = loadingContext.getRegistry();
    this.loadingContext = loadingContext;
    this.componentProvider = new ClassPathScanningComponentProvider();
    componentProvider.setMetadataReaderFactory(loadingContext.getMetadataReaderFactory());

    List<BeanDefinitionLoadingStrategy> strategies = TodayStrategies.getStrategies(
            BeanDefinitionLoadingStrategy.class);
    this.scanningStrategies = new BeanDefinitionLoadingStrategies(strategies);
  }

  /**
   * Load {@link BeanDefinition}s from input package locations
   *
   * @param basePackages package locations
   * @throws BeanDefinitionStoreException If BeanDefinition could not be store
   * @since 4.0
   */
  public int scan(String... basePackages) throws BeanDefinitionStoreException {
    // Loading candidates components
    log.info("Scanning candidates components from packages: {}", Arrays.toString(basePackages));

    int beanDefinitionCount = registry.getBeanDefinitionCount();
    for (String location : basePackages) {
      doScanning(location);
    }
    int afterScanCount = registry.getBeanDefinitionCount();
    log.info("There are [{}] components in {}", afterScanCount - beanDefinitionCount, Arrays.toString(basePackages));
    return afterScanCount - beanDefinitionCount;
  }

  public void doScanning(String basePackage) {
    if (log.isDebugEnabled()) {
      log.debug("Scanning component candidates from pattern location: [{}]", basePackage);
    }
    try {
      componentProvider.scan(basePackage, (metadataReader, metadataReaderFactory) -> {
        scanningStrategies.loadBeanDefinitions(metadataReader, loadingContext);
      });
    }
    catch (IOException e) {
      throw new BeanDefinitionStoreException("I/O failure during classpath scanning", e);
    }
  }

  @SafeVarargs
  public final void addLoadingStrategies(
          Class<? extends BeanDefinitionLoadingStrategy>... loadingStrategies) {
    for (Class<? extends BeanDefinitionLoadingStrategy> loadingStrategy : loadingStrategies) {
      BeanDefinitionLoadingStrategy strategy = loadingContext.instantiate(loadingStrategy);
      scanningStrategies.addStrategies(strategy);
    }
  }

  public void addLoadingStrategies(
          Collection<Class<? extends BeanDefinitionLoadingStrategy>> loadingStrategies) {
    for (Class<? extends BeanDefinitionLoadingStrategy> loadingStrategy : loadingStrategies) {
      BeanDefinitionLoadingStrategy strategy = loadingContext.instantiate(loadingStrategy);
      scanningStrategies.addStrategies(strategy);
    }
  }
}
