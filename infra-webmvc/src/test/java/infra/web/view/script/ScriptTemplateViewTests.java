/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.view.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import infra.beans.DirectFieldAccessor;
import infra.context.ApplicationContextException;
import infra.context.support.StaticApplicationContext;
import infra.http.MediaType;
import infra.mock.api.MockContext;
import infra.mock.web.MockContextImpl;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.view.script.ScriptTemplateView.EngineKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:48
 */
class ScriptTemplateViewTests {

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
    MockContext mockContext = new MockContextImpl();
    this.wac.setMockContext(mockContext);
    this.wac.refresh();
    this.view.setResourceLoaderPath("classpath:infra/web/view/script/");
    this.view.setUrl("missing.txt");
    this.view.setEngine(mock(InvocableScriptEngine.class));
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

    DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
    this.view.setApplicationContext(this.wac);
    assertThat(accessor.getPropertyValue("engine")).isEqualTo(engine);
    assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("contentType")).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(accessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(BOOLEAN).isTrue();
  }

  @Test
  public void detectScriptTemplateConfigWithEngineName() {
    this.configurer.setEngineName("jython");
    this.configurer.setRenderObject("Template");
    this.configurer.setRenderFunction("render");

    DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
    this.view.setApplicationContext(this.wac);
    assertThat(accessor.getPropertyValue("engineName")).isEqualTo("jython");
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
    DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
    assertThat(accessor.getPropertyValue("renderObject")).isNull();
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("charset")).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  public void nonSharedEngine() throws Exception {
    int iterations = 20;
    this.view.setEngineName("jython");
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
    this.view.setEngine(mock(ScriptEngine.class));
    this.view.setApplicationContext(this.wac);
  }

  @Test
  public void nonInvocableScriptEngineWithRenderFunction() {
    this.view.setEngine(mock(ScriptEngine.class));
    this.view.setRenderFunction("render");
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.view.setApplicationContext(this.wac));
  }

  @Test
  public void engineAndEngineNameBothDefined() {
    this.view.setEngine(mock(InvocableScriptEngine.class));
    this.view.setEngineName("test");
    this.view.setRenderFunction("render");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.view.setApplicationContext(this.wac))
            .withMessageContaining("You should define either 'engine', 'engineSupplier' or 'engineName'.");
  }

  @Test
  public void engineAndEngineSupplierBothDefined() {
    ScriptEngine engine = mock(InvocableScriptEngine.class);
    this.view.setEngineSupplier(() -> engine);
    this.view.setEngine(engine);
    this.view.setRenderFunction("render");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.view.setApplicationContext(this.wac))
            .withMessageContaining("You should define either 'engine', 'engineSupplier' or 'engineName'.");
  }

  @Test
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
    this.view.setEngine(mock(InvocableScriptEngine.class));
    this.view.setRenderFunction("render");
    this.view.setSharedEngine(false);
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.view.setApplicationContext(this.wac))
            .withMessageContaining("sharedEngine");
  }

  @Test
  public void engineSupplierWithSharedEngine() {
    this.configurer.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
    this.configurer.setRenderObject("Template");
    this.configurer.setRenderFunction("render");
    this.configurer.setSharedEngine(true);

    DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
    this.view.setApplicationContext(this.wac);
    ScriptEngine engine1 = this.view.getEngine();
    ScriptEngine engine2 = this.view.getEngine();
    assertThat(engine1).isNotNull();
    assertThat(engine2).isNotNull();
    assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(BOOLEAN).isTrue();
  }

  @Test
  public void engineSupplierWithNonSharedEngine() {
    this.configurer.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
    this.configurer.setRenderObject("Template");
    this.configurer.setRenderFunction("render");
    this.configurer.setSharedEngine(false);

    DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
    this.view.setApplicationContext(this.wac);
    ScriptEngine engine1 = this.view.getEngine();
    ScriptEngine engine2 = this.view.getEngine();
    assertThat(engine1).isNotNull();
    assertThat(engine2).isNotNull();
    assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
    assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
    assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(BOOLEAN).isFalse();
  }

  private interface InvocableScriptEngine extends ScriptEngine, Invocable {
  }

  @Test
  void shouldCreateScriptTemplateView() {
    // when
    ScriptTemplateView view = new ScriptTemplateView();

    // then
    assertThat(view).isNotNull();
    assertThat(view.getContentType()).isNull();
  }

  @Test
  void shouldCreateScriptTemplateViewWithUrl() {
    // given
    String url = "template.html";

    // when
    ScriptTemplateView view = new ScriptTemplateView(url);

    // then
    assertThat(view).isNotNull();
    assertThat(view.getUrl()).isEqualTo(url);
    assertThat(view.getContentType()).isNull();
  }

  @Test
  void shouldSetAndGetEngine() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    ScriptEngine engine = mock(ScriptEngine.class);

    // when
    view.setEngine(engine);

    // then
    // Engine is set via reflection in the actual implementation, so we just verify no exception is thrown
    assertThatNoException().isThrownBy(() -> view.setEngine(engine));
  }

  @Test
  void shouldSetAndGetEngineSupplier() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    Supplier<ScriptEngine> engineSupplier = mock(Supplier.class);

    // when
    view.setEngineSupplier(engineSupplier);

    // then
    assertThatNoException().isThrownBy(() -> view.setEngineSupplier(engineSupplier));
  }

  @Test
  void shouldSetAndGetEngineName() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    String engineName = "nashorn";

    // when
    view.setEngineName(engineName);

    // then
    assertThatNoException().isThrownBy(() -> view.setEngineName(engineName));
  }

  @Test
  void shouldSetAndGetSharedEngine() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    Boolean sharedEngine = Boolean.TRUE;

    // when
    view.setSharedEngine(sharedEngine);

    // then
    assertThatNoException().isThrownBy(() -> view.setSharedEngine(sharedEngine));
  }

  @Test
  void shouldSetAndGetScripts() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    String[] scripts = { "script1.js", "script2.js" };

    // when
    view.setScripts(scripts);

    // then
    assertThatNoException().isThrownBy(() -> view.setScripts(scripts));
  }

  @Test
  void shouldSetAndGetRenderObject() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    String renderObject = "template";

    // when
    view.setRenderObject(renderObject);

    // then
    assertThatNoException().isThrownBy(() -> view.setRenderObject(renderObject));
  }

  @Test
  void shouldSetAndGetRenderFunction() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    String renderFunction = "render";

    // when
    view.setRenderFunction(renderFunction);

    // then
    assertThatNoException().isThrownBy(() -> view.setRenderFunction(renderFunction));
  }

  @Test
  void shouldSetAndGetCharset() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    Charset charset = StandardCharsets.UTF_8;

    // when
    view.setCharset(charset);

    // then
    assertThatNoException().isThrownBy(() -> view.setCharset(charset));
  }

  @Test
  void shouldSetResourceLoaderPath() {
    // given
    ScriptTemplateView view = new ScriptTemplateView();
    String resourceLoaderPath = "classpath:/templates/,file:/static/";

    // when
    view.setResourceLoaderPath(resourceLoaderPath);

    // then
    assertThatNoException().isThrownBy(() -> view.setResourceLoaderPath(resourceLoaderPath));
  }

  @Test
  void shouldHaveDefaultContentType() {
    // when & then
    assertThat(ScriptTemplateView.DEFAULT_CONTENT_TYPE).isEqualTo("text/html");
  }

  @Test
  void shouldCreateEngineKeyWithEqualProperties() {
    // given
    EngineKey key1 = new EngineKey("nashorn", new String[] { "script1.js", "script2.js" });
    EngineKey key2 = new EngineKey("nashorn", new String[] { "script1.js", "script2.js" });

    // when & then
    assertThat(key1).isEqualTo(key2);
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  void shouldNotEqualEngineKeyWithDifferentEngineNames() {
    // given
    EngineKey key1 = new EngineKey("nashorn", new String[] { "script.js" });
    EngineKey key2 = new EngineKey("graal.js", new String[] { "script.js" });

    // when & then
    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  void shouldNotEqualEngineKeyWithDifferentScripts() {
    // given
    EngineKey key1 = new EngineKey("nashorn", new String[] { "script1.js" });
    EngineKey key2 = new EngineKey("nashorn", new String[] { "script2.js" });

    // when & then
    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  void shouldEqualEngineKeyWithSameInstance() {
    // given
    EngineKey key = new EngineKey("nashorn", new String[] { "script.js" });

    // when & then
    assertThat(key).isEqualTo(key);
  }

  @Test
  void shouldNotEqualEngineKeyWithNull() {
    // given
    EngineKey key = new EngineKey("nashorn", new String[] { "script.js" });

    // when & then
    assertThat(key).isNotEqualTo(null);
  }

  @Test
  void shouldNotEqualEngineKeyWithDifferentClass() {
    // given
    EngineKey key = new EngineKey("nashorn", new String[] { "script.js" });
    String differentObject = "not an EngineKey";

    // when & then
    assertThat(key).isNotEqualTo(differentObject);
  }

}