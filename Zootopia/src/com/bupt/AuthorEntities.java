package com.bupt;

import java.util.Map;

public class AuthorEntities {
	long AuId = -1;
	Map<Long, Integer> AfId;
	Map<Long, Integer> Ids;
	Map<PaperEntities,Integer> Paper;

	public Map<PaperEntities,Integer> getPaper() {
		return Paper;
	}

	public void setPaper(Map<PaperEntities,Integer> paper) {
		Paper = paper;
	}

	public long getAuId() {
		return AuId;
	}

	public void setAuId(long l) {
		AuId = l;
	}

	public Map<Long, Integer> getAfId() {
		return AfId;
	}

	public void setAfId(Map<Long, Integer> afId) {
		AfId = afId;
	}

	public Map<Long, Integer> getIds() {
		return Ids;
	}

	public void setIds(Map<Long, Integer> ids) {
		Ids = ids;
	}

}
