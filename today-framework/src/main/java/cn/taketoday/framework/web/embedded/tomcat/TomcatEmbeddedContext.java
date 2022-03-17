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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.session.ManagerBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import jakarta.servlet.ServletException;

/**
 * Tomcat {@link StandardContext} used by {@link TomcatWebServer} to support deferred
 * initialization.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TomcatEmbeddedContext extends StandardContext {

  @Nullable
  private TomcatStarter starter;

  @Override
  public boolean loadOnStartup(Container[] children) {
    // deferred until later (see deferredLoadOnStartup)
    return true;
  }

  @Override
  public void setManager(Manager manager) {
    if (manager instanceof ManagerBase) {
      manager.setSessionIdGenerator(new LazySessionIdGenerator());
    }
    super.setManager(manager);
  }

  void deferredLoadOnStartup() throws LifecycleException {
    doWithThreadContextClassLoader(getLoader().getClassLoader(),
            () -> getLoadOnStartupWrappers(findChildren()).forEach(this::load));
  }

  private Stream<Wrapper> getLoadOnStartupWrappers(Container[] children) {
    Map<Integer, List<Wrapper>> grouped = new TreeMap<>();
    for (Container child : children) {
      Wrapper wrapper = (Wrapper) child;
      int order = wrapper.getLoadOnStartup();
      if (order >= 0) {
        grouped.computeIfAbsent(order, (o) -> new ArrayList<>()).add(wrapper);
      }
    }
    return grouped.values().stream().flatMap(List::stream);
  }

  private void load(Wrapper wrapper) {
    try {
      wrapper.load();
    }
    catch (ServletException ex) {
      String message = sm.getString("standardContext.loadOnStartup.loadException", getName(), wrapper.getName());
      if (getComputedFailCtxIfServletStartFails()) {
        throw new WebServerException(message, ex);
      }
      getLogger().error(message, StandardWrapper.getRootCause(ex));
    }
  }

  /**
   * Some older Servlet frameworks (e.g. Struts, BIRT) use the Thread context class
   * loader to create servlet instances in this phase. If they do that and then try to
   * initialize them later the class loader may have changed, so wrap the call to
   * loadOnStartup in what we think is going to be the main webapp classloader at
   * runtime.
   *
   * @param classLoader the class loader to use
   * @param code the code to run
   */
  private void doWithThreadContextClassLoader(@Nullable ClassLoader classLoader, Runnable code) {
    ClassLoader existingLoader = (classLoader != null)
                                 ? ClassUtils.overrideThreadContextClassLoader(classLoader)
                                 : null;
    try {
      code.run();
    }
    finally {
      if (existingLoader != null) {
        ClassUtils.overrideThreadContextClassLoader(existingLoader);
      }
    }
  }

  void setStarter(@Nullable TomcatStarter starter) {
    this.starter = starter;
  }

  @Nullable
  TomcatStarter getStarter() {
    return this.starter;
  }

}
