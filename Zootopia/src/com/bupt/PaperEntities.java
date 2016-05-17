package com.bupt;

public class PaperEntities {
	long Id = -1;
	long[] FId;
	long[] RId;
	AuthorEntities[] AA;
	long CId = -1;
	long JId = -1;
	
	
	public long getId() {
		return Id;
	}
	public void setId(long id) {
		Id = id;
	}
	public long[] getFId() {
		return FId;
	}
	public void setFId(long[] fId) {
		FId = fId;
	}
	public long[] getRId() {
		return RId;
	}
	public void setRId(long[] rId) {
		RId = rId;
	}
	public AuthorEntities[] getAA() {
		return AA;
	}
	public void setAA(AuthorEntities[] aA) {
		AA = aA;
	}
	public long getCId() {
		return CId;
	}
	public void setCId(long cId) {
		CId = cId;
	}
	public long getJId() {
		return JId;
	}
	public void setJId(long jId) {
		JId = jId;
	}
	
	

	
}
