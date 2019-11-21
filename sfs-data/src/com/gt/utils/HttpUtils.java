package com.gt.utils;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.List;
import java.util.Map;

public class HttpUtils
{
	public static Data get(String url, Map<String, String> headers, boolean serializeToJSON)
	{
        HttpGet request = new HttpGet(url);
		if( headers != null )
			for( String h : headers.keySet() )
				request.addHeader(h, headers.get(h));
		
		CloseableHttpClient client = HttpClients.createDefault();
		try
        {
			HttpResponse response = client.execute(request);
			return new Data(response.getStatusLine().getStatusCode(), convertStreamToString(response.getEntity().getContent()), serializeToJSON);
		} catch (Exception e) {
			return new Data(e.hashCode(), e.getMessage(), serializeToJSON);
		}
	}
	
	public static Data post(String url, List <NameValuePair> params, boolean serializeToJSON)
	{
		return post(url, params, serializeToJSON, false);
	}

	public static Data post(String url, List<NameValuePair> params, boolean serializeToJSON, boolean jsonEntity)
	{
		HttpPost request = new HttpPost(url);
        CloseableHttpClient client = HttpClients.createDefault();
        try
        {
        	if( params != null )
			{
				if( !jsonEntity )
        			request.setEntity(new UrlEncodedFormEntity(params));
				else
				{
					JSONObject json = new JSONObject();
					for (NameValuePair nameValue : params) {
						json.put(nameValue.getName(), nameValue.getValue());
					}
					request.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
				}
					
			}
			HttpResponse response = client.execute(request);
			return new Data(response.getStatusLine().getStatusCode(), convertStreamToString(response.getEntity().getContent()), serializeToJSON);
		} catch (Exception e) {
			return new Data(e.hashCode(), e.getMessage(), serializeToJSON);
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
		public Data(int statusCode, String text, boolean serializeToJSON)
		{
			this.statusCode = statusCode;
			this.text= text;
			if( serializeToJSON )
		        json = (JSONObject) JSONSerializer.toJSON(text);
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return super.toString();
		}
		
	}
}