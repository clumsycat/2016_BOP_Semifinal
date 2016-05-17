package com.bupt;

/**
 * 
 * 
 * for Id to Id 题意解释： hop的定义，只有Id-Id, Id-FId, FId-Id, Id-JId, JId-Id, Id-CId,
 * CId-Id, AuId-AFId, AFId-AuId, AuId-Id, Id-AuId.11种，组合就好 Id-Id 1跳，直达
 * 2跳，Id1-Id-Id2,这种情况单独处理，用RId=Id2的反向查询更快捷。 Id1-AuId-Id2, Id1-FId-Id2,
 * Id1-JId-Id2, Id1-CId-Id2, 剩下的1跳和2跳其实直接分析两个实体的属性就行，不需要复杂查询
 * 3跳，Id1-Id-Id-Id2,这种情况比较麻烦，需要前向和反向查询，url复杂度较高 Id1-AuId-Id-Id2, Id1-FId-Id-Id2,
 * Id1-JId-Id-Id2, Id1-CId-Id-Id2,
 * 
 ** Id1-Id-AuId-Id2, Id1-Id-FId-Id2,Id1-Id-JId-Id2, Id1-Id-CId-Id2,
 * 所有的含义都是，同一个作者/领域/期刊/会议 发表的文献引用了最后一个Id2
 * 
 * Id-AuId 1跳，直达 2跳，Id-Id-AuId,1次查询就好 3跳，Id-Id-Id-AuId,
 * Id-AuId-AfId-AuId,Id-AuId-Id-AuId,Id-FId-Id-AuId,Id-JId-Id-AuId,Id-CId-Id-
 * AuId
 * 
 * AuId-Id 1跳，直达 2跳，AuId-Id-Id 3跳，AuId-Id-Id-Id, AuId-AfId-AuId-Id,
 * AuId-Id-JId-Id, AuId-Id-CId-Id, AuId-Id-FId-Id **AuId-Id-AuId-Id 漏掉的
 * 
 * 
 * AuId-AuId 1跳，木有 2跳，AuId-Id-AuId, AuId-AfId-AuId, 3跳，AuId-Id-Id-AuId
 * 
 * @author weilu
 *
 */
public class KittyUrlUtil {

	/**
	 * 根据Id获取url; Id 查 Id,AA.AuId,AA.AfId,F.FId,J.JId,C.CId,RId
	 * 
	 * @param Id
	 * @param count
	 * @param set
	 * @return
	 */
	public static String getIdURL(long Id, int count) {
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=Id=" + Id + "&count=1"
				+ "&attributes=Id,AA.AuId,AA.AfId,F.FId,J.JId,C.CId,RId&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
	}

	/**
	 * 根据AuId获取url AuId 查Id,AA.AuId,AA.AfId,F.FId,J.JId,C.CId,RId
	 * 
	 * @param AfId
	 * @param count
	 * @param set
	 * @return
	 */
	public static String getAuIdURL(long AuId, int count) {
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=Composite(AA.AuId=" + AuId + ")&count="
				+ count
				+ "&attributes=Id,AA.AuId,AA.AfId,F.FId,J.JId,C.CId,RId&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";

	}

	/**
	 * 3hop id-id-id-id/Auid, 通用函数：查找id.Rid[].Rid[],返回Id,RId
	 * 
	 * @param startPaper
	 * @param endId
	 * @param count
	 * @return
	 */
	public static String[] get3hopId1URL(PaperEntities startPaper, int count) {
		int n = (int) Math.ceil((double)startPaper.getRId().length / 90);
		String[] expr = new String[n];
		for (int i = 0; i < n; i++) {
			expr[i] = new String();
		}
		long[] rid = startPaper.getRId();
		for (int i = 0; i < rid.length; i++) {
			if (expr[(int) Math.floor((double)i / 90)].equals(""))
				expr[(int) Math.floor((double)i / 90)] = "Id=" + rid[i];
			else
				expr[(int) Math.floor((double)i / 90)] = "OR(" + expr[(int) Math.floor((double)i / 90)] + ",Id=" + rid[i] + ")";
		}
		for (int i = 0; i < n; i++)
			expr[i] = "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr[i] + "&count=" + count
					+ "&attributes=Id,RId&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
		return expr;

	}

	/**
	 * 通用函数：返回引用了Id2的所有文章,值为Id
	 * 
	 * @param startPaper
	 * @param endId
	 * @param count
	 * @return
	 */
	public static String getRIdEqualsId2URL(long endId, int count) {
		String expr = "RId=" + endId;
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr + "&count=" + count
				+ "&attributes=Id&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
	}

	/**
	 * Id1-AuId-Id-Id2: Id1.AuId=Id3.AuId and Id3.RId=Id2 返回Id，AA.AuId
	 * 
	 * @param startPaper
	 * @param endId
	 * @param count
	 * @return
	 */
	public static String getII3hopAuIdURL(PaperEntities startPaper, long endId, int count) {

		String expr = "";
		for (AuthorEntities auid : startPaper.getAA()) {
			if (expr.equals(""))
				expr = "Composite(AA.AuId=" + auid.getAuId() + ")";
			else
				expr = "OR(" + expr + ",Composite(AA.AuId=" + auid.getAuId() + "))";
		}
		expr = "AND(" + expr + ",RId=" + endId + ")";
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr + "&count=" + count
				+ "&attributes=Id,AA.AuId&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
	}



	/**
	 * Id1-FId-Id-Id2：和Id1同领域的Id3引用了Id2，返回Id，F.FId
	 * 
	 * @param startPaper
	 * @param endId
	 * @param count
	 * @return
	 */
	public static String getII3hopFIdURL(PaperEntities startPaper, long endId, int count) {
		String expr = "";
		for (long fid : startPaper.getFId()) {
			if (expr.equals(""))
				expr = "Composite(F.FId=" + fid + ")";
			else
				expr = "OR(" + expr + ",Composite(F.FId=" + fid + "))";
		}
		expr = "AND(" + expr + ",RId=" + endId + ")";
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr + "&count=" + count
				+ "&attributes=Id,F.FId&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
	}

	/**
	 * Id1-Id-FId-Id2: Id1.RId中和Id2同领域的，返回Id，F.FId
	 * 
	 * @param startPaper
	 * @param endPaper
	 * @param count
	 * @return
	 */
	public static String[] getII3hopFId2URL(PaperEntities startPaper, PaperEntities endPaper, int count) {
		int n = (int) Math.ceil((double)startPaper.getRId().length / 90);
		String[] expr = new String[n];
		for (int i = 0; i < n; i++) {
			expr[i] = new String();
		}
		long[] rid = startPaper.getRId();
		for (int i = 0; i < rid.length; i++) {
			if (expr[(int) Math.floor((double)i / 90)].equals(""))
				expr[(int) Math.floor((double)i / 90)] = "Id=" + rid[i];
			else
				expr[(int) Math.floor((double)i / 90)] = "OR(" + expr[(int) Math.floor((double)i / 90)] + ",Id=" + rid[i] + ")";
		}
		String expr2 = "";
		for (long fid : endPaper.getFId()) {
			if (expr2.equals(""))
				expr2 = "Composite(F.FId=" + fid + ")";
			else
				expr2 = "OR(" + expr2 + ",Composite(F.FId=" + fid + "))";
		}
		
		for (int i = 0; i < n; i++)
		{
			expr[i] = "AND(" + expr[i] + "," + expr2 + ")";
			expr[i] = "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr[i] + "&count=" + count
				+ "&attributes=Id,F.FId&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
		}
		return expr;
	}

	/**
	 * Id1-JId-Id-Id2: 和Id1同期刊的引用了Id2,返回Id
	 * 
	 * @param startPaper
	 * @param endId
	 * @param count
	 * @return
	 */
	public static String getII3hopJIdURL(PaperEntities startPaper, long endId, int count) {

		String expr = "AND(Composite(J.JId=" + startPaper.getJId() + "),RId=" + endId + ")";
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr + "&count=" + count
				+ "&attributes=Id&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
	}

	/**
	 * Id1-Id-JId-Id2: id1.RId中，和Id2同一期刊的,返回Id就好
	 * 
	 * @param startPaper
	 * @param endPaper
	 * @param count
	 * @return
	 */
	public static String[] getII3hopJId2URL(PaperEntities startPaper, PaperEntities endPaper, int count) {

		int n = (int) Math.ceil((double)startPaper.getRId().length / 90);
		String[] expr = new String[n];
		for (int i = 0; i < n; i++) {
			expr[i] = new String();
		}
		long[] rid = startPaper.getRId();
		for (int i = 0; i < rid.length; i++) {
			if (expr[(int) Math.floor((double)i / 90)].equals(""))
				expr[(int) Math.floor((double)i / 90)] = "Id=" + rid[i];
			else
				expr[(int) Math.floor((double)i / 90)] = "OR(" + expr[(int) Math.floor((double)i / 90)] + ",Id=" + rid[i] + ")";
		}
		for (int i = 0; i < n; i++)
		{
			expr[i] = "AND(" + expr[i] + ",Composite(J.JId=" + endPaper.getJId() + "))";
			expr[i] = "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr[i] + "&count=" + count
					+ "&attributes=Id&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
		}
		return expr;

	}

	/**
	 * Id1-CId-Id-Id2：和id1同一会议，并引用了id2，返回Id
	 * 
	 * @param startPaper
	 * @param endId
	 * @param count
	 * @return
	 */
	public static String getII3hopCIdURL(PaperEntities startPaper, long endId, int count) {
		String expr = "AND(Composite(C.CId=" + startPaper.getCId() + "),RId=" + endId + ")";
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr + "&count=" + count
				+ "&attributes=Id&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
	}

	/**
	 * Id1-Id-CId-Id2: Id1.RId中和id2同一会议的
	 * 
	 * @param startPaper
	 * @param endPaper
	 * @param count
	 * @return
	 */
	public static String[] getII3hopCId2URL(PaperEntities startPaper, PaperEntities endPaper, int count) {

		int n = (int) Math.ceil((double)startPaper.getRId().length / 90);
		String[] expr = new String[n];
		for (int i = 0; i < n; i++) {
			expr[i] = new String();
		}
		long[] rid = startPaper.getRId();
		for (int i = 0; i < rid.length; i++) {
			if (expr[(int) Math.floor((double)i / 90)].equals(""))
				expr[(int) Math.floor((double)i / 90)] = "Id=" + rid[i];
			else
				expr[(int) Math.floor((double)i / 90)] = "OR(" + expr[(int) Math.floor((double)i / 90)] + ",Id=" + rid[i] + ")";
		}
		for (int i = 0; i < n; i++)
		{
			expr[i] = "AND(" + expr[i] + ",Composite(C.CId=" + endPaper.getCId() + "))";
			expr[i] = "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr[i] + "&count=" + count
					+ "&attributes=Id&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
		}
		return expr;
	}

	/**
	 * 返回auids所写的所有文章，返回带Id,AA.AuId,AA.AfId
	 * 
	 * @param endId
	 * @param count
	 * @return
	 */
	public static String getIdOfAuIdsURL(PaperEntities startPaper, int count) {
		String expr = "";
		for (AuthorEntities auid : startPaper.getAA()) {
			if (expr.equals(""))
				expr = "Composite(AA.AuId=" + auid.getAuId() + ")";
			else
				expr = "OR(" + expr + ",Composite(AA.AuId=" + auid.getAuId() + "))";
		}
		return "https://oxfordhk.azure-api.net/academic/v1.0/evaluate?expr=" + expr + "&count=" + count
				+ "&attributes=Id,AA.AuId,AA.AfId&subscription-key=f7cc29509a8443c5b3a5e56b0e38b5a6";
	}
}
