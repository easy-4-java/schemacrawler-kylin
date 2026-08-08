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

import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import schemacrawler.schemacrawler.DatabaseServerType;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.executable.commandline.PluginCommand;
import schemacrawler.tools.iosource.ClasspathInputResource;

/**
 * SchemaCrawler database connector plug-in for Apache Kylin.
 *
 * <p>This connector integrates the Apache Kylin OLAP engine with
 * SchemaCrawler so that schemas, tables, columns and other relational
 * metadata exposed by a Kylin JDBC endpoint can be inspected using the
 * standard SchemaCrawler command line. It declares the {@code kylin}
 * database system identifier, loads the bundled connection configuration
 * from the classpath, points the information-schema views builder at the
 * Kylin-specific resource folder, and eagerly resolves the
 * {@code org.apache.kylin.jdbc.Driver} class so that the JDBC driver is
 * registered with the {@link java.sql.DriverManager} as soon as the
 * plug-in is instantiated.</p>
 *
 * <p>The plug-in follows the standard SchemaCrawler contract for
 * {@link DatabaseConnector} subclasses:</p>
 * <ul>
 *   <li>Advertises the {@code kylin} system identifier through
 *       {@link #getDatabaseServerType()}.</li>
 *   <li>Provides a {@link PluginCommand} describing the available
 *       command-line options (server, host, port and database) via
 *       {@link #getHelpCommand()}.</li>
 *   <li>Recognises Kylin JDBC URLs of the form {@code jdbc:kylin:...}
 *       through {@link #supportsUrlPredicate()}.</li>
 * </ul>
 *
 * <p>The class is declared {@code final} and has a single public
 * constructor; it is intended to be instantiated reflectively by the
 * SchemaCrawler plug-in discovery mechanism (see the
 * {@code META-INF/services} registration file in this module).</p>
 *
 * @author easy-4-java contributors
 * @since 3.0.0
 * @see DatabaseConnector
 * @see DatabaseServerType
 * @see <a href="https://kylin.apache.org/">Apache Kylin</a>
 */
public final class KylinDatabaseConnector extends DatabaseConnector {

    /**
     * Builds a new {@code KylinDatabaseConnector}.
     *
     * <p>The constructor wires the connector to SchemaCrawler with a
     * {@link DatabaseServerType} named {@code "kylin"} (display name
     * {@code "Apache Kylin"}), loads
     * {@code /schemacrawler-kylin.config.properties} as the default
     * connection configuration, points the information-schema views
     * builder at the bundled {@code /kylin.information_schema} resource
     * folder, and forces the loading of {@code org.apache.kylin.jdbc.Driver}
     * so that the JDBC driver is visible to the
     * {@link java.sql.DriverManager} before any connection is opened.</p>
     *
     * @throws IOException if the bundled configuration resource cannot be
     *         read while creating the parent {@link DatabaseConnector}.
     * @throws RuntimeException wrapping any {@link ClassNotFoundException}
     *         thrown when the Kylin JDBC driver is not present on the
     *         classpath.
     */
    public KylinDatabaseConnector() throws IOException {

        super(new DatabaseServerType("kylin", "Apache Kylin"),
                new ClasspathInputResource("/schemacrawler-kylin.config.properties"), (informationSchemaViewsBuilder,
                        connection) -> informationSchemaViewsBuilder.fromResourceFolder("/kylin.information_schema"));
        try {
            Class.forName("org.apache.kylin.jdbc.Driver");
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Could not load Kylin JDBC driver", e);
        }
    }

    /**
     * Returns the SchemaCrawler command-line help description for this
     * connector, augmented with Kylin-specific connection options.
     *
     * <p>The base {@link DatabaseConnector#getHelpCommand()} provides the
     * plug-in name and description; this override enriches it with the
     * following Kylin-oriented options:</p>
     * <ul>
     *   <li>{@code server} &mdash; instructs SchemaCrawler to load the
     *       Kylin plug-in.</li>
     *   <li>{@code host} &mdash; Kylin server host (default
     *       {@code localhost}).</li>
     *   <li>{@code port} &mdash; Kylin server port (default
     *       {@code 3306}).</li>
     *   <li>{@code database} &mdash; target Kylin database name.</li>
     * </ul>
     *
     * @return a non-null {@link PluginCommand} containing the Kylin
     *         connection options; never {@code null}.
     */
    @Override
    public PluginCommand getHelpCommand() {
        final PluginCommand pluginCommand = super.getHelpCommand();
        pluginCommand.addOption("server", "--server=kylin%n" + "Loads SchemaCrawler plug-in for Apache Kylin", String.class)
                .addOption("host", "Host name%n" + "Optional, defaults to localhost", String.class)
                .addOption("port", "Port number%n" + "Optional, defaults to 3306", Integer.class)
                .addOption("database", "Database name", String.class);
        return pluginCommand;
    }

    /**
     * Returns a predicate that recognises Kylin JDBC URLs.
     *
     * <p>The predicate matches any JDBC URL whose scheme is
     * {@code jdbc:kylin} followed by zero or more characters, allowing
     * optional query parameters and connection properties appended after
     * the colon. This is invoked by {@link DatabaseConnector#supportsUrl(String)}
     * to decide whether the connector can serve a given connection URL.</p>
     *
     * @return a non-null {@link Predicate} that yields {@code true} for
     *         Kylin JDBC URLs and {@code false} for every other URL.
     */
    @Override
    protected Predicate<String> supportsUrlPredicate() {
        return url -> Pattern.matches("jdbc:kylin:.*", url);
    }

}