package cn.taketoday.framework.config;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.IgnoreDuplicates;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.framework.server.JettyServer;
import cn.taketoday.framework.server.TomcatServer;
import cn.taketoday.framework.server.UndertowServer;
import cn.taketoday.framework.server.WebServer;

@Configuration
@IgnoreDuplicates
public class WebServerAutoConfiguration {

  @MissingBean
  WebServer webServer() {
    if(ClassUtils.isPresent("org.eclipse.jetty.server.Server")) {
      return new JettyServer();
    }
    else if(ClassUtils.isPresent("io.undertow.Undertow")) {
      return new UndertowServer();
    }
    return new TomcatServer();
  }

}


