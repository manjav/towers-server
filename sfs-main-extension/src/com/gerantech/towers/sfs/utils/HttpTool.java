package com.gerantech.towers.sfs.utils;

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpTool
{
	public static String get(String url)
	{
        HttpGet request = new HttpGet(url);
		CloseableHttpClient client = HttpClients.createDefault();
        try
        {
			HttpResponse response = client.execute(request);
			return(convertStreamToString(response.getEntity().getContent()));
		} catch (Exception e) {
	        e.printStackTrace();
		}
		return null;
	}
	
	public static String post(String url, List <NameValuePair> params)
	{
		HttpPost request = new HttpPost(url);
        request.setHeader("access_token", "t19UX8TKJOF6ycJ8LHu5gHRqxIT8oV");
        CloseableHttpClient client = HttpClients.createDefault();
        try
        {
        	if(params != null)
        		request.setEntity(new UrlEncodedFormEntity(params));
			HttpResponse response = client.execute(request);
			return(convertStreamToString(response.getEntity().getContent()));
		} catch (Exception e) {
	       // e.printStackTrace();
	        return e.toString();
		}
		//return null;
	}
	static String convertStreamToString(java.io.InputStream is) {
	    @SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
}