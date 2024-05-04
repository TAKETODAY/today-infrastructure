/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.propertyeditors.CustomDateEditor;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.MatrixParam;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.support.WebBindingInitializer;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.view.AbstractView;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewRef;
import cn.taketoday.web.view.ViewResolver;
import cn.taketoday.web.mock.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class UriTemplateServletAnnotationControllerHandlerMethodTests extends AbstractServletHandlerMethodTests {

  @Test
  void simple() throws Exception {
    initDispatcherServlet(SimpleUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-42-7");
  }

  @Test
    // gh-25864
  void literalMappingWithPathParams() throws Exception {
    initDispatcherServlet(MultipleUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/data");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("test");

    request = new MockHttpServletRequest("GET", "/data;jsessionid=123");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("test");
  }

  @Test
  void multiple() throws Exception {
    initDispatcherServlet(MultipleUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42;q=24/bookings/21-other;q=12");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("test-42-q24-21-other-q12");
  }

  @Test
  void pathVarsInModel() throws Exception {
    final Map<String, Object> pathVars = new HashMap<>();
    pathVars.put("hotel", "42");
    pathVars.put("booking", 21);
    pathVars.put("other", "other");

    WebApplicationContext wac = initDispatcherServlet(ViewRenderingController.class, context -> {
      RootBeanDefinition beanDef = new RootBeanDefinition(ModelValidatingViewResolver.class);
      beanDef.getConstructorArgumentValues().addGenericArgumentValue(pathVars);
      context.registerBeanDefinition("viewResolver", beanDef);
    });

    var request = new MockHttpServletRequest("GET", "/hotels/42;q=1,2/bookings/21-other;q=3;r=R");
    getServlet().service(request, new MockHttpServletResponse());

    ModelValidatingViewResolver resolver = wac.getBean(ModelValidatingViewResolver.class);
    assertThat(resolver.validatedAttrCount).isEqualTo(3);
  }

  @Test
  void binding() throws Exception {
    initDispatcherServlet(BindingUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-11-18");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-foo-bar");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);

    initDispatcherServlet(NonBindingUriTemplateController.class, wac -> {
      wac.registerSingleton((WebBindingInitializer) binder -> {
        // no conversion service
      });
    });
    request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-foo-bar");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(500);
  }

  @Test
  void ambiguous() throws Exception {
    initDispatcherServlet(AmbiguousUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/new");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("specific");
  }

  @Test
  void relative() throws Exception {
    initDispatcherServlet(RelativePathUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/bookings/21");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-42-21");
  }

  @Test
  void extension() throws Exception {
    initDispatcherServlet(SimpleUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42;jsessionid=c0o7fszeb1;q=24.xml");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString())
            .isEqualTo("test-42-24.xml");
  }

  @Test
  void typeConversionError() throws Exception {
    initDispatcherServlet(SimpleUriTemplateController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.xml");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  void explicitSubPath() throws Exception {
    initDispatcherServlet(ExplicitSubPathController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-42");
  }

  @Test
  void implicitSubPath() throws Exception {
    initDispatcherServlet(ImplicitSubPathController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("test-42");
  }

  @Test
  void crud() throws Exception {
    initDispatcherServlet(CrudController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("list");

    request = new MockHttpServletRequest("POST", "/hotels");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("create");

    request = new MockHttpServletRequest("GET", "/hotels/42");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("show-42");

    request = new MockHttpServletRequest("PUT", "/hotels/42");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("createOrUpdate-42");

    request = new MockHttpServletRequest("DELETE", "/hotels/42");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("remove-42");
  }

  @Test
  void methodNotSupported() throws Exception {
    initDispatcherServlet(MethodNotAllowedController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    request = new MockHttpServletRequest("POST", "/hotels/1");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(405);

    request = new MockHttpServletRequest("GET", "/hotels");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    request = new MockHttpServletRequest("POST", "/hotels");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(405);
  }

  @Test
  void multiPaths() throws Exception {
    initDispatcherServlet(MultiPathController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/category/page/5");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("handle4-page-5");
  }

  @Test
  void customRegex() throws Exception {
    initDispatcherServlet(CustomRegexController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42;q=1;q=2");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString())
            .isEqualTo("test-42--[1, 2]");
  }

  @Test
    // gh-11306
  void menuTree() throws Exception {
    initDispatcherServlet(MenuTreeController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/book/menu/type/M5");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("M5");
  }

  @Test
    // gh-11542
  void variableNames() throws Exception {
    initDispatcherServlet(VariableNamesController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/foo");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("foo-foo");

    request = new MockHttpServletRequest("DELETE", "/test/bar");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("bar-bar");
  }

  @Test
    // gh-13187
  void variableNamesWithUrlExtension() throws Exception {
    initDispatcherServlet(VariableNamesController.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/foo.json");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("foo-foo.json");
  }

  @Test
    // gh-11643
  void doIt() throws Exception {
    initDispatcherServlet(Spr6978Controller.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/100");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("loadEntity:foo:100");

    request = new MockHttpServletRequest("POST", "/foo/100");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("publish:foo:100");

    request = new MockHttpServletRequest("GET", "/module/100");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("loadModule:100");

    request = new MockHttpServletRequest("POST", "/module/100");
    response = new MockHttpServletResponse();
    getServlet().service(request, response);
    assertThat(response.getContentAsString()).isEqualTo("publish:module:100");

  }


  /*
   * Controllers
   */

  @Controller
  public static class SimpleUriTemplateController {

    @RequestMapping("/{root}")
    void handle(@PathVariable("root") int root,
            @MatrixParam(required = false, defaultValue = "7") String q, Writer writer) throws IOException {

      assertThat(root).as("Invalid path variable value").isEqualTo(42);
      writer.write("test-" + root + "-" + q);
    }

  }

  @Controller
  public static class MultipleUriTemplateController {

    @RequestMapping("/hotels/{hotel}/bookings/{booking}-{other}")
    void handle(@PathVariable("hotel") String hotel,
            @PathVariable int booking,
            @PathVariable String other,
            @MatrixParam(name = "q", pathVar = "hotel") int qHotel,
            @MatrixParam(name = "q", pathVar = "other") int qOther,
            Writer writer) throws IOException {
      assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
      assertThat(booking).as("Invalid path variable value").isEqualTo(21);
      writer.write("test-" + hotel + "-q" + qHotel + "-" + booking + "-" + other + "-q" + qOther);
    }

    @RequestMapping("/data")
    void handleWithLiteralMapping(Writer writer) throws IOException {
      writer.write("test");
    }
  }

  @Controller
  public static class ViewRenderingController {

    @RequestMapping("/hotels/{hotel}/bookings/{booking}-{other}")
    ViewRef handle(@PathVariable("hotel") String hotel, @PathVariable int booking,
            @PathVariable String other, @MatrixParam MultiValueMap<String, String> params) {

      assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
      assertThat(booking).as("Invalid path variable value").isEqualTo(21);
      assertThat(params.get("q")).containsExactlyInAnyOrder("1", "2", "3");
      assertThat(params.getFirst("r")).isEqualTo("R");
      return ViewRef.forViewName("/text");
    }

  }

  @Controller
  public static class BindingUriTemplateController {

    @InitBinder
    void initBinder(WebDataBinder binder, @PathVariable("hotel") String hotel) {
      assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
      binder.initBeanPropertyAccess();
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      dateFormat.setLenient(false);
      binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }

    @RequestMapping("/hotels/{hotel}/dates/{date}")
    void handle(@PathVariable("hotel") String hotel, @PathVariable Date date, Writer writer)
            throws IOException {
      assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
      assertThat(date).as("Invalid path variable value").isEqualTo(new GregorianCalendar(2008, 10, 18).getTime());
      writer.write("test-" + hotel);
    }
  }

  @Controller
  public static class NonBindingUriTemplateController {

    @RequestMapping("/hotels/{hotel}/dates/{date}")
    void handle(@PathVariable("hotel") String hotel, @PathVariable Date date, Writer writer)
            throws IOException {
    }

  }

  @Controller
  @RequestMapping("/hotels/{hotel}")
  public static class RelativePathUriTemplateController {

    @RequestMapping("bookings/{booking}")
    void handle(@PathVariable("hotel") String hotel, @PathVariable int booking, Writer writer)
            throws IOException {
      assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
      assertThat(booking).as("Invalid path variable value").isEqualTo(21);
      writer.write("test-" + hotel + "-" + booking);
    }

  }

  @Controller
  @RequestMapping("/hotels")
  public static class AmbiguousUriTemplateController {

    @RequestMapping("/{hotel}")
    void handleVars(@PathVariable("hotel") String hotel, Writer writer) throws IOException {
      assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
      writer.write("variables");
    }

    @RequestMapping("/new")
    void handleSpecific(Writer writer) throws IOException {
      writer.write("specific");
    }

    @RequestMapping("/*")
    void handleWildCard(Writer writer) throws IOException {
      writer.write("wildcard");
    }

  }

  @Controller
  @RequestMapping("/hotels/*")
  public static class ExplicitSubPathController {

    @RequestMapping("{hotel}")
    void handleHotel(@PathVariable String hotel, Writer writer) throws IOException {
      writer.write("test-" + hotel);
    }

  }

  @Controller
  @RequestMapping("hotels")
  public static class ImplicitSubPathController {

    @RequestMapping("{hotel}")
    void handleHotel(@PathVariable String hotel, Writer writer) throws IOException {
      writer.write("test-" + hotel);
    }
  }

  @Controller
  public static class CustomRegexController {

    @RequestMapping("/{root:\\d+}{params}")
    void handle(@PathVariable("root") int root, @PathVariable("params") String paramString,
            @MatrixParam List<Integer> q, Writer writer) throws IOException {

      assertThat(root).as("Invalid path variable value").isEqualTo(42);
      writer.write("test-" + root + "-" + paramString + "-" + q);
    }
  }

  @Controller
  public static class DoubleController {

    @RequestMapping("/lat/{latitude}/long/{longitude}")
    void testLatLong(@PathVariable Double latitude, @PathVariable Double longitude, Writer writer)
            throws IOException {
      writer.write("latitude-" + latitude + "-longitude-" + longitude);
    }
  }

  @Controller
  @RequestMapping("hotels")
  public static class CrudController {

    @RequestMapping(method = HttpMethod.GET)
    void list(Writer writer) throws IOException {
      writer.write("list");
    }

    @RequestMapping(method = HttpMethod.POST)
    void create(Writer writer) throws IOException {
      writer.write("create");
    }

    @RequestMapping(value = "/{hotel}", method = HttpMethod.GET)
    void show(@PathVariable String hotel, Writer writer) throws IOException {
      writer.write("show-" + hotel);
    }

    @RequestMapping(value = "{hotel}", method = HttpMethod.PUT)
    void createOrUpdate(@PathVariable String hotel, Writer writer) throws IOException {
      writer.write("createOrUpdate-" + hotel);
    }

    @RequestMapping(value = "{hotel}", method = HttpMethod.DELETE)
    void remove(@PathVariable String hotel, Writer writer) throws IOException {
      writer.write("remove-" + hotel);
    }

  }

  @Controller
  @RequestMapping("/hotels")
  public static class MethodNotAllowedController {

    @RequestMapping(method = HttpMethod.GET)
    void list(Writer writer) {
    }

    @RequestMapping(method = HttpMethod.GET, value = "{hotelId}")
    void show(@PathVariable long hotelId, Writer writer) {
    }

    @RequestMapping(method = HttpMethod.PUT, value = "{hotelId}")
    void createOrUpdate(@PathVariable long hotelId, Writer writer) {
    }

    @RequestMapping(method = HttpMethod.DELETE, value = "/{hotelId}")
    void remove(@PathVariable long hotelId, Writer writer) {
    }
  }

  @Controller
  @RequestMapping("/category")
  public static class MultiPathController {

    @RequestMapping(value = { "/{category}/page/{page}", "/*/{category}/page/{page}" })
    void category(@PathVariable String category, @PathVariable int page, Writer writer) throws IOException {
      writer.write("handle1-");
      writer.write("category-" + category);
      writer.write("page-" + page);
    }

    @RequestMapping(value = { "/{category}", "/*/{category}" })
    void category(@PathVariable String category, Writer writer) throws IOException {
      writer.write("handle2-");
      writer.write("category-" + category);
    }

    @RequestMapping(value = { "" })
    void category(Writer writer) throws IOException {
      writer.write("handle3");
    }

    @RequestMapping(value = { "/page/{page}" })
    void category(@PathVariable int page, Writer writer) throws IOException {
      writer.write("handle4-");
      writer.write("page-" + page);
    }

  }

  @Controller
  @RequestMapping("/*/menu/") // was /*/menu/**
  public static class MenuTreeController {

    @RequestMapping("type/{var}")
    void getFirstLevelFunctionNodes(@PathVariable("var") String var, Writer writer) throws IOException {
      writer.write(var);
    }
  }

  @Controller
  @RequestMapping("/test")
  public static class VariableNamesController {

    @RequestMapping(value = "/{foo}", method = HttpMethod.GET)
    void foo(@PathVariable String foo, Writer writer) throws IOException {
      writer.write("foo-" + foo);
    }

    @RequestMapping(value = "/{bar}", method = HttpMethod.DELETE)
    void bar(@PathVariable String bar, Writer writer) throws IOException {
      writer.write("bar-" + bar);
    }
  }

  @Controller
  public static class Spr6978Controller {

    @RequestMapping(value = "/{type}/{id}", method = HttpMethod.GET)
    void loadEntity(@PathVariable final String type, @PathVariable final long id, Writer writer)
            throws IOException {
      writer.write("loadEntity:" + type + ":" + id);
    }

    @RequestMapping(value = "/module/{id}", method = HttpMethod.GET)
    void loadModule(@PathVariable final long id, Writer writer) throws IOException {
      writer.write("loadModule:" + id);
    }

    @RequestMapping(value = "/{type}/{id}", method = HttpMethod.POST)
    void publish(@PathVariable final String type, @PathVariable final long id, Writer writer)
            throws IOException {
      writer.write("publish:" + type + ":" + id);
    }
  }

  public static class ModelValidatingViewResolver implements ViewResolver {

    private final Map<String, Object> attrsToValidate;

    int validatedAttrCount;

    public ModelValidatingViewResolver(Map<String, Object> attrsToValidate) {
      this.attrsToValidate = attrsToValidate;
    }

    @Override
    public View resolveViewName(final String viewName, Locale locale) throws Exception {
      return new AbstractView() {
        @Override
        public String getContentType() {
          return null;
        }

        @Override
        protected void renderMergedOutputModel(Map<String, Object> model,
                RequestContext request) throws Exception {
          for (String key : attrsToValidate.keySet()) {
            assertThat(model.containsKey(key)).as("Model should contain attribute named " + key).isTrue();
            assertThat(model.get(key)).isEqualTo(attrsToValidate.get(key));
            validatedAttrCount++;
          }
        }
      };
    }
  }

}
