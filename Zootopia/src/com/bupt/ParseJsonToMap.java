package com.bupt;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ParseJsonToMap {
	/**
	 * 取得查询json对象
	 * 
	 * @param urlpath
	 * @return
	 */
	public static JSONObject getUrlRequestJson(String urlpath) {
		JSONObject jsonObj = null;
		//System.out.println(urlpath);
		try {
			URL url = new URL(urlpath);
			jsonObj = new JSONObject(new JSONTokener(url.openStream()));
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("url解析失败！");
		}
		return jsonObj;
	}
}
