package cn.taketoday.framework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.framework.server.JettyServer;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author TODAY 2021/3/30 23:47
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Import(JettyConfig.class)
public @interface EnableJettyHandling {

}

class JettyConfig {

  @MissingBean
  @Props(prefix = { "server.", "server.jetty." })
  JettyServer jettyServer() {
    return new JettyServer();
  }

}
