/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package schemacrawler.server.kylin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.function.Predicate;

import org.junit.BeforeClass;
import org.junit.Test;

import schemacrawler.schemacrawler.DatabaseServerType;
import schemacrawler.tools.executable.commandline.PluginCommand;
import schemacrawler.tools.executable.commandline.PluginCommandOption;

/**
 * Unit tests for {@link KylinDatabaseConnector}.
 *
 * <p>These tests verify the contract that {@link KylinDatabaseConnector}
 * exposes to SchemaCrawler's plug-in registry: the database system
 * identifier is {@code "kylin"}, the help command advertises the four
 * expected Kylin connection options and the URL predicate accepts every
 * {@code jdbc:kylin:...} URL while rejecting arbitrary other JDBC URLs.
 * Constructor behaviour is exercised end-to-end so the JDBC driver
 * loading step is covered as well.</p>
 *
 * @since 3.0.0
 */
public class KylinDatabaseConnectorTest {

    /**
     * Cached connector under test.
     */
    private static KylinDatabaseConnector connector;

    /**
     * Cached help command under test.
     */
    private static PluginCommand helpCommand;

    /**
     * Cached URL predicate under test.
     */
    private static Predicate<String> predicate;

    /**
     * Bootstraps the connector exactly once for the whole class so each
     * test method observes the same instance and the JDBC driver loading
     * side effect happens only once.
     *
     * @throws Exception if the underlying {@link KylinDatabaseConnector}
     *                   constructor fails (e.g. JDBC driver missing on
     *                   the classpath).
     */
    @BeforeClass
    public static void setUp() throws Exception {
        connector = new KylinDatabaseConnector();
        helpCommand = connector.getHelpCommand();
        predicate = connector.supportsUrlPredicate();
    }

    /**
     * The connector must be instantiable via its no-arg constructor and
     * produce a non-null instance; this also covers the implicit
     * {@code Class.forName("org.apache.kylin.jdbc.Driver")} branch.
     *
     * @throws Exception never thrown under the standard classpath.
     */
    @Test
    public void shouldInstantiateViaDefaultConstructor() throws Exception {
        assertNotNull(connector);
    }

    /**
     * The {@link DatabaseServerType} returned by the connector must
     * advertise the {@code "kylin"} identifier and the
     * {@code "Apache Kylin"} display name so SchemaCrawler can route
     * URLs to this plug-in.
     */
    @Test
    public void shouldExposeKylinDatabaseServerType() {
        DatabaseServerType serverType = connector.getDatabaseServerType();
        assertNotNull("database server type must not be null", serverType);
        assertEquals("kylin", serverType.getDatabaseSystemIdentifier());
        assertEquals("Apache Kylin", serverType.getDatabaseSystemName());
    }

    /**
     * The connector's help command must always be non-null so callers
     * can safely iterate over its options.
     */
    @Test
    public void shouldReturnNonNullHelpCommand() {
        assertNotNull(helpCommand);
    }

    /**
     * The help command must expose exactly the four Kylin-specific
     * options declared by
     * {@link KylinDatabaseConnector#getHelpCommand()}.
     */
    @Test
    public void shouldExposeFourKylinHelpOptions() {
        int optionCount = 0;
        for (PluginCommandOption option : helpCommand) {
            assertNotNull("option must not be null", option);
            optionCount++;
        }
        assertEquals("expected exactly 4 help options for Kylin", 4, optionCount);
    }

    /**
     * The four advertised options must be {@code server}, {@code host},
     * {@code port} and {@code database} in that exact order.
     */
    @Test
    public void shouldAdvertiseServerHostPortDatabaseOptions() {
        Iterator<PluginCommandOption> iterator = helpCommand.iterator();
        assertEquals("server", iterator.next().getName());
        assertEquals("host", iterator.next().getName());
        assertEquals("port", iterator.next().getName());
        assertEquals("database", iterator.next().getName());
        assertFalse("no extra options expected", iterator.hasNext());
    }

    /**
     * The {@code server} option must reference {@code --server=kylin} in
     * its help text so users know how to load the plug-in from the CLI.
     */
    @Test
    public void shouldDocumentServerOptionWithKylin() {
        PluginCommandOption serverOption = helpCommand.iterator().next();
        assertEquals("server", serverOption.getName());
        assertNotNull("server help text must not be null", serverOption.getHelpText());
        assertTrue("server help text must mention --server=kylin",
                serverOption.getHelpText().contains("--server=kylin"));
    }

    /**
     * The help command must iterate as expected because SchemaCrawler
     * iterates the options when rendering the command-line help, and the
     * iteration must be stable across passes.
     */
    @Test
    public void shouldIterateHelpOptionsConsistently() {
        int firstCount = 0;
        for (PluginCommandOption ignored : helpCommand) {
            firstCount++;
        }
        int secondCount = 0;
        for (PluginCommandOption ignored : helpCommand) {
            secondCount++;
        }
        assertEquals("iteration must be stable across passes", firstCount, secondCount);
        assertTrue("help command must advertise at least one option", firstCount > 0);
    }

    /**
     * The URL predicate must accept a plain Kylin JDBC URL of the form
     * {@code jdbc:kylin://localhost:7070/default}.
     */
    @Test
    public void shouldAcceptCanonicalKylinJdbcUrl() {
        assertTrue(predicate.test("jdbc:kylin://localhost:7070/default"));
    }

    /**
     * The URL predicate must also accept Kylin URLs that carry
     * additional connection properties after the colon (the {@code .*}
     * in the regex).
     */
    @Test
    public void shouldAcceptKylinJdbcUrlWithTrailingProperties() {
        assertTrue(predicate.test("jdbc:kylin:http://localhost:7070?project=learn_kylin"));
        assertTrue(predicate.test("jdbc:kylin://kylin-host:443/kylin;user=alice"));
    }

    /**
     * The URL predicate must reject URLs that do not begin with the
     * {@code jdbc:kylin:} scheme; case sensitivity matters.
     */
    @Test
    public void shouldRejectNonKylinJdbcUrls() {
        assertFalse(predicate.test("jdbc:mysql://localhost:3306/kylin"));
        assertFalse(predicate.test("jdbc:postgresql://localhost:5432/db"));
        assertFalse(predicate.test("jdbc:hive2://localhost:10000/default"));
        assertFalse(predicate.test("http://localhost:7070"));
        assertFalse(predicate.test(""));
        assertFalse(predicate.test("jdbc:Kylin://localhost:7070/default"));
    }

    /**
     * Calling {@code supportsUrl(String)} (which delegates to the
     * predicate) must behave identically to probing the predicate
     * directly; this guarantees the public API still works after the
     * override.
     */
    @Test
    public void shouldBehaveConsistentlyWithSupportsUrl() {
        assertTrue(connector.supportsUrl("jdbc:kylin://localhost:7070/default"));
        assertFalse(connector.supportsUrl("jdbc:mysql://localhost:3306/db"));
    }
}