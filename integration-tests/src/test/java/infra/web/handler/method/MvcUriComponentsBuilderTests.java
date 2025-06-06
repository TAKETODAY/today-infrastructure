/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import infra.context.annotation.Bean;
import infra.core.annotation.AliasFor;
import infra.format.annotation.DateTimeFormat;
import infra.http.HttpEntity;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.stereotype.Controller;
import infra.util.MultiValueMap;
import infra.web.RequestContextHolder;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.PathMatchConfigurer;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;
import infra.web.view.ModelAndView;

import static infra.web.handler.method.MvcUriComponentsBuilder.fromController;
import static infra.web.handler.method.MvcUriComponentsBuilder.fromMappingName;
import static infra.web.handler.method.MvcUriComponentsBuilder.fromMethodCall;
import static infra.web.handler.method.MvcUriComponentsBuilder.fromMethodName;
import static infra.web.handler.method.MvcUriComponentsBuilder.on;
import static infra.web.handler.method.MvcUriComponentsBuilder.relativeTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/31 11:34
 */
class MvcUriComponentsBuilderTests {

  private final HttpMockRequestImpl request = new HttpMockRequestImpl();
  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  @BeforeEach
  public void setup() {
    RequestContextHolder.set(new MockRequestContext(
            null, this.request, response));
  }

  @AfterEach
  public void reset() {
    RequestContextHolder.cleanup();
  }

  @Test
  public void fromControllerPlain() {
    UriComponents uriComponents = fromController(PersonControllerImpl.class).build();
    assertThat(uriComponents.toUriString()).endsWith("/people");
  }

  @Test
  public void fromControllerUriTemplate() {
    UriComponents uriComponents = fromController(PersonsAddressesController.class).buildAndExpand(15);
    assertThat(uriComponents.toUriString()).endsWith("/people/15/addresses");
  }

  @Test
  public void fromControllerSubResource() {
    UriComponents uriComponents = fromController(PersonControllerImpl.class).pathSegment("something").build();

    assertThat(uriComponents.toUriString()).endsWith("/people/something");
  }

  @Test
  public void fromControllerTwoTypeLevelMappings() {
    UriComponents uriComponents = fromController(InvalidController.class).build();
    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/persons");
  }

  @Test
  public void fromControllerNotMapped() {
    UriComponents uriComponents = fromController(UnmappedController.class).build();
    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/");
  }

  @Test
  public void fromControllerWithCustomBaseURIViaStaticCall() {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://example.org:9090/base");
    UriComponents uriComponents = fromController(builder, PersonControllerImpl.class).build();

    assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/people");
    assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
  }

  @Test
  public void fromControllerWithCustomBaseURIViaInstance() {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://example.org:9090/base");
    MvcUriComponentsBuilder mvcBuilder = relativeTo(builder);
    UriComponents uriComponents = mvcBuilder.withController(PersonControllerImpl.class).build();

    assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/people");
    assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
  }

  @Test
  public void fromMethodNamePathVariable() {
    UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
            "methodWithPathVariable", "1").build();

    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/something/1/foo");
  }

  @Test
  public void fromMethodNameTypeLevelPathVariable() {
    UriComponents uriComponents = fromMethodName(
            PersonsAddressesController.class, "getAddressesForCountry", "DE").buildAndExpand("1");

    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/people/1/addresses/DE");
  }

  @Test
  public void fromMethodNameTwoPathVariables() {
    UriComponents uriComponents = fromMethodName(
            ControllerWithMethods.class, "methodWithTwoPathVariables", 1, "2009-10-31").build();

    assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo/2009-10-31");
  }

  @Test
  public void fromMethodNameWithPathVarAndRequestParam() {
    UriComponents uriComponents = fromMethodName(
            ControllerWithMethods.class, "methodForNextPage", "1", 10, 5).build();

    assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo");
    MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
    assertThat(queryParams.get("limit")).contains("5");
    assertThat(queryParams.get("offset")).contains("10");
  }

  @Test
  public void fromMethodNameWithBridgedMethod() {
    UriComponents uriComponents = fromMethodName(PersonCrudController.class, "get", (long) 42).build();

    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/42");
  }

  @Test
  public void fromMethodNameTypeLevelPathVariableWithoutArgumentValue() {
    UriComponents uriComponents = fromMethodName(UserContactController.class, "showCreate", 123).build();

    assertThat(uriComponents.getPath()).isEqualTo("/user/123/contacts/create");
  }

  @Test
  public void fromMethodNameInUnmappedController() {
    UriComponents uriComponents = fromMethodName(UnmappedController.class, "requestMappingMethod").build();

    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/");
  }

  @Test  // gh-29897
  public void fromMethodNameInUnmappedControllerMethod() {
    UriComponents uriComponents = fromMethodName(UnmappedControllerMethod.class, "getMethod").build();

    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/path");
  }

  @Test
  public void fromMethodNameWithCustomBaseURIViaStaticCall() {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://example.org:9090/base");
    UriComponents uriComponents = fromMethodName(builder, ControllerWithMethods.class,
            "methodWithPathVariable", "1").build();

    assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/something/1/foo");
    assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
  }

  @Test
  public void fromMethodNameWithCustomBaseURIViaInstance() {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://example.org:9090/base");
    MvcUriComponentsBuilder mvcBuilder = relativeTo(builder);
    UriComponents uriComponents = mvcBuilder.withMethodName(ControllerWithMethods.class,
            "methodWithPathVariable", "1").build();

    assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/something/1/foo");
    assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
  }

  @Test
  public void fromMethodNameWithOptionalParam() {
    UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
            "methodWithOptionalParam", new Object[] { null }).build();

    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/something/optional-param");
  }

  @Test  // gh-22656
  public void fromMethodNameWithOptionalNamedParam() {
    UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
            "methodWithOptionalNamedParam", Optional.of("foo")).build();

    assertThat(uriComponents.toUriString())
            .isEqualTo("http://localhost/something/optional-param-with-name?search=foo");
  }

  @Test
  public void fromMethodNameWithMetaAnnotation() {
    UriComponents uriComponents = fromMethodName(MetaAnnotationController.class, "handleInput").build();

    assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/input");
  }

  @Test
  public void fromMethodCallOnSubclass() {
    UriComponents uriComponents = fromMethodCall(on(ExtendedController.class).myMethod(null)).build();

    assertThat(uriComponents.toUriString()).startsWith("http://localhost");
    assertThat(uriComponents.toUriString()).endsWith("/extended/else");
  }

  @Test
  public void fromMethodCallPlain() {
    UriComponents uriComponents = fromMethodCall(on(ControllerWithMethods.class).myMethod(null)).build();

    assertThat(uriComponents.toUriString()).startsWith("http://localhost");
    assertThat(uriComponents.toUriString()).endsWith("/something/else");
  }

  @Test
  public void fromMethodCallPlainWithNoArguments() {
    UriComponents uriComponents = fromMethodCall(on(ControllerWithMethods.class).myMethod()).build();

    assertThat(uriComponents.toUriString()).startsWith("http://localhost");
    assertThat(uriComponents.toUriString()).endsWith("/something/noarg");
  }

  @Test
  public void fromMethodCallPlainOnInterface() {
    UriComponents uriComponents = fromMethodCall(on(ControllerInterface.class).myMethod(null)).build();

    assertThat(uriComponents.toUriString()).startsWith("http://localhost");
    assertThat(uriComponents.toUriString()).endsWith("/something/else");
  }

  @Test
  public void fromMethodCallPlainWithNoArgumentsOnInterface() {
    UriComponents uriComponents = fromMethodCall(on(ControllerInterface.class).myMethod()).build();

    assertThat(uriComponents.toUriString()).startsWith("http://localhost");
    assertThat(uriComponents.toUriString()).endsWith("/something/noarg");
  }

  @Test
  public void fromMethodCallWithTypeLevelUriVars() {
    UriComponents uriComponents = fromMethodCall(
            on(PersonsAddressesController.class).getAddressesForCountry("DE")).buildAndExpand(15);

    assertThat(uriComponents.toUriString()).endsWith("/people/15/addresses/DE");
  }

  @Test
  public void fromMethodCallWithPathVariable() {
    UriComponents uriComponents = fromMethodCall(
            on(ControllerWithMethods.class).methodWithPathVariable("1")).build();

    assertThat(uriComponents.toUriString()).startsWith("http://localhost");
    assertThat(uriComponents.toUriString()).endsWith("/something/1/foo");
  }

  @Test
  public void fromMethodCallWithPathVariableAndRequestParams() {
    UriComponents uriComponents = fromMethodCall(
            on(ControllerWithMethods.class).methodForNextPage("1", 10, 5)).build();

    assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo");

    MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
    assertThat(queryParams.get("limit")).contains("5");
    assertThat(queryParams.get("offset")).contains("10");
  }

  @Test
  public void fromMethodCallWithPathVariableAndMultiValueRequestParams() {
    UriComponents uriComponents = fromMethodCall(
            on(ControllerWithMethods.class).methodWithMultiValueRequestParams("1", Arrays.asList(3, 7), 5)).build();

    assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo");

    MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
    assertThat(queryParams.get("limit")).contains("5");
    assertThat(queryParams.get("items")).containsExactly("3", "7");
  }

  @Test
  public void fromMethodCallWithCustomBaseURIViaStaticCall() {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://example.org:9090/base");
    UriComponents uriComponents = fromMethodCall(builder, on(ControllerWithMethods.class).myMethod(null)).build();

    assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/something/else");
    assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
  }

  @Test
  public void fromMethodCallWithCustomBaseURIViaInstance() {
    UriComponentsBuilder builder = UriComponentsBuilder.forURIString("https://example.org:9090/base");
    MvcUriComponentsBuilder mvcBuilder = relativeTo(builder);
    UriComponents result = mvcBuilder.withMethodCall(on(ControllerWithMethods.class).myMethod(null)).build();

    assertThat(result.toString()).isEqualTo("https://example.org:9090/base/something/else");
    assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
  }

  @Test
  public void fromMethodCallWithModelAndViewReturnType() {
    UriComponents uriComponents = fromMethodCall(
            on(BookingControllerWithModelAndView.class).getBooking(21L)).buildAndExpand(42);

    assertThat(uriComponents.encode().toURI().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
  }

  @Test
  public void fromMethodCallWithObjectReturnType() {
    UriComponents uriComponents = fromMethodCall(
            on(BookingControllerWithObject.class).getBooking(21L)).buildAndExpand(42);

    assertThat(uriComponents.encode().toURI().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
  }

  @Test
  public void fromMethodCallWithStringReturnType() {
    assertThatIllegalStateException().isThrownBy(() -> {
      UriComponents uriComponents = fromMethodCall(
              on(BookingControllerWithString.class).getBooking(21L)).buildAndExpand(42);
      uriComponents.encode().toURI().toString();
    });
  }

  @Test
  public void fromMethodNameWithStringReturnType() {
    UriComponents uriComponents = fromMethodName(
            BookingControllerWithString.class, "getBooking", 21L).buildAndExpand(42);

    assertThat(uriComponents.encode().toURI().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
  }

  @Test  // gh-30210
  public void fromMethodCallWithCharSequenceReturnType() {
    UriComponents uriComponents = fromMethodCall(
            on(BookingControllerWithCharSequence.class).getBooking(21L))
            .buildAndExpand(42);

    assertThat(uriComponents.encode().toURI().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
  }

  @Test  // gh-30210
  public void fromMethodCallWithJdbc30115ReturnType() {
    UriComponents uriComponents = fromMethodCall(
            on(BookingControllerWithJdbcSavepoint.class).getBooking(21L)).buildAndExpand(42);

    assertThat(uriComponents.encode().toURI().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
  }

  @Test
  public void fromMappingNamePlain() {
    initWebApplicationContext(WebConfig.class);

    this.request.setServerName("example.org");
    this.request.setServerPort(9999);

    String mappingName = "PAC#getAddressesForCountry";
    String url = fromMappingName(mappingName).arg(0, "DE").buildAndExpand(123);
    assertThat(url).isEqualTo("/people/123/addresses/DE");
  }

  @Test
  public void fromMappingNameWithCustomBaseURI() {
    initWebApplicationContext(WebConfig.class);

    UriComponentsBuilder baseUrl = UriComponentsBuilder.forURIString("https://example.org:9999/base");
    MvcUriComponentsBuilder mvcBuilder = relativeTo(baseUrl);
    String url = mvcBuilder.withMappingName("PAC#getAddressesForCountry").arg(0, "DE").buildAndExpand(123);
    assertThat(url).isEqualTo("https://example.org:9999/base/people/123/addresses/DE");
  }

  @Test
  public void fromMappingNameWithEncoding() {
    initWebApplicationContext(WebConfig.class);

    this.request.setServerName("example.org");
    this.request.setServerPort(9999);

    String mappingName = "PAC#getAddressesForCountry";
    String url = fromMappingName(mappingName).arg(0, "DE;FR").encode().buildAndExpand("_+_");
    assertThat(url).isEqualTo("/people/_%2B_/addresses/DE%3BFR");
  }

  @Test
  public void fromMappingNameWithPathWithoutLeadingSlash() {
    initWebApplicationContext(PathWithoutLeadingSlashConfig.class);

    this.request.setServerName("example.org");
    this.request.setServerPort(9999);

    String mappingName = "PWLSC#getAddressesForCountry";
    String url = fromMappingName(mappingName).arg(0, "DE;FR").encode().buildAndExpand("_+_");
    assertThat(url).isEqualTo("/people/DE%3BFR");
  }

  @Test
  public void fromControllerWithPrefix() {
    initWebApplicationContext(PathPrefixWebConfig.class);

    this.request.setScheme("https");
    this.request.setServerName("example.org");
    this.request.setServerPort(9999);

    assertThat(fromController(PersonsAddressesController.class).buildAndExpand("123").toString())
            .isEqualTo("https://example.org:9999/api/people/123/addresses");
  }

  @Test
  public void fromMethodWithPrefix() {
    initWebApplicationContext(PathPrefixWebConfig.class);

    this.request.setScheme("https");
    this.request.setServerName("example.org");
    this.request.setServerPort(9999);

    String url = fromMethodCall(on(PersonsAddressesController.class)
            .getAddressesForCountry("DE"))
            .buildAndExpand("123")
            .toString();

    assertThat(url).isEqualTo("https://example.org:9999/api/people/123/addresses/DE");
  }

  private void initWebApplicationContext(Class<?> configClass) {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setMockContext(new MockContextImpl());
    context.register(configClass);
    context.refresh();
    RequestContextHolder.set(new MockRequestContext(context, request, new MockHttpResponseImpl()));
  }

  static class Person {

    Long id;

    public Long getId() {
      return id;
    }
  }

  @RequestMapping("/people")
  interface PersonController {
  }

  static class PersonControllerImpl implements PersonController {
  }

  @Controller
  @RequestMapping("/people/{id}/addresses")
  static class PersonsAddressesController {

    @RequestMapping("/{country}")
    HttpEntity<Void> getAddressesForCountry(@PathVariable String country) {
      return null;
    }
  }

  @Controller
  @RequestMapping({ "people" })
  static class PathWithoutLeadingSlashController {

    @RequestMapping("/{country}")
    HttpEntity<Void> getAddressesForCountry(@PathVariable String country) {
      return null;
    }
  }

  @RequestMapping({ "/persons", "/people" })
  private class InvalidController {
  }

  private class UnmappedController {

    @RequestMapping
    public void requestMappingMethod() {
    }
  }

  @RequestMapping("/path")
  private class UnmappedControllerMethod {

    @GetMapping
    public void getMethod() {
    }
  }

  @RequestMapping("/something")
  static class ControllerWithMethods {

    @RequestMapping("/else")
    HttpEntity<Void> myMethod(@RequestBody Object payload) {
      return null;
    }

    @RequestMapping("/noarg")
    HttpEntity<Void> myMethod() {
      return null;
    }

    @RequestMapping("/{id}/foo")
    HttpEntity<Void> methodWithPathVariable(@PathVariable String id) {
      return null;
    }

    @RequestMapping("/{id}/foo/{date}")
    HttpEntity<Void> methodWithTwoPathVariables(
            @PathVariable Integer id, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PathVariable Date date) {
      return null;
    }

    @RequestMapping(value = "/{id}/foo")
    HttpEntity<Void> methodForNextPage(@PathVariable String id,
            @RequestParam Integer offset, @RequestParam Integer limit) {
      return null;
    }

    @RequestMapping(value = "/{id}/foo")
    HttpEntity<Void> methodWithMultiValueRequestParams(@PathVariable String id,
            @RequestParam List<Integer> items, @RequestParam Integer limit) {
      return null;
    }

    @RequestMapping("/optional-param")
    HttpEntity<Void> methodWithOptionalParam(@RequestParam(defaultValue = "") String q) {
      return null;
    }

    @GetMapping("/optional-param-with-name")
    HttpEntity<Void> methodWithOptionalNamedParam(@RequestParam("search") Optional<String> q) {
      return null;
    }
  }

  @RequestMapping("/extended")
  @SuppressWarnings("WeakerAccess")
  static class ExtendedController extends ControllerWithMethods {
  }

  @RequestMapping("/something")
  public interface ControllerInterface {

    @RequestMapping("/else")
    HttpEntity<Void> myMethod(@RequestBody Object payload);

    @RequestMapping("/noarg")
    HttpEntity<Void> myMethod();
  }

  @RequestMapping("/user/{userId}/contacts")
  static class UserContactController {

    @RequestMapping("/create")
    public String showCreate(@PathVariable Integer userId) {
      return null;
    }
  }

  static abstract class AbstractCrudController<T, ID> {

    abstract T get(ID id);
  }

  static class PersonCrudController extends AbstractCrudController<Person, Long> {

    @Override
    @RequestMapping(path = "/{id}", method = HttpMethod.GET)
    public Person get(@PathVariable Long id) {
      return new Person();
    }
  }

  @Controller
  static class MetaAnnotationController {

    @RequestMapping
    public void handle() {
    }

    @PostJson(path = "/input")
    public void handleInput() {
    }
  }

  @RequestMapping(method = HttpMethod.POST,
          produces = MediaType.APPLICATION_JSON_VALUE,
          consumes = MediaType.APPLICATION_JSON_VALUE)
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  private @interface PostJson {

    @AliasFor(annotation = RequestMapping.class)
    String[] path() default {};
  }

  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Bean
    public PersonsAddressesController controller() {
      return new PersonsAddressesController();
    }
  }

  @EnableWebMvc
  static class PathWithoutLeadingSlashConfig implements WebMvcConfigurer {

    @Bean
    public PathWithoutLeadingSlashController controller() {
      return new PathWithoutLeadingSlashController();
    }
  }

  @EnableWebMvc
  static class PathPrefixWebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
      configurer.addPathPrefix("/api", PersonsAddressesController.class::equals);
    }

    @Bean
    public PersonsAddressesController controller() {
      return new PersonsAddressesController();
    }
  }

  @Controller
  @RequestMapping("/hotels/{hotel}")
  static class BookingControllerWithModelAndView {

    @GetMapping("/bookings/{booking}")
    public ModelAndView getBooking(@PathVariable Long booking) {
      return new ModelAndView("url");
    }
  }

  @Controller
  @RequestMapping("/hotels/{hotel}")
  static class BookingControllerWithObject {

    @GetMapping("/bookings/{booking}")
    public Object getBooking(@PathVariable Long booking) {
      return "url";
    }
  }

  @Controller
  @RequestMapping("/hotels/{hotel}")
  static class BookingControllerWithString {

    @GetMapping("/bookings/{booking}")
    public String getBooking(@PathVariable Long booking) {
      return "url";
    }
  }

  @Controller
  @RequestMapping("/hotels/{hotel}")
  static class BookingControllerWithCharSequence {

    @GetMapping("/bookings/{booking}")
    public CharSequence getBooking(@PathVariable Long booking) {
      return "url";
    }
  }

  @Controller
  @RequestMapping("/hotels/{hotel}")
  static class BookingControllerWithJdbcSavepoint {

    @GetMapping("/bookings/{booking}")
    public Savepoint getBooking(@PathVariable Long booking) {
      return null;
    }
  }

}