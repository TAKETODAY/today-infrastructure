/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.context.junit4.spr4868;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * @author Sam Brannen
 * @since 4.0
 */
class LifecycleBean {

  private final Logger logger = LoggerFactory.getLogger(LifecycleBean.class);

  @PostConstruct
  public void init() {
    logger.info("initializing");
  }

  @PreDestroy
  public void destroy() {
    logger.info("destroying");
  }

}
