/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.ssl;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultSslBundleRegistry}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class DefaultSslBundleRegistryTests {

  private final SslBundle bundle1 = mock(SslBundle.class);

  private final SslBundle bundle2 = mock(SslBundle.class);

  private DefaultSslBundleRegistry registry;

  @BeforeEach
  void setUp() {
    this.registry = new DefaultSslBundleRegistry();
  }

  @Test
  void createWithNameAndBundleRegistersBundle() {
    DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry("test", this.bundle1);
    assertThat(registry.getBundle("test")).isSameAs(this.bundle1);
  }

  @Test
  void registerBundleWhenNameIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerBundle(null, this.bundle1))
            .withMessage("Name must not be null");
  }

  @Test
  void registerBundleWhenBundleIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerBundle("test", null))
            .withMessage("Bundle must not be null");
  }

  @Test
  void registerBundleWhenNameIsTakenThrowsException() {
    this.registry.registerBundle("test", this.bundle1);
    assertThatIllegalStateException().isThrownBy(() -> this.registry.registerBundle("test", this.bundle2))
            .withMessage("Cannot replace existing SSL bundle 'test'");
  }

  @Test
  void registerBundleRegistersBundle() {
    this.registry.registerBundle("test", this.bundle1);
    assertThat(this.registry.getBundle("test")).isSameAs(this.bundle1);
  }

  @Test
  void getBundleWhenNameIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.registry.getBundle(null))
            .withMessage("Name must not be null");
  }

  @Test
  void getBundleWhenNoSuchBundleThrowsException() {
    this.registry.registerBundle("test", this.bundle1);
    assertThatExceptionOfType(NoSuchSslBundleException.class).isThrownBy(() -> this.registry.getBundle("missing"))
            .satisfies((ex) -> assertThat(ex.getBundleName()).isEqualTo("missing"));
  }

  @Test
  void getBundleReturnsBundle() {
    this.registry.registerBundle("test1", this.bundle1);
    this.registry.registerBundle("test2", this.bundle2);
    assertThat(this.registry.getBundle("test1")).isSameAs(this.bundle1);
    assertThat(this.registry.getBundle("test2")).isSameAs(this.bundle2);
  }

  @Test
  void updateBundleShouldNotifyUpdateHandlers() {
    AtomicReference<SslBundle> updatedBundle = new AtomicReference<>();
    this.registry.registerBundle("test1", this.bundle1);
    this.registry.addBundleUpdateHandler("test1", updatedBundle::set);
    this.registry.updateBundle("test1", this.bundle2);
    Awaitility.await().untilAtomic(updatedBundle, Matchers.equalTo(this.bundle2));
  }

  @Test
  void shouldFailIfUpdatingNonRegisteredBundle() {
    assertThatExceptionOfType(NoSuchSslBundleException.class)
            .isThrownBy(() -> this.registry.updateBundle("dummy", this.bundle1))
            .withMessageContaining("'dummy'");
  }

  @Test
  void shouldLogIfUpdatingBundleWithoutListeners(CapturedOutput output) {
    this.registry.registerBundle("test1", this.bundle1);
    this.registry.getBundle("test1");
    this.registry.updateBundle("test1", this.bundle2);
    assertThat(output).contains(
            "SSL bundle 'test1' has been updated but may be in use by a technology that doesn't support SSL reloading");
  }

}
