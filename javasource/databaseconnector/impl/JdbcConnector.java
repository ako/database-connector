package databaseconnector.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import databaseconnector.interfaces.ConnectionManager;
import databaseconnector.interfaces.ObjectInstantiator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class JdbcConnector {
  private final ILogNode logNode;
  private ObjectInstantiator objectInstantiator;
  private ConnectionManager connectionManager;

  public JdbcConnector(final ILogNode logNode, ObjectInstantiator objectInstantiator, ConnectionManager connectionManager) {
    this.logNode = logNode;
    this.objectInstantiator = objectInstantiator;
    this.connectionManager = connectionManager;
  }

  public JdbcConnector(ILogNode logNode) {
    this(logNode, new ObjectInstantiatorImpl(), ConnectionManagerSingleton.getInstance());
  }

  /**
   * Returns result of jdbc query as a list of IMendixObjects. Query columns are mapped to mendix attributes based on name.
   *
   * @param jdbcUrl
   * @param userName
   * @param password
   * @param entityName
   * @param sql
   * @param context
   * @return
     * @throws SQLException
     */
  public List<IMendixObject> executeQuery(String jdbcUrl, String userName, String password, String entityName, String sql,
      IContext context) throws SQLException {
    List<IMendixObject> resultList = new ArrayList<>();
    logNode.info(String.format("executeQuery: %s, %s, %s", jdbcUrl, userName, sql));

    // TODO: make sure user doesn't crash runtime due to retrieving too many records
    try (Connection connection = connectionManager.getConnection(jdbcUrl, userName, password);
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
      int columnCount = resultSetMetaData.getColumnCount();

      while (resultSet.next()) {
        IMendixObject obj = objectInstantiator.instantiate(context, entityName);
        for (int i = 0; i < columnCount; i++) {
          String columnName = resultSetMetaData.getColumnName(i + 1);
          Object columnValue = resultSet.getObject(i + 1);
          logNode.info(String.format("setting col: %s = %s", columnName, columnValue));
          obj.setValue(context, columnName, columnValue);
        }
        resultList.add(obj);
        logNode.info("obj: " + obj);
      }
    }
    logNode.info(String.format("List: %d", resultList.size()));
    return resultList;
  }

  /**
   * returns result of jdbc query as a json string
     *
     * @param jdbcUrl
     * @param userName
     * @param password
     * @param entityName
     * @param sql
     * @param context
     * @return
     * @throws SQLException
     */
  public String executeQueryToJson(String jdbcUrl, String userName, String password, String sql,
                                          IContext context) throws SQLException {
    JSONArray jsonArray = new JSONArray();
    logNode.info(String.format("executeQuery: %s, %s, %s", jdbcUrl, userName, sql));

    // TODO: make sure user doesn't crash runtime due to retrieving too many records
    try (Connection connection = connectionManager.getConnection(jdbcUrl, userName, password);
         PreparedStatement preparedStatement = connection.prepareStatement(sql);
         ResultSet resultSet = preparedStatement.executeQuery()) {
      ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
      int columnCount = resultSetMetaData.getColumnCount();

      while (resultSet.next()) {
        JSONObject obj = new JSONObject();
        /*
        IMendixObject obj = objectInstantiator.instantiate(context, entityName);
        */
        for (int i = 0; i < columnCount; i++) {
          String columnName = resultSetMetaData.getColumnName(i + 1);
          Object columnValue = resultSet.getObject(i + 1);
          logNode.info(String.format("setting col: %s = %s", columnName, columnValue));
          obj.put(columnName, columnValue);
          //obj.setValue(context, columnName, columnValue);
        }
        jsonArray.put(obj);
        logNode.info("obj: " + obj);
      }
    }
    //logNode.info(String.format("List: %d", jsonArray.size()));
    return jsonArray.toString();
  }

  public long executeStatement(String jdbcUrl, String userName, String password, String sql) throws SQLException {
    logNode.info(String.format("executeStatement: %s, %s, %s", jdbcUrl, userName, sql));

    try (Connection connection = connectionManager.getConnection(jdbcUrl, userName, password);
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      return preparedStatement.executeUpdate();
    }
  }
}
