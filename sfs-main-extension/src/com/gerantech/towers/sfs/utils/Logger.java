package com.gerantech.towers.sfs.utils;

import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSErrorData;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSLoginException;

public class Logger 
{

	public static void throwLoginException(SFSErrorCode errorCode, String message, String param) throws SFSException
	{
    	//trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		SFSErrorData errData = new SFSErrorData(errorCode);
		errData.addParameter(param);
		throw new SFSLoginException(message, errData);		
	}
}