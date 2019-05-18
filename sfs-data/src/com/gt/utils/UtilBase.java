package com.gt.utils;

import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class UtilBase {
    static protected SFSExtension ext = null;
    static public void setExtension(SFSExtension extension) {
        if( ext == null )
        {
            ext = extension;

            // load all settings
            ext.getParentZone().setProperty("startTime", System.currentTimeMillis());
            //RankingUtils.getInstance().fillStatistics();
            RankingUtils.getInstance().fillActives();
            LobbyUtils.getInstance().loadAll();
            ChallengeUtils.getInstance().loadAll();
            ext.getParentZone().removeProperty("startTime");
        }
    }
    static public void setBattleClass(Class battleClass) {
            ext.getParentZone().setProperty("battleClass", battleClass);
    }

    static public Object get(Class _class) {
        //ext.trace("get", _class.getSimpleName(), ext.getParentZone());
        if( !ext.getParentZone().containsProperty(_class.getSimpleName()) )
        {
            try {
                ext.getParentZone().setProperty(_class.getSimpleName(), _class.newInstance());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return ext.getParentZone().getProperty(_class.getSimpleName());
    }

    public void trace(Object... args) {
        ext.trace(args);
    }
    public void trace(ExtensionLogLevel level, Object... args) {
        ext.trace(level, args);
    }
    public Game getGame(User user)
    {
        return (Game) user.getSession().getProperty("core");
    }
}