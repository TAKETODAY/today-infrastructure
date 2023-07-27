/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.testfixture.http.server.reactive.bootstrap;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.http.server.reactive.ServletHttpHandlerAdapter;
import cn.taketoday.http.server.reactive.TomcatHttpHandlerAdapter;
import cn.taketoday.lang.Assert;

/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class TomcatHttpServer extends AbstractHttpServer {

  private final String baseDir;

  private final Class<?> wsListener;

  private String contextPath = "";

  private String servletMapping = "/";

  private Tomcat tomcatServer;

  /**
   * Create a new Tomcat HTTP server using the {@code java.io.tmpdir} JVM
   * system property as the {@code baseDir}.
   */
  public TomcatHttpServer() {
    this(ApplicationTemp.instance.getDir().getAbsolutePath());
  }

  public TomcatHttpServer(String baseDir) {
    this(baseDir, null);
  }

  public TomcatHttpServer(String baseDir, Class<?> wsListener) {
    Assert.notNull(baseDir, "Base dir must not be null");
    this.baseDir = baseDir;
    this.wsListener = wsListener;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public void setServletMapping(String servletMapping) {
    this.servletMapping = servletMapping;
  }

  @Override
  protected void initServer() throws Exception {
    this.tomcatServer = new Tomcat();
    this.tomcatServer.setBaseDir(baseDir);
    this.tomcatServer.setHostname(getHost());
    this.tomcatServer.setPort(getPort());

    ServletHttpHandlerAdapter servlet = initServletAdapter();

    File base = ApplicationTemp.instance.getDir();
    Context rootContext = tomcatServer.addContext(this.contextPath, base.getAbsolutePath());
    Tomcat.addServlet(rootContext, "httpHandlerServlet", servlet).setAsyncSupported(true);
    rootContext.addServletMappingDecoded(this.servletMapping, "httpHandlerServlet");
    if (wsListener != null) {
      rootContext.addApplicationListener(wsListener.getName());
    }
  }

  private ServletHttpHandlerAdapter initServletAdapter() {
    return new TomcatHttpHandlerAdapter(resolveHttpHandler());
  }

  @Override
  protected void startInternal() throws LifecycleException {
    this.tomcatServer.start();
    setPort(this.tomcatServer.getConnector().getLocalPort());
  }

  @Override
  protected void stopInternal() throws Exception {
    this.tomcatServer.stop();
    this.tomcatServer.destroy();
  }

  @Override
  protected void resetInternal() {
    this.tomcatServer = null;
  }

}
