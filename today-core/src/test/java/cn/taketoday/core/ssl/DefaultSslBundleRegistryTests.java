/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import org.junit.jupiter.api.Test;

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
class DefaultSslBundleRegistryTests {

  private SslBundle bundle1 = mock(SslBundle.class);

  private SslBundle bundle2 = mock(SslBundle.class);

  private DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();

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

}
