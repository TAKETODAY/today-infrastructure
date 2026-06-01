package infra.http;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/6/1 15:23
 */
class DecoratingHttpHeadersTests {

  @Test
  void constructor_withNullDelegate_shouldThrowException() {
    assertThatThrownBy(() -> new DecoratingHttpHeaders(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getFirst_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value1");
    delegate.add("Test-Header", "value2");

    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.getFirst("Test-Header")).isEqualTo("value1");
  }

  @Test
  void getFirst_withCaseInsensitiveName_shouldDelegateCorrectly() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Content-Type", "application/json");

    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.getFirst("content-type")).isEqualTo("application/json");
    assertThat(headers.getFirst("CONTENT-TYPE")).isEqualTo("application/json");
  }

  @Test
  void getFirst_withNonExistentHeader_shouldReturnNull() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.getFirst("Non-Existent")).isNull();
  }

  @Test
  void add_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Test-Header", "value1");
    headers.add("Test-Header", "value2");

    assertThat(delegate.get("Test-Header")).containsExactly("value1", "value2");
    assertThat(headers.get("Test-Header")).containsExactly("value1", "value2");
  }

  @Test
  void add_withNullValue_shouldDelegateCorrectly() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Test-Header", (String) null);

    assertThat(headers.contains("Test-Header")).isFalse();
  }

  @Test
  void setHeader_withSingleValue_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "old-value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> oldValues = headers.setHeader("Test-Header", "new-value");

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.get("Test-Header")).containsExactly("new-value");
  }

  @Test
  void setHeader_withCollection_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "old-value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> newValues = new ArrayList<>();
    newValues.add("new-value1");
    newValues.add("new-value2");
    List<String> oldValues = headers.setHeader("Test-Header", newValues);

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.get("Test-Header")).containsExactly("new-value1", "new-value2");
  }

  @Test
  void setHeader_withNullCollection_shouldRemoveHeader() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> oldValues = headers.setHeader("Test-Header", (Collection<String>) null);

    assertThat(oldValues).containsExactly("value");
    assertThat(headers.get("Test-Header")).isNull();
  }

  @Test
  void clear_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header-1", "value1");
    delegate.add("Header-2", "value2");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.clear();

    assertThat(delegate.isEmpty()).isTrue();
    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void size_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header-1", "value");
    delegate.add("Header-2", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.size()).isEqualTo(2);
  }

  @Test
  void isEmpty_shouldReturnTrueWhenNoHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void isEmpty_shouldReturnFalseWhenHasHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.isEmpty()).isFalse();
  }

  @Test
  void get_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value1");
    delegate.add("Test-Header", "value2");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> values = headers.get("Test-Header");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void get_withCaseInsensitiveName_shouldDelegateCorrectly() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Content-Type", "application/json");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.get("content-type")).containsExactly("application/json");
    assertThat(headers.get("CONTENT-TYPE")).containsExactly("application/json");
  }

  @Test
  void get_withNonExistentHeader_shouldReturnNull() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.get("Non-Existent")).isNull();
  }

  @Test
  void remove_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value1");
    delegate.add("Test-Header", "value2");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> removedValues = headers.remove("Test-Header");

    assertThat(removedValues).containsExactly("value1", "value2");
    assertThat(delegate.get("Test-Header")).isNull();
    assertThat(headers.get("Test-Header")).isNull();
  }

  @Test
  void remove_withNonExistentHeader_shouldReturnNull() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> removedValues = headers.remove("Non-Existent");

    assertThat(removedValues).isNull();
  }

  @Test
  void names_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header-1", "value");
    delegate.add("Header-2", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    Set<String> names = headers.names();

    assertThat(names).containsExactlyInAnyOrder("Header-1", "Header-2");
  }

  @Test
  void entries_shouldDelegateToUnderlyingHeaders() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header-1", "value1");
    delegate.add("Header-2", "value2");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    Set<Map.Entry<String, List<String>>> entries = headers.entries();

    assertThat(entries).hasSize(2);
    assertThat(entries.stream().map(Map.Entry::getKey))
            .containsExactlyInAnyOrder("Header-1", "Header-2");
  }

  @Test
  void toString_shouldIncludeClassNameAndDelegate() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    String result = headers.toString();

    assertThat(result).startsWith("DecoratingHttpHeaders [delegate=");
    assertThat(result).endsWith("]");
  }

  @Test
  void toString_withSubclass_shouldIncludeSubclassName() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate) {
      // Anonymous subclass
    };

    String result = headers.toString();

    assertThat(result).startsWith(headers.getClass().getSimpleName());
  }

  @Test
  void modificationsToDelegate_shouldReflectInDecorator() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    delegate.add("Test-Header", "value");

    assertThat(headers.getFirst("Test-Header")).isEqualTo("value");
  }

  @Test
  void modificationsThroughDecorator_shouldReflectInDelegate() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Test-Header", "value");

    assertThat(delegate.getFirst("Test-Header")).isEqualTo("value");
  }

  @Test
  void multipleOperations_shouldMaintainConsistency() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");
    headers.add("Header-1", "value3");

    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("Header-1")).containsExactly("value1", "value3");
    assertThat(headers.get("Header-2")).containsExactly("value2");
    assertThat(delegate.size()).isEqualTo(2);
  }

  @Test
  void caseInsensitivity_shouldBePreserved() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Content-Type", "application/json");

    assertThat(headers.getFirst("content-type")).isEqualTo("application/json");
    assertThat(headers.getFirst("CONTENT-TYPE")).isEqualTo("application/json");
  }

  @Test
  void setHeader_caseInsensitive_shouldReplaceExisting() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "old-value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> oldValues = headers.setHeader("test-header", "new-value");

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.getFirst("Test-Header")).isEqualTo("new-value");
  }

  @Test
  void remove_caseInsensitive_shouldRemoveHeader() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> removedValues = headers.remove("test-header");

    assertThat(removedValues).containsExactly("value");
    assertThat(headers.get("Test-Header")).isNull();
  }

  @Test
  void emptyDecorator_shouldHaveZeroSize() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.size()).isEqualTo(0);
    assertThat(headers.isEmpty()).isTrue();
    assertThat(headers.names()).isEmpty();
    assertThat(headers.entries()).isEmpty();
  }

  @Test
  void decoratorWithPrePopulatedHeaders_shouldReflectInitialState() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header-1", "value1");
    delegate.add("Header-2", "value2");
    delegate.add("Header-2", "value3");

    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("Header-1")).containsExactly("value1");
    assertThat(headers.get("Header-2")).containsExactly("value2", "value3");
  }

  @Test
  void clear_onEmptyDecorator_shouldNotThrowException() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThatNoException().isThrownBy(headers::clear);
    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void sequentialOperations_shouldMaintainState() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header", "value1");
    headers.add("Header", "value2");
    assertThat(headers.get("Header")).containsExactly("value1", "value2");

    headers.setHeader("Header", "value3");
    assertThat(headers.get("Header")).containsExactly("value3");

    headers.remove("Header");
    assertThat(headers.get("Header")).isNull();
  }

  @Test
  void names_shouldReturnLiveView() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header-1", "value");
    Set<String> names1 = headers.names();
    assertThat(names1).containsExactly("Header-1");

    headers.add("Header-2", "value");
    assertThat(names1).containsExactlyInAnyOrder("Header-1", "Header-2");
  }

  @Test
  void entries_shouldReturnLiveView() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header-1", "value1");
    Set<Map.Entry<String, List<String>>> entries1 = headers.entries();
    assertThat(entries1).hasSize(1);

    headers.add("Header-2", "value2");
    assertThat(entries1).hasSize(2);
  }

  @Test
  void setHeader_withEmptyCollection_shouldRemoveHeader() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> oldValues = headers.setHeader("Test-Header", new ArrayList<>());

    assertThat(oldValues).containsExactly("value");
    assertThat(headers.get("Test-Header")).isEmpty();
  }

  @Test
  void get_afterClear_shouldReturnNull() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.clear();

    assertThat(headers.get("Test-Header")).isNull();
    assertThat(headers.getFirst("Test-Header")).isNull();
  }

  @Test
  void size_afterAddAndRemove_shouldBeZero() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header", "value");
    assertThat(headers.size()).isEqualTo(1);

    headers.remove("Header");
    assertThat(headers.size()).isEqualTo(0);
  }

  @Test
  void containsKey_shouldWorkThroughGet() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Test-Header", "value");

    assertThat(headers.get("Test-Header")).isNotNull();
    assertThat(headers.get("Non-Existent")).isNull();
  }

  @Test
  void multipleValues_shouldMaintainOrder() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Test-Header", "first");
    headers.add("Test-Header", "second");
    headers.add("Test-Header", "third");

    assertThat(headers.get("Test-Header")).containsExactly("first", "second", "third");
  }

  @Test
  void setHeader_overwriteMultipleValues_shouldReplaceAll() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value1");
    delegate.add("Test-Header", "value2");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> oldValues = headers.setHeader("Test-Header", "single-value");

    assertThat(oldValues).containsExactly("value1", "value2");
    assertThat(headers.get("Test-Header")).containsExactly("single-value");
  }

  @Test
  void delegateIdentity_shouldBePreserved() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    assertThat(headers.toString()).contains(delegate.toString());
  }

  @Test
  void concurrentModifications_shouldBeReflected() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header-1", "value1");
    delegate.add("Header-2", "value2");

    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get("Header-1")).containsExactly("value1");
    assertThat(headers.get("Header-2")).containsExactly("value2");
  }

  @Test
  void nullHandling_inAdd_shouldBePreserved() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header", (String) null);

    assertThat(headers.getFirst("Header")).isNull();
  }

  @Test
  void isEmpty_afterClear_shouldReturnTrue() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.clear();

    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void size_afterOverwrite_shouldReflectNewCount() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header-1", "value1");
    delegate.add("Header-2", "value2");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.setHeader("Header-1", "new-value");

    assertThat(headers.size()).isEqualTo(2);
  }

  @Test
  void getFirst_withMultipleValues_shouldReturnFirstOnly() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Test-Header", "first");
    headers.add("Test-Header", "second");
    headers.add("Test-Header", "third");

    assertThat(headers.getFirst("Test-Header")).isEqualTo("first");
  }

  @Test
  void entries_iteration_shouldProvideAllData() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");
    headers.add("Header-2", "value3");

    Map<String, List<String>> collected = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : headers.entries()) {
      collected.put(entry.getKey(), entry.getValue());
    }

    assertThat(collected).hasSize(2);
    assertThat(collected.get("Header-1")).containsExactly("value1");
    assertThat(collected.get("Header-2")).containsExactly("value2", "value3");
  }

  @Test
  void names_iteration_shouldProvideAllNames() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header-1", "value");
    headers.add("Header-2", "value");

    List<String> collectedNames = new ArrayList<>();
    for (String name : headers.names()) {
      collectedNames.add(name);
    }

    assertThat(collectedNames).containsExactlyInAnyOrder("Header-1", "Header-2");
  }

  @Test
  void complexScenario_addRemoveAdd_shouldWorkCorrectly() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("Header", "value1");
    assertThat(headers.get("Header")).containsExactly("value1");

    headers.remove("Header");
    assertThat(headers.get("Header")).isNull();

    headers.add("Header", "value2");
    assertThat(headers.get("Header")).containsExactly("value2");
  }

  @Test
  void setHeader_withSameValue_shouldStillReturnOldValues() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Test-Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    List<String> oldValues = headers.setHeader("Test-Header", "value");

    assertThat(oldValues).containsExactly("value");
    assertThat(headers.get("Test-Header")).containsExactly("value");
  }

  @Test
  void toString_multipleTimes_shouldBeConsistent() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    delegate.add("Header", "value");
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    String toString1 = headers.toString();
    String toString2 = headers.toString();

    assertThat(toString1).isEqualTo(toString2);
  }

  @Test
  void delegateChanges_shouldAffectDecoratorImmediately() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    delegate.add("Header", "value1");
    assertThat(headers.getFirst("Header")).isEqualTo("value1");

    delegate.setHeader("Header", "value2");
    assertThat(headers.getFirst("Header")).isEqualTo("value2");

    delegate.remove("Header");
    assertThat(headers.getFirst("Header")).isNull();
  }

  @Test
  void allMethods_shouldWorkAfterMultipleOperations() {
    DefaultHttpHeaders delegate = new DefaultHttpHeaders();
    DecoratingHttpHeaders headers = new DecoratingHttpHeaders(delegate);

    headers.add("H1", "v1");
    headers.add("H2", "v2");
    headers.add("H1", "v3");

    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.isEmpty()).isFalse();
    assertThat(headers.getFirst("H1")).isEqualTo("v1");
    assertThat(headers.get("H1")).containsExactly("v1", "v3");
    assertThat(headers.names()).containsExactlyInAnyOrder("H1", "H2");
    assertThat(headers.entries()).hasSize(2);

    headers.remove("H1");

    assertThat(headers.size()).isEqualTo(1);
    assertThat(headers.get("H1")).isNull();
    assertThat(headers.getFirst("H2")).isEqualTo("v2");
  }

}