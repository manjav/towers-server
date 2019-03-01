package com.gt.utils;

import com.smartfoxserver.v2.extensions.SFSExtension;

public class UtilBase
{
    static protected SFSExtension ext = null;
    protected boolean debugMode = true;

    static public void setExtension(SFSExtension extension)
    {
        if( ext == null )
            ext = extension;
    }

    static public Object get(Class _class)
    {
        //trace("get", _class.getSimpleName(), ext.getParentZone());
        if( !ext.getParentZone().containsProperty(_class.getSimpleName()) ) {
            try {
                ext.getParentZone().setProperty(_class.getSimpleName(), _class.newInstance());
            } catch (InstantiationException e) { e.printStackTrace(); } catch (IllegalAccessException e) { e.printStackTrace(); }
        }
        return ext.getParentZone().getProperty(_class.getSimpleName());
    }

    protected void trace(Object... args)
    {
        if( debugMode )
            ext.trace(args);
    }
}