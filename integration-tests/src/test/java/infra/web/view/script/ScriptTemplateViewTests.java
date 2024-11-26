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

package infra.web.view.script;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import infra.beans.DirectFieldAccessor;
import infra.context.ApplicationContextException;
import infra.context.support.StaticApplicationContext;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 */
@DisabledForJreRange(min = JRE.JAVA_15) // Nashorn JavaScript engine removed in Java 15
public class ScriptTemplateViewTests {

  private ScriptTemplateView view;

  private ScriptTemplateConfigurer configurer;

  private StaticWebApplicationContext wac;

  @BeforeEach
  public void setup() {
    this.configurer = new ScriptTemplateConfigurer();
    this.wac = new StaticWebApplicationContext();
    this.wac.getBeanFactory().registerSingleton("scriptTemplateConfigurer", this.configurer);
    this.view = new ScriptTemplateView();
  }

  @Test
  public void missingTemplate() throws Exception {
    MockContextImpl mockContext = new MockContextImpl();
    this.wac.setMockContext(mockContext);
    this.wac.refresh();
    this.view.setResourceLoaderPath("classpath:infra/web/servlet/view/script/");
    this.view.setUrl("missing.txt");
    this.view.setEngine(Mockito.mock(InvocableScriptEngine.class));
    this.configurer.setRenderFunction("render");
    this.view.setApplicationContext(this.wac);
    assertThat(this.view.checkResource(Locale.ENGLISH)).isFalse();
  }

  @Test
  public void missingScriptTemplateConfig() {
    assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() ->
                    this.view.setApplicationContext(new StaticApplicationContext()))
            .withMessageContaining("ScriptTemplateConfig");
  }

  @Test
  public void detectScriptTemplateConfigWithEngine() {
    InvocableScriptEngine engine = mock(InvocableScriptEngine.class);
    this.configurer.setEngine(engine);
    this.configurer.setRenderObject("Template");
    this.configurer.setRenderFunction("render");
    this.configurer.setContentType(MediaType.TEXT_PLAIN_VALUE);
    this.configurer.setCharset(StandardCharsets.ISO_8859_1);
    this.configurer.setSharedEngine(true);
    DirectFieldAccessor accessor = new DirectFieldAccessor(view);

    this.view.setApplicationContext(this.wac);
    assertThat(accessor.getPropertyValue("engine")).isEqualTo(engine);
    assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("contentType")).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(accessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(InstanceOfAssertFactories.BOOLEAN).isTrue();
  }

  @Test
  public void detectScriptTemplateConfigWithEngineName() {
    this.configurer.setEngineName("nashorn");
    this.configurer.setRenderObject("Template");
    this.configurer.setRenderFunction("render");

    DirectFieldAccessor accessor = new DirectFieldAccessor(view);
    this.view.setApplicationContext(this.wac);
    assertThat(accessor.getPropertyValue("engineName")).isEqualTo("nashorn");
    assertThat(accessor.getPropertyValue("engine")).isNotNull();
    assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("contentType")).isEqualTo(MediaType.TEXT_HTML_VALUE);
    assertThat(accessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  public void customEngineAndRenderFunction() {
    ScriptEngine engine = mock(InvocableScriptEngine.class);
    given(engine.get("key")).willReturn("value");
    this.view.setEngine(engine);
    this.view.setRenderFunction("render");
    this.view.setApplicationContext(this.wac);
    engine = this.view.getEngine();
    assertThat(engine).isNotNull();
    assertThat(engine.get("key")).isEqualTo("value");
    DirectFieldAccessor accessor = new DirectFieldAccessor(view);
    assertThat(accessor.getPropertyValue("renderObject")).isNull();
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  public void nonSharedEngine() throws Exception {
    int iterations = 20;
    this.view.setEngineName("nashorn");
    this.view.setRenderFunction("render");
    this.view.setSharedEngine(false);
    this.view.setApplicationContext(this.wac);
    ExecutorService executor = Executors.newFixedThreadPool(4);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < iterations; i++) {
      results.add(executor.submit(() -> view.getEngine() != null));
    }
    assertThat(results.size()).isEqualTo(iterations);
    for (int i = 0; i < iterations; i++) {
      assertThat((boolean) results.get(i).get()).isTrue();
    }
    executor.shutdown();
  }

  @Test
  public void nonInvocableScriptEngine() {
    this.view.setEngine(Mockito.mock(ScriptEngine.class));
    this.view.setApplicationContext(this.wac);
  }

  @Test
  public void nonInvocableScriptEngineWithRenderFunction() {
    this.view.setEngine(Mockito.mock(ScriptEngine.class));
    this.view.setRenderFunction("render");
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.view.setApplicationContext(this.wac));
  }

  @Test
  public void engineAndEngineNameBothDefined() {
    this.view.setEngine(Mockito.mock(InvocableScriptEngine.class));
    this.view.setEngineName("test");
    this.view.setRenderFunction("render");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.view.setApplicationContext(this.wac))
            .withMessageContaining("You should define either 'engine', 'engineSupplier' or 'engineName'.");
  }

  @Test  // gh-23258
  public void engineAndEngineSupplierBothDefined() {
    ScriptEngine engine = mock(InvocableScriptEngine.class);
    this.view.setEngineSupplier(() -> engine);
    this.view.setEngine(engine);
    this.view.setRenderFunction("render");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.view.setApplicationContext(this.wac))
            .withMessageContaining("You should define either 'engine', 'engineSupplier' or 'engineName'.");
  }

  @Test  // gh-23258
  public void engineNameAndEngineSupplierBothDefined() {
    this.view.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
    this.view.setEngineName("test");
    this.view.setRenderFunction("render");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.view.setApplicationContext(this.wac))
            .withMessageContaining("You should define either 'engine', 'engineSupplier' or 'engineName'.");
  }

  @Test
  public void engineSetterAndNonSharedEngine() {
    this.view.setEngine(Mockito.mock(InvocableScriptEngine.class));
    this.view.setRenderFunction("render");
    this.view.setSharedEngine(false);
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.view.setApplicationContext(this.wac))
            .withMessageContaining("sharedEngine");
  }

  @Test // SPR-14210
  public void resourceLoaderPath() throws Exception {
    MockContextImpl mockContext = new MockContextImpl();
    this.wac.setMockContext(mockContext);
    this.wac.refresh();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    Map<String, Object> model = new HashMap<>();
    InvocableScriptEngine engine = mock(InvocableScriptEngine.class);
    given(engine.invokeFunction(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn("foo");
    this.view.setEngine(engine);
    this.view.setRenderFunction("render");
    this.view.setApplicationContext(this.wac);
    this.view.setUrl("infra/web/mock/view/script/empty.txt");
    this.view.render(model, new MockRequestContext(wac, request, response));
    assertThat(response.getContentAsString()).isEqualTo("foo");

    response = new MockHttpResponseImpl();
    this.view.setResourceLoaderPath("classpath:infra/web/servlet/view/script/");
    this.view.setUrl("empty.txt");
    this.view.render(model, new MockRequestContext(wac, request, response));
    assertThat(response.getContentAsString()).isEqualTo("foo");

    response = new MockHttpResponseImpl();
    this.view.setResourceLoaderPath("classpath:infra/web/servlet/view/script");
    this.view.setUrl("empty.txt");
    this.view.render(model, new MockRequestContext(wac, request, response));
    assertThat(response.getContentAsString()).isEqualTo("foo");
  }

  @Test // SPR-13379
  public void contentType() throws Exception {
    MockContextImpl mockContext = new MockContextImpl();
    this.wac.setMockContext(mockContext);
    this.wac.refresh();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    Map<String, Object> model = new HashMap<>();
    this.view.setEngine(Mockito.mock(InvocableScriptEngine.class));
    this.view.setRenderFunction("render");
    this.view.setResourceLoaderPath("classpath:infra/web/servlet/view/script/");
    this.view.setUrl("empty.txt");
    this.view.setApplicationContext(this.wac);

    this.view.render(model, new MockRequestContext(wac, request, response));
    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo((MediaType.TEXT_HTML_VALUE + ";charset=" +
            StandardCharsets.UTF_8));

    response = new MockHttpResponseImpl();
    this.view.setContentType(MediaType.TEXT_PLAIN_VALUE);
    this.view.render(model, new MockRequestContext(wac, request, response));
    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo((MediaType.TEXT_PLAIN_VALUE + ";charset=" +
            StandardCharsets.UTF_8));

    response = new MockHttpResponseImpl();
    this.view.setCharset(StandardCharsets.ISO_8859_1);
    this.view.render(model, new MockRequestContext(wac, request, response));
    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo((MediaType.TEXT_PLAIN_VALUE + ";charset=" +
            StandardCharsets.ISO_8859_1));

  }

  @Test  // gh-23258
  public void engineSupplierWithSharedEngine() {
    this.configurer.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
    this.configurer.setRenderObject("Template");
    this.configurer.setRenderFunction("render");
    this.configurer.setSharedEngine(true);

    DirectFieldAccessor accessor = new DirectFieldAccessor(view);
    this.view.setApplicationContext(this.wac);
    ScriptEngine engine1 = this.view.getEngine();
    ScriptEngine engine2 = this.view.getEngine();
    assertThat(engine1).isNotNull();
    assertThat(engine2).isNotNull();
    assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(InstanceOfAssertFactories.BOOLEAN).isTrue();
  }

  @Test  // gh-23258
  public void engineSupplierWithNonSharedEngine() {
    this.configurer.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
    this.configurer.setRenderObject("Template");
    this.configurer.setRenderFunction("render");
    this.configurer.setSharedEngine(false);

    DirectFieldAccessor accessor = new DirectFieldAccessor(view);
    this.view.setApplicationContext(this.wac);
    ScriptEngine engine1 = this.view.getEngine();
    ScriptEngine engine2 = this.view.getEngine();
    assertThat(engine1).isNotNull();
    assertThat(engine2).isNotNull();
    assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(InstanceOfAssertFactories.BOOLEAN).isFalse();
  }

  private interface InvocableScriptEngine extends ScriptEngine, Invocable {
  }

}
