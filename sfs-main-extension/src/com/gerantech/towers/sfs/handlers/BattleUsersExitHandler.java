package com.gerantech.towers.sfs.handlers;

import com.gt.towers.Game;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import java.io.IOException;

/**
 * Created by ManJav on 9/2/2017.
 */
public class BattleUsersExitHandler extends BaseServerEventHandler
{
    public void handleServerEvent(ISFSEvent arg) throws SFSException
    {
        User user = (User) arg.getParameter(SFSEventParam.USER);
        if( user.isNpc() )
            return;

        ((Game)user.getSession().getProperty("core")).player.inFriendlyBattle = false;
        if( user.getBuddyProperties().getState() == "Available" )
            return;

        if ( !user.containsProperty("hasBuddyList") )
        {
            try {
			    SmartFoxServer.getInstance().getAPIManager().getBuddyApi().initBuddyList(user, true);
			    user.setProperty("hasBuddyList", true);
		    } catch (IOException e) { e.printStackTrace(); }
        }

       // user.getBuddyProperties().setVariable(new SFSBuddyVariable("$point", user.getVariable("point").getIntValue()));
        user.getBuddyProperties().setState("Available");
        SmartFoxServer.getInstance().getAPIManager().getBuddyApi().setBuddyVariables(user, user.getBuddyProperties().getVariables(), true, false);
    }
}