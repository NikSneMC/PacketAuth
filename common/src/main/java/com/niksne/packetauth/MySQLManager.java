package com.niksne.packetauth;

import java.sql.*;

public class MySQLManager {
    private Connection connection;
    private String tableName;

    public MySQLManager(String host, int port, String database, String tableName, String username, String password) {
        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            connection = DriverManager.getConnection(url, username, password);
            Statement statement = connection.createStatement();
            String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (name VARCHAR(16), token VARCHAR(4096))";
            statement.executeUpdate(query);
            this.tableName = tableName;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getToken(String playerName) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE name = ?");
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("token");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveToken(String playerName, String token) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (name, token) VALUES (?, ?)");
            statement.setString(1, playerName);
            statement.setString(2, token);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPlayer(String playerName) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE name = ?");
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}