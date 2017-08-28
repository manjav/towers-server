package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.socials.handlers.LobbyDataHandler;
import com.gerantech.towers.sfs.socials.handlers.LobbyInfoHandler;
import com.gerantech.towers.sfs.socials.handlers.LobbyRoomServerEventsHandler;
import com.gerantech.towers.sfs.socials.handlers.PublicMessageHandler;
import com.gt.towers.Game;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.time.Instant;

/**
 * Created by ManJav on 8/25/2017.
 */
public class LobbyRoom extends SFSExtension
{

    private Room room;

    public SFSArray messages;

    public void init()
    {
        room = getParentRoom();
        messages = new SFSArray();

        addEventHandler(SFSEventType.ROOM_REMOVED, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_JOIN_ROOM, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, LobbyRoomServerEventsHandler.class);

        addRequestHandler("m", PublicMessageHandler.class);
        addRequestHandler("lobbyInfo", LobbyInfoHandler.class);
    }

    public void handleServerEvent(ISFSEvent event) throws Exception
    {
        super.handleServerEvent(event);
     //   trace("handleServerEvent", event);
    }

    @Override
    public void handleClientRequest(String requestId, User sender, ISFSObject params)
    {
        if( requestId.equals("m") )
        {
            // Add time and user-id to message
            Game game = ((Game) sender.getSession().getProperty("core"));
            params.putInt("u", (int) Instant.now().getEpochSecond());
            params.putInt("i", game.player.id);

            // Max 30 len message queue
            if (messages.size() > 30)
                messages.removeElementAt(0);

            // Merge messages from a sender
            ISFSObject last = messages.size() > 0 ? messages.getSFSObject(messages.size() - 1) : null;
            if (last != null && last.getInt("i").equals(game.player.id))
            {
                params.putUtfString("t", last.getUtfString("t") + "\n" + params.getUtfString("t"));
                messages.removeElementAt(messages.size() - 1);
            }
            messages.addSFSObject(params);
        }
        else if( requestId.equals("lobbyInfo") )
        {
            LobbyDataHandler.fillUsersData(getParentZone().getExtension(), room, sender);
            params.putSFSArray("messages", messages);
        }
        //trace(requestId, params.getDump());
        super.handleClientRequest(requestId, sender, params);
    }
}