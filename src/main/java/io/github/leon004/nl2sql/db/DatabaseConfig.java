package io.github.leon004.nl2sql.db;

/**
 * MySQL connection settings, read from (highest priority first) a JVM system property,
 * an environment variable, or a default that matches a local development install.
 *
 * <table>
 *   <tr><th>System property</th><th>Environment variable</th><th>Default</th></tr>
 *   <tr><td>nl2sql.db.url</td><td>NL2SQL_DB_URL</td><td>jdbc:mysql://localhost:3306/</td></tr>
 *   <tr><td>nl2sql.db.user</td><td>NL2SQL_DB_USER</td><td>root</td></tr>
 *   <tr><td>nl2sql.db.password</td><td>NL2SQL_DB_PASSWORD</td><td>(empty)</td></tr>
 * </table>
 */
public record DatabaseConfig(String url, String user, String password) {

    public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/";

    public static DatabaseConfig fromEnvironment() {
        return new DatabaseConfig(
                setting("nl2sql.db.url", "NL2SQL_DB_URL", DEFAULT_URL),
                setting("nl2sql.db.user", "NL2SQL_DB_USER", "root"),
                setting("nl2sql.db.password", "NL2SQL_DB_PASSWORD", ""));
    }

    private static String setting(String property, String envVar, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public String toString() {
        // Never print the password.
        return "DatabaseConfig[url=" + url + ", user=" + user + "]";
    }
}
