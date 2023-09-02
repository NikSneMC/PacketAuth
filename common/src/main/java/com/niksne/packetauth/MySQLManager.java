package com.niksne.packetauth;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySQLManager {
    private Connection connection;
    private String tokensTableName;

    private final Logger logger = Logger.getLogger("Packet Auth");

    public MySQLManager(String host, int port, String database, String tokensTableName, String username, String password) {
        try {
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setServerName(host);
            dataSource.setPort(port);
            dataSource.setUser(username);
            dataSource.setPassword(password);
            dataSource.setDatabaseName(database);
            connection = dataSource.getConnection();
            createTable(tokensTableName, "token");
            this.tokensTableName = tokensTableName;
        } catch (SQLException e) {
            raise(e);
        }
    }

    public void createTable(String tableName, String valueKey) {
        execute("", String.format("CREATE TABLE IF NOT EXISTS %s (name VARCHAR(16), %s VARCHAR(4096))", tableName, valueKey));
    }

    public String getToken(String playerName) {
        try (ResultSet resultSet = execute("query", String.format("SELECT * FROM %s WHERE name = '%s'", tokensTableName, playerName)).getQueryResult()) {
            if (resultSet.next()) return resultSet.getString("token");
        } catch (SQLException e) {
            raise(e);
        }
        return null;
    }

    public String saveToken(String playerName, String token) {
        execute("update", String.format("INSERT INTO %s (name, token) VALUES ('%s', '%s')", tokensTableName, playerName, token));
        return token;
    }

    public boolean hasRecord(String tableName, String key) {
        try (ResultSet resultSet = execute("query", String.format("SELECT * FROM %s WHERE name = '%s'", tableName, key)).getQueryResult()) {
            return resultSet.next();
        } catch (SQLException e) {
            raise(e);
            return false;
        }
    }

    public String getReason(String tableName, String playerName) {
        try (ResultSet resultSet = execute("query", String.format("SELECT * FROM %s WHERE name = '%s'", tableName, playerName)).getQueryResult()) {
            if (resultSet.next()) return resultSet.getString("reason").replace("\"\"", "");
        } catch (SQLException e) {
            raise(e);
        }
        return "";
    }

    public ExecuteReturn execute(String type, String query) {
        try {
            Statement statement = connection.createStatement();
            return switch (type) {
                case "update" -> new ExecuteReturn(statement.executeUpdate(query));
                case "query" -> new ExecuteReturn(statement.executeQuery(query));
                default -> new ExecuteReturn(statement.execute(query));
            };
        }  catch (SQLException e) {
            raise(e);
            return null;
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            raise(e);
        }
    }

    private void raise(Exception e) {
        logger.log(Level.SEVERE, "MySQL error: ", e);
    }
}