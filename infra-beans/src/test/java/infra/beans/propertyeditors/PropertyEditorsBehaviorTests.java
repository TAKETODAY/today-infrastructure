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

package infra.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import infra.core.io.Resource;
import infra.core.io.ResourceEditor;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PropertyEditorsBehaviorTests {

  @Test
  void convertsCommonScalarValuesAndUsesEmptyTextForNullValues() {
    var charset = new CharsetEditor();
    charset.setAsText(" UTF-8 ");
    assertThat(charset.getValue()).isEqualTo(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(charset.getAsText()).isEqualTo("UTF-8");
    charset.setAsText(" ");
    assertThat(charset.getValue()).isNull();
    assertThat(charset.getAsText()).isEmpty();

    UUID id = UUID.randomUUID();
    var uuid = new UUIDEditor();
    uuid.setAsText(" " + id + " ");
    assertThat(uuid.getValue()).isEqualTo(id);
    assertThat(uuid.getAsText()).isEqualTo(id.toString());
    uuid.setAsText("");
    assertThat(uuid.getValue()).isNull();
    assertThat(uuid.getAsText()).isEmpty();

    var currency = new CurrencyEditor();
    currency.setAsText(" USD ");
    assertThat(currency.getAsText()).isEqualTo("USD");
    assertThat(currency.getValue()).isEqualTo(java.util.Currency.getInstance("USD"));
    assertThatIllegalArgumentException().isThrownBy(() -> currency.setAsText(""));
    currency.setValue(null);
    assertThat(currency.getAsText()).isEmpty();

    var timeZone = new TimeZoneEditor();
    timeZone.setAsText(" UTC ");
    assertThat(timeZone.getAsText()).isEqualTo("UTC");
    assertThatIllegalArgumentException().isThrownBy(() -> timeZone.setAsText(""));
    timeZone.setValue(null);
    assertThat(timeZone.getAsText()).isEmpty();

    var zoneId = new ZoneIdEditor();
    assertThatIllegalArgumentException().isThrownBy(() -> zoneId.setAsText(""));

    var pattern = new PatternEditor(Pattern.CASE_INSENSITIVE);
    pattern.setAsText("hello");
    assertThat(((Pattern) pattern.getValue()).pattern()).isEqualTo("hello");
    assertThat(((Pattern) pattern.getValue()).flags()).isEqualTo(Pattern.CASE_INSENSITIVE);
    assertThat(pattern.getAsText()).isEqualTo("hello");
    pattern.setAsText(null);
    assertThat(pattern.getValue()).isNull();
    assertThat(pattern.getAsText()).isEmpty();
  }

  @Test
  void parsesDatesWithEmptyAndExactLengthPolicies() throws Exception {
    var format = new SimpleDateFormat("yyyy-MM-dd");
    Date expected = format.parse("2026-01-02");
    var editor = new CustomDateEditor(format, true, 10);
    editor.setAsText("2026-01-02");
    assertThat(editor.getValue()).isEqualTo(expected);
    assertThat(editor.getAsText()).isEqualTo("2026-01-02");

    editor.setAsText("  ");
    assertThat(editor.getValue()).isNull();
    assertThat(editor.getAsText()).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> editor.setAsText("2026-1-2"))
            .withMessageContaining("not exactly10characters");

    var strict = new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), false);
    assertThatIllegalArgumentException().isThrownBy(() -> strict.setAsText(""));
    assertThatIllegalArgumentException().isThrownBy(() -> strict.setAsText("not-a-date"))
            .withMessageStartingWith("Could not parse date:");
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void formatsNumbersAndRejectsNonNumberTypes() {
    var number = new CustomNumberEditor(Integer.class, NumberFormat.getIntegerInstance(Locale.US), false);
    number.setAsText("1,234");
    assertThat(number.getValue()).isEqualTo(1234);
    assertThat(number.getAsText()).isEqualTo("1,234");
    assertThatIllegalArgumentException().isThrownBy(() -> new CustomNumberEditor((Class) String.class, false));
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void convertsMapsUsingTargetImplementationsAndConversionHooks() {
    var source = new TreeMap<Object, Object>();
    source.put("one", "1");
    source.put("two", "2");
    var editor = new ConvertingMapEditor();
    editor.setValue(source);
    assertThat(editor.getValue()).isInstanceOf(TreeMap.class)
            .isNotSameAs(source)
            .isEqualTo(Map.of("ONE", "1!", "TWO", "2!"));
    assertThat(editor.getAsText()).isNull();

    var sorted = new CustomMapEditor(SortedMap.class);
    sorted.setValue(Map.of("b", 2, "a", 1));
    assertThat(sorted.getValue()).isInstanceOf(TreeMap.class);
    assertThat(sorted.getValue()).isEqualTo(new TreeMap<String, Integer>(Map.of("a", 1, "b", 2)));

    var same = new LinkedHashMap<String, Integer>();
    var reuse = new CustomMapEditor(LinkedHashMap.class);
    reuse.setValue(same);
    assertThat(reuse.getValue()).isSameAs(same);
    reuse.setValue(null);
    assertThat(reuse.getValue()).isNull();

    var empty = new CustomMapEditor(LinkedHashMap.class, true);
    empty.setValue(null);
    assertThat(empty.getValue()).isEqualTo(Map.of());
    assertThatIllegalArgumentException().isThrownBy(() -> reuse.setAsText("not a map"));
    assertThatIllegalArgumentException().isThrownBy(() -> new CustomMapEditor((Class) String.class));

    var interfaceEditor = new CustomMapEditor(Map.class, true);
    interfaceEditor.setValue(null);
    assertThat(interfaceEditor.getValue()).isInstanceOf(LinkedHashMap.class).isEqualTo(Map.of());

    var abstractEditor = new CustomMapEditor(java.util.AbstractMap.class, true);
    assertThatIllegalArgumentException().isThrownBy(() -> abstractEditor.setValue(null))
            .withMessageContaining("Could not instantiate map class");
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void convertsCollectionsFromArraysCollectionsAndPlainValues() {
    var list = new CustomCollectionEditor(List.class, true);
    list.setValue(new int[] { 3, 1, 2 });
    assertThat(list.getValue()).isEqualTo(List.of(3, 1, 2));
    list.setValue("one");
    assertThat(list.getValue()).isEqualTo(List.of("one"));
    list.setValue(null);
    assertThat(list.getValue()).isEqualTo(List.of());

    var set = new CustomCollectionEditor(SortedSet.class);
    set.setValue(List.of("b", "a", "b"));
    assertThat(set.getValue()).isInstanceOf(TreeSet.class).isEqualTo(new TreeSet<>(List.of("a", "b")));

    var source = new ArrayList<>(List.of("same"));
    var reuse = new CustomCollectionEditor(ArrayList.class);
    reuse.setValue(source);
    assertThat(reuse.getValue()).isSameAs(source);
    assertThat(reuse.getAsText()).isNull();
    assertThatIllegalArgumentException().isThrownBy(() -> new CustomCollectionEditor((Class) String.class));
  }

  @Test
  void resolvesInputSourceFromClasspathAndReportsItsSystemId() {
    var editor = new InputSourceEditor();
    editor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/"
            + ClassUtils.getShortName(getClass()) + ".class");
    assertThat(editor.getValue()).isInstanceOf(org.xml.sax.InputSource.class);
    assertThat(editor.getAsText()).startsWith("file:");

    var empty = new InputSourceEditor();
    empty.setAsText("");
    assertThat(empty.getValue()).isNull();
    assertThat(empty.getAsText()).isEmpty();
  }

  @Test
  void handlesFileAndPathEditorsBlankValuesAndResourceResolutionFailures() throws IOException {
    var file = new FileEditor();
    file.setAsText(" ");
    assertThat(file.getValue()).isNull();
    assertThat(file.getAsText()).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> new FileEditor(null));

    var fileResourceEditor = mock(ResourceEditor.class);
    var fileResource = mock(Resource.class);
    given(fileResourceEditor.getValue()).willReturn(fileResource);
    given(fileResource.getFile()).willThrow(new IOException("cannot read file"));
    assertThatIllegalArgumentException().isThrownBy(() ->
            new FileEditor(fileResourceEditor).setAsText("file:test"))
            .withMessageContaining("Could not retrieve file");

    var path = new PathEditor();
    path.setAsText("");
    assertThat(path.getValue()).isNull();
    assertThat(path.getAsText()).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> new PathEditor(null));

    var pathResourceEditor = mock(ResourceEditor.class);
    var pathResource = mock(Resource.class);
    given(pathResourceEditor.getValue()).willReturn(pathResource);
    given(pathResource.isFile()).willReturn(true);
    given(pathResource.exists()).willReturn(true);
    given(pathResource.getFilePath()).willThrow(new IOException("cannot read path"));
    assertThatIllegalArgumentException().isThrownBy(() ->
            new PathEditor(pathResourceEditor).setAsText("file:test"))
            .withMessageContaining("Could not resolve");
    assertThatIllegalArgumentException().isThrownBy(() ->
            new PathEditor(pathResourceEditor).setAsText("test.txt"))
            .withMessageContaining("In case of ambiguity");
  }

  @Test
  void reportsInputSourceAndUriResolutionFailures() throws Exception {
    var resourceEditor = mock(ResourceEditor.class);
    var resource = mock(Resource.class);
    given(resourceEditor.getValue()).willReturn(resource);
    given(resource.getURL()).willThrow(new IOException("cannot read URL"));
    assertThatIllegalArgumentException().isThrownBy(() ->
            new InputSourceEditor(resourceEditor).setAsText("file:test"))
            .withMessageContaining("Could not retrieve URL");
    assertThatIllegalArgumentException().isThrownBy(() -> new InputSourceEditor(null));

    var classLoader = getClass().getClassLoader();
    assertThatIllegalArgumentException().isThrownBy(() ->
            new URIEditor(classLoader).setAsText("classpath:/missing-resource"))
            .withMessageContaining("Could not retrieve URI");
    assertThatIllegalArgumentException().isThrownBy(() ->
            new URIEditor(false).setAsText("invalid path"))
            .withMessageStartingWith("Invalid URI syntax:");
    var uri = new URIEditor(null, false);
    uri.setAsText("relative/path");
    assertThat(uri.getValue()).isEqualTo(URI.create("relative/path"));
    var defaultEditor = new URIEditor();
    defaultEditor.setAsText("relative/path");
    assertThat(defaultEditor.getValue()).isEqualTo(URI.create("relative/path"));
  }

  private static class ConvertingMapEditor extends CustomMapEditor {

    ConvertingMapEditor() {
      super(SortedMap.class);
    }

    @Override
    protected Object convertKey(Object key) {
      return key.toString().toUpperCase(java.util.Locale.ROOT);
    }

    @Override
    protected Object convertValue(Object value) {
      return value + "!";
    }

    @Override
    protected boolean alwaysCreateNewMap() {
      return true;
    }

  }

}
