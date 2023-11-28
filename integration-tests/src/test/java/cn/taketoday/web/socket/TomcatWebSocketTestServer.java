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

package cn.taketoday.web.socket;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.websocket.server.WsContextListener;

import java.io.File;

import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;

/**
 * Tomcat based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class TomcatWebSocketTestServer implements WebSocketTestServer {

  private static final Logger logger = LoggerFactory.getLogger(TomcatWebSocketTestServer.class);

  private Tomcat tomcatServer;

  private int port;

  private Context context;

  @Override
  public void setup() {
    Connector connector = new Connector(Http11NioProtocol.class.getName());
    connector.setPort(0);

    File baseDir = createTempDir("tomcat");
    String baseDirPath = baseDir.getAbsolutePath();

    this.tomcatServer = new Tomcat();
    this.tomcatServer.setBaseDir(baseDirPath);
    this.tomcatServer.setPort(0);
    this.tomcatServer.getService().addConnector(connector);
    this.tomcatServer.setConnector(connector);
  }

  private File createTempDir(String prefix) {
    File tempFolder = ApplicationTemp.instance.createFile(null, prefix + ".", "." + getPort()).toFile();
    tempFolder.delete();
    tempFolder.mkdir();
    tempFolder.deleteOnExit();
    return tempFolder;
  }

  @Override
  public void deployConfig(WebApplicationContext wac, Filter... filters) {
    Assert.state(this.port != -1, "setup() was never called.");
    this.context = this.tomcatServer.addContext("", System.getProperty("java.io.tmpdir"));
    this.context.addApplicationListener(WsContextListener.class.getName());
    Tomcat.addServlet(this.context, "dispatcherServlet", new DispatcherServlet(wac)).setAsyncSupported(true);
    this.context.addServletMappingDecoded("/", "dispatcherServlet");
    for (Filter filter : filters) {
      FilterDef filterDef = new FilterDef();
      filterDef.setFilterName(filter.getClass().getName());
      filterDef.setFilter(filter);
      filterDef.setAsyncSupported("true");
      this.context.addFilterDef(filterDef);
      FilterMap filterMap = new FilterMap();
      filterMap.setFilterName(filter.getClass().getName());
      filterMap.addURLPattern("/*");
      filterMap.setDispatcher("REQUEST,FORWARD,INCLUDE,ASYNC");
      this.context.addFilterMap(filterMap);
    }
  }

  @Override
  public void undeployConfig() {
    if (this.context != null) {
      this.context.removeServletMapping("/");
      this.tomcatServer.getHost().removeChild(this.context);
    }
  }

  @Override
  public void start() throws Exception {
    this.tomcatServer.start();
    this.port = this.tomcatServer.getConnector().getLocalPort();
    this.context.addLifecycleListener(event -> {
      if (logger.isDebugEnabled()) {
        logger.debug("Event: " + event.getType());
      }
    });
  }

  @Override
  public void stop() throws Exception {
    this.tomcatServer.stop();
    this.port = 0;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public ServletContext getServletContext() {
    return this.context.getServletContext();
  }

}
