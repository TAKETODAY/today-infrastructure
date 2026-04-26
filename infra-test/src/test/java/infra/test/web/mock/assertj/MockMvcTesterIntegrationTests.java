/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.assertj;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.core.io.ClassPathResource;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.mock.api.http.Cookie;
import infra.mock.web.MockMemoryFilePart;
import infra.mock.web.MockMemoryPart;
import infra.session.Session;
import infra.session.config.EnableSession;
import infra.stereotype.Controller;
import infra.test.context.junit.jupiter.DisabledOnMac;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.test.web.Person;
import infra.test.web.mock.ResultMatcher;
import infra.test.web.mock.assertj.MockMvcTester.MockMultipartMvcRequestBuilder;
import infra.test.web.mock.setup.InternalResourceViewResolver;
import infra.ui.Model;
import infra.validation.Errors;
import infra.web.HandlerInterceptor;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.PostMapping;
import infra.web.annotation.PutMapping;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.bind.resolver.MissingRequestPartException;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.handler.StreamingResponseBody;
import infra.web.multipart.Part;
import infra.web.server.ResponseStatusException;
import infra.web.view.ModelAndView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Integration tests for {@link MockMvcTester}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@JUnitWebConfig
public class MockMvcTesterIntegrationTests {

  private static final MockMemoryFilePart file = new MockMemoryFilePart("file", "content.txt", null,
          "value".getBytes(StandardCharsets.UTF_8));

  private final MockMvcTester mvc;

  MockMvcTesterIntegrationTests(ApplicationContext wac) {
    this.mvc = MockMvcTester.from(wac);
  }

  @Nested
  class PerformTests {

    @Test
    void syncRequestWithDefaultExchange() {
      assertThat(mvc.get().uri("/greet")).hasStatusOk();
    }

    @Test
    void asyncRequestWithDefaultExchange() {
      assertThat(mvc.get().uri("/streaming").param("timeToWait", "100")).hasStatusOk()
              .hasBodyTextEqualTo("name=Joe&someBoolean=true");
    }

    @Test
    void asyncMultipartRequestWithDefaultExchange() {
      assertThat(mvc.post().uri("/multipart-streaming").multipart()
              .file(file).param("timeToWait", "100"))
              .hasStatusOk().hasBodyTextEqualTo("name=Joe&file=content.txt");
    }

    @Test
    void syncRequestWithExplicitExchange() {
      assertThat(mvc.get().uri("/greet").exchange()).hasStatusOk();
    }

    @Test
    void asyncRequestWithExplicitExchange() {
      assertThat(mvc.get().uri("/streaming").param("timeToWait", "100").exchange())
              .hasStatusOk().hasBodyTextEqualTo("name=Joe&someBoolean=true");
    }

    @Test
    void asyncMultipartRequestWitExplicitExchange() {
      assertThat(mvc.post().uri("/multipart-streaming").multipart()
              .file(file).param("timeToWait", "100").exchange())
              .hasStatusOk().hasBodyTextEqualTo("name=Joe&file=content.txt");
    }

    @Test
    void syncRequestWithExplicitExchangeIgnoresDuration() {
      Duration timeToWait = mock(Duration.class);
      assertThat(mvc.get().uri("/greet").exchange(timeToWait)).hasStatusOk();
      verifyNoInteractions(timeToWait);
    }

    @Test
    @DisabledOnMac
    void asyncRequestWithExplicitExchangeAndEnoughTimeToWait() {
      assertThat(mvc.get().uri("/streaming").param("timeToWait", "100").exchange(Duration.ofMillis(200)))
              .hasStatusOk().hasBodyTextEqualTo("name=Joe&someBoolean=true");
    }

    @Test
    @DisabledOnMac
    void asyncMultipartRequestWithExplicitExchangeAndEnoughTimeToWait() {
      assertThat(mvc.post().uri("/multipart-streaming").multipart()
              .file(file).param("timeToWait", "100").exchange(Duration.ofMillis(200)))
              .hasStatusOk().hasBodyTextEqualTo("name=Joe&file=content.txt");
    }

    @Test
    void asyncRequestWithExplicitExchangeAndNotEnoughTimeToWait() {
      MockMvcTester.MockMvcRequestBuilder builder = mvc.get().uri("/streaming").param("timeToWait", "500");
      assertThatIllegalStateException()
              .isThrownBy(() -> builder.exchange(Duration.ofMillis(100)))
              .withMessageContaining("was not set during the specified timeToWait=100");
    }

    @Test
    void asyncMultipartRequestWithExplicitExchangeAndNotEnoughTimeToWait() {
      MockMultipartMvcRequestBuilder builder = mvc.post().uri("/multipart-streaming").multipart()
              .file(file).param("timeToWait", "500");
      assertThatIllegalStateException()
              .isThrownBy(() -> builder.exchange(Duration.ofMillis(100)))
              .withMessageContaining("was not set during the specified timeToWait=100");
    }
  }

  @Nested
  class RequestTests {

    @Test
    void hasAsyncStartedTrue() {
      assertThat(mvc.get().uri("/callable").accept(MediaType.APPLICATION_JSON).asyncExchange())
              .request().hasAsyncStarted(true);
    }

    @Test
    void hasAsyncStartedForMultipartTrue() {
      assertThat(mvc.post().uri("/multipart-streaming").multipart()
              .file(file).param("timeToWait", "100").asyncExchange())
              .request().hasAsyncStarted(true);
    }

    @Test
    void hasAsyncStartedFalse() {
      assertThat(mvc.get().uri("/greet").asyncExchange()).request().hasAsyncStarted(false);
    }

    @Test
    void hasAsyncStartedForMultipartFalse() {
      assertThat(mvc.put().uri("/multipart-put").multipart().file(file).asyncExchange())
              .request().hasAsyncStarted(false);
    }

    @Test
    void attributes() {
      assertThat(mvc.get().uri("/greet")).request().attributes()
              .containsKey(RedirectModel.INPUT_ATTRIBUTE);
    }

    @Test
    void sessionAttributes() {
      assertThat(mvc.get().uri("/locale")).request().sessionAttributes()
              .containsOnly(entry("locale", Locale.UK));
    }
  }

  @Nested
  class MultipartTests {

    private final MockMemoryFilePart JSON_PART_FILE = new MockMemoryFilePart("json", "json", "application/json", """
            {
            	"name": "test"
            }""".getBytes(StandardCharsets.UTF_8));

    @Test
    void multipartSetsContentType() {
      assertThat(mvc.put().uri("/multipart-put").multipart().file(file).file(JSON_PART_FILE))
              .request().satisfies(request -> assertThat(request.getContentType())
                      .isEqualTo(MediaType.MULTIPART_FORM_DATA_VALUE));
    }

    @Test
    void multipartWithPut() {
      assertThat(mvc.put().uri("/multipart-put").multipart().file(file).file(JSON_PART_FILE))
              .hasStatusOk()
              .hasViewName("index")
              .model().contains(entry("name", "file"));
    }

    @Test
    void multipartWithMissingPart() {
      assertThat(mvc.put().uri("/multipart-put").multipart().file(JSON_PART_FILE))
              .hasStatus(HttpStatus.BAD_REQUEST)
              .failure().isInstanceOfSatisfying(MissingRequestPartException.class,
                      ex -> assertThat(ex.getRequestPartName()).isEqualTo("file"));
    }

    @Test
    void multipartWithNamedPart() {
      MockMemoryPart part = new MockMemoryPart("part", "content.txt", "value".getBytes(StandardCharsets.UTF_8));
      assertThat(mvc.post().uri("/part").multipart().part(part).file(JSON_PART_FILE))
              .hasStatusOk()
              .hasViewName("index")
              .model().contains(entry("part", "content.txt"), entry("name", "test"));
    }
  }

  @Nested
  class CookieTests {

    @Test
    void containsCookie() {
      Cookie cookie = new Cookie("test", "value");
      assertThat(withCookie(cookie).get().uri("/greet")).cookies().containsCookie("test");
    }

    @Test
    void hasValue() {
      Cookie cookie = new Cookie("test", "value");
      assertThat(withCookie(cookie).get().uri("/greet")).cookies().hasValue("test", "value");
    }

    private MockMvcTester withCookie(Cookie cookie) {
      return MockMvcTester.of(List.of(new TestController()), builder -> builder.addInterceptors(
              new HandlerInterceptor() {

                @Override
                public boolean preProcessing(RequestContext request, Object handler) throws Throwable {
                  request.addCookie(ResponseCookie.forSimple(cookie.getName(), cookie.getValue()));
                  return true;
                }
              }).build());
    }
  }

  @Nested
  class StatusTests {

    @Test
    void statusOk() {
      assertThat(mvc.get().uri("/greet")).hasStatusOk();
    }

    @Test
    void statusSeries() {
      assertThat(mvc.get().uri("/greet")).hasStatus2xxSuccessful();
    }
  }

  @Nested
  class HeadersTests {

    @Test
    void shouldAssertHeader() {
      assertThat(mvc.get().uri("/greet"))
              .hasHeader("Content-Type", "text/plain;charset=UTF-8");
    }

    @Test
    void shouldAssertHeaderWithCallback() {
      assertThat(mvc.get().uri("/greet")).headers().satisfies(textContent("UTF-8"));
    }

    private Consumer<HttpHeaders> textContent(String charset) {
      return headers -> assertThat(headers.hasValues("Content-Type", List.of("text/plain;charset=%s".formatted(charset))))
              .as("hasHeaderValues").isTrue();
    }
  }

  @Nested
  class ModelAndViewTests {

    @Test
    void hasViewName() {
      assertThat(mvc.get().uri("/persons/{0}", "Andy")).hasViewName("persons/index");
    }

    @Test
    void viewNameWithCustomAssertion() {
      assertThat(mvc.get().uri("/persons/{0}", "Andy")).viewName().startsWith("persons");
    }

    @Test
    void containsAttributes() {
      assertThat(mvc.post().uri("/persons").param("name", "Andy")).model()
              .containsKey("name").containsEntry("name", "Andy");
    }

    @Test
    void hasErrors() {
      assertThat(mvc.post().uri("/persons")).model().hasErrors();
    }

    @Test
    void hasAttributeErrors() {
      assertThat(mvc.post().uri("/persons")).model().hasAttributeErrors("person");
    }

    @Test
    void hasAttributeErrorsCount() {
      assertThat(mvc.post().uri("/persons")).model().extractingBindingResult("person").hasErrorsCount(1);
    }
  }

  @Nested
  class FlashTests {

    @Test
    void containsAttributes() {
      assertThat(mvc.post().uri("/persons").param("name", "Andy")).flash()
              .containsOnlyKeys("message").hasEntrySatisfying("message",
                      value -> assertThat(value).isInstanceOfSatisfying(String.class,
                              stringValue -> assertThat(stringValue).startsWith("success")));
    }
  }

  @Nested
  class BodyTests {

    @Test
    void asyncResult() {
      MvcTestResult result = mvc.get().uri("/callable").accept(MediaType.APPLICATION_JSON).asyncExchange();
      assertThat(result.getMvcResult().getAsyncResult())
              .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
              .containsOnly(entry("key", "value"));
    }

    @Test
    void stringContent() {
      assertThat(mvc.get().uri("/greet")).body().asString().isEqualTo("hello");
    }

    @Test
    void jsonPathContent() {
      assertThat(mvc.get().uri("/message")).bodyJson()
              .extractingPath("$.message").asString().isEqualTo("hello");
    }

    @Test
    void jsonContentCanLoadResourceFromClasspath() {
      assertThat(mvc.get().uri("/message")).bodyJson().isLenientlyEqualTo(
              new ClassPathResource("message.json", MockMvcTesterIntegrationTests.class));
    }

    @Test
    void jsonContentUsingResourceLoaderClass() {
      assertThat(mvc.get().uri("/message")).bodyJson().withResourceLoadClass(MockMvcTesterIntegrationTests.class)
              .isLenientlyEqualTo("message.json");
    }
  }

  @Nested
  class HandlerTests {

    @Test
    void handlerOn404() {
      assertThat(mvc.get().uri("/unknown-resource")).handler().isNull();
    }

    @Test
    void hasType() {
      assertThat(mvc.get().uri("/greet")).handler().hasType(TestController.class);
    }

    @Test
    void isMethodHandler() {
      assertThat(mvc.get().uri("/greet")).handler().isMethodHandler();
    }

    @Test
    @Disabled
    void isInvokedOn() {
      assertThat(mvc.get().uri("/callable")).handler()
              .isInvokedOn(AsyncController.class, AsyncController::getCallable);
    }
  }

  @Nested
  class DebugTests {

    private final PrintStream standardOut = System.out;

    private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
      System.setOut(new PrintStream(capturedOut));
    }

    @AfterEach
    public void tearDown() {
      System.setOut(standardOut);
    }

    @Test
    void debugUsesSystemOutByDefault() {
      assertThat(mvc.get().uri("/greet")).debug().hasStatusOk();
      assertThat(capturedOut()).contains("HttpRequest:", "HttpResponse:");
    }

    @Test
    void debugCanPrintToCustomOutputStream() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      assertThat(mvc.get().uri("/greet")).debug(out).hasStatusOk();
      assertThat(out.toString(StandardCharsets.UTF_8))
              .contains("HttpRequest:", "HttpResponse:");
      assertThat(capturedOut()).isEmpty();
    }

    @Test
    void debugCanPrintToCustomWriter() {
      StringWriter out = new StringWriter();
      assertThat(mvc.get().uri("/greet")).debug(out).hasStatusOk();
      assertThat(out.toString())
              .contains("HttpRequest:", "HttpResponse:");
      assertThat(capturedOut()).isEmpty();
    }

    private String capturedOut() {
      return this.capturedOut.toString(StandardCharsets.UTF_8);
    }

  }

  @Nested
  class ExceptionTests {

    @Test
    void hasFailedWithUnresolvedException() {
      assertThat(mvc.get().uri("/error/1")).hasFailed();
    }

    @Test
    void hasFailedWithResolvedException() {
      assertThat(mvc.get().uri("/error/2")).hasFailed().hasStatus(HttpStatus.PAYMENT_REQUIRED);
    }

    @Test
    void doesNotHaveFailedWithoutException() {
      assertThat(mvc.get().uri("/greet")).doesNotHaveFailed();
    }

    @Test
    void doesNotHaveFailedWithUnresolvedException() {
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertThat(mvc.get().uri("/error/1")).doesNotHaveFailed())
              .withMessage("Expected request to succeed, but it failed");
    }

    @Test
    void doesNotHaveFailedWithResolvedException() {
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertThat(mvc.get().uri("/error/2")).doesNotHaveFailed())
              .withMessage("Expected request to succeed, but it failed");
    }

    @Test
    void hasFailedWithoutException() {
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertThat(mvc.get().uri("/greet")).hasFailed())
              .withMessage("Expected request to fail, but it succeeded");
    }

    @Test
    void failureWithUnresolvedException() {
      assertThat(mvc.get().uri("/error/1")).failure()
              .isInstanceOf(IllegalStateException.class).hasMessage("Expected");
    }

    @Test
    void failureWithResolvedException() {
      assertThat(mvc.get().uri("/error/2")).failure()
              .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                      assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));
    }

    @Test
    void failureWithoutException() {
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertThat(mvc.get().uri("/greet")).failure())
              .withMessage("Expected request to fail, but it succeeded");
    }

    // Check that assertions fail immediately if request failed with unresolved exception

    @Test
    void assertAndApplyWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).apply(mvcResult -> { }));
    }

    @Test
    void assertContentTypeWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).contentType());
    }

    @Test
    void assertCookiesWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).cookies());
    }

    @Test
    void assertFlashWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).flash());
    }

    @Test
    void assertStatusWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).hasStatus(3));
    }

    @Test
    void assertHeaderWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).headers());
    }

    @Test
    void assertViewNameWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).hasViewName("test"));
    }

    @Test
    void assertForwardedUrlWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).hasForwardedUrl("test"));
    }

    @Test
    void assertRedirectedUrlWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).hasRedirectedUrl("test"));
    }

    @Test
    void assertErrorMessageWithUnresolvedException() {
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertThat(mvc.get().uri("/error/message")).hasErrorMessage("invalid"))
              .withMessageContainingAll("[Servlet error message]", "invalid", "expected error message");
    }

    @Test
    void assertRequestWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).request());
    }

    @Test
    void assertModelWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).model());
    }

    @Test
    void assertBodyWithUnresolvedException() {
      testAssertionFailureWithUnresolvableException(
              result -> assertThat(result).body());
    }

    private void testAssertionFailureWithUnresolvableException(Consumer<MvcTestResult> assertions) {
      MvcTestResult result = mvc.get().uri("/error/1").exchange();
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertions.accept(result))
              .withMessageContainingAll("Request failed unexpectedly:", IllegalStateException.class.getName(), "Expected");
    }
  }

//  @Test
//  void hasForwardUrl() {
//    assertThat(mvc.get().uri("/persons/John")).hasForwardedUrl("persons/index");
//  }

  @Test
  void hasRedirectUrl() {
    assertThat(mvc.post().uri("/persons").param("name", "Andy")).hasStatus(HttpStatus.FOUND)
            .hasRedirectedUrl("/persons/Andy");
  }

  @Test
  void satisfiesAllowsAdditionalAssertions() {
    assertThat(mvc.get().uri("/greet")).satisfies(result -> {
      assertThat(result).isInstanceOf(MvcTestResult.class);
      assertThat(result).hasStatusOk();
    });
  }

  @Test
  void resultMatcherCanBeReused() throws Exception {
    MvcTestResult result = mvc.get().uri("/greet").exchange();
    ResultMatcher matcher = mock(ResultMatcher.class);
    assertThat(result).matches(matcher);
    verify(matcher).match(result.getMvcResult());
  }

  @Test
  void resultMatcherFailsWithDedicatedException() {
    ResultMatcher matcher = result -> assertThat(result.getResponse().getStatus())
            .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(mvc.get().uri("/greet")).matches(matcher))
            .withMessageContaining("expected: 404").withMessageContaining(" but was: 200");
  }

  @Test
  void shouldApplyResultHandler() { // Spring RESTDocs example
    AtomicBoolean applied = new AtomicBoolean();
    assertThat(mvc.get().uri("/greet")).apply(result -> applied.set(true));
    assertThat(applied).isTrue();
  }

  @EnableSession
  @Configuration
  @EnableWebMvc
  @Import({ TestController.class, PersonController.class, AsyncController.class,
          InternalResourceViewResolver.class,
          MultipartController.class, SessionController.class, ErrorController.class })
  static class WebConfiguration {
  }

  @RestController
  static class TestController {

    @GetMapping(path = "/greet", produces = "text/plain")
    String greet() {
      return "hello";
    }

    @GetMapping(path = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    String message() {
      return "{\"message\": \"hello\"}";
    }
  }

  @Controller
  @RequestMapping("/persons")
  static class PersonController {

    @GetMapping("/{name}")
    public String get(@PathVariable String name, Model model) {
      model.addAttribute(new Person(name));
      return "persons/index";
    }

    @PostMapping
    String create(@Valid Person person, Errors errors, Model model, RedirectModel redirectAttrs) {
      if (errors.hasErrors()) {
        return "persons/add";
      }
      model.addAttribute("name", person.getName());
      redirectAttrs.addAttribute("message", "success!");
      return "redirect:/persons/{name}";
    }
  }

  @RestController
  static class AsyncController {

    @GetMapping("/callable")
    public Callable<Map<String, String>> getCallable() {
      return () -> Collections.singletonMap("key", "value");
    }

    @GetMapping("/streaming")
    StreamingResponseBody streaming(@RequestParam long timeToWait) {
      return out -> {
        PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);
        stream.print("name=Joe");
        try {
          Thread.sleep(timeToWait);
          stream.print("&someBoolean=true");
        }
        catch (InterruptedException e) {
          /* no-op */
        }
      };
    }
  }

  @Controller
  static class MultipartController {

    @PostMapping("/part")
    ModelAndView part(@RequestPart Part part, @RequestPart Map<String, String> json) {
      Map<String, Object> model = new HashMap<>(json);
      model.put(part.getName(), part.getOriginalFilename());
      return new ModelAndView("index", model);
    }

    @PutMapping("/multipart-put")
    ModelAndView multiPartViaHttpPut(@RequestParam Part file) {
      return new ModelAndView("index", Map.of("name", file.getName()));
    }

    @PostMapping("/multipart-streaming")
    StreamingResponseBody streaming(@RequestParam Part file, @RequestParam long timeToWait) {
      return out -> {
        PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);
        stream.print("name=Joe");
        try {
          Thread.sleep(timeToWait);
          stream.print("&file=" + file.getOriginalFilename());
        }
        catch (InterruptedException e) {
          /* no-op */
        }
      };
    }
  }

  @Controller
  static class SessionController {

    @RequestMapping("/locale")
    String handle(Session session) {
      session.setAttribute("locale", Locale.UK);
      return "view";
    }
  }

  @Controller
  static class ErrorController {

    @GetMapping("/error/1")
    public String one() {
      throw new IllegalStateException("Expected");
    }

    @GetMapping("/error/2")
    public String two() {
      throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED);
    }

    @GetMapping("/error/validation/{id}")
    public String validation(@PathVariable @Size(max = 4) String id) {
      return "Hello " + id;
    }

    @GetMapping("/error/message")
    @ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "expected error message")
    public void errorMessage() {

    }

  }

}
