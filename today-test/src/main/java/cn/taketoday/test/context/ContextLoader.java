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

package cn.taketoday.test.context;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;
import cn.taketoday.test.context.support.GenericPropertiesContextLoader;
import cn.taketoday.test.context.support.GenericXmlContextLoader;

/**
 * Strategy interface for loading an {@link ApplicationContext application context}
 * for an integration test managed by the Spring TestContext Framework.
 *
 * <p><b>Note</b>: as of Spring 3.1, implement {@link SmartContextLoader} instead
 * of this interface in order to provide support for annotated classes, active
 * bean definition profiles, and application context initializers.
 *
 * <p>Clients of a ContextLoader should call
 * {@link #processLocations(Class, String...) processLocations()} prior to
 * calling {@link #loadContext(String...) loadContext()} in case the
 * ContextLoader provides custom support for modifying or generating locations.
 * The results of {@link #processLocations(Class, String...) processLocations()}
 * should then be supplied to {@link #loadContext(String...) loadContext()}.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * <p>Spring provides the following out-of-the-box implementations:
 * <ul>
 * <li>{@link GenericXmlContextLoader GenericXmlContextLoader}</li>
 * <li>{@link GenericPropertiesContextLoader GenericPropertiesContextLoader}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see SmartContextLoader
 * @see AnnotationConfigContextLoader AnnotationConfigContextLoader
 * @since 2.5
 */
public interface ContextLoader {

  /**
   * Processes application context resource locations for a specified class.
   * <p>Concrete implementations may choose to modify the supplied locations,
   * generate new locations, or simply return the supplied locations unchanged.
   *
   * @param clazz the class with which the locations are associated: used to
   * determine how to process the supplied locations
   * @param locations the unmodified locations to use for loading the
   * application context (can be {@code null} or empty)
   * @return an array of application context resource locations
   */
  String[] processLocations(Class<?> clazz, String... locations);

  /**
   * Loads a new {@link ApplicationContext context} based on the supplied
   * {@code locations}, configures the context, and finally returns
   * the context in fully <em>refreshed</em> state.
   * <p>Configuration locations are generally considered to be classpath
   * resources by default.
   * <p>Concrete implementations should register annotation configuration
   * processors with bean factories of {@link ApplicationContext application
   * contexts} loaded by this ContextLoader. Beans will therefore automatically
   * be candidates for annotation-based dependency injection using
   * {@link cn.taketoday.beans.factory.annotation.Autowired @Autowired},
   * {@link jakarta.annotation.Resource @Resource}, and
   * {@link jakarta.inject.Inject @Inject}.
   * <p>Any ApplicationContext loaded by a ContextLoader <strong>must</strong>
   * register a JVM shutdown hook for itself. Unless the context gets closed
   * early, all context instances will be automatically closed on JVM
   * shutdown. This allows for freeing external resources held by beans within
   * the context, e.g. temporary files.
   *
   * @param locations the resource locations to use to load the application context
   * @return a new application context
   * @throws Exception if context loading failed
   */
  ApplicationContext loadContext(String... locations) throws Exception;

}
