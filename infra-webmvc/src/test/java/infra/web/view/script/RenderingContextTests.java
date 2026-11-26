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

import java.util.Locale;
import java.util.function.Function;

import infra.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RenderingContextTests {

  @Test
  void exposesTemplateRenderingInputs() {
    ApplicationContext application = mock(ApplicationContext.class);
    Function<String, String> loader = path -> "template:" + path;
    RenderingContext context = new RenderingContext(application, Locale.CANADA, loader, "sample.html");
    assertThat(context.getApplicationContext()).isSameAs(application);
    assertThat(context.getLocale()).isEqualTo(Locale.CANADA);
    assertThat(context.getTemplateLoader()).isSameAs(loader);
    assertThat(context.getTemplateLoader().apply("partial.html")).isEqualTo("template:partial.html");
    assertThat(context.getUrl()).isEqualTo("sample.html");
  }

}
