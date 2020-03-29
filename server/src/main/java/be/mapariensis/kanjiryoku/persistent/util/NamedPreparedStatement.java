package be.mapariensis.kanjiryoku.persistent.util;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * Wrapper around PreparedStatement to handle named variables.
 * 
 * @author Matthias Valvekens
 */
@SuppressWarnings("unused")
public class NamedPreparedStatement implements Statement, AutoCloseable {
    private final PreparedStatement backend;
    private final Map<String, Integer> index;

    NamedPreparedStatement(PreparedStatement backend, Map<String, Integer> index) {
        this.backend = backend;
        this.index = index;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return backend.unwrap(iface);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return backend.executeQuery(sql);
    }

    public ResultSet executeQuery() throws SQLException {
        return backend.executeQuery();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return backend.isWrapperFor(iface);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return backend.executeUpdate(sql);
    }

    public int executeUpdate() throws SQLException {
        return backend.executeUpdate();
    }

    public void setNull(String varName, int sqlType) throws SQLException {
        backend.setNull(getIndex(varName), sqlType);
    }

    @Override
    public void close() throws SQLException {
        backend.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return backend.getMaxFieldSize();
    }

    public void setBoolean(String varName, boolean x) throws SQLException {
        backend.setBoolean(getIndex(varName), x);
    }

    public void setByte(String varName, byte x) throws SQLException {
        backend.setByte(getIndex(varName), x);
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        backend.setMaxFieldSize(max);
    }

    public void setShort(String varName, short x) throws SQLException {
        backend.setShort(getIndex(varName), x);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return backend.getMaxRows();
    }

    public void setInt(String varName, int x) throws SQLException {
        backend.setInt(getIndex(varName), x);
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        backend.setMaxRows(max);
    }

    public void setLong(String varName, long x) throws SQLException {
        backend.setLong(getIndex(varName), x);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        backend.setEscapeProcessing(enable);
    }

    public void setFloat(String varName, float x) throws SQLException {
        backend.setFloat(getIndex(varName), x);
    }

    public void setDouble(String varName, double x) throws SQLException {
        backend.setDouble(getIndex(varName), x);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return backend.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        backend.setQueryTimeout(seconds);
    }

    public void setBigDecimal(String varName, BigDecimal x) throws SQLException {
        backend.setBigDecimal(getIndex(varName), x);
    }

    public void setString(String varName, String x) throws SQLException {
        backend.setString(getIndex(varName), x);
    }

    public void setBytes(String varName, byte[] x) throws SQLException {
        backend.setBytes(getIndex(varName), x);
    }

    @Override
    public void cancel() throws SQLException {
        backend.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return backend.getWarnings();
    }

    public void setDate(String varName, Date x) throws SQLException {
        backend.setDate(getIndex(varName), x);
    }

    public void setTime(String varName, Time x) throws SQLException {
        backend.setTime(getIndex(varName), x);
    }

    @Override
    public void clearWarnings() throws SQLException {
        backend.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        backend.setCursorName(name);
    }

    public void setTimestamp(String varName, Timestamp x) throws SQLException {
        backend.setTimestamp(getIndex(varName), x);
    }

    public void setAsciiStream(String varName, InputStream x, int length)
            throws SQLException {
        backend.setAsciiStream(getIndex(varName), x, length);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return backend.execute(sql);
    }

    @Deprecated
    public void setUnicodeStream(String varName, InputStream x, int length)
            throws SQLException {
        backend.setUnicodeStream(getIndex(varName), x, length);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return backend.getResultSet();
    }

    public void setBinaryStream(String varName, InputStream x, int length)
            throws SQLException {
        backend.setBinaryStream(getIndex(varName), x, length);
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return backend.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return backend.getMoreResults();
    }

    public void clearParameters() throws SQLException {
        backend.clearParameters();
    }

    public void setObject(String varName, Object x, int targetSqlType)
            throws SQLException {
        backend.setObject(getIndex(varName), x, targetSqlType);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        backend.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return backend.getFetchDirection();
    }

    public void setObject(String varName, Object x) throws SQLException {
        backend.setObject(getIndex(varName), x);
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        backend.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return backend.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return backend.getResultSetConcurrency();
    }

    public boolean execute() throws SQLException {
        return backend.execute();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return backend.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        backend.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        backend.clearBatch();
    }

    public void addBatch() throws SQLException {
        backend.addBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return backend.executeBatch();
    }

    public void setCharacterStream(String varName, Reader reader, int length)
            throws SQLException {
        backend.setCharacterStream(getIndex(varName), reader, length);
    }

    public void setRef(String varName, Ref x) throws SQLException {
        backend.setRef(getIndex(varName), x);
    }

    public void setBlob(String varName, Blob x) throws SQLException {
        backend.setBlob(getIndex(varName), x);
    }

    public void setClob(String varName, Clob x) throws SQLException {
        backend.setClob(getIndex(varName), x);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return backend.getConnection();
    }

    public void setArray(String varName, Array x) throws SQLException {
        backend.setArray(getIndex(varName), x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return backend.getMetaData();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return backend.getMoreResults(current);
    }

    public void setDate(String varName, Date x, Calendar cal)
            throws SQLException {
        backend.setDate(getIndex(varName), x, cal);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return backend.getGeneratedKeys();
    }

    public void setTime(String varName, Time x, Calendar cal)
            throws SQLException {
        backend.setTime(getIndex(varName), x, cal);
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException {
        return backend.executeUpdate(sql, autoGeneratedKeys);
    }

    public void setTimestamp(String varName, Timestamp x, Calendar cal)
            throws SQLException {
        backend.setTimestamp(getIndex(varName), x, cal);
    }

    public void setNull(String varName, int sqlType, String typeName)
            throws SQLException {
        backend.setNull(getIndex(varName), sqlType, typeName);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes)
            throws SQLException {
        return backend.executeUpdate(sql, columnIndexes);
    }

    public void setURL(String varName, URL x) throws SQLException {
        backend.setURL(getIndex(varName), x);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames)
            throws SQLException {
        return backend.executeUpdate(sql, columnNames);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return backend.getParameterMetaData();
    }

    public void setRowId(String varName, RowId x) throws SQLException {
        backend.setRowId(getIndex(varName), x);
    }

    public void setNString(String varName, String value) throws SQLException {
        backend.setNString(getIndex(varName), value);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys)
            throws SQLException {
        return backend.execute(sql, autoGeneratedKeys);
    }

    public void setNCharacterStream(String varName, Reader value, long length)
            throws SQLException {
        backend.setNCharacterStream(getIndex(varName), value, length);
    }

    public void setNClob(String varName, NClob value) throws SQLException {
        backend.setNClob(getIndex(varName), value);
    }

    public void setClob(String varName, Reader reader, long length)
            throws SQLException {
        backend.setClob(getIndex(varName), reader, length);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return backend.execute(sql, columnIndexes);
    }

    public void setBlob(String varName, InputStream inputStream, long length)
            throws SQLException {
        backend.setBlob(getIndex(varName), inputStream, length);
    }

    public void setNClob(String varName, Reader reader, long length)
            throws SQLException {
        backend.setNClob(getIndex(varName), reader, length);
    }

    @Override
    public boolean execute(String sql, String[] columnNames)
            throws SQLException {
        return backend.execute(sql, columnNames);
    }

    public void setSQLXML(String varName, SQLXML xmlObject) throws SQLException {
        backend.setSQLXML(getIndex(varName), xmlObject);
    }

    public void setObject(String varName, Object x, int targetSqlType,
            int scaleOrLength) throws SQLException {
        backend.setObject(getIndex(varName), x, targetSqlType, scaleOrLength);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return backend.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return backend.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        backend.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return backend.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        backend.closeOnCompletion();
    }

    public void setAsciiStream(String varName, InputStream x, long length)
            throws SQLException {
        backend.setAsciiStream(getIndex(varName), x, length);
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return backend.isCloseOnCompletion();
    }

    public void setBinaryStream(String varName, InputStream x, long length)
            throws SQLException {
        backend.setBinaryStream(getIndex(varName), x, length);
    }

    public void setCharacterStream(String varName, Reader reader, long length)
            throws SQLException {
        backend.setCharacterStream(getIndex(varName), reader, length);
    }

    public void setAsciiStream(String varName, InputStream x)
            throws SQLException {
        backend.setAsciiStream(getIndex(varName), x);
    }

    public void setBinaryStream(String varName, InputStream x)
            throws SQLException {
        backend.setBinaryStream(getIndex(varName), x);
    }

    public void setCharacterStream(String varName, Reader reader)
            throws SQLException {
        backend.setCharacterStream(getIndex(varName), reader);
    }

    public void setNCharacterStream(String varName, Reader value)
            throws SQLException {
        backend.setNCharacterStream(getIndex(varName), value);
    }

    public void setClob(String varName, Reader reader) throws SQLException {
        backend.setClob(getIndex(varName), reader);
    }

    public void setBlob(String varName, InputStream inputStream)
            throws SQLException {
        backend.setBlob(getIndex(varName), inputStream);
    }

    public void setNClob(String varName, Reader reader) throws SQLException {
        backend.setNClob(getIndex(varName), reader);
    }

    private int getIndex(String varName) throws SQLException {
        Integer i = index.get(varName);
        if (i == null)
            throw new SQLException(String.format(
                    "Unknown variable: %s. Indexed: %s", varName, index
                            .keySet().toString()));
        else return i;
    }

}
