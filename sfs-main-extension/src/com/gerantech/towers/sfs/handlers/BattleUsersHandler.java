package com.gerantech.towers.sfs.handlers;

import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

/**
 * Created by ManJav on 9/2/2017.
 */
public class BattleUsersHandler extends BaseServerEventHandler
{
    public void handleServerEvent(ISFSEvent arg) throws SFSException
    {
        trace("BATTLE-USERS-HANDLER", arg);
        User user = (User) arg.getParameter(SFSEventParam.USER);
        Room room = (Room) arg.getParameter(SFSEventParam.ROOM);

        String state = arg.getType()== SFSEventType.USER_JOIN_ROOM && room.getGroupId() != "lobbies" ? "Occupied" : "Available";
        updateUserVariables(user, (int)user.getProperty("point"), state);
    }

    private void updateUserVariables(User user, int point, String state)
    {
        user.getBuddyProperties().setVariable(new SFSBuddyVariable("$point", point));
        user.getBuddyProperties().setState(state);
        try {
            getParentExtension().getBuddyApi().setBuddyVariables(user, user.getBuddyProperties().getVariables(), true, true);
        } catch (SFSBuddyListException e) {
            e.printStackTrace();
        }
    }
}