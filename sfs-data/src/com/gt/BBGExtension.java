package com.gt;

import com.gt.towers.Game;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.List;

public class BBGExtension
{
    protected final SFSExtension ext;
    protected final SmartFoxServer smartfox;

    public BBGExtension() {
        smartfox = SmartFoxServer.getInstance();
        ext = (SFSExtension) smartfox.getZoneManager().getZoneByName("towers").getExtension();
    }
    protected void send(String cmdName, ISFSObject params, List<User> recipients) {
        ext.send(cmdName, params, recipients, false);
    }
    protected void send(String cmdName, ISFSObject params, User recipient) {
        ext.send(cmdName, params, recipient, false);
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
