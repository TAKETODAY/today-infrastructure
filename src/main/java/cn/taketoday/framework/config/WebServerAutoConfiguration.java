package cn.taketoday.framework.config;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.IgnoreDuplicates;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.framework.server.AbstractServletWebServer;
import cn.taketoday.framework.server.JettyServer;
import cn.taketoday.framework.server.TomcatServer;
import cn.taketoday.framework.server.UndertowServer;

@Configuration
@IgnoreDuplicates
public class WebServerAutoConfiguration {

  @MissingBean
  @Props(prefix = "server.") // @since 1.0.3
  static AbstractServletWebServer webServer() {
    if (ClassUtils.isPresent("org.eclipse.jetty.server.Server")) {
      return new JettyServer();
    }
    else if (ClassUtils.isPresent("io.undertow.Undertow")) {
      return new UndertowServer();
    }
    return new TomcatServer();
  }

}


