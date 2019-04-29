package com.ie.naukri.migration.DataCleanup;

public class QueryRange {
	
	private Integer dbStartId;
	private Integer dbEndId;
	
	public QueryRange(Integer startId, Integer endId) {
		this.dbStartId = startId;
		this.dbEndId = endId;
	}

	public Integer getStartId() {
		return dbStartId;
	}

	public void setStartId(Integer startId) {
		this.dbStartId = startId;
	}

	public Integer getEndId() {
		return dbEndId;
	}

	public void setEndId(Integer endId) {
		this.dbEndId = endId;
	}
	
}
