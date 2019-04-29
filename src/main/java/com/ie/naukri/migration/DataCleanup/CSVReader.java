package com.ie.naukri.migration.DataCleanup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVReader {
	static String tableName = "";
	private static final Logger LOGGER = LoggerFactory.getLogger(CSVReader.class);

	public static void mainfgfg(String[] args) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException, InterruptedException {
		//
		tableName = args[1];
		String isDatainCsv = args[0];

		if (tableName.contains("cv_info")) {
			for (int i = 0; i < 160; i++) {
				LOGGER.info("Script started for table::" + tableName + i);
				cleanupData(tableName + i, isDatainCsv);
				Thread.sleep(10000);
			}
		} else {
			cleanupData(tableName, isDatainCsv);
		}

	}

	private static void cleanupData(String tableName, String isDatainCsv)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String sql = "";

		if (isDatainCsv.equals("1")) {
			LOGGER.info("Getting database invalid values for table::" + tableName);
			sql = getResid();
		} else {
			String csvFile = "/home/service/service/DataCleanup/data_collation/" + tableName + ".csv";
			LOGGER.info("CSV :: Getting invalid values for table::" + tableName);
			sql = processInputFile(csvFile);
		}

		if ((sql) != null) {

			Connection con = null;
			PreparedStatement ps = null;
			int rsSize = 0;
			Class.forName("com.mysql.jdbc.Driver").newInstance();

			// con = DriverManager.getConnection(
			// "jdbc:mysql://192.168.40.127:3308/resman5?zeroDateTimeBehavior=convertToNull",
			// "root", "Km7Iv80l");

			con = DriverManager.getConnection(
					"jdbc:mysql://resman1.resdex.com/resman5?zeroDateTimeBehavior=convertToNull", "editService",
					"editServiceKm7Iv80l");
			ps = con.prepareStatement(sql);

			try {
				LOGGER.info("Command Executing finally" + sql);
				rsSize = ps.executeUpdate();
				System.out.println(rsSize);
				LOGGER.info("updated number of rows::" + rsSize);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error("could not delete, some execution occured::" + e);
				LOGGER.error(e.getMessage());

			} finally {
				if (null != ps)
					ps.close();
				if (null != con)
					con.close();
			}
		}
	}

	private static String getResid()
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Set<String> inputList = new HashSet<>();
		int batch = 1000;
		int startIndex = 0;
		String sql = "Select distinct(c.resid) as RESID from test  LIMIT " + startIndex + ",1000 ;";
		LOGGER.info("SQL:: " + sql);
		batch = batch + 1000;

		Class.forName("com.mysql.jdbc.Driver").newInstance();
		Connection con = null;
		PreparedStatement ps = null;
		con = DriverManager.getConnection(
				"jdbc:mysql://resman.haslave.resdex.com/resman5?zeroDateTimeBehavior=convertToNull", "service",
				"serviceKm7Iv80l");
		// con =
		// DriverManager.getConnection("jdbc:mysql://192.168.40.139:3308/resman5?zeroDateTimeBehavior=convertToNull",
		// "root", "Km7Iv80l");
		ps = con.prepareStatement(sql);
		ResultSet rs = null;
		try {
			rs = ps.executeQuery();
			while (null != rs && rs.next()) {
				String resid = rs.getString("RESID");
				inputList.add(resid);
			}

		} catch (Exception e) {
			e.printStackTrace();
			// LOGGER.error("could not get invalid resids::" + e);
			// LOGGER.error(e.getMessage());
		} finally {
			if (null != ps)
				ps.close();
			if (null != con)
				con.close();
		}
		if (inputList.size() > 0) {

			String query = "DELETE from " + tableName + " where resid in (" + String.join(",", inputList) + ");";
			// LOGGER.info("Deleting resid's:: " + query);
			return query;
		}
		LOGGER.info("No invalid resid found");
		return null;
	}

	private static String processInputFile(String inputFilePath) {

		String line = "";
		Set<String> inputList = new TreeSet<>();
		try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
			while ((line = br.readLine()) != null) {
				inputList.add(line);
			}
		} catch (IOException e) {
			LOGGER.error("could not get invalid resids::" + e);
			LOGGER.error(e.getMessage());

			e.printStackTrace();
		}
		if (inputList.size() > 0) {

			String query = "DELETE from " + tableName + " where resid in (" + String.join(",", inputList) + ");";
			System.out.println(inputList.size() + "::" + query);
			LOGGER.info("SQL:: " + query);
			LOGGER.info("SQL deeleting size:: " + inputList.size());
			return query;
		}
		LOGGER.info("No invalid resid found");

		return null;
	}
}
