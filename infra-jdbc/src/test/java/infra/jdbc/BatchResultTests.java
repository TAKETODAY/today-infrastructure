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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 11:32
 */
class BatchResultTests {

  private final RepositoryManager repositoryManager = new RepositoryManager("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");

  @Test
  void shouldReturnLastBatchResultWhenSet() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    int[] expectedResult = { 1, 2, 3 };
    batchResult.setBatchResult(expectedResult);

    int[] actualResult = batchResult.getLastBatchResult();

    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @Test
  void shouldThrowExceptionWhenGetLastBatchResultNotSet() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    assertThatThrownBy(() -> batchResult.getLastBatchResult())
            .isInstanceOf(PersistenceException.class)
            .hasMessage("It is required to call executeBatch() method before calling getLastBatchResult().");
  }

  @Test
  void shouldCalculateAffectedRowsCorrectly() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    batchResult.setBatchResult(new int[] { 1, 1 });
    batchResult.setBatchResult(new int[] { 1, 1, 1 });

    int affectedRows = batchResult.getAffectedRows();

    assertThat(affectedRows).isEqualTo(5);
  }

  @Test
  void shouldThrowExceptionWhenGetAffectedRowsNotSet() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    assertThatThrownBy(() -> batchResult.getAffectedRows())
            .isInstanceOf(PersistenceException.class)
            .hasMessage("It is required to call executeBatch() method before calling getAffectedRows().");
  }

  @Test
  void shouldGetFirstGeneratedKey() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    setGeneratedKeys(batchResult, new Object[] { 1L, 2L, 3L });

    Object key = batchResult.getKey();

    assertThat(key).isEqualTo(1L);
  }

  @Test
  void shouldReturnNullWhenNoGeneratedKeys() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    setGeneratedKeys(batchResult, new Object[] {});

    Object key = batchResult.getKey();

    assertThat(key).isNull();
  }

  @Test
  void shouldThrowExceptionWhenGetKeyWithoutGeneratedKeys() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    assertThatThrownBy(() -> batchResult.getKey())
            .isInstanceOf(GeneratedKeysException.class)
            .hasMessageContaining("Keys where not fetched from database");
  }

  @Test
  void shouldGetGeneratedKeyWithReturnType() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    setGeneratedKeys(batchResult, new Object[] { 42L });

    Long key = batchResult.getKey(Long.class);

    assertThat(key).isEqualTo(42L);
  }

  @Test
  void shouldGetAllGeneratedKeys() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    Object[] keys = { 1L, 2L, 3L };
    setGeneratedKeys(batchResult, keys);

    Object[] resultKeys = batchResult.getKeys();

    assertThat(resultKeys).containsExactly(keys);
  }

  @Test
  void shouldThrowExceptionWhenGetKeysWithoutGeneratedKeys() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    assertThatThrownBy(() -> batchResult.getKeys())
            .isInstanceOf(GeneratedKeysException.class)
            .hasMessageContaining("Keys where not fetched from database");
  }

  @Test
  void shouldGetGeneratedKeysWithReturnType() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    Object[] keys = { 1L, 2L, 3L };
    setGeneratedKeys(batchResult, keys);

    List<Long> resultKeys = batchResult.getKeys(Long.class);

    assertThat(resultKeys).containsExactly(1L, 2L, 3L);
  }

  @Test
  void shouldReturnNullWhenGetKeysWithNoGeneratedKeys() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    setGeneratedKeys(batchResult, new Object[] {});

    Object[] keys = batchResult.getKeys();

    assertThat(keys).isEmpty();
  }

  @Test
  void shouldReturnNullWhenGetKeysWithReturnTypeAndNoGeneratedKeys() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    setGeneratedKeys(batchResult, new Object[] {});

    List<Long> keys = batchResult.getKeys(Long.class);

    assertThat(keys).isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenGetKeyWithReturnTypeAndInvalidConversion() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    setGeneratedKeys(batchResult, new Object[] { "invalid" });

    assertThatThrownBy(() -> batchResult.getKey(Long.class))
            .isInstanceOf(GeneratedKeysConversionException.class)
            .hasMessageContaining("Exception occurred while converting value from database to type class java.lang.Long");
  }

  @Test
  void shouldThrowExceptionWhenGetKeysWithReturnTypeAndInvalidConversion() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    setGeneratedKeys(batchResult, new Object[] { "invalid" });

    assertThatThrownBy(() -> batchResult.getKeys(Long.class))
            .isInstanceOf(GeneratedKeysConversionException.class)
            .hasMessageContaining("Exception occurred while converting value from database to type class java.lang.Long");
  }

  @Test
  void shouldThrowExceptionWhenGetKeyWithNullConversionService() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    setGeneratedKeys(batchResult, new Object[] { 1L });

    assertThatThrownBy(() -> batchResult.getKey(Long.class, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("conversionService is required");
  }

  @Test
  void shouldThrowExceptionWhenGetKeysWithNullConversionService() throws Exception {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    setGeneratedKeys(batchResult, new Object[] { 1L });

    assertThatThrownBy(() -> batchResult.getKeys(Long.class, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("conversionService is required");
  }

  @Test
  void shouldHandleEmptyBatchResult() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));
    batchResult.setBatchResult(new int[] {});

    int[] result = batchResult.getLastBatchResult();
    int affectedRows = batchResult.getAffectedRows();

    assertThat(result).isEmpty();
    assertThat(affectedRows).isZero();
  }

  @Test
  void shouldAccumulateAffectedRowsCorrectly() {
    BatchResult batchResult = new BatchResult(new JdbcConnection(repositoryManager, repositoryManager.obtainDataSource()));

    batchResult.setBatchResult(new int[] { 1 });
    assertThat(batchResult.getAffectedRows()).isEqualTo(1);

    batchResult.setBatchResult(new int[] { 1, 1 });
    assertThat(batchResult.getAffectedRows()).isEqualTo(3);

    batchResult.setBatchResult(new int[] { 1, 1, 1, 1 });
    assertThat(batchResult.getAffectedRows()).isEqualTo(7);
  }

  private void setGeneratedKeys(BatchResult batchResult, Object[] keys) throws Exception {
    var field = BatchResult.class.getDeclaredField("generatedKeys");
    field.setAccessible(true);
    ArrayList<Object> keysList = new ArrayList<>();
    Collections.addAll(keysList, keys);
    field.set(batchResult, keysList);
  }

}