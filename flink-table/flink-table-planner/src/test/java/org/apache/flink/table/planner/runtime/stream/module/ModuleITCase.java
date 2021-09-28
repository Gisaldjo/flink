/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.planner.runtime.stream.module;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.Factory;
import org.apache.flink.table.module.Module;
import org.apache.flink.table.planner.factories.TableFactoryHarness;
import org.apache.flink.table.planner.runtime.utils.StreamingTestBase;

import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static org.apache.flink.core.testutils.CommonTestUtils.assertThrows;

/** Tests for modules. */
public class ModuleITCase extends StreamingTestBase {

    @Test
    public void testTableSourceFactory() {
        tEnv().createTemporaryTable(
                        "T",
                        TableFactoryHarness.newBuilder()
                                .schema(Schema.newBuilder().build())
                                .source(
                                        new TableFactoryHarness.ScanSourceBase() {
                                            @Override
                                            public ScanRuntimeProvider getScanRuntimeProvider(
                                                    ScanContext runtimeProviderContext) {
                                                throw new UnsupportedOperationException(
                                                        "Discovered factory should not be used");
                                            }
                                        })
                                .build());

        final Table table = tEnv().from("T");

        // Sanity check: without our module loaded, the factory discovery process is used.
        assertThrows(
                "Discovered factory should not be used",
                UnsupportedOperationException.class,
                table::explain);

        // The module has precedence over factory discovery.
        tEnv().loadModule("M", new SourceSinkFactoryOverwriteModule());
        table.explain();
    }

    // ---------------------------------------------------------------------------------------------

    private static class SourceSinkFactoryOverwriteModule implements Module {
        @Override
        public Optional<DynamicTableSourceFactory> getTableSourceFactory() {
            return Optional.of(new SourceFactory());
        }
    }

    private static class SourceFactory extends FactoryBase implements DynamicTableSourceFactory {
        @Override
        public DynamicTableSource createDynamicTableSource(Context context) {
            return new TableFactoryHarness.ScanSourceBase() {};
        }
    }

    private static class FactoryBase implements Factory {
        @Override
        public String factoryIdentifier() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<ConfigOption<?>> requiredOptions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<ConfigOption<?>> optionalOptions() {
            throw new UnsupportedOperationException();
        }
    }
}
