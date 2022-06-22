package se.fortnox.reactivewizard.db;

import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.stubbing.Stubber;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class MockDb {

    public static final int INFINITE = -1;
    private Connection con = mock(Connection.class);
    private ResultSetMetaData meta = mock(ResultSetMetaData.class);
    private PreparedStatement ps = mock(PreparedStatement.class);
    private ResultSet rs = mock(ResultSet.class);
    private ConnectionProvider connectionProvider;
    private int columnCount = 0;
    private DatabaseConfig databaseConfig;
    private int rows;
    private int connectionsUsed;
    private int parameterCount;

    ArgumentCaptor<String> generatedKeysCaptor = ArgumentCaptor.forClass(String.class);

    public MockDb() {
        databaseConfig = new DatabaseConfig();
        databaseConfig.setPoolSize(1);
        databaseConfig.setUrl("");

        try {
            when(con.prepareStatement(any())).thenReturn(ps);
            when(con.prepareStatement(any(), eq(RETURN_GENERATED_KEYS))).thenAnswer(inv -> ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(ps.getGeneratedKeys()).thenReturn(rs);
            when(rs.getMetaData()).thenReturn(meta);
            when(ps.getConnection()).thenReturn(con);

            ParameterMetaData parameterMetaData = mock(ParameterMetaData.class);
            when(parameterMetaData.getParameterCount()).thenAnswer(inv -> parameterCount);
            when(ps.getParameterMetaData()).thenReturn(parameterMetaData);

            Stubber answer = doAnswer(inv -> {
                int index = inv.getArgument(0);
                if (index >= parameterCount) {
                    parameterCount = index;
                }
                return null;
            });

            answer.when(ps).setObject(anyInt(), any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initRs() {
        try {
            if (rows == INFINITE) {
                when(rs.next()).thenReturn(true);
                return;
            }
            AtomicInteger count = new AtomicInteger();
            when(rs.next())
                    .thenAnswer(invocation -> {
                        count.incrementAndGet();
                        if (count.incrementAndGet() < rows) {
                            return true;
                        }
                        return false;
                    });
            OngoingStubbing<Boolean> rsNext = when(rs.next());
            for (int i = 0; i < rows; i++) {
                rsNext = rsNext.thenReturn(true);
            }
            rsNext.thenReturn(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void addRowColumn(int row, int column, String name, Class<?> type, Object value) throws SQLException {
        if (row != INFINITE && row > rows) {
            rows = row;
        }
        if (column > columnCount) {
            columnCount = column;
        }
        when(meta.getColumnCount()).thenReturn(columnCount);
        when(meta.getColumnLabel(column)).thenReturn(name);
        if (type.equals(String.class)) {
            when(rs.getString(column)).thenReturn((String) value);
        } else if (type.equals(Long.class)) {
            when(rs.getLong(column)).thenReturn((Long) value);
        } else if (type.equals(Integer.class)) {
            when(rs.getInt(column)).thenReturn((Integer) value);
        } else {
            throw new RuntimeException("Unsupported type " + type);
        }
    }

    public <T> T mockDao(int cnt, Class<T> tClass) throws SQLException {
        con = mock(Connection.class);
        mockDbResultSet(cnt);
        DbProxy dbProxy = new DbProxy(new DatabaseConfig(), () -> con);
        return dbProxy.create(tClass);
    }

    public void mockDbResultSet(int cnt) throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        ParameterMetaData paramMeta = mock(ParameterMetaData.class);
        when(paramMeta.getParameterCount())
                .thenReturn(0);
        when(ps.getParameterMetaData())
                .thenReturn(paramMeta);
        when(con.prepareStatement(any()))
                .thenReturn(ps);

        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData())
                .thenReturn(meta);
        AtomicInteger nextCalls = new AtomicInteger(0);
        when(rs.next())
                .then(invocation -> nextCalls.getAndIncrement() < cnt);
        when(rs.getString(anyInt()))
                .thenReturn("test");
        when(meta.getColumnCount())
                .thenReturn(columnCount);
        when(meta.getColumnLabel(anyInt()))
                .thenReturn("col1");
        when(ps.executeQuery())
                .thenReturn(rs);
    }

    public ConnectionProvider getConnectionProvider() {
        if (connectionProvider != null) {
            throw new RuntimeException("getConnectionProvider called more than once");
        }
        connectionProvider = new ConnectionProvider() {
            @Override
            public Connection get() {
                connectionsUsed++;
                initRs();
                return con;
            }
        };
        return connectionProvider;
    }

    public void setUpdatedRows(int updatedRows) throws SQLException {
        when(ps.getUpdateCount()).thenReturn(updatedRows);
        when(ps.executeUpdate()).thenReturn(updatedRows);

    }

    private Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Europe/Stockholm"));
        return calendar;
    }

    public void verifyUpdate(String query, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                verify(ps).setNull(eq(i + 1), anyInt());
            } else if (args[i].getClass().equals(Timestamp.class)) {
                Calendar cal = getCalendar();
                cal.setTimeInMillis(((Timestamp) args[i]).getTime());
                verify(ps).setTimestamp(i + 1, (Timestamp) args[i], cal);
            } else {
                verify(ps).setObject(i + 1, args[i]);
            }
        }

        verify(con, atLeast(0)).prepareStatement(generatedKeysCaptor.capture(),
                eq(RETURN_GENERATED_KEYS));

        if (!generatedKeysCaptor.getAllValues().isEmpty()) {
            verify(con).prepareStatement(query, RETURN_GENERATED_KEYS);
        } else {
            verify(con).prepareStatement(query);
        }
        verify(ps).executeUpdate();
    }

    public void verifyUpdateGeneratedKeys(String query, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                verify(ps).setNull(eq(i + 1), anyInt());
            } else if (args[i].getClass().equals(Timestamp.class)) {
                Calendar cal = getCalendar();
                cal.setTimeInMillis(((Timestamp) args[i]).getTime());
                verify(ps).setTimestamp(i + 1, (Timestamp) args[i], cal);
            } else {
                verify(ps).setObject(i + 1, args[i]);
            }
        }
        verify(con).prepareStatement(query, RETURN_GENERATED_KEYS);
        verify(ps).executeUpdate();
    }

    public void verifySelect(String query, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            verify(ps).setObject(i + 1, args[i]);
        }
        verify(con).prepareStatement(query);
        verify(ps).executeQuery();
    }

    public void verifyConnectionsUsed(int expectedConnectionsUsed) {
        assertThat(connectionsUsed).isEqualTo(expectedConnectionsUsed);
    }

    public void addUpdatedRowId(Object key) throws SQLException {
        setUpdatedRows(1);
        addRowColumn(1, 1, "key", key.getClass(), key);
    }

    public void addRows(int count, Object value) throws SQLException {
        for (int i = 0; i < count; i++) {
            addRowColumn(i + 1, 1, "col1", value.getClass(), value);
        }

    }

    public void addRows(int count) throws SQLException {
        addRows(count, "test");
    }

    public void setRowCount(int count) throws SQLException {
        if (count == INFINITE) {
            rs = mock(ResultSet.class, withSettings().stubOnly());
            meta = mock(ResultSetMetaData.class, withSettings().stubOnly());
            when(ps.executeQuery()).thenReturn(rs);
            when(ps.getGeneratedKeys()).thenReturn(rs);
            when(rs.getMetaData()).thenReturn(meta);
        }
        this.rows = count;
    }

    public PreparedStatement getPreparedStatement() {
        return ps;
    }

    public Connection getConnection() {
        return con;
    }

    public ResultSet getResultSet() {
        return rs;
    }
}
