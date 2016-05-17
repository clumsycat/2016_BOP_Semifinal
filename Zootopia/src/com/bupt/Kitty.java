package com.bupt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Servlet implementation class Kiity
 */
@WebServlet("/Kitty")
public class Kitty extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// 保留查询结果
	public static JSONArray jsonResult;
	// 存储访问过的文章和Author实体
	public boolean CACHE = true;
	public static Map<Long, PaperEntities> map_PaperEntities = new HashMap<Long, PaperEntities>();
	public static Map<Long, AuthorEntities> map_AuthorEntities = new HashMap<Long, AuthorEntities>();
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	// public static long starttime;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Kitty() {
		super();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// starttime = System.currentTimeMillis();

		jsonResult = new JSONArray();
		// 为了存储可能的id1和id2的实体类型
		PaperEntities StartPaper = new PaperEntities();
		PaperEntities EndPaper = new PaperEntities();
		AuthorEntities StartAuthor = new AuthorEntities();
		AuthorEntities EndAuthor = new AuthorEntities();
		String startType, endType;
		long startId = Long.parseLong(request.getParameter("id1"));
		long endId = Long.parseLong(request.getParameter("id2"));
		if (CACHE && map_PaperEntities.containsKey(startId)) {
			startType = "Id";
			StartPaper = map_PaperEntities.get(startId);
		} else if (CACHE && map_AuthorEntities.containsKey(startId)) {
			startType = "AuId";
			StartAuthor = map_AuthorEntities.get(startId);
		} else
			startType = TypeOfId(startId, StartPaper, StartAuthor);

		if (CACHE && map_PaperEntities.containsKey(endId)) {
			endType = "Id";
			EndPaper = map_PaperEntities.get(endId);
		} else if (CACHE && map_AuthorEntities.containsKey(endId)) {
			endType = "AuId";
			EndAuthor = map_AuthorEntities.get(endId);
		} else
			endType = TypeOfId(endId, EndPaper, EndAuthor);
		System.out.println("start:" + startType + " end:" + endType);

		// System.out.println("初始化" + (//System.currentTimeMillis() -
		// starttime));
		if (startType.equals("Id") && endType.equals("Id")) {
			FromIdToId(StartPaper, EndPaper, startId, endId);
		} else if (startType.equals("AuId") && endType.equals("Id")) {
			FromAuIdToId(StartAuthor, EndPaper, startId, endId);
		} else if (startType.equals("Id") && endType.equals("AuId")) {
			FromIdToAuId(StartPaper, EndAuthor, startId, endId);
		} else if (startType.equals("AuId") && endType.equals("AuId")) {
			FromAuIdToAuId(StartAuthor, EndAuthor, startId, endId);
		}
		response.getWriter().println(jsonResult);
	}

	/**
	 * AuId-AuId: 1跳，木有; 2跳，AuId-Id-AuId, AuId-AfId-AuId; 3跳，AuId-Id-Id-AuId;
	 * 
	 * @param startAuthor
	 * @param endAuthor
	 * @param startId
	 * @param endId
	 */
	private void FromAuIdToAuId(AuthorEntities startAuthor, AuthorEntities endAuthor, long startId, long endId) {
		new HashMap<Long, Integer>();
		new HashMap<Long, Integer>();

		Map<PaperEntities, Integer> startmap = startAuthor.getPaper();
		Map<PaperEntities, Integer> endmap = endAuthor.getPaper();
		// auid-afid-auid
		for (long safid : startAuthor.getAfId().keySet())
			for (long eafid : endAuthor.getAfId().keySet())
				if (safid == eafid)
					Put2Hop(startId, safid, endId);

		for (PaperEntities startpaper : startmap.keySet())
			for (PaperEntities endpaper : endmap.keySet()) {
				long cursId = startpaper.getId();
				long cureId = endpaper.getId();
				// auid-id-auid
				if (cursId == cureId) {
					Put2Hop(startId, cursId, endId);
				}
				// auid-id-id-auid
				for (long rid : startpaper.getRId())
					if (rid == cureId)
						Put3Hop(startId, cursId, cureId, endId);
			}
	}

	/**
	 * Id-AuId,文章到作者 1跳，直达 2跳，Id-Id-AuId,1次查询就好 3跳，Id-Id-Id-AuId,
	 * Id-AuId-AfId-AuId,Id-AuId-Id-AuId,Id-FId-Id-AuId,Id-JId-Id-AuId,Id-CId-Id
	 * -AuId
	 * 
	 * @param startPaper
	 * @param endAuthor
	 * @param startId
	 * @param endId
	 */
	private void FromIdToAuId(PaperEntities startPaper, AuthorEntities endAuthor, long startId, long endId) {
		String url;
		String urls[];
		JSONObject json;
		JSONArray jsonarray;
		// 1hop
		for (AuthorEntities au : startPaper.getAA()) {
			if (au.getAuId() == endId) {
				Put1Hop(startId, endId);
			}
		}
		Map<PaperEntities, Integer> map = endAuthor.getPaper();
		Map<Long, Integer> idmap = endAuthor.getIds();
		long cid = startPaper.getCId();
		long jid = startPaper.getJId();
		if (map.isEmpty() == false) {
			// 3 hop Id-Id-Id-AuId
			if (startPaper.getRId().length > 0) {
				// 获得id1的RID的所有RID
				urls = KittyUrlUtil.get3hopId1URL(startPaper, startPaper.getRId().length);
				for (int k = 0; k < urls.length; k++) {
					json = ParseJsonToMap.getUrlRequestJson(urls[k]);
					jsonarray = json.getJSONArray("entities");
					for (int i = 0; i < jsonarray.length(); i++) {
						JSONObject jsonobj = jsonarray.getJSONObject(i);
						Set<String> jsonset = jsonobj.keySet();
						long curId = jsonobj.getLong("Id");
						if (jsonset.contains("RId")) {
							JSONArray arrayRId = jsonobj.getJSONArray("RId");
							for (int j = 0; j < arrayRId.length(); j++) {
								long curRId = arrayRId.getLong(j);
								if (idmap.get(curRId) != null) {
									Put3Hop(startId, curId, curRId, endId);
								}
							}
						}
					}
				}
			}
			// 3hop Id-AuId-AfId-AuId
			for (AuthorEntities aus : startPaper.getAA()) {
				if (endId == aus.getAuId()) {
					for (long afide : endAuthor.getAfId().keySet())
						for (long afids : aus.getAfId().keySet())
							if (afide == afids)
								Put3Hop(startId, aus.getAuId(), afids, endId);
				}
			}
			for (PaperEntities paper : map.keySet()) {
				long curId = paper.getId();

				// 2hop Id-Id-AuId
				for (long id : startPaper.getRId())
					if (curId == id)
						Put2Hop(startId, curId, endId);
				// 3 hop Id-FId-Id-AuId
				if (paper.getFId() != null && startPaper.getFId() != null) {
					for (long fids : startPaper.getFId())
						for (long fide : paper.getFId())
							if (fids == fide)
								Put3Hop(startId, fids, curId, endId);
				}
				// id-jid-id-auid
				if (paper.getJId() != -1 && jid == paper.getJId()) {
					Put3Hop(startId, jid, curId, endId);
				}
				// id-cid-id-auid
				if (paper.getCId() != -1 && cid == paper.getCId())
					Put3Hop(startId, cid, curId, endId);
				// id-auid-id-auid
				if (paper.getAA() != null) {
					for (AuthorEntities aue : paper.getAA())
						for (AuthorEntities aus : startPaper.getAA())
							if (aue.getAuId() == aus.getAuId())
								Put3Hop(startId, aus.getAuId(), curId, endId);
				}

			}

		}
	}

	/**
	 * id1是作者，id2是文章 AuId-Id 1跳，直达 2跳，AuId-Id-Id 3跳，AuId-Id-Id-Id,
	 * AuId-AfId-AuId-Id, AuId-Id-JId-Id, AuId-Id-CId-Id, AuId-Id-FId-Id
	 * 天啦噜的，优化完了以后貌似不需要再做访问了
	 * 
	 * 
	 * @param startAuthor
	 * @param endPaper
	 * @param startId
	 * @param endId
	 */
	private void FromAuIdToId(AuthorEntities startAuthor, PaperEntities endPaper, long startId, long endId) {
		String url;
		JSONObject json;
		JSONArray jsonarray;
		Map<Long, Integer> map_fidforid2 = new HashMap<Long, Integer>();
		Map<Long, Integer> map_afidAuid = new HashMap<Long, Integer>();
		// 存保存过的auid和afid对儿
		// 1 hop
		for (AuthorEntities au : endPaper.getAA()) {
			if (au.getAuId() == startId) {
				Put1Hop(startId, endId);
			}
		}
		// System.out.println("1跳" + (//System.currentTimeMillis() -
		// starttime));

		// 3hop AuId-Id-Id-Id 1 先从后面的id2查询引用了id2的文章
		// TODO 现有问题是：当Id2的引用量超级大的时候，运算比较缓慢。。
		url = KittyUrlUtil.getRIdEqualsId2URL(endId, MAX_COUNT);
		json = ParseJsonToMap.getUrlRequestJson(url);
		jsonarray = json.getJSONArray("entities");
		for (int i = 0; i < jsonarray.length(); i++) {
			JSONObject jsonobj = jsonarray.getJSONObject(i);
			long curId = jsonobj.getLong("Id");
			map_fidforid2.put(curId, 1);

		}
		// 现在，由于AuId的属性中包含了所写的文章和他的文章实体，所以直接求！
		long ejid = endPaper.getJId();
		long ecid = endPaper.getJId();
		Map<PaperEntities, Integer> map = startAuthor.getPaper();
		if (map.isEmpty() == false) {
			for (PaperEntities paper : map.keySet()) {
				long curId = paper.getId();
				if (paper.getRId() != null) {
					for (long rid : paper.getRId()) {
						// auid-id-id
						if (rid == endId) {
							Put2Hop(startId, curId, endId);
						}
						// auid-id-id-id
						if (map_fidforid2.get(rid) != null) {
							Put3Hop(startId, curId, rid, endId);
						}
					}
				}
				// Auid-Id-Auid-Id & Auid-AfId-Auid-Id
				if (paper.getAA() != null) {
					for (AuthorEntities aus : paper.getAA()) {
						for (AuthorEntities aue : endPaper.getAA()) {
							if (aus.getAuId() == aue.getAuId()) {
								// auid-id-auid-id
								Put3Hop(startId, curId, aue.getAuId(), endId);
								if (aus.getAfId() != null && aue.getAfId() != null) {
									for (long afs : aus.getAfId().keySet())
										for (long afe : aue.getAfId().keySet())
											if (afs == afe
													&& map_afidAuid.containsKey(aue.getAuId() + afs * 1000) == false) {
												// auid-afid-auid
												Put3Hop(startId, afs, aue.getAuId(), endId);
												map_afidAuid.put(aue.getAuId() + afs * 1000, 1);
											}
								}
							}
						}
					}
				}
				// AuId-Id-JId-Id
				if (paper.getJId() != -1 && paper.getJId() == ejid) {
					Put3Hop(startId, curId, ejid, endId);
				}
				// AuId-Id-CId-Id
				if (paper.getCId() != -1 && paper.getCId() == ecid) {
					Put3Hop(startId, curId, ecid, endId);

				}
				// 3hop AuId-Id-FId-Id
				if (paper.getFId() != null && endPaper.getFId() != null) {
					long[] efid = endPaper.getFId();
					long[] sfid = paper.getFId();
					for (int i = 0; i < sfid.length; i++) {
						for (int j = 0; j < efid.length; j++)
							if (sfid[i] == efid[j])
								Put3Hop(startId, curId, sfid[i], endId);
					}
				}
			}
		}
		// System.out.println("3跳的一堆" + (//System.currentTimeMillis() -
		// starttime));

	}

	/**
	 * id1,id2均为文章时的几种情况
	 * 
	 * Id-Id 1跳，直达。 2跳，Id1-Id-Id2,这种情况单独处理，用RId=Id2的反向查询更快捷。 Id1-AuId-Id2,
	 * Id1-FId-Id2, Id1-JId-Id2, Id1-CId-Id2, 剩下的1跳和2跳其实直接分析两个实体的属性就行，不需要复杂查询。
	 * 3跳，Id1-Id-Id-Id2,这种情况比较麻烦，需要前向和反向查询，url复杂度较高 Id1-AuId-Id-Id2,
	 * Id1-FId-Id-Id2, Id1-JId-Id-Id2, Id1-CId-Id-Id2,
	 * 所有的含义都是，同一个作者/领域/期刊/会议发表的文献引用了最后一个Id2
	 * 
	 * @param StartPaper
	 * @param EndPaper
	 * @param startId
	 * @param endId
	 */
	private void FromIdToId(PaperEntities StartPaper, PaperEntities EndPaper, long startId, long endId) {
		String url;
		String urls[];
		JSONObject json;
		JSONArray jsonarray;
		// 3hop的从后面的id2查询
		Map<Long, Integer> map_fidforid2 = new HashMap<Long, Integer>();
		// 1跳
		for (Long key : StartPaper.getRId()) {
			if (key == EndPaper.getId()) {
				Put1Hop(startId, endId);
			}

		}
		// System.out.println("1跳" + (//System.currentTimeMillis() -
		// starttime));
		// 2跳-id-id-id，在RId中找，数量不多
		/*
		 * if (StartPaper.getRId().length > 0) { url =
		 * KittyUrlUtil.getIIId2URL(StartPaper, endId,
		 * StartPaper.getRId().length); json =
		 * ParseJsonToMap.getUrlRequestJson(url); jsonarray =
		 * json.getJSONArray("entities"); for (int i = 0; i <
		 * jsonarray.length(); i++) { JSONObject jsonobj =
		 * jsonarray.getJSONObject(i); long curId = jsonobj.getLong("Id");
		 * Put2Hop(startId, curId, endId); } }
		 */
		// System.out.println("2跳-id-id-id" + (//System.currentTimeMillis() -
		// starttime));
		// Id1-AuId-Id2
		if (StartPaper.getAA() != null && EndPaper.getAA() != null) {
			for (AuthorEntities saa : StartPaper.getAA())
				for (AuthorEntities eaa : EndPaper.getAA())
					if (saa.getAuId() == eaa.getAuId())
						Put2Hop(startId, saa.getAuId(), endId);
		}
		// id-CId-id
		long scid = StartPaper.getCId();
		long ecid = EndPaper.getCId();
		if (scid == ecid && scid != -1 && ecid != -1) {
			Put2Hop(startId, scid, endId);
		}
		// id-JID-id
		long sjid = StartPaper.getJId();
		long ejid = EndPaper.getJId();
		if (sjid == ejid && sjid != -1 && ejid != -1) {
			Put2Hop(startId, sjid, endId);
		}
		// id-Fid-id
		if (StartPaper.getFId() != null && EndPaper.getFId() != null) {
			for (long sfid : StartPaper.getFId())
				for (long efid : EndPaper.getFId()) {
					if (sfid == efid) {
						Put2Hop(startId, sfid, endId);
					}
				}
		}
		// System.out.println("2跳-CJF" + (//System.currentTimeMillis() -
		// starttime));
		// 3跳 id-id-id-id
		// TODO url 长度炸了
		// 先从后面的id2求，引用了id2的所有文章。结果放在一个map里面，这个count可以适当设大一些
		if (StartPaper.getRId().length > 0) {
			url = KittyUrlUtil.getRIdEqualsId2URL(endId, MAX_COUNT);
			json = ParseJsonToMap.getUrlRequestJson(url);
			jsonarray = json.getJSONArray("entities");
			for (int i = 0; i < jsonarray.length(); i++) {
				JSONObject jsonobj = jsonarray.getJSONObject(i);
				long curId = jsonobj.getLong("Id");
				map_fidforid2.put(curId, 1);
			}
			// 获得id1的RID的所有RID，数量应该小于等于id1.RId.length
			urls = KittyUrlUtil.get3hopId1URL(StartPaper, StartPaper.getRId().length);
			for (int k = 0; k < urls.length; k++) {
				json = ParseJsonToMap.getUrlRequestJson(urls[k]);
				jsonarray = json.getJSONArray("entities");
				for (int i = 0; i < jsonarray.length(); i++) {
					JSONObject jsonobj = jsonarray.getJSONObject(i);
					Set<String> jsonset = jsonobj.keySet();
					long curId = jsonobj.getLong("Id");
					if (jsonset.contains("RId")) {
						JSONArray arrayRId = jsonobj.getJSONArray("RId");
						for (int j = 0; j < arrayRId.length(); j++) {
							long curRId = arrayRId.getLong(j);
							// id-id-id
							if (curRId == endId)
								Put2Hop(startId, curId, endId);
							// 若存在这样的id-rid-rid-id的链
							if (map_fidforid2.get(curRId) != null) {
								Put3Hop(startId, curId, curRId, endId);
							}
						}
					}
				}
			}
		}
		// System.out.println("3跳 id-id-id-id" + (//System.currentTimeMillis() -
		// starttime));
		// Id1-AuId-Id-Id2
		url = KittyUrlUtil.getII3hopAuIdURL(StartPaper, endId, count);
		json = ParseJsonToMap.getUrlRequestJson(url);
		jsonarray = json.getJSONArray("entities");
		for (int i = 0; i < jsonarray.length(); i++) {
			JSONObject jsonobj = jsonarray.getJSONObject(i);
			long curId = jsonobj.getLong("Id");
			JSONArray arrayAA = jsonobj.getJSONArray("AA");
			for (int j = 0; j < arrayAA.length(); j++) {
				JSONObject jsonAA = arrayAA.getJSONObject(j);
				long curAuId = jsonAA.getLong("AuId");
				for (AuthorEntities au : StartPaper.getAA()) {
					if (au.getAuId() == curAuId) {
						Put3Hop(startId, curAuId, curId, endId);
					}
				}
			}
		}
		// System.out.println("3跳Id1-AuId-Id-Id2" +
		// (//System.currentTimeMillis() - starttime));
		// Id1-Id-AuId-Id2 遗漏的宝宝，在Id1.Rid中找，所以超不过RId的长度
		// 由于id2的作者所写的文章和id1的Rid均已知，所以直接查找
		if (StartPaper.getRId().length > 0) {
			AuthorEntities au[] = EndPaper.getAA();
			for (AuthorEntities a : au) {
				Map<Long, Integer> map = a.getIds();
				for (long rid : StartPaper.getRId()) {
					if (map.get(rid) != null)
						Put3Hop(startId, rid, a.getAuId(), endId);
				}
			}
		}
		// System.out.println("3跳Id1-Id-AuId-Id2 " +
		// (//System.currentTimeMillis() - starttime));
		// Id1-FId-Id-Id2, 统一领域的，同样可以设置较多的count
		if (StartPaper.getFId() != null) {
			url = KittyUrlUtil.getII3hopFIdURL(StartPaper, endId, MAX_COUNT);
			json = ParseJsonToMap.getUrlRequestJson(url);
			jsonarray = json.getJSONArray("entities");
			for (int i = 0; i < jsonarray.length(); i++) {
				JSONObject jsonobj = jsonarray.getJSONObject(i);
				long curId = jsonobj.getLong("Id");
				JSONArray arrayF = jsonobj.getJSONArray("F");
				for (int j = 0; j < arrayF.length(); j++) {
					JSONObject jsonF = arrayF.getJSONObject(j);
					long curFId = jsonF.getLong("FId");
					for (long fid : StartPaper.getFId()) {
						if (fid == curFId)
							Put3Hop(startId, curFId, curId, endId);
					}
				}
			}
		}
		// System.out.println("3跳Id1-FId-Id-Id2 " +
		// (//System.currentTimeMillis() - starttime));
		// Id1-Id-FId-Id2, 遗漏的宝宝，在Id1.RId中找的，数量不多
		if (EndPaper.getFId() != null && StartPaper.getRId().length > 0) {
			urls = KittyUrlUtil.getII3hopFId2URL(StartPaper, EndPaper, StartPaper.getRId().length);
			for (int k = 0; k < urls.length; k++) {
				json = ParseJsonToMap.getUrlRequestJson(urls[k]);
				jsonarray = json.getJSONArray("entities");
				for (int i = 0; i < jsonarray.length(); i++) {
					JSONObject jsonobj = jsonarray.getJSONObject(i);
					long curId = jsonobj.getLong("Id");
					Set<String> jsonset = jsonobj.keySet();
					if (jsonset.contains("F")) {
						JSONArray arrayF = jsonobj.getJSONArray("F");
						for (int j = 0; j < arrayF.length(); j++) {
							JSONObject jsonF = arrayF.getJSONObject(j);
							long curFId = jsonF.getLong("FId");
							for (long fid : EndPaper.getFId()) {
								if (fid == curFId)
									Put3Hop(startId, curId, curFId, endId);
							}
						}
					}
				}
			}
		}
		// System.out.println("3跳Id1-Id-FId-Id2 " +
		// (//System.currentTimeMillis() - starttime));
		// Id1-JId-Id-Id2, 同一期刊的，同样可以设置较多的count
		if (sjid != -1) {
			url = KittyUrlUtil.getII3hopJIdURL(StartPaper, endId, MAX_COUNT);
			json = ParseJsonToMap.getUrlRequestJson(url);
			jsonarray = json.getJSONArray("entities");
			for (int i = 0; i < jsonarray.length(); i++) {
				JSONObject jsonobj = jsonarray.getJSONObject(i);
				long curId = jsonobj.getLong("Id");
				Put3Hop(startId, StartPaper.getJId(), curId, endId);
			}
		}
		// System.out.println("3跳Id1-JId-Id-Id2 " +
		// (//System.currentTimeMillis() - starttime));
		// Id1-Id-JId-Id2, 同一期刊的，遗漏的宝宝，在rid中找，注定数量不多
		if (ejid != -1 && StartPaper.getRId().length > 0) {
			urls = KittyUrlUtil.getII3hopJId2URL(StartPaper, EndPaper, StartPaper.getRId().length);
			for (int k = 0; k < urls.length; k++) {
				json = ParseJsonToMap.getUrlRequestJson(urls[k]);
				jsonarray = json.getJSONArray("entities");
				for (int i = 0; i < jsonarray.length(); i++) {
					JSONObject jsonobj = jsonarray.getJSONObject(i);
					long curId = jsonobj.getLong("Id");
					Put3Hop(startId, curId, StartPaper.getJId(), endId);
				}
			}
		}
		// System.out.println("3跳Id1-Id-JId-Id2" + (//System.currentTimeMillis()
		// - starttime));
		// Id1-CId-Id-Id2
		if (scid != -1) {
			url = KittyUrlUtil.getII3hopCIdURL(StartPaper, endId, MAX_COUNT);
			json = ParseJsonToMap.getUrlRequestJson(url);
			jsonarray = json.getJSONArray("entities");
			for (int i = 0; i < jsonarray.length(); i++) {
				JSONObject jsonobj = jsonarray.getJSONObject(i);
				long curId = jsonobj.getLong("Id");
				Put3Hop(startId, StartPaper.getCId(), curId, endId);
			}
		}
		// System.out.println("3跳Id1-CId-Id-Id2 " +
		// (//System.currentTimeMillis() - starttime));
		// id1-id-CId-id2,遗漏的宝宝,在rid中找，注定数量不多
		if (ecid != -1 && StartPaper.getRId().length > 0) {
			urls = KittyUrlUtil.getII3hopCId2URL(StartPaper, EndPaper, StartPaper.getRId().length);
			for (int k = 0; k < urls.length; k++) {
				json = ParseJsonToMap.getUrlRequestJson(urls[k]);
				jsonarray = json.getJSONArray("entities");
				for (int i = 0; i < jsonarray.length(); i++) {
					JSONObject jsonobj = jsonarray.getJSONObject(i);
					long curId = jsonobj.getLong("Id");
					Put3Hop(startId, curId, StartPaper.getCId(), endId);
				}
			}
		}
		// System.out.println("3跳id1-id-CId-id2 " +
		// (//System.currentTimeMillis() - starttime));
	}

	/**
	 * 把1跳结果塞入json字符串中
	 * 
	 * @param item1
	 * @param item2
	 */
	public static void Put1Hop(long item1, long item2) {
		JSONArray curjson = new JSONArray();
		curjson.put(item1);
		curjson.put(item2);
		jsonResult.put(curjson);
	}

	/**
	 * 把2跳结果塞入json字符串中
	 * 
	 * @param item1
	 * @param item2
	 * @param item3
	 */
	public static void Put2Hop(long item1, long item2, long item3) {
		JSONArray curjson = new JSONArray();
		curjson.put(item1);
		curjson.put(item2);
		curjson.put(item3);
		jsonResult.put(curjson);
	}

	/**
	 * 把3跳结果塞入json字符串中
	 * 
	 * @param item1
	 * @param item2
	 * @param item3
	 * @param item4
	 */
	public static void Put3Hop(long item1, long item2, long item3, long item4) {
		JSONArray curjson = new JSONArray();
		curjson.put(item1);
		curjson.put(item2);
		curjson.put(item3);
		curjson.put(item4);
		jsonResult.put(curjson);
	}

	/**
	 * 判断ID的属性，并保存实体属性，如果是文章存入Paper,作者就存入Author Paper属性：
	 * id,FId,RId,AuthorEntities[] AA,CId,JId 这里的Author含有除了MapPaper的Author属性
	 * Author属性： AuId，Map<Long, Integer> AfId,Map<Long, Integer> Ids, Map
	 * <PaperEntities,Integer> Paper
	 * 
	 * @param startAuthorEntities
	 * @param startPaper
	 * 
	 * @param startEntities
	 * @param start
	 * @return type
	 */
	public static String TypeOfId(long StartId, PaperEntities Paper, AuthorEntities Author) {
		String url = KittyUrlUtil.getIdURL(StartId, count);
		JSONObject json = ParseJsonToMap.getUrlRequestJson(url);
		JSONArray jsonarray = json.getJSONArray("entities");
		Map<Long, Integer> map_AfIds = new HashMap<Long, Integer>();
		Map<Long, Integer> map_Ids = new HashMap<Long, Integer>();
		if (jsonarray.length() > 0) {
			JSONObject jsonobj = jsonarray.getJSONObject(0);
			Set<String> jsonset = jsonobj.keySet();
			if (jsonset.contains("AA")) {
				JSONArray arrayAA = jsonobj.getJSONArray("AA");
				AuthorEntities[] AuEn = new AuthorEntities[arrayAA.length()];
				for (int j = 0; j < arrayAA.length(); j++) {
					AuthorEntities Au = new AuthorEntities();
					JSONObject jsonAA = arrayAA.getJSONObject(j);
					// TODO: 这里的AfId做不做多个判定？？
					if (jsonAA.keySet().contains("AuId")) {
						long curAuId = jsonAA.getLong("AuId");
						Au.setAuId(curAuId);
					}
					Au.setAfId(map_AfIds);
					Au.setIds(map_Ids);
					AuEn[j] = Au;
				}
				Paper.setAA(AuEn);
				// 同下，先不做AfId初始化
				// 由于求AfId的过程太耗时了，所以初始化时做好AfId,Ids的保存
				url = KittyUrlUtil.getIdOfAuIdsURL(Paper, MAX_COUNT);
				json = ParseJsonToMap.getUrlRequestJson(url);
				JSONArray auidjsonarray = json.getJSONArray("entities");
				for (int i = 0; i < auidjsonarray.length(); i++) {
					JSONObject auidjsonobj = auidjsonarray.getJSONObject(i);
					long curId = auidjsonobj.getLong("Id");
					if (auidjsonobj.keySet().contains("AA")) {
						JSONArray auidarrayAA = auidjsonobj.getJSONArray("AA");
						for (int j = 0; j < auidarrayAA.length(); j++) {
							JSONObject jsonAA = auidarrayAA.getJSONObject(j);
							if (jsonAA.keySet().contains("AfId")) {
								long curAfId = jsonAA.getLong("AfId");
								long curAuId = jsonAA.getLong("AuId");
								for (AuthorEntities au : AuEn) {
									if (au.getAuId() == curAuId) {
										// 保存每个作者的AfIds和Ids
										map_AfIds = au.getAfId();
										map_AfIds.put(curAfId, 1);
										au.setAfId(map_AfIds);
										map_Ids = au.getIds();
										map_Ids.put(curId, 1);
										au.setIds(map_Ids);
										break;
									}
								}
							}
						}
					}
				}
			} else { // 这里作为判断Id类型的依据,
				// System.out.println("不是文章是作者啊");
				// TODO: 群里提到一个AuId可能对应多个AfId，所以初始化时，做好Id,AFId的保存
				url = KittyUrlUtil.getAuIdURL(StartId, count);
				map_AfIds = new HashMap<Long, Integer>();
				map_Ids = new HashMap<Long, Integer>();
				HashMap<PaperEntities, Integer> map_Papers = new HashMap<PaperEntities, Integer>();
				PaperEntities AuPaper;
				JSONObject aujson = ParseJsonToMap.getUrlRequestJson(url);
				JSONArray aujsonarray = aujson.getJSONArray("entities");
				for (int i = 0; i < aujsonarray.length(); i++) {
					JSONObject aujsonobj = aujsonarray.getJSONObject(i);
					AuPaper = new PaperEntities();
					long curId = aujsonobj.getLong("Id");
					map_Ids.put(curId, 1);
					AuPaper.setId(curId);
					Set<String> aujsonset = aujsonobj.keySet();
					if (aujsonset.contains("RId")) {
						JSONArray arrayRId = aujsonobj.getJSONArray("RId");
						long[] papers = new long[arrayRId.length()];
						for (int j = 0; j < arrayRId.length(); j++) {
							long curRId = arrayRId.getLong(j);
							papers[j] = curRId;
						}
						AuPaper.setRId(papers);
					}

					if (aujsonset.contains("F")) {
						JSONArray arrayF = aujsonobj.getJSONArray("F");
						long[] paperF = new long[arrayF.length()];
						for (int j = 0; j < arrayF.length(); j++) {
							JSONObject jsonF = arrayF.getJSONObject(j);
							if (jsonF.keySet().contains("FId")) {
								long curFId = jsonF.getLong("FId");
								paperF[j] = curFId;
							}
						}
						AuPaper.setFId(paperF);
					}
					if (aujsonset.contains("J")) {
						JSONObject jsonJ = aujsonobj.getJSONObject("J");
						if (jsonJ.keySet().contains("JId")) {
							long curJId = jsonJ.getLong("JId");
							AuPaper.setJId(curJId);
						}
					}
					if (aujsonset.contains("C")) {
						JSONObject jsonC = aujsonobj.getJSONObject("C");
						if (jsonC.keySet().contains("CId")) {
							long curCId = jsonC.getLong("CId");
							AuPaper.setCId(curCId);
						}
					}
					JSONArray arrayAA = aujsonobj.getJSONArray("AA");
					AuthorEntities[] aus = new AuthorEntities[arrayAA.length()];
					for (int j = 0; j < arrayAA.length(); j++) {
						JSONObject jsonAA = arrayAA.getJSONObject(j);
						AuthorEntities au = new AuthorEntities();
						long curAuId = jsonAA.getLong("AuId");
						au.setAuId(curAuId);
						// 不保存作者的AfId,因为数据不够，判断不完全
						if (jsonAA.keySet().contains("AfId")) {
							long curAfId = jsonAA.getLong("AfId");
							if (curAuId == StartId)
								map_AfIds.put(curAfId, 1);

						}
						aus[j] = au;
					}
					AuPaper.setAA(aus);
					map_Papers.put(AuPaper, 1);
				}
				Author.setAfId(map_AfIds);
				Author.setIds(map_Ids);
				Author.setAuId(StartId);
				Author.setPaper(map_Papers);
				map_AuthorEntities.put(StartId, Author);
				return "AuId";
			}
			if (jsonset.contains("Id")) {
				long curId = jsonobj.getLong("Id");
				Paper.setId(curId);
			}
			if (jsonset.contains("RId")) {
				JSONArray arrayRId = jsonobj.getJSONArray("RId");
				long[] papers = new long[arrayRId.length()];
				for (int j = 0; j < arrayRId.length(); j++) {
					long curRId = arrayRId.getLong(j);
					papers[j] = curRId;
				}
				Paper.setRId(papers);
			}

			if (jsonset.contains("F")) {
				JSONArray arrayF = jsonobj.getJSONArray("F");
				long[] paperF = new long[arrayF.length()];
				for (int j = 0; j < arrayF.length(); j++) {
					JSONObject jsonF = arrayF.getJSONObject(j);
					if (jsonF.keySet().contains("FId")) {
						long curFId = jsonF.getLong("FId");
						paperF[j] = curFId;
					}
				}
				Paper.setFId(paperF);
			}
			if (jsonset.contains("J")) {
				JSONObject jsonJ = jsonobj.getJSONObject("J");
				if (jsonJ.keySet().contains("JId")) {
					long curJId = jsonJ.getLong("JId");
					Paper.setJId(curJId);
				}
			}
			if (jsonset.contains("C")) {
				JSONObject jsonC = jsonobj.getJSONObject("C");
				if (jsonC.keySet().contains("CId")) {
					long curCId = jsonC.getLong("CId");
					Paper.setCId(curCId);
				}
			}
			map_PaperEntities.put(StartId, Paper);
			return "Id";
		} else
			return "Wrong";

	}
}
