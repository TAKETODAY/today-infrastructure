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

package infra.web.view.xslt;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import infra.context.support.StaticApplicationContext;
import infra.util.ClassUtils;
import infra.web.mock.MockServerHttpExchange;
import infra.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class XsltViewResolverTests {

  @Test
  public void resolveView() throws Exception {
    StaticApplicationContext ctx = new StaticApplicationContext();

    String prefix = ClassUtils.classPackageAsResourcePath(getClass());
    String suffix = ".xsl";
    String viewName = "products";

    XsltViewResolver viewResolver = new XsltViewResolver();
    viewResolver.setPrefix(prefix);
    viewResolver.setSuffix(suffix);
    viewResolver.setApplicationContext(ctx);

    XsltView view = (XsltView) viewResolver.resolveViewName(viewName, Locale.ENGLISH);
    assertThat(view).as("View should not be null").isNotNull();
    assertThat(view.getUrl()).as("Incorrect URL").isEqualTo((prefix + viewName + suffix));
  }

  @Test
  void rendersNamedSourceWithConfiguredOutputProperties() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(OutputKeys.METHOD, "text");
    XsltViewResolver resolver = new XsltViewResolver();
    resolver.setPrefix(ClassUtils.classPackageAsResourcePath(getClass()) + "/");
    resolver.setSuffix(".xsl");
    resolver.setSourceKey("reportXml");
    resolver.setOutputProperties(properties);
    resolver.setIndent(false);
    resolver.setCacheTemplates(false);

    try (StaticApplicationContext applicationContext = new StaticApplicationContext()) {
      applicationContext.refresh();
      resolver.setApplicationContext(applicationContext);
      View view = resolver.resolveViewName("report", Locale.ENGLISH);
      assertThat(view).isInstanceOf(XsltView.class);
      MockServerHttpExchange context = new MockServerHttpExchange();

      view.render(Map.of("unusedXml", new StringReader("<report><title>Wrong source</title></report>"),
              "reportXml", new StringReader("<report><title>Quarterly report</title></report>")), context);

      assertThat(context.response.getContentAsString()).isEqualTo("Quarterly report");
    }
  }
}
