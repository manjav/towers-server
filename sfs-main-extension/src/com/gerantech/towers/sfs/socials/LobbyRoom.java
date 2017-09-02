package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.handlers.*;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
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

        addEventHandler(SFSEventType.USER_JOIN_ROOM, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, LobbyRoomServerEventsHandler.class);

        addRequestHandler(Commands.LOBBY_PUBLIC_MESSAGE, PublicMessageHandler.class);
        addRequestHandler(Commands.LOBBY_INFO, LobbyInfoHandler.class);
        addRequestHandler(Commands.LOBBY_KICK, LobbyKickHandler.class);
    }

    public void handleServerEvent(ISFSEvent event) throws Exception
    {
        super.handleServerEvent(event);
        trace("handleServerEvent", event);
    }

    @Override
    public void handleClientRequest(String requestId, User sender, ISFSObject params)
    {
        if( requestId.equals(Commands.LOBBY_PUBLIC_MESSAGE) )
        {
            // Add time and user-id to message
            Game game = ((Game) sender.getSession().getProperty("core"));
            params.putShort("m", (short) MessageTypes.M0_TEXT);
            params.putInt("u", (int) Instant.now().getEpochSecond());
            params.putInt("i", game.player.id);
            params.putText("s", game.player.nickName);

            // Max 30 len message queue
            while( messages.size() > 30 )
                messages.removeElementAt(0);

            // Merge messages from a sender
            ISFSObject last = messages.size() > 0 ? messages.getSFSObject(messages.size() - 1) : null;
            if( last != null && last.getShort("m") == MessageTypes.M0_TEXT && last.getInt("i") == game.player.id )
            {
                params.putUtfString("t", last.getUtfString("t") + "\n" + params.getUtfString("t"));
                messages.removeElementAt(messages.size() - 1);
            }
            messages.addSFSObject(params);
        }
        else if( requestId.equals(Commands.LOBBY_INFO) )
        {
            LobbyDataHandler.fillUsersData(getParentZone().getExtension(), room, sender);
            params.putSFSArray("messages", messages);
        }
        //trace(requestId, params.getDump());
        super.handleClientRequest(requestId, sender, params);
    }

    public void sendComment(short mode, String subject, String object, short permissionId)
    {
        ISFSObject params = new SFSObject();
        params.putUtfString("t", "");
        params.putShort("m", mode);
        params.putText("s", subject);
        params.putText("o", object);
        params.putShort("p", permissionId);
        messages.addSFSObject(params);
        super.handleClientRequest(Commands.LOBBY_PUBLIC_MESSAGE, null, params);
    }
}