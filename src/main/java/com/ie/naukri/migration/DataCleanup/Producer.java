package com.ie.naukri.migration.DataCleanup;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Producer.class);

	private BlockingQueue<QueryRange> queue;
	private int startId;
	private int endId;
	private int dbBatch;

	public Producer(BlockingQueue<QueryRange> queue, int startId, int totalRecords, int dbBatch) {
		this.queue = queue;
		this.startId = startId;
		this.endId = totalRecords;
		this.dbBatch = dbBatch;

	}

	@Override
	public void run() {

		long starttime = System.currentTimeMillis();
		try {
			while (startId <= endId) {
				QueryRange range = new QueryRange(startId, dbBatch); // put this
																		// range
																		// in
																		// queue
				queue.put(range);
				LOGGER.info("Range added in queue is startId: {} & endId : {}", range.getStartId(), range.getEndId());
				startId = startId + dbBatch;
			}

			// Pushing terminating condition
			// queue.put(new QueryRange(-1,-1));
		} catch (Exception e) {
			LOGGER.error("Exception occured in migration script producer: ", e);
		} finally {
			LOGGER.info("Total time taken by producer in processing in Seconds : {} ",
					(System.currentTimeMillis() - starttime) / 1000L);
			LOGGER.info("Migration script producer task completed");
		}

	}

}
