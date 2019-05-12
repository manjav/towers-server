package com.gerantech.towers.sfs.battle.handlers;

import com.gt.BBGRoom;
import com.gt.towers.battle.BattleField;
import com.gt.utils.BattleUtils;
import com.smartfoxserver.v2.SmartFoxServer;
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
        if( user.getBuddyProperties().getState() == "Available" )
            return;

        Object core = user.getSession().getProperty("core");
        if( core == null)
            return;

        if( !user.containsProperty("hasBuddyList") )
        {
            try {
			    SmartFoxServer.getInstance().getAPIManager().getBuddyApi().initBuddyList(user, true);
			    user.setProperty("hasBuddyList", true);
		    } catch (IOException e) { e.printStackTrace(); }
        }

       // user.getBuddyProperties().setVariable(new SFSBuddyVariable("$point", user.getVariable("point").getIntValue()));
        user.getBuddyProperties().setState("Available");
        SmartFoxServer.getInstance().getAPIManager().getBuddyApi().setBuddyVariables(user, user.getBuddyProperties().getVariables(), true, false);

        BattleUtils bu = BattleUtils.getInstance();

        BBGRoom room = bu.find(bu.getGame(user).player.id, BattleField.STATE_0_WAITING, BattleField.STATE_4_ENDED);
        if( room == null )
            return;
        if( room.getPropertyAsInt("state") < BattleField.STATE_1_CREATED )
        {
            BattleUtils.getInstance().remove(room);
        }
        else
        {
            BattleUtils.getInstance().leave(room, user);
            /*for(User u:r.getPlayersList())
            {
                if(!u.isNpc() && !u.equals(user))
                {
                    SFSObject sfsO = SFSObject.newInstance();
                    sfsO.putText("user", ((Game) user.getSession().getProperty("core")).player.nickName);
                    send("battleLeft", sfsO, u);
                }
            }*/
        }
    }
}