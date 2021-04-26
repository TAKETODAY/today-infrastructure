package test.framework.registry;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.event.EnableMethodEventDriven;
import cn.taketoday.context.utils.MediaType;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.annotation.EnableTomcatHandling;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.registry.EnableRequestPathMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY 2021/4/22 22:32
 */
@Slf4j
@RestController
@RestControllerAdvice
@EnableRequestPathMapping
@RequestMapping("users")
@Import(RequestPathMappingHandlerMethodRegistryApp.AppConfig.class)
public class RequestPathMappingHandlerMethodRegistryApp {

  public static void main(String[] args) {
    WebApplication.run(RequestPathMappingHandlerMethodRegistryApp.class, args);
  }

  @GET("/index")
  public String index() {
    return "Hello";
  }

  @GET
  public String hello() {
    return "Hello";
  }

  @GET(params = "name=TODAY")
  public String today() {
    return "TODAY";
  }

  @GET(params = "name")
  public String params() {
    return "params";
  }

  @GET(params = "name", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Body paramsConsumes(@RequestBody Body body) {
    return body;
  }

  @GET("/header")
  public HttpHeaders header(HttpHeaders headers) {
    return headers;
  }

  @GET("/body/{name}/{age}")
  public Body index(String name, int age) {
    return new Body(name, age);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class Body {
    String name;
    int age;
  }

  @Configuration
  @EnableTomcatHandling
  @EnableMethodEventDriven
  static class AppConfig {

  }

}


