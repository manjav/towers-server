package com.gt.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigUtils
 */
public class ConfigUtils extends UtilBase
{
	public static String DEFAULT = "extensions/MMOry/default.properties";
	static public ConfigUtils getInstance()
	{
		return (ConfigUtils)UtilBase.get(ConfigUtils.class);
	}
	private Map<String, Properties> propertyList;
	public Properties load(String name)
	{
		if( this.propertyList == null )
			this.propertyList = new HashMap<>();
			
		if( this.propertyList.containsKey(name) )
			return this.propertyList.get(name);

		// load and cache properties
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(name));
		}
		catch (IOException e) {
			// TODO: requires to log as error after 2203-logs branch merge.
			// getLogger().error("Could not load config: " + name);
		}
		this.propertyList.put(name, properties);
		return properties;
	}
	
	public void save(Properties props)
	{
		try {
			props.store(new FileOutputStream(DEFAULT), "");
		}
		catch (IOException e) {
			// getLogger().error("Could not save config in: " + propertyFileName);
		}
	}
}