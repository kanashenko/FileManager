package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBWorker {
	private static final Logger LOGGER = Logger.getLogger(DBWorker.class.getName());
	private static final String CREATE_TABLE ="CREATE TABLE records(\r\n" + 
			"												id serial PRIMARY KEY,\r\n" + 
			"												action VARCHAR (50) NOT NULL,\r\n" + 
			"												file VARCHAR (255) NOT NULL,\r\n" + 
			"												directory VARCHAR (255)  NOT NULL,\r\n" + 
			"												time TIMESTAMP default current_timestamp\r\n" + 
			"											);";
	private Connection conn = null;
	private Statement st = null;

	public DBWorker() {
		try {
			// Class.forName("org.mysql.Driver");
			//Class.forName("org.sqlite.JDBC");
			Class.forName("org.postgresql.Driver");			
			System.out.println("Loaded");
		} catch (ClassNotFoundException e) {
			System.out.println("Failed");
		}

		String url = "jdbc:postgresql://localhost:3307/postgres?user=postgres&password=postgres";
		//String url = "jdbc:sqlite:test.db";
		try {
			System.out.println("Obtaining connection...");
			conn = DriverManager.getConnection(url);
			System.out.println("Success");
		} catch (SQLException e) {
			System.out.println("SQLexception:" + e.getMessage());
			System.out.println("SQLState:" + e.getSQLState());
			System.out.println("VendorError:" + e.getErrorCode());
		}

		try {
			st = conn.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insert(String sql, Object... values) {
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			for (int i = 0; values != null && i < values.length; i++) {
				statement.setObject(i + 1, values[i]);
			}
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}

	public void createTable() {
		try {
			st = conn.createStatement();
			st.execute(CREATE_TABLE);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void select() {
		try {
			st = conn.createStatement();
			ResultSet rs = st.executeQuery("select * from records");
			while(rs.next()) {
				System.out.print(rs.getInt(1)+" ");
				System.out.println(rs.getString(2));
			}			
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}

	public static void main(String[] args) {
		DBWorker worker = new DBWorker();
		worker.createTable();
	}
}