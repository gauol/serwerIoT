import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Properties;

public class JDB {
    public final String schemaName = "SENSORDB";
    private String framework = "embedded";
    private String protocol = "jdbc:derby:";
    private PreparedStatement psInsert;
    private Statement s;
    private Connection conn;
    private ArrayList<Statement> statements;

    JDB() {
        go();		// konstruktor klasy inicjujący baze
    }

    void go() {
        Server.print("Uruchamiam derby " + framework + " mode");		// wyświetlenie trybu pracy bazy
        conn = null;
        statements = new ArrayList<>(); // list of Statements, PreparedStatements

        try {
            String dbName = "derbyDB"; // nazwa bazy danych
            conn = DriverManager.getConnection(protocol + dbName
                    + ";create=true", getProperties());			// połączenie z bazą
            Server.print("Połączono z bazą " + dbName);		//
			// informacja po pomyślnym połączeniu
            conn.setAutoCommit(false);
			// wyłączenie autymatycznego zapisu stanu bazy
            s = conn.createStatement();
            statements.add(s);
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }

    public static void printSQLException(SQLException e) {// gotowa metoda do wyświetlania błędów
        while (e != null) {
            Server.print("\n----- SQLException -----");
            Server.print("  SQL State:  " + e.getSQLState());
            Server.print("  Error Code: " + e.getErrorCode());
            Server.print("  Message:    " + e.getMessage());

            e = e.getNextException();
        }
    }

    public void createSchema(String schemaName) {  // metoda do tworzenia schematów bazy  
        try {
            // We create a table...
            s.execute("create schema : " + schemaName);
            Server.print("Created schema : " + schemaName);
        } catch (SQLException sqlex) {
            printSQLException(sqlex);
        }
    }

    public void createTable(String tableName) {			// metoda do tworzenia tabeli
        try {
            s.execute("create table " + schemaName + "." + tableName + " (temp1 float, temp2 float, Czas TIMESTAMP )");
            Server.print("Created table: " + tableName);
        } catch (SQLException sqlex) {
            printSQLException(sqlex);
        }
    }

    public void delateTable(String tableName) {		// metoda do usuwania tabeli
        try {
            // We create a table...
            s.execute("drop table " + schemaName + "." + tableName);
            Server.print("Dropped table: " + tableName);
        } catch (SQLException sqlex) {
            printSQLException(sqlex);
        }
    }

    public void closeDatabase() {					// zamknięcie połączenia z bazą
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException se) {
            if (((se.getErrorCode() == 50000)
                    && ("XJ015".equals(se.getSQLState())))) {
                Server.print("Derby shut down normally");
            } else {
                System.err.println("Derby did not shut down normally");
                printSQLException(se);
            }
        }
    }

    private Properties getProperties() {  // parametry logowania do bazy
        Properties props = new Properties(); // connection properties

        props.put("user", "user1");
        props.put("password", "user1");
        return props;
    }

    public void closeStatements() {		// zamknij zapytania do bazy
        int i = 0;
        while (!statements.isEmpty()) {

            Statement st = statements.remove(i);
            try {
                if (st != null) {
                    st.close();
                    st = null;
                }
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }
        }
        try {
            conn.commit();
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }

    public void listTables() {		// wyświetl wszystkie tabele z bazy
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            ResultSet resultSet = dbmd.getTables(
                    "derbyDB", schemaName, "%", null);
            while (resultSet.next()) {
                String strTableName = resultSet.getString("TABLE_NAME");
                Server.print("TABLE_NAME is " + strTableName + " schema - " + resultSet.getString(2));
            }
            Server.printRln("separator");
        } catch (SQLException sqlex) {
            printSQLException(sqlex);
        }

    }

    public ArrayList<String> listTablesToArray() { // metoda zwracająca tabele do tablicy
        ArrayList<String> rsp = new ArrayList<>();
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            ResultSet resultSet = dbmd.getTables(
                    "derbyDB", schemaName, "%", null);

            while (resultSet.next()) {
                rsp.add(resultSet.getString("TABLE_NAME"));
            }
            return rsp;
        } catch (SQLException sqlex) {
            printSQLException(sqlex);
            rsp.add("Brak sensorów!");
            return rsp;
        }

    }

    public void addData(String tableName, float value1, float value2) {	// dodanie do bazy pojedyńczego pomiaru
        try {
            psInsert = conn.prepareStatement(
                    "insert into " + schemaName + "." + tableName + " values (?, ?, CURRENT_TIMESTAMP )");    //(temp1 float, temp2 float, Czas time, dzien int, miesiac int, rok int)
            statements.add(psInsert);

            psInsert.setFloat(1, value1);
            psInsert.setFloat(2, value2);
            //psInsert.setTime(3, new Timestamp());
            psInsert.executeUpdate();

            Server.print("Zapisano odczyt z sensora: " + tableName + " : " + value1 + " : " + value2);
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }

    public void getData(String tableName) {		// pobieranie danych z tabeli
        try {
            ResultSet rs = s.executeQuery(
                    "SELECT * FROM "+ schemaName + "." + tableName + " ORDER BY Czas");            //(temp float, Czas time, dzien int, miesiac int, rok int)
            while (rs.next()) {
                Server.print(rs.getFloat(1) + " " + rs.getFloat(2) + " : " + rs.getString(3));
            }

            if (rs != null) {
                rs.close();
                rs = null;
            }

        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }

    public String getDataHTML(String tableName) {		// pobieranie danych z tabeli w formacie gotowym dla przeglądarki
        StringBuilder str = new StringBuilder();
        try {
            Statement stmt = conn.createStatement();
            stmt.setMaxRows(100);
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM "+ schemaName + "." + tableName + " ORDER BY Czas DESC");            //(temp float, Czas time, dzien int, miesiac int, rok int)
            while (rs.next()) {
                String eventDate = rs.getTimestamp(3).toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                str.insert(0, "\n,['" + eventDate + "', " + rs.getFloat(1) +", "+rs.getFloat(2) +"]");
            }

            if (rs != null) {
                rs.close();
                rs = null;
            }

            return str.toString();

        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
        return "FATAL ERROR";
    }

    public void deleteData(String tableName) {  // usunięcie danych z tabeli
        try {
            ResultSet rs = s.executeQuery(
                    "DELETE FROM "+ schemaName + "." + tableName);            //(temp float, Czas time, dzien int, miesiac int, rok int)

            if (rs != null) {
                rs.close();
                rs = null;
            }

        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }

    public void executeQuery(String query) {		// metoda pozwalająca na wykonanie polecenia w bazie danych w formacie SQL
        try {
            ResultSet rs = s.executeQuery(
                    query);            //(temp float, Czas time, dzien int, miesiac int, rok int)
            if (rs != null) {
                rs.close();
                rs = null;
            }
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }
}
