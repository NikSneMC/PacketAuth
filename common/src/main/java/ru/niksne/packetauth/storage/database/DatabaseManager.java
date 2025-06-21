package ru.niksne.packetauth.storage.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.storage.StorageType;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String tokensTableName;

    @NotNull
    private final Logger logger = Logger.getLogger("Packet Auth");

    public DatabaseManager(
            @NotNull
            StorageType storageType,
            @NotNull
            String host,
            @NotNull
            Integer port,
            @NotNull
            String user,
            @NotNull
            String password,
            @NotNull
            String database,
            @NotNull
            String tokensTableName
    ) {
        this.jdbcUrl = String.format("jdbc:%s://%s:%d/%s", storageType, host, port, database);
        this.user = user;
        this.password = password;

        createTable(tokensTableName, "token");

        this.tokensTableName = tokensTableName;
    }

    public void createTable(
            @NotNull
            String tableName,
            @NotNull
            String valueKey
    ) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS ? (name VARCHAR(16), ? VARCHAR(4096))");

            statement.setString(1, tableName);
            statement.setString(2, valueKey);

            statement.execute();
        } catch (SQLException e) {
            raise(e);
        }
    }

    @Nullable
    public String getToken(
            @NotNull
            String playerName
    ) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM ? WHERE name = ?");

            statement.setString(1, tokensTableName);
            statement.setString(2, playerName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getString("token");
            }
        } catch (SQLException e) {
            raise(e);
            return null;
        }
        return null;
    }

    public void saveToken(
            @NotNull
            String playerName,
            @NotNull
            String token
    ) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO ? VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, tokensTableName);
            statement.setString(2, playerName);
            statement.setString(3, token);

            statement.executeUpdate();
        } catch (SQLException e) {
            raise(e);
        }
    }

    @NotNull
    public Boolean hasRecord(
            @NotNull
            String tableName,
            @NotNull
            String key,
            @NotNull
            String value
    ) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM ? WHERE ? = ?");

            statement.setString(1, tableName);
            statement.setString(2, key);
            statement.setString(3, value);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            raise(e);
            return false;
        }
    }

    @NotNull
    public String getReason(
            @NotNull
            String tableName,
            @NotNull
            String playerName
    ) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM ? WHERE name = ?");

            statement.setString(1, tableName);
            statement.setString(2, playerName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getString("reason")
                        .replace("\"\"", "");
            }
        } catch (SQLException e) {
            raise(e);
        }
        return "";
    }

    private void raise(
            @NotNull
            Exception e
    ) {
        logger.log(Level.SEVERE, "Database error: ", e);
    }
}