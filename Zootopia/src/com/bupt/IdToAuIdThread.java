package com.bupt;

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

public class IdToAuIdThread {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	public static JSONArray jsonResult;
	static ExecutorService pool = Executors.newFixedThreadPool(2);

	public static JSONArray RunThread(PaperEntities StartPaper, AuthorEntities EndAuthor, long startId, long endId) {
		Lock lock = new ReentrantLock();
		jsonResult = new JSONArray();
		Callable c1 = new EasyToAuId(StartPaper, EndAuthor, startId, endId, lock);
		Future f1 = pool.submit(c1);
		Callable c2 = new MapToAuId(StartPaper, EndAuthor, startId, endId, lock);
		Future f2 = pool.submit(c2);
		try {
			f1.get().toString();
			f2.get().toString();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonResult;
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

class EasyToAuId implements Callable {
	PaperEntities StartPaper;
	AuthorEntities EndAuthor;
	long startId;
	long endId;
	private Lock lock;

	EasyToAuId(PaperEntities StartPaper, AuthorEntities EndAuthor, long startId, long endId, Lock lock) {
		this.StartPaper = StartPaper;
		this.startId = startId;
		this.EndAuthor = EndAuthor;
		this.endId = endId;
		this.lock = lock;
	}

	public Boolean call() {
		Map<PaperEntities, Integer> map = EndAuthor.getPaper();
		EndAuthor.getIds();
		long cid = StartPaper.getCId();
		long jid = StartPaper.getJId();
		if (map.isEmpty() == false) {
			// 3hop Id-AuId-AfId-AuId
			for (AuthorEntities aus : StartPaper.getAA()) {
				for (long afide : EndAuthor.getAfId().keySet())
					for (long afids : aus.getAfId().keySet())
						if (afide == afids) {
							lock.lock();
							IdToAuIdThread.Put3Hop(startId, aus.getAuId(), afids, endId);
							lock.unlock();
						}
			}
			for (PaperEntities paper : map.keySet()) {
				long curId = paper.getId();

				// 2hop Id-Id-AuId
				for (long id : StartPaper.getRId())
					if (curId == id) {
						lock.lock();
						IdToAuIdThread.Put2Hop(startId, curId, endId);
						lock.unlock();
					}
				// 3 hop Id-FId-Id-AuId
				if (paper.getFId() != null && StartPaper.getFId() != null) {
					for (long fids : StartPaper.getFId())
						for (long fide : paper.getFId())
							if (fids == fide) {
								lock.lock();
								IdToAuIdThread.Put3Hop(startId, fids, curId, endId);
								lock.unlock();
							}
				}
				// id-jid-id-auid
				if (paper.getJId() != -1 && jid == paper.getJId()) {
					lock.lock();
					IdToAuIdThread.Put3Hop(startId, jid, curId, endId);
					lock.unlock();
				}
				// id-cid-id-auid
				if (paper.getCId() != -1 && cid == paper.getCId()) {
					lock.lock();
					IdToAuIdThread.Put3Hop(startId, cid, curId, endId);
					lock.unlock();
				}
				// id-auid-id-auid
				if (paper.getAA() != null) {
					for (AuthorEntities aue : paper.getAA())
						for (AuthorEntities aus : StartPaper.getAA())
							if (aue.getAuId() == aus.getAuId()) {
								lock.lock();
								IdToAuIdThread.Put3Hop(startId, aus.getAuId(), curId, endId);
								lock.unlock();
							}
				}

			}
		}
		return true;
	}
}

class MapToAuId implements Callable {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	PaperEntities StartPaper;
	AuthorEntities EndAuthor;
	long startId;
	long endId;
	private Lock lock;

	MapToAuId(PaperEntities StartPaper, AuthorEntities EndAuthor, long startId, long endId, Lock lock) {
		this.StartPaper = StartPaper;
		this.startId = startId;
		this.EndAuthor = EndAuthor;
		this.endId = endId;
		this.lock = lock;
	}

	public Boolean call() {
		String url;
		String urls[];
		JSONObject json;
		JSONArray jsonarray;
		// 1hop
		for (AuthorEntities au : StartPaper.getAA()) {
			if (au.getAuId() == endId) {
				lock.lock();
				IdToAuIdThread.Put1Hop(startId, endId);
				lock.unlock();
			}
		}
		Map<PaperEntities, Integer> map = EndAuthor.getPaper();
		Map<Long, Integer> idmap = EndAuthor.getIds();
		StartPaper.getCId();
		StartPaper.getJId();
		if (map.isEmpty() == false) {
			// 3 hop Id-Id-Id-AuId
			if (StartPaper.getRId().length > 0) {
				// 获得id1的RID的所有RID
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
								if (idmap.get(curRId) != null) {
									lock.lock();
									IdToAuIdThread.Put3Hop(startId, curId, curRId, endId);
									lock.unlock();
								}
							}
						}
					}
				}
			}
		}
		return true;
	}
}