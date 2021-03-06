/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.connectors.jdbc.internal.cli;

import static org.apache.geode.connectors.jdbc.internal.cli.CreateMappingCommand.CREATE_MAPPING;
import static org.apache.geode.connectors.jdbc.internal.cli.CreateMappingCommand.CREATE_MAPPING__CONNECTION_NAME;
import static org.apache.geode.connectors.jdbc.internal.cli.CreateMappingCommand.CREATE_MAPPING__FIELD_MAPPING;
import static org.apache.geode.connectors.jdbc.internal.cli.CreateMappingCommand.CREATE_MAPPING__PDX_CLASS_NAME;
import static org.apache.geode.connectors.jdbc.internal.cli.CreateMappingCommand.CREATE_MAPPING__REGION_NAME;
import static org.apache.geode.connectors.jdbc.internal.cli.CreateMappingCommand.CREATE_MAPPING__TABLE_NAME;
import static org.apache.geode.connectors.jdbc.internal.cli.CreateMappingCommand.CREATE_MAPPING__VALUE_CONTAINS_PRIMARY_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.connectors.jdbc.internal.JdbcConnectorService;
import org.apache.geode.connectors.jdbc.internal.configuration.ConnectorService;
import org.apache.geode.distributed.internal.InternalLocator;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.management.internal.cli.util.CommandStringBuilder;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.categories.JDBCConnectorTest;
import org.apache.geode.test.junit.rules.GfshCommandRule;
import org.apache.geode.test.junit.rules.serializable.SerializableTestName;

@Category({JDBCConnectorTest.class})
public class CreateMappingCommandDUnitTest {

  @Rule
  public transient GfshCommandRule gfsh = new GfshCommandRule();

  @Rule
  public ClusterStartupRule startupRule = new ClusterStartupRule();

  @Rule
  public SerializableTestName testName = new SerializableTestName();

  private MemberVM locator;
  private MemberVM server;

  @Before
  public void before() throws Exception {
    locator = startupRule.startLocatorVM(0);
    server = startupRule.startServerVM(1, locator.getPort());

    gfsh.connectAndVerify(locator);
  }

  @Test
  public void createsMappingWithAllOptions() {
    CommandStringBuilder csb = new CommandStringBuilder(CREATE_MAPPING);
    csb.addOption(CREATE_MAPPING__REGION_NAME, "testRegion");
    csb.addOption(CREATE_MAPPING__CONNECTION_NAME, "connection");
    csb.addOption(CREATE_MAPPING__TABLE_NAME, "myTable");
    csb.addOption(CREATE_MAPPING__PDX_CLASS_NAME, "myPdxClass");
    csb.addOption(CREATE_MAPPING__VALUE_CONTAINS_PRIMARY_KEY, "true");
    csb.addOption(CREATE_MAPPING__FIELD_MAPPING, "field1:column1,field2:column2");

    gfsh.executeAndAssertThat(csb.toString()).statusIsSuccess();

    locator.invoke(() -> {
      String xml = InternalLocator.getLocator().getConfigurationPersistenceService()
          .getConfiguration("cluster").getCacheXmlContent();
      assertThat(xml).isNotNull().contains("jdbc:connector-service")
          .doesNotContain("jdbc:connection").contains("jdbc:region-mapping")
          .contains("jdbc:field-mapping");
    });

    server.invoke(() -> {
      InternalCache cache = ClusterStartupRule.getCache();
      ConnectorService.RegionMapping mapping =
          cache.getService(JdbcConnectorService.class).getMappingForRegion("testRegion");
      assertThat(mapping.getConnectionConfigName()).isEqualTo("connection");
      assertThat(mapping.getTableName()).isEqualTo("myTable");
      assertThat(mapping.getPdxClassName()).isEqualTo("myPdxClass");
      assertThat(mapping.isPrimaryKeyInValue()).isEqualTo(true);
      List<ConnectorService.RegionMapping.FieldMapping> fieldMappings = mapping.getFieldMapping();
      assertThat(fieldMappings).hasSize(2);
      assertThat(fieldMappings.get(0).getFieldName()).isEqualTo("field1");
      assertThat(fieldMappings.get(0).getColumnName()).isEqualTo("column1");
      assertThat(fieldMappings.get(1).getFieldName()).isEqualTo("field2");
      assertThat(fieldMappings.get(1).getColumnName()).isEqualTo("column2");
    });
  }

  @Test
  public void validateThatPrimaryKeyInValueDefaultsToFalse() {
    CommandStringBuilder csb = new CommandStringBuilder(CREATE_MAPPING);
    csb.addOption(CREATE_MAPPING__REGION_NAME, "testRegion");
    csb.addOption(CREATE_MAPPING__CONNECTION_NAME, "connection");
    csb.addOption(CREATE_MAPPING__TABLE_NAME, "myTable");
    csb.addOption(CREATE_MAPPING__PDX_CLASS_NAME, "myPdxClass");
    csb.addOption(CREATE_MAPPING__FIELD_MAPPING, "field1:column1,field2:column2");

    gfsh.executeAndAssertThat(csb.toString()).statusIsSuccess();

    locator.invoke(() -> {
      String xml = InternalLocator.getLocator().getConfigurationPersistenceService()
          .getConfiguration("cluster").getCacheXmlContent();
      assertThat(xml).isNotNull().contains("primary-key-in-value=\"false\"");
    });

    server.invoke(() -> {
      InternalCache cache = ClusterStartupRule.getCache();
      ConnectorService.RegionMapping mapping =
          cache.getService(JdbcConnectorService.class).getMappingForRegion("testRegion");
      assertThat(mapping.getConnectionConfigName()).isEqualTo("connection");
      assertThat(mapping.getTableName()).isEqualTo("myTable");
      assertThat(mapping.getPdxClassName()).isEqualTo("myPdxClass");
      assertThat(mapping.isPrimaryKeyInValue()).isEqualTo(false);
      List<ConnectorService.RegionMapping.FieldMapping> fieldMappings = mapping.getFieldMapping();
      assertThat(fieldMappings).hasSize(2);
      assertThat(fieldMappings.get(0).getFieldName()).isEqualTo("field1");
      assertThat(fieldMappings.get(0).getColumnName()).isEqualTo("column1");
      assertThat(fieldMappings.get(1).getFieldName()).isEqualTo("field2");
      assertThat(fieldMappings.get(1).getColumnName()).isEqualTo("column2");
    });
  }

  @Test
  public void createsRegionMappingWithoutFieldMap() {
    CommandStringBuilder csb = new CommandStringBuilder(CREATE_MAPPING);
    csb.addOption(CREATE_MAPPING__REGION_NAME, "testRegion");
    csb.addOption(CREATE_MAPPING__CONNECTION_NAME, "connection");
    csb.addOption(CREATE_MAPPING__TABLE_NAME, "myTable");
    csb.addOption(CREATE_MAPPING__PDX_CLASS_NAME, "myPdxClass");
    csb.addOption(CREATE_MAPPING__VALUE_CONTAINS_PRIMARY_KEY, "false");

    gfsh.executeAndAssertThat(csb.toString()).statusIsSuccess();

    locator.invoke(() -> {
      String xml = InternalLocator.getLocator().getConfigurationPersistenceService()
          .getConfiguration("cluster").getCacheXmlContent();
      assertThat(xml).isNotNull().contains("jdbc:connector-service");
      assertThat(xml).isNotNull().contains("jdbc:region-mapping");
      assertThat(xml).isNotNull().doesNotContain("jdbc:field-mapping");
    });
  }
}
