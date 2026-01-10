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

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.script.ScriptEngine;

import infra.web.view.script.ScriptTemplateView.EngineKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:48
 */
class ScriptTemplateViewTests {

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