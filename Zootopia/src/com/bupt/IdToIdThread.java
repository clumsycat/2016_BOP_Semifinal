package com.bupt;

import java.io.IOException;
import java.net.URL;
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
import org.json.JSONTokener;

public class IdToIdThread {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	public static JSONArray jsonResult;
	static ExecutorService pool = Executors.newFixedThreadPool(4);

	public static JSONArray RunThread(PaperEntities StartPaper, PaperEntities EndPaper, long startId, long endId) {
		Lock lock = new ReentrantLock();
		jsonResult = new JSONArray();
		Callable c1 = new DirectIdToId(StartPaper, EndPaper, startId, endId, lock);
		Future f1 = pool.submit(c1);
		Callable c2 = new IdIdIdId(StartPaper, EndPaper, startId, endId, lock);
		Future f2 = pool.submit(c2);
		Callable c3 = new ThreeHopwJCId(StartPaper, EndPaper, startId, endId, lock);
		Future f3 = pool.submit(c3);
		Callable c4 = new ThreeHopFId(StartPaper, EndPaper, startId, endId, lock);
		Future f4 = pool.submit(c4);
		Callable c5 = new ThreeHopAuId(StartPaper, EndPaper, startId, endId, lock);
		Future f5 = pool.submit(c5);
		try {
			f1.get();
			f2.get();
			f3.get();
			f4.get();
			f5.get();
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

class DirectIdToId implements Callable {
	PaperEntities StartPaper;
	PaperEntities EndPaper;
	long startId;
	long endId;
	private Lock lock;

	DirectIdToId(PaperEntities StartPaper, PaperEntities EndPaper, long startId, long endId, Lock lock) {
		this.StartPaper = StartPaper;
		this.startId = startId;
		this.EndPaper = EndPaper;
		this.endId = endId;
		this.lock = lock;
	}

	public Boolean call() {
		String[] urls;
		JSONObject json = null;
		JSONArray jsonarray;
		// TODO Auto-generated method stub
		//long starttime = System.currentTimeMillis();
		// 1跳 Id-Id
		for (Long key : StartPaper.getRId()) {
			if (key == EndPaper.getId()) {
				lock.lock();
				IdToIdThread.Put1Hop(startId, endId);
				lock.unlock();
			}

		}
		// Id1-AuId-Id2
		if (StartPaper.getAA() != null && EndPaper.getAA() != null) {
			for (AuthorEntities saa : StartPaper.getAA())
				for (AuthorEntities eaa : EndPaper.getAA())
					if (saa.getAuId() == eaa.getAuId()) {
						lock.lock();
						IdToIdThread.Put2Hop(startId, saa.getAuId(), endId);
						lock.unlock();
					}
		}
		// id-CId-id
		long scid = StartPaper.getCId();
		long ecid = EndPaper.getCId();
		if (scid == ecid && scid != -1 && ecid != -1) {
			lock.lock();
			IdToIdThread.Put2Hop(startId, scid, endId);
			lock.unlock();
		}
		// id-JID-id
		long sjid = StartPaper.getJId();
		long ejid = EndPaper.getJId();
		if (sjid == ejid && sjid != -1 && ejid != -1) {
			lock.lock();
			IdToIdThread.Put2Hop(startId, sjid, endId);
			lock.unlock();
		}
		// id-Fid-id
		if (StartPaper.getFId() != null && EndPaper.getFId() != null) {
			for (long sfid : StartPaper.getFId())
				for (long efid : EndPaper.getFId()) {
					if (sfid == efid) {
						lock.lock();
						IdToIdThread.Put2Hop(startId, sfid, endId);
						lock.unlock();
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
				//System.out.println("Mapsize:" + map.size());
				for (long rid : StartPaper.getRId()) {
					if (map.get(rid) != null) {
						lock.lock();
						IdToIdThread.Put3Hop(startId, rid, a.getAuId(), endId);
						lock.unlock();
					}

				}
			}
		}
		// Id-id-fid-id
		if (EndPaper.getFId() != null && StartPaper.getRId().length > 0) {
			urls = KittyUrlUtil.getII3hopFId2URL(StartPaper, EndPaper, StartPaper.getRId().length);
			for (int k = 0; k < urls.length; k++) {
				try {
					URL urlurl = new URL(urls[k]);
					json = new JSONObject(new JSONTokener(urlurl.openStream()));
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("url解析失败！");
				}
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
								if (fid == curFId) {
									lock.lock();
									IdToIdThread.Put3Hop(startId, curId, curFId, endId);
									lock.unlock();
								}
							}
						}
					}
				}
			}
		}
		//System.out.println("Direct:" + (System.currentTimeMillis() - starttime));
		return true;
	}
}

class IdIdIdId implements Callable {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	PaperEntities StartPaper;
	PaperEntities EndPaper;
	long startId;
	long endId;
	private Lock lock;

	IdIdIdId(PaperEntities StartPaper, PaperEntities EndPaper, long startId, long endId, Lock lock) {
		this.StartPaper = StartPaper;
		this.startId = startId;
		this.EndPaper = EndPaper;
		this.endId = endId;
		this.lock = lock;
	}

	public Boolean call() {
		String url;
		String urls[];
		JSONObject json;
		JSONArray jsonarray;
		//long starttime = System.currentTimeMillis();
		// 3hop的从后面的id2查询
		Map<Long, Integer> map_fidforid2 = new HashMap<Long, Integer>();
		// 3跳 id-id-id-id
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
							if (curRId == endId) {
								lock.lock();
								IdToIdThread.Put2Hop(startId, curId, endId);
								lock.unlock();
							}
							// 若存在这样的id-rid-rid-id的链
							if (map_fidforid2.get(curRId) != null) {
								lock.lock();
								IdToIdThread.Put3Hop(startId, curId, curRId, endId);
								lock.unlock();
							}
						}
					}
				}
			}
		}
		//System.out.println("IdIdIdId:" + (System.currentTimeMillis() - starttime));
		return true;
	}
}

class ThreeHopAuId implements Callable {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	String url;
	JSONObject json;
	JSONArray jsonarray;
	PaperEntities StartPaper;
	PaperEntities EndPaper;
	long startId;
	long endId;
	private Lock lock;

	ThreeHopAuId(PaperEntities StartPaper, PaperEntities EndPaper, long startId, long endId, Lock lock) {
		this.StartPaper = StartPaper;
		this.startId = startId;
		this.EndPaper = EndPaper;
		this.endId = endId;
		this.lock = lock;
	}

	public Boolean call() {
		//long starttime = System.currentTimeMillis();
		// Id1-AuId-Id-Id2
		url = KittyUrlUtil.getII3hopAuIdURL(StartPaper, endId, count);
		// 多线程会卡住在url获取的问题
		try {
			URL urlurl = new URL(url);
			json = new JSONObject(new JSONTokener(urlurl.openStream()));
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("url解析失败！");
		}
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
						lock.lock();
						IdToIdThread.Put3Hop(startId, curAuId, curId, endId);
						lock.unlock();
					}
				}
			}
		}
		//System.out.println("ThreeHopAuId:" + (System.currentTimeMillis() - starttime));
		return true;
	}
}

class ThreeHopFId implements Callable {
	public static int count = 10000;
	public static int MAX_COUNT = 100000;
	String url;
	JSONObject json;
	JSONArray jsonarray;
	PaperEntities StartPaper;
	PaperEntities EndPaper;
	long startId;
	long endId;
	private Lock lock;

	ThreeHopFId(PaperEntities StartPaper, PaperEntities EndPaper, long startId, long endId, Lock lock) {
		this.StartPaper = StartPaper;
		this.startId = startId;
		this.EndPaper = EndPaper;
		this.endId = endId;
		this.lock = lock;
	}

	public Boolean call() {
		//long starttime = System.currentTimeMillis();
		// Id1-FId-Id-Id2, 统一领域的，同样可以设置较多的count
		if (StartPaper.getFId() != null) {
			url = KittyUrlUtil.getII3hopFIdURL(StartPaper, endId, MAX_COUNT);
			try {
				URL urlurl = new URL(url);
				json = new JSONObject(new JSONTokener(urlurl.openStream()));
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("url解析失败！");
			}
			jsonarray = json.getJSONArray("entities");
			for (int i = 0; i < jsonarray.length(); i++) {
				JSONObject jsonobj = jsonarray.getJSONObject(i);
				long curId = jsonobj.getLong("Id");
				JSONArray arrayF = jsonobj.getJSONArray("F");
				for (int j = 0; j < arrayF.length(); j++) {
					JSONObject jsonF = arrayF.getJSONObject(j);
					long curFId = jsonF.getLong("FId");
					for (long fid : StartPaper.getFId()) {
						if (fid == curFId) {
							lock.lock();
							IdToIdThread.Put3Hop(startId, curFId, curId, endId);
							lock.unlock();
						}
					}
				}
			}
		}
		//System.out.println("ThreeHopFId:" + (System.currentTimeMillis() - starttime));
		return true;
	}
}

class ThreeHopwJCId implements Callable {
	public static int MAX_COUNT = 100000;
	String url;
	String[] urls;
	JSONObject json;
	JSONArray jsonarray;
	PaperEntities StartPaper;
	PaperEntities EndPaper;
	long startId;
	long endId;
	private Lock lock;

	ThreeHopwJCId(PaperEntities StartPaper, PaperEntities EndPaper, long startId, long endId, Lock lock) {
		this.StartPaper = StartPaper;
		this.startId = startId;
		this.EndPaper = EndPaper;
		this.endId = endId;
		this.lock = lock;
	}

	public Boolean call() {
		//long starttime = System.currentTimeMillis();
		long scid = StartPaper.getCId();
		long ecid = EndPaper.getCId();
		// id-JID-id
		long sjid = StartPaper.getJId();
		long ejid = EndPaper.getJId();
		// Id1-JId-Id-Id2, 同一期刊的，同样可以设置较多的count
		if (sjid != -1) {
			url = KittyUrlUtil.getII3hopJIdURL(StartPaper, endId, MAX_COUNT);
			try {
				URL urlurl = new URL(url);
				json = new JSONObject(new JSONTokener(urlurl.openStream()));
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("url解析失败！");
			}
			jsonarray = json.getJSONArray("entities");
			for (int i = 0; i < jsonarray.length(); i++) {
				JSONObject jsonobj = jsonarray.getJSONObject(i);
				long curId = jsonobj.getLong("Id");
				lock.lock();
				IdToIdThread.Put3Hop(startId, StartPaper.getJId(), curId, endId);
				lock.unlock();
			}
		}
		// Id1-Id-JId-Id2, 同一期刊的，遗漏的宝宝，在rid中找，注定数量不多
		if (ejid != -1 && StartPaper.getRId().length > 0) {
			urls = KittyUrlUtil.getII3hopJId2URL(StartPaper, EndPaper, StartPaper.getRId().length);
			for (int k = 0; k < urls.length; k++) {
				try {
					URL urlurl = new URL(urls[k]);
					json = new JSONObject(new JSONTokener(urlurl.openStream()));
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("url解析失败！");
				}
				jsonarray = json.getJSONArray("entities");
				for (int i = 0; i < jsonarray.length(); i++) {
					JSONObject jsonobj = jsonarray.getJSONObject(i);
					long curId = jsonobj.getLong("Id");
					lock.lock();
					IdToIdThread.Put3Hop(startId, curId, StartPaper.getJId(), endId);
					lock.unlock();
				}
			}
		}
		// Id1-CId-Id-Id2
		if (scid != -1) {
			url = KittyUrlUtil.getII3hopCIdURL(StartPaper, endId, MAX_COUNT);
			try {
				URL urlurl = new URL(url);
				json = new JSONObject(new JSONTokener(urlurl.openStream()));
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("url解析失败！");
			}
			jsonarray = json.getJSONArray("entities");
			for (int i = 0; i < jsonarray.length(); i++) {
				JSONObject jsonobj = jsonarray.getJSONObject(i);
				long curId = jsonobj.getLong("Id");
				lock.lock();
				IdToIdThread.Put3Hop(startId, StartPaper.getCId(), curId, endId);
				lock.unlock();
			}
		}
		// id1-id-CId-id2,遗漏的宝宝,在rid中找，注定数量不多
		if (ecid != -1 && StartPaper.getRId().length > 0) {
			urls = KittyUrlUtil.getII3hopCId2URL(StartPaper, EndPaper, StartPaper.getRId().length);
			for (int k = 0; k < urls.length; k++) {
				try {
					URL urlurl = new URL(urls[k]);
					json = new JSONObject(new JSONTokener(urlurl.openStream()));
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("url解析失败！");
				}
				jsonarray = json.getJSONArray("entities");
				for (int i = 0; i < jsonarray.length(); i++) {
					JSONObject jsonobj = jsonarray.getJSONObject(i);
					long curId = jsonobj.getLong("Id");
					lock.lock();
					IdToIdThread.Put3Hop(startId, curId, StartPaper.getCId(), endId);
					lock.unlock();
				}
			}
		}
		//System.out.println("ThreeHopwJCId:" + (System.currentTimeMillis() - starttime));
		return true;
	}
}