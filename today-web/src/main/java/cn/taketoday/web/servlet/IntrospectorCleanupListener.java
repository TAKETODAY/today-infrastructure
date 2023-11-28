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

package cn.taketoday.web.servlet;

import java.beans.Introspector;

import cn.taketoday.beans.CachedIntrospectionResults;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Listener that flushes the JDK's {@link java.beans.Introspector JavaBeans Introspector}
 * cache on web app shutdown. Register this listener in your {@code web.xml} to
 * guarantee proper release of the web application class loader and its loaded classes.
 *
 * <p><b>If the JavaBeans Introspector has been used to analyze application classes,
 * the system-level Introspector cache will hold a hard reference to those classes.
 * Consequently, those classes and the web application class loader will not be
 * garbage-collected on web app shutdown!</b> This listener performs proper cleanup,
 * to allow for garbage collection to take effect.
 *
 * <p>Unfortunately, the only way to clean up the Introspector is to flush
 * the entire cache, as there is no way to specifically determine the
 * application's classes referenced there. This will remove cached
 * introspection results for all other applications in the server too.
 *
 * <p>Note that this listener is <i>not</i> necessary when using Framework's beans
 * infrastructure within the application, as Framework's own introspection results
 * cache will immediately flush an analyzed class from the JavaBeans Introspector
 * cache and only hold a cache within the application's own ClassLoader.
 *
 * <b>Although Framework itself does not create JDK Introspector leaks, note that this
 * listener should nevertheless be used in scenarios where the Framework framework classes
 * themselves reside in a 'common' ClassLoader (such as the system ClassLoader).</b>
 * In such a scenario, this listener will properly clean up Framework's introspection cache.
 *
 * <p>Application classes hardly ever need to use the JavaBeans Introspector
 * directly, so are normally not the cause of Introspector resource leaks.
 * Rather, many libraries and frameworks do not clean up the Introspector:
 * e.g. Struts and Quartz.
 *
 * <p>Note that a single such Introspector leak will cause the entire web
 * app class loader to not get garbage collected! This has the consequence that
 * you will see all the application's static class resources (like singletons)
 * around after web app shutdown, which is not the fault of those classes!
 *
 * <p><b>This listener should be registered as the first one in {@code web.xml},
 * before any application listeners such as Framework's ContextLoaderListener.</b>
 * This allows the listener to take full effect at the right time of the lifecycle.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.beans.Introspector#flushCaches()
 * @see cn.taketoday.beans.CachedIntrospectionResults#acceptClassLoader
 * @see cn.taketoday.beans.CachedIntrospectionResults#clearClassLoader
 * @since 4.0 2022/2/23 11:12
 */
public class IntrospectorCleanupListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent event) {
    CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
    Introspector.flushCaches();
  }

}
