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

package infra.mock.api;

import java.util.Set;

import infra.mock.api.annotation.HandlesTypes;

/**
 * Interface which allows a library/runtime to be notified of a web application's startup phase and perform any required
 * programmatic registration of Mocks, filters, and listeners in response to it.
 *
 * <p>
 * Implementations of this interface may be annotated with {@link HandlesTypes HandlesTypes},
 * in order to receive (at their {@link #onStartup} method) the Set of application classes that implement, extend, or
 * have been annotated with the class types specified by the annotation.
 *
 * <p>
 * If an implementation of this interface does not use <tt>HandlesTypes</tt> annotation, or none of the application
 * classes match the ones specified by the annotation, the container must pass a <tt>null</tt> Set of classes to
 * {@link #onStartup}.
 *
 * <p>
 * When examining the classes of an application to see if they match any of the criteria specified by the
 * <tt>HandlesTypes</tt> annotation of a <tt>MockContainerInitializer</tt>, the container may run into classloading
 * problems if any of the application's optional JAR files are missing. Because the container is not in a position to
 * decide whether these types of classloading failures will prevent the application from working correctly, it must
 * ignore them, while at the same time providing a configuration option that would log them.
 *
 * <p>
 * Implementations of this interface must be declared by a JAR file resource located inside the
 * <tt>META-INF/services</tt> directory and named for the fully qualified class name of this interface, and will be
 * discovered using the runtime's service provider lookup mechanism or a container specific mechanism that is
 * semantically equivalent to it. In either case, <tt>MockContainerInitializer</tt> services from web fragment JAR
 * files excluded from an absolute ordering must be ignored, and the order in which these services are discovered must
 * follow the application's classloading delegation model.
 *
 * @see HandlesTypes
 */
public interface MockContainerInitializer {

  /**
   * Notifies this <tt>MockContainerInitializer</tt> of the startup of the application represented by the given
   * <tt>MockContext</tt>.
   *
   * <p>
   * If this <tt>MockContainerInitializer</tt> is bundled in a JAR file inside the <tt>WEB-INF/lib</tt> directory of an
   * application, its <tt>onStartup</tt> method will be invoked only once during the startup of the bundling application.
   * If this <tt>MockContainerInitializer</tt> is bundled inside a JAR file outside of any <tt>WEB-INF/lib</tt>
   * directory, but still discoverable as described above, its <tt>onStartup</tt> method will be invoked every time an
   * application is started.
   *
   * @param c the Set of application classes that extend, implement, or have been annotated with the class types specified
   * by the {@link HandlesTypes HandlesTypes} annotation, or <tt>null</tt> if there are no
   * matches, or this <tt>MockContainerInitializer</tt> has not been annotated with <tt>HandlesTypes</tt>
   * @param ctx the <tt>MockContext</tt> of the web application that is being started and in which the classes
   * contained in <tt>c</tt> were found
   * @throws MockException if an error has occurred
   */
  void onStartup(Set<Class<?>> c, MockContext ctx) throws MockException;

}
