package cn.taketoday.framework.reactive.vertx;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.framework.server.WebServer;
import cn.taketoday.web.session.EnableWebSession;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enable Netty, Enable {@link cn.taketoday.web.session.WebSession}
 *
 * @author TODAY 2021/5/23 22:06
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@EnableWebSession
@Import(VertxConfig.class)
public @interface EnableVertxHandling {

}

class VertxConfig {

  @MissingBean
  Vertx vertx(VertxOptions options) {
    return Vertx.vertx(options);
  }

  @MissingBean
  VertxOptions vertxOptions() {
    return new VertxOptions();
  }

  @MissingBean(type = WebServer.class)
  VertxWebServer vertxWebServer() {
    return new VertxWebServer();
  }
}
