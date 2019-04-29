package com.ie.naukri.migration.DataCleanup;

import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCleanup {
	static String tableName = "";
	private static final Logger LOGGER = LoggerFactory.getLogger(DataCleanup.class);
	static AtomicInteger counter = new AtomicInteger(0);

	public static void main(String[] args) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException, InterruptedException {
		LOGGER.info("Migration script started");
		long starttime = System.currentTimeMillis();

		int startId = Integer.parseInt(args[0]);
		int endId = Integer.parseInt(args[1]);
		int dbBatch = Integer.parseInt(args[2]);
		int threadsInConsumer = Integer.parseInt(args[3]);

		int capacity = 50;

		BlockingQueue<QueryRange> queue = new ArrayBlockingQueue<>(capacity);

		ExecutorService producerExecutor = Executors.newFixedThreadPool(1);
		producerExecutor.submit(new Producer(queue, startId, endId, dbBatch));

		ExecutorService consumerExecutor = Executors.newFixedThreadPool(threadsInConsumer);
		for (int i = 0; i < threadsInConsumer; i++) {
			DataCleanupTask taskGenerateAlias = new DataCleanupTask(queue, counter, endId);
			consumerExecutor.submit(taskGenerateAlias);
		}

	}

}
