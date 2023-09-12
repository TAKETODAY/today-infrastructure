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

package cn.taketoday.framework.web.embedded.jetty;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import jakarta.servlet.ServletContainerInitializer;

/**
 * Jetty {@link AbstractLifeCycle} to initialize Jasper.
 *
 * @author Vladimir Tsanev
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JasperInitializer extends AbstractLifeCycle {

  private static final String[] INITIALIZER_CLASSES = {
          "org.eclipse.jetty.apache.jsp.JettyJasperInitializer",
          "org.apache.jasper.servlet.JasperInitializer"
  };

  private final WebAppContext context;
  @Nullable
  private final ServletContainerInitializer initializer;

  JasperInitializer(WebAppContext context) {
    this.context = context;
    this.initializer = newInitializer();
  }

  @Nullable
  private ServletContainerInitializer newInitializer() {
    for (String className : INITIALIZER_CLASSES) {
      try {
        Class<ServletContainerInitializer> initializerClass = ClassUtils.forName(className, null);
        return initializerClass.getDeclaredConstructor().newInstance();
      }
      catch (Exception ex) {
        // Ignore
      }
    }
    return null;
  }

  @Override
  protected void doStart() throws Exception {
    ServletContainerInitializer initializer = this.initializer;
    if (initializer == null) {
      return;
    }
    try {
      if (ClassUtils.isPresent("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory",
              getClass().getClassLoader())) {
        TomcatURLStreamHandlerFactory.register();
      }
      else {
        URL.setURLStreamHandlerFactory(new WarUrlStreamHandlerFactory());
      }
    }
    catch (Error ex) {
      // Ignore
    }
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.context.getClassLoader());
      try {
        setExtendedListenerTypes(true);
        initializer.onStartup(null, this.context.getServletContext());
      }
      finally {
        setExtendedListenerTypes(false);
      }
    }
    finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }

  private void setExtendedListenerTypes(boolean extended) {
    try {
      this.context.getContext().setExtendedListenerTypes(extended);
    }
    catch (NoSuchMethodError ex) {
      // Not available on Jetty 8
    }
  }

  /**
   * {@link URLStreamHandlerFactory} to support {@literal war} protocol.
   */
  private static class WarUrlStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    @Nullable
    public URLStreamHandler createURLStreamHandler(String protocol) {
      if ("war".equals(protocol)) {
        return new WarUrlStreamHandler();
      }
      return null;
    }

  }

  /**
   * {@link URLStreamHandler} for {@literal war} protocol compatible with jasper's
   * {@link URL urls} produced by
   * {@link org.apache.tomcat.util.scan.JarFactory#getJarEntryURL(URL, String)}.
   */
  private static class WarUrlStreamHandler extends URLStreamHandler {

    @Override
    protected void parseURL(URL u, String spec, int start, int limit) {
      String path = "jar:" + spec.substring("war:".length());
      int separator = path.indexOf("*/");
      if (separator >= 0) {
        path = path.substring(0, separator) + "!/" + path.substring(separator + 2);
      }
      setURL(u, u.getProtocol(), "", -1, null, null, path, null, null);
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      return new WarURLConnection(u);
    }

  }

  /**
   * {@link URLConnection} to support {@literal war} protocol.
   */
  private static class WarURLConnection extends URLConnection {

    private final URLConnection connection;

    protected WarURLConnection(URL url) throws IOException {
      super(url);
      this.connection = new URL(url.getFile()).openConnection();
    }

    @Override
    public void connect() throws IOException {
      if (!this.connected) {
        this.connection.connect();
        this.connected = true;
      }
    }

    @Override
    public InputStream getInputStream() throws IOException {
      connect();
      return this.connection.getInputStream();
    }

  }

}
