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

package infra.dao.support;

import org.junit.jupiter.api.Test;

import infra.dao.DataAccessException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Rod Johnson
 * @since 2.0
 */
class ChainedPersistenceExceptionTranslatorTests {

  @Test
  void empty() {
    ChainedPersistenceExceptionTranslator pet = new ChainedPersistenceExceptionTranslator();
    //MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
    RuntimeException in = new RuntimeException("in");
    assertThat(DataAccessUtils.translateIfNecessary(in, pet)).isSameAs(in);
  }

  @Test
  void exceptionTranslationWithTranslation() {
    MapPersistenceExceptionTranslator mpet1 = new MapPersistenceExceptionTranslator();
    RuntimeException in1 = new RuntimeException("in");
    InvalidDataAccessApiUsageException out1 = new InvalidDataAccessApiUsageException("out");
    InvalidDataAccessApiUsageException out2 = new InvalidDataAccessApiUsageException("out");
    mpet1.addTranslation(in1, out1);

    ChainedPersistenceExceptionTranslator chainedPet1 = new ChainedPersistenceExceptionTranslator();
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should not translate yet").isSameAs(in1);
    chainedPet1.addDelegate(mpet1);
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should now translate").isSameAs(out1);

    // Now add a new translator and verify it wins
    MapPersistenceExceptionTranslator mpet2 = new MapPersistenceExceptionTranslator();
    mpet2.addTranslation(in1, out2);
    chainedPet1.addDelegate(mpet2);
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should still translate the same due to ordering").isSameAs(out1);

    ChainedPersistenceExceptionTranslator chainedPet2 = new ChainedPersistenceExceptionTranslator();
    chainedPet2.addDelegate(mpet2);
    chainedPet2.addDelegate(mpet1);
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet2)).as("Should translate differently due to ordering").isSameAs(out2);

    RuntimeException in2 = new RuntimeException("in2");
    OptimisticLockingFailureException out3 = new OptimisticLockingFailureException("out2");
    assertThat(chainedPet2.translateExceptionIfPossible(in2)).isNull();
    MapPersistenceExceptionTranslator mpet3 = new MapPersistenceExceptionTranslator();
    mpet3.addTranslation(in2, out3);
    chainedPet2.addDelegate(mpet3);
    assertThat(chainedPet2.translateExceptionIfPossible(in2)).isSameAs(out3);
  }

  @Test
  void constructorCreatesEmptyChain() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();

    PersistenceExceptionTranslator[] delegates = translator.getDelegates();
    assertThat(delegates).isEmpty();
  }

  @Test
  void addDelegateAndRetrieveDelegates() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate1 = new MapPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate2 = new MapPersistenceExceptionTranslator();

    translator.addDelegate(delegate1);
    translator.addDelegate(delegate2);

    PersistenceExceptionTranslator[] delegates = translator.getDelegates();
    assertThat(delegates).hasSize(2);
    assertThat(delegates[0]).isSameAs(delegate1);
    assertThat(delegates[1]).isSameAs(delegate2);
  }

  @Test
  void addNullDelegateThrowsException() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();

    assertThatThrownBy(() -> translator.addDelegate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PersistenceExceptionTranslator is required");
  }

  @Test
  void translateExceptionIfPossibleWithNoDelegatesReturnsNull() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();
    RuntimeException ex = new RuntimeException("test");

    DataAccessException result = translator.translateExceptionIfPossible(ex);
    assertThat(result).isNull();
  }

  @Test
  void translateExceptionIfPossibleWithFirstDelegateMatching() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate1 = new MapPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate2 = new MapPersistenceExceptionTranslator();

    RuntimeException ex = new RuntimeException("test");
    InvalidDataAccessApiUsageException translatedException = new InvalidDataAccessApiUsageException("translated");
    delegate1.addTranslation(ex, translatedException);

    translator.addDelegate(delegate1);
    translator.addDelegate(delegate2);

    DataAccessException result = translator.translateExceptionIfPossible(ex);
    assertThat(result).isSameAs(translatedException);
  }

  @Test
  void translateExceptionIfPossibleWithSecondDelegateMatching() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate1 = new MapPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate2 = new MapPersistenceExceptionTranslator();

    RuntimeException ex = new RuntimeException("test");
    InvalidDataAccessApiUsageException translatedException = new InvalidDataAccessApiUsageException("translated");
    delegate2.addTranslation(ex, translatedException);

    translator.addDelegate(delegate1);
    translator.addDelegate(delegate2);

    DataAccessException result = translator.translateExceptionIfPossible(ex);
    assertThat(result).isSameAs(translatedException);
  }

  @Test
  void translateExceptionIfPossibleWithNoMatchesReturnsNull() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate1 = new MapPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate2 = new MapPersistenceExceptionTranslator();

    RuntimeException ex = new RuntimeException("test");

    translator.addDelegate(delegate1);
    translator.addDelegate(delegate2);

    DataAccessException result = translator.translateExceptionIfPossible(ex);
    assertThat(result).isNull();
  }

  @Test
  void delegatesAreCheckedInOrder() {
    ChainedPersistenceExceptionTranslator translator = new ChainedPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate1 = new MapPersistenceExceptionTranslator();
    MapPersistenceExceptionTranslator delegate2 = new MapPersistenceExceptionTranslator();

    RuntimeException ex = new RuntimeException("test");
    InvalidDataAccessApiUsageException translatedException1 = new InvalidDataAccessApiUsageException("translated1");
    InvalidDataAccessApiUsageException translatedException2 = new InvalidDataAccessApiUsageException("translated2");

    delegate1.addTranslation(ex, translatedException1);
    delegate2.addTranslation(ex, translatedException2);

    translator.addDelegate(delegate1);
    translator.addDelegate(delegate2);

    DataAccessException result = translator.translateExceptionIfPossible(ex);
    assertThat(result).isSameAs(translatedException1); // First delegate should win
  }

}
