package com.ie.naukri.migration.DataCleanup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.naukri.services.resume.update.thrift9.TUpdateResult;
import com.naukri.services.resume.update.thrift9.TUpdateResume;

public class DataCleanupTask implements Callable<List<TUpdateResult>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataCleanupTask.class);
	private static final Logger FAILURE = LoggerFactory.getLogger("MigrationFailureLogger");

	private BlockingQueue<QueryRange> queue;
	private AtomicInteger counter;
	private int maxUserId;

	public DataCleanupTask(BlockingQueue<QueryRange> queue, AtomicInteger counter, int maxUserId) {
		super();
		this.queue = queue;
		this.counter = counter;
		this.maxUserId = maxUserId;
	}

	@Override
	public List<TUpdateResult> call() throws Exception {
		long starttime = System.currentTimeMillis();
		while (true) {
			QueryRange range = queue.take();
			int startId = range.getStartId();
			int endId = range.getEndId();

			LOGGER.info("Thread start with : {} and end with : {} for ThreadId : {}", startId, endId,
					Thread.currentThread().getId());

			List<String> residId = getResid(startId, endId);
			List<TUpdateResult> results = callUpdateCalAPI(residId);

			LOGGER.info("Total number of records successfully processed till now: {}", counter.get());
			LOGGER.info("Size of resultList is: {} for threadId : {}", results.size(), Thread.currentThread().getId());

			if (endId >= maxUserId) {
				LOGGER.info("Total time taken by consumer in processing in Seconds : {} ",
						(System.currentTimeMillis() - starttime) / 1000L);
				LOGGER.info("Migration script consumer task completed");
				return results;
			}

		}

	}

	private static ArrayList<String> getResid(int startid, int batch)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		ArrayList<String> lstResid = new ArrayList<String>();

		String sql = "Select residupd as RESID from residbkp LIMIT " + startid + ", " + batch + ";";
		LOGGER.info("SQL:: " + sql);

		Class.forName("com.mysql.jdbc.Driver").newInstance();
		Connection con = null;
		PreparedStatement ps = null;

		con = DriverManager.getConnection("jdbc:mysql://172.10.113.176:/test?zeroDateTimeBehavior=convertToNull",
				"pushdownStaging", "pushStagingJX49t");

		ps = con.prepareStatement(sql);
		ResultSet rs = null;
		try {
			rs = ps.executeQuery();
			while (null != rs && rs.next()) {
				String resid = rs.getString("RESID");
				lstResid.add(resid);
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
		return lstResid;
	}

	private List<TUpdateResult> callUpdateCalAPI(List<String> lstResid) {
		Integer userId = null;
		List<TUpdateResult> results = new ArrayList<>();

		// Now need to call API for each record
		TUpdateResume.Client updateResumeClient = null;
		TTransport transport = null;
		try {
			// transport = new TSocket("192.168.40.139", 7915); // Test5 config
			transport = new TSocket("172.10.115.103", 7915);
			transport.open();
			TProtocol protocol = new TBinaryProtocol(transport);
			updateResumeClient = new TUpdateResume.Client(protocol);

		} catch (Exception e) {
			LOGGER.error("Exception occured while connecting thrift service, so terminating execution : {}", e);
			LOGGER.info("Total number of records successfully processed till now: {}", counter.get());
			System.exit(0);
		}

		for (String rid : lstResid) {
			try {
				LOGGER.info("Sending API request for ThreadId : {} and resid : {}", Thread.currentThread().getId(),
						rid);

				Map<String, String> ids = new HashMap<String, String>();
				ids.put("requestId", rid); // For
											// tracking
											// purpose

				updateResumeClient.updateProfileFlagFromResumeId(ids, 104, Integer.parseInt(rid), "noChange",
						"resman5_1");

				LOGGER.info("Response added in resultList for ThreadId : {} and userId : {}",
						Thread.currentThread().getId(), rid);
			} catch (Exception e) {
				FAILURE.error("Alias API failed for userId : {}", userId);
				LOGGER.error("Generate Alias API calling failed for ThreadId : {} and userId : {} with exception : {}",
						Thread.currentThread().getId(), userId, e);
			}

		}
		transport.close();
		return results;

	}
}
