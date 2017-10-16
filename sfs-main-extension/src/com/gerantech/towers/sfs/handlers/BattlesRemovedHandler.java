package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import java.util.List;

/**
 * Created by ManJav on 9/2/2017.
 */
public class BattlesRemovedHandler extends BaseServerEventHandler
{
    public void handleServerEvent(ISFSEvent arg) throws SFSException
    {
        Room room = (Room) arg.getParameter(SFSEventParam.ROOM);
        if( room.getGroupId().equals("lobbies") )//|| !room.containsProperty("registeredPlayers") )
            return;

        List<Room> lobbies = getParentExtension().getParentZone().getRoomListFromGroup("lobbies");
        for (int i = 0; i < lobbies.size(); i++)
        {
            Room lobby = lobbies.get(i);
            if ( lobby.containsProperty(room.getName()) )
            {
                ISFSArray messageQueue = lobby.getVariable("msg").getSFSArrayValue();
                int msgSize = messageQueue.size();
                for (int j = msgSize-1; j >= 0; j--)
                {
                    ISFSObject msg = messageQueue.getSFSObject(j);
                    if( msg.containsKey("bid") && msg.getInt("bid") == room.getId() && msg.getShort("st") < 2 )
                    {
                        msg.putShort("st", (short)2);
                        if( lobby.getUserList().size() > 0 )
                            getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, lobby.getUserList(), lobby, false);
                    }
                }
                //trace(room.getName(), lobby.getName());
            }
        }

    }
}