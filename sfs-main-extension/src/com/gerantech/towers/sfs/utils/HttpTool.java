package com.gerantech.towers.sfs.utils;

import java.util.List;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpTool
{
	public static Data get(String url)
	{
        HttpGet request = new HttpGet(url);
		CloseableHttpClient client = HttpClients.createDefault();
        try
        {
			HttpResponse response = client.execute(request);
			return new Data(response.getStatusLine().getStatusCode(), convertStreamToString(response.getEntity().getContent()));
		} catch (Exception e) {
			return new Data(e.hashCode(), e.getMessage());
		}
	}
	
	public static Data post(String url, List <NameValuePair> params)
	{
		HttpPost request = new HttpPost(url);
        CloseableHttpClient client = HttpClients.createDefault();
        try
        {
        	if(params != null)
        		request.setEntity(new UrlEncodedFormEntity(params));
			HttpResponse response = client.execute(request);
			return new Data(response.getStatusLine().getStatusCode(), convertStreamToString(response.getEntity().getContent()));
		} catch (Exception e) {
			return new Data(e.hashCode(), e.getMessage());
		}
	}
	static String convertStreamToString(java.io.InputStream is) {
	    @SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
	public static class Data extends Object
	{
		public int statusCode;
		public String text;
		public JSONObject json;
		public Data(int statusCode, String text) 
		{
			this.statusCode = statusCode;
			this.text= text;
	        json = (JSONObject) JSONSerializer.toJSON(text);
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return super.toString();
		}
		
	}
}