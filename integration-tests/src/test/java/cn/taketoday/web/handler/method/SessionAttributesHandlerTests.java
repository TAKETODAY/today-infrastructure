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

import java.util.HashSet;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.web.bind.annotation.SessionAttributes;
import cn.taketoday.web.bind.support.DefaultSessionAttributeStore;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.ui.ModelMap;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/17 17:10
 */
class SessionAttributesHandlerTests {

  private final DefaultSessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

  private final SessionAttributesHandler sessionAttributesHandler = new SessionAttributesHandler(
          SessionAttributeHandler.class, sessionAttributeStore);
  AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SessionConfig.class);

  private final ServletRequestContext request = new ServletRequestContext(
          context, new MockHttpServletRequest(), new MockHttpServletResponse());

  @EnableWebSession
  static class SessionConfig {

  }

  {
    sessionAttributeStore.setSessionManager(context.getBean(SessionManager.class));
  }

  @Test
  public void isSessionAttribute() throws Exception {
    assertThat(sessionAttributesHandler.isHandlerSessionAttribute("attr1", String.class)).isTrue();
    assertThat(sessionAttributesHandler.isHandlerSessionAttribute("attr2", String.class)).isTrue();
    assertThat(sessionAttributesHandler.isHandlerSessionAttribute("simple", TestBean.class)).isTrue();
    assertThat(sessionAttributesHandler.isHandlerSessionAttribute("simple", String.class)).isFalse();
  }

  @Test
  public void retrieveAttributes() throws Exception {
    sessionAttributeStore.storeAttribute(request, "attr1", "value1");
    sessionAttributeStore.storeAttribute(request, "attr2", "value2");
    sessionAttributeStore.storeAttribute(request, "attr3", new TestBean());
    sessionAttributeStore.storeAttribute(request, "attr4", new TestBean());

    assertThat(sessionAttributesHandler.retrieveAttributes(request).keySet()).as("Named attributes (attr1, attr2) should be 'known' right away").isEqualTo(new HashSet<>(asList("attr1", "attr2")));

    // Resolve 'attr3' by type
    sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);

    assertThat(sessionAttributesHandler.retrieveAttributes(request).keySet()).as("Named attributes (attr1, attr2) and resolved attribute (att3) should be 'known'")
            .isEqualTo(new HashSet<>(asList("attr1", "attr2", "attr3")));
  }

  @Test
  public void cleanupAttributes() throws Exception {
    sessionAttributeStore.storeAttribute(request, "attr1", "value1");
    sessionAttributeStore.storeAttribute(request, "attr2", "value2");
    sessionAttributeStore.storeAttribute(request, "attr3", new TestBean());

    sessionAttributesHandler.cleanupAttributes(request);

    assertThat(sessionAttributeStore.retrieveAttribute(request, "attr1")).isNull();
    assertThat(sessionAttributeStore.retrieveAttribute(request, "attr2")).isNull();
    assertThat(sessionAttributeStore.retrieveAttribute(request, "attr3")).isNotNull();

    // Resolve 'attr3' by type
    sessionAttributesHandler.isHandlerSessionAttribute("attr3", TestBean.class);
    sessionAttributesHandler.cleanupAttributes(request);

    assertThat(sessionAttributeStore.retrieveAttribute(request, "attr3")).isNull();
  }

  @Test
  public void storeAttributes() throws Exception {
    ModelMap model = new ModelMap();
    model.put("attr1", "value1");
    model.put("attr2", "value2");
    model.put("attr3", new TestBean());

    sessionAttributesHandler.storeAttributes(request, model);

    assertThat(sessionAttributeStore.retrieveAttribute(request, "attr1")).isEqualTo("value1");
    assertThat(sessionAttributeStore.retrieveAttribute(request, "attr2")).isEqualTo("value2");
    boolean condition = sessionAttributeStore.retrieveAttribute(request, "attr3") instanceof TestBean;
    assertThat(condition).isTrue();
  }

  @SessionAttributes(names = { "attr1", "attr2" }, types = { TestBean.class })
  private static class SessionAttributeHandler {
  }

}
