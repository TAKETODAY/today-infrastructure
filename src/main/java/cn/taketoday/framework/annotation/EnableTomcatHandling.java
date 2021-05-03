package cn.taketoday.framework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.framework.server.TomcatServer;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author TODAY 2021/3/30 23:45
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Import(TomcatConfig.class)
public @interface EnableTomcatHandling {

}

class TomcatConfig {

  @MissingBean
  @Props(prefix = { "server.", "server.tomcat." })
  TomcatServer tomcatServer() {
    return new TomcatServer();
  }
}
