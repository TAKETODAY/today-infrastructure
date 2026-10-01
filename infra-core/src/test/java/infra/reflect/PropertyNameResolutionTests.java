/*
 * Copyright 2002-present the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.reflect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PropertyNameResolutionTests {

  @Test
  void resolveStandardAndDataClassAccessors() throws Exception {
    assertName(TestBean.class, "getName", "name");
    assertName(TestBean.class, "isEnabled", "enabled");
    assertName(TestBean.class, "isTarget", "target");
    assertThat(new Property(TestBean.class, null, TestBean.class.getMethod("setName", String.class)).getName()).isEqualTo("name");
    for (String name : new String[] { "name", "budget", "issue" }) {
      assertName(SampleRecord.class, name, name);
      assertName(SampleDataClass.class, name, name);
    }
    assertName(SampleRecord.class, "getWidget", "widget");
    for (String name : new String[] { "get", "is", "getWidget" }) {
      assertName(EdgeRecord.class, name, name);
    }
    assertName(SampleDataClass.class, "getWidget", "widget");
    assertName(SampleDataClass.class, "isUrgent", "isUrgent");
    assertName(StaticEdgeBean.class, "getCount", "count");
    assertName(StaticEdgeBean.class, "getLabel", "label");
  }

  private void assertName(Class<?> type, String method, String expected) throws Exception {
    assertThat(new Property(type, type.getMethod(method), null).getName()).isEqualTo(expected);
  }

  @Test
  void rejectNonSetterWriteMethods() {
    for (String name : new String[] { "updateName", "offsetX", "upset" }) {
      assertThatIllegalArgumentException().isThrownBy(() ->
              new Property(TestBean.class, null, TestBean.class.getMethod(name, String.class)));
    }
  }

  static class TestBean {
    public String getName() { return null; }
    public boolean isEnabled() { return false; }
    public boolean isTarget() { return false; }
    public void setName(String name) { }
    public void updateName(String name) { }
    public void offsetX(String name) { }
    public void upset(String name) { }
  }

  record SampleRecord(String name, String budget, String issue) {
    public String getWidget() { return null; }
  }
  record EdgeRecord(String get, String is, String getWidget) { }
  static class SampleDataClass {
    private String name;
    private String budget;
    private String issue;
    private boolean isUrgent;
    public String name() { return name; }
    public String budget() { return budget; }
    public String issue() { return issue; }
    public boolean isUrgent() { return isUrgent; }
    public String getWidget() { return null; }
  }
  static class StaticEdgeBean {
    private static String getCount;
    private String getLabel;
    public String getCount() { return null; }
    public static String getLabel() { return null; }
  }
}
