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

package cn.taketoday.framework.web.server;

import cn.taketoday.beans.factory.BeanPostProcessor;

/**
 * Strategy interface for customizing {@link WebServerFactory web server factories}. Any
 * beans of this type will get a callback with the server factory before the server itself
 * is started, so you can set the port, address, error pages etc.
 * <p>
 * Beware: calls to this interface are usually made from a
 * {@link WebServerFactoryCustomizerBeanPostProcessor} which is a
 * {@link BeanPostProcessor} (so called very early in the ApplicationContext lifecycle).
 * It might be safer to lookup dependencies lazily in the enclosing BeanFactory rather
 * than injecting them with {@code @Autowired}.
 *
 * @param <T> the configurable web server factory
 * @author Phillip Webb
 * @author Dave Syer
 * @author Brian Clozel
 * @see WebServerFactoryCustomizerBeanPostProcessor
 * @since 4.0
 */
@FunctionalInterface
public interface WebServerFactoryCustomizer<T extends WebServerFactory> {

  /**
   * Customize the specified {@link WebServerFactory}.
   *
   * @param factory the web server factory to customize
   */
  void customize(T factory);

}
