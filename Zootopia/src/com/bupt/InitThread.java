package com.bupt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONObject;

public class InitThread {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	public static String[] Type = new String[2];
	static ExecutorService pool = Executors.newFixedThreadPool(2);
	public static boolean CACHE = true;

	public static String[] RunThread(PaperEntities[] papers, AuthorEntities[] authors, long startId, long endId,
			Map<Long, PaperEntities> map_PaperEntities, Map<Long, AuthorEntities> map_AuthorEntities)
					throws InterruptedException, ExecutionException {
		// 创建一个线程池
		Lock lock = new ReentrantLock();
		Future f1 = null, f2 = null;
		Type[0] = new String();
		Type[1] = new String();
		if (CACHE && map_PaperEntities.containsKey(startId)) {
			Type[0] = "Id";
			papers[0] = map_PaperEntities.get(startId);
		} else if (CACHE && map_AuthorEntities.containsKey(startId)) {
			Type[0] = "AuId";
			authors[0] = map_AuthorEntities.get(startId);
		} else {
			Callable c1 = new Init(papers[0], authors[0], startId, lock, map_PaperEntities, map_AuthorEntities,
					Type[0]);
			f1 = pool.submit(c1);
		}
		if (CACHE && map_PaperEntities.containsKey(endId)) {
			Type[1] = "Id";
			papers[1] = map_PaperEntities.get(endId);
		} else if (CACHE && map_AuthorEntities.containsKey(endId)) {
			Type[1] = "AuId";
			authors[1] = map_AuthorEntities.get(endId);
		} else {
			Callable c2 = new Init(papers[1], authors[1], endId, lock, map_PaperEntities, map_AuthorEntities, Type[1]);
			f2 = pool.submit(c2);
		}
		if (f1 != null)
			Type[0] = f1.get().toString();
		if (f2 != null)
			Type[1] = f2.get().toString();
		//System.out.println("执行完成 以后：" + map_PaperEntities.size());
		return Type;
	}

}

class Init implements Callable {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	long StartId;
	PaperEntities Paper;
	AuthorEntities Author;
	Lock lock;
	Map<Long, PaperEntities> map_PaperEntities;
	Map<Long, AuthorEntities> map_AuthorEntities;
	String type;

	Init(PaperEntities Paper, AuthorEntities Author, long StartId, Lock lock,
			Map<Long, PaperEntities> map_PaperEntities, Map<Long, AuthorEntities> map_AuthorEntities, String type) {
		this.StartId = StartId;
		this.Paper = Paper;
		this.Author = Author;
		this.lock = lock;
		this.map_PaperEntities = map_PaperEntities;
		this.map_AuthorEntities = map_AuthorEntities;
		this.type = type;
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
	public String call() throws Exception {
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
							if (jsonAA.keySet().contains("AuId")) {
								long curAuId = jsonAA.getLong("AuId");
								for (AuthorEntities au : AuEn) {
									if (au.getAuId() == curAuId) {
										// 保存每个作者的AfIds和Ids
										if (jsonAA.keySet().contains("AfId")) {
											long curAfId = jsonAA.getLong("AfId");
											map_AfIds = au.getAfId();
											map_AfIds.put(curAfId, 1);
											au.setAfId(map_AfIds);
										}
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
				Paper.setAA(AuEn);
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
			//System.out.println("put 以后：" + map_PaperEntities.size());
			return "Id";
		} else
			return "Wrong";

	}
}