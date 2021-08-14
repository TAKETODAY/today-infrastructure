package cn.taketoday.framework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.beans.Import;
import cn.taketoday.beans.MissingBean;
import cn.taketoday.context.Props;
import cn.taketoday.framework.server.UndertowServer;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author TODAY 2021/3/30 23:47
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Import(UndertowConfig.class)
public @interface EnableUndertowHandling {

}

class UndertowConfig {

  @MissingBean
  @Props(prefix = { "server.", "server.undertow." })
  UndertowServer undertowServer() {
    return new UndertowServer();
  }

}
