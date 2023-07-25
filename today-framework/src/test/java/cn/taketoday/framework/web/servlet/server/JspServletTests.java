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

package cn.taketoday.framework.web.servlet.server;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.jasper.EmbeddedServletOptions;
import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyWebServer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatWebServer;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.util.function.ThrowingSupplier;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/25 09:53
 */
public class JspServletTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("arguments")
  @interface JspServletTest {

  }

  public static Stream<Arguments> arguments() throws Exception {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
    JettyServletWebServerFactory serverFactory = new JettyServletWebServerFactory(0);
    return Stream.of(
            Arguments.arguments("Tomcat", new TestArgument(
                    () -> (JspServlet) getJspServlet((TomcatWebServer) factory.getWebServer()), factory)),
            Arguments.arguments("Jetty", new TestArgument(
                    () -> (JspServlet) getJspServlet((JettyWebServer) serverFactory.getWebServer()), serverFactory))
    );
  }

  @JspServletTest
  void disableJspServletRegistration(String displayName, TestArgument data) throws Exception {
    data.factory.getJsp().setRegistered(false);
    assertThat(data.jspServlet.get()).isNull();
  }

  @JspServletTest
  void jspServletInitParameters(String displayName, TestArgument data) {
    Map<String, String> initParameters = new HashMap<>();
    initParameters.put("a", "alpha");
    data.factory.getJsp().setInitParameters(initParameters);
    assertThat(data.jspServlet.get().getInitParameter("a")).isEqualTo("alpha");
  }

  @JspServletTest
  void jspServletIsNotInDevelopmentModeByDefault(String displayName, TestArgument data) {
    EmbeddedServletOptions options = (EmbeddedServletOptions) ReflectionTestUtils.getField(
            data.jspServlet.get(), "options");
    assertThat(options.getDevelopment()).isFalse();
  }

  static Servlet getJspServlet(TomcatWebServer webServer) throws ServletException {
    Tomcat tomcat = webServer.getTomcat();
    Container container = tomcat.getHost().findChildren()[0];
    StandardWrapper standardWrapper = (StandardWrapper) container.findChild("jsp");
    if (standardWrapper == null) {
      return null;
    }
    standardWrapper.load();
    return standardWrapper.getServlet();
  }

  static Servlet getJspServlet(JettyWebServer webServer) throws Exception {
    WebAppContext context = findWebAppContext(webServer);
    ServletHolder holder = context.getServletHandler().getServlet("jsp");
    if (holder == null) {
      return null;
    }
    holder.start();
    holder.initialize();
    return holder.getServlet();
  }

  private static WebAppContext findWebAppContext(JettyWebServer webServer) {
    return findWebAppContext(webServer.getServer().getHandler());
  }

  private static WebAppContext findWebAppContext(Handler handler) {
    if (handler instanceof WebAppContext webAppContext) {
      return webAppContext;
    }
    if (handler instanceof HandlerWrapper wrapper) {
      return findWebAppContext(wrapper.getHandler());
    }
    throw new IllegalStateException("No WebAppContext found");
  }

  static class TestArgument {

    final ThrowingSupplier<JspServlet> jspServlet;
    final AbstractServletWebServerFactory factory;

    TestArgument(ThrowingSupplier<JspServlet> jspServlet, AbstractServletWebServerFactory factory) {
      this.jspServlet = jspServlet;
      this.factory = factory;
    }
  }

}
