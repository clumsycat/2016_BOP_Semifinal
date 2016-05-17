package com.bupt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Servlet implementation class ThreadKitty
 */
@WebServlet("/ThreadKitty")

public class ThreadKitty extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// 保留查询结果
	public static JSONArray jsonResult;
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	// 缓存
	public boolean CACHE = true;
	public static Map<Long, PaperEntities> map_PaperEntities = new HashMap<Long, PaperEntities>();
	public static Map<Long, AuthorEntities> map_AuthorEntities = new HashMap<Long, AuthorEntities>();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ThreadKitty() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		jsonResult = new JSONArray();
		// 为了存储可能的id1和id2的实体类型
		PaperEntities Papers[] = new PaperEntities[2];
		AuthorEntities Authors[] = new AuthorEntities[2];
		for (int i = 0; i < Papers.length; i++) {
			Papers[i] = new PaperEntities();
			Authors[i] = new AuthorEntities();
		}

		String[] Type = new String[2];
		long startId = Long.parseLong(request.getParameter("id1"));
		long endId = Long.parseLong(request.getParameter("id2"));

		try {
			Type = InitThread.RunThread(Papers, Authors, startId, endId, map_PaperEntities, map_AuthorEntities);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Type[0].equals("Id") && Type[1].equals("Id")) {
			jsonResult = IdToIdThread.RunThread(Papers[0], Papers[1], startId, endId);
		} else if (Type[0].equals("AuId") && Type[1].equals("Id")) {
			FromAuIdToId(Authors[0], Papers[1], startId, endId);
		} else if (Type[0].equals("Id") && Type[1].equals("AuId")) {
			jsonResult = IdToAuIdThread.RunThread(Papers[0], Authors[1], startId, endId);
		} else if (Type[0].equals("AuId") && Type[1].equals("AuId")) {
			FromAuIdToAuId(Authors[0], Authors[1], startId, endId);
		}
		response.getWriter().println(jsonResult);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
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
		//Auid-AfId-Auid-Id
		for(AuthorEntities aue : endPaper.getAA())
		{
			if (startAuthor.getAfId() != null && aue.getAfId() != null) {
				for (long afs : startAuthor.getAfId().keySet())
					for (long afe : aue.getAfId().keySet())
						if (afs == afe && map_afidAuid.get(aue.getAuId() + afs * 1000) == null) {
							Put3Hop(startId, afs, aue.getAuId(), endId);
							map_afidAuid.put(aue.getAuId() + afs * 1000, 1);
						}
			}
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
				// Auid-Id-Auid-Id 
				if (paper.getAA() != null) {
					for (AuthorEntities aus : paper.getAA()) {
						for (AuthorEntities aue : endPaper.getAA()) {
							if (aus.getAuId() == aue.getAuId()) {
								// auid-id-auid
								Put3Hop(startId, curId, aue.getAuId(), endId);
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
}