/*
 * Copyright 2012-present the original author or authors.
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

package infra.jdbc.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import javax.sql.DataSource;

import infra.jdbc.metadata.DataSourcePoolMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link infra.jdbc.metadata.CompositeDataSourcePoolMetadataProvider}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
class CompositeDataSourcePoolMetadataProviderTests {

  @Mock
  private infra.jdbc.metadata.DataSourcePoolMetadataProvider firstProvider;

  @Mock
  private DataSourcePoolMetadata first;

  @Mock
  private DataSource firstDataSource;

  @Mock
  private infra.jdbc.metadata.DataSourcePoolMetadataProvider secondProvider;

  @Mock
  private DataSourcePoolMetadata second;

  @Mock
  private DataSource secondDataSource;

  @Mock
  private DataSource unknownDataSource;

  @BeforeEach
  void setup() {
    given(this.firstProvider.getDataSourcePoolMetadata(this.firstDataSource)).willReturn(this.first);
    given(this.firstProvider.getDataSourcePoolMetadata(this.secondDataSource)).willReturn(this.second);
  }

  @Test
  void createWithProviders() {
    infra.jdbc.metadata.CompositeDataSourcePoolMetadataProvider provider = new infra.jdbc.metadata.CompositeDataSourcePoolMetadataProvider(
            Arrays.asList(this.firstProvider, this.secondProvider));
    assertThat(provider.getDataSourcePoolMetadata(this.firstDataSource)).isSameAs(this.first);
    assertThat(provider.getDataSourcePoolMetadata(this.secondDataSource)).isSameAs(this.second);
    assertThat(provider.getDataSourcePoolMetadata(this.unknownDataSource)).isNull();
  }

}
