package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.hazelcast.internal.cluster.impl.JoinRequest;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyJoinHandler extends BaseClientRequestHandler
{
    private static final int REQUEST_SENT = 1;
    private static final int JOINED = 0;
    private static final int NOT_ALLOWED = -1;
    private static final int MULTI_LOBBY_ILLEGAL = -2;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        if(sender.getLastJoinedRoom() != null) {
            params.putInt("response", MULTI_LOBBY_ILLEGAL);
            trace(sender.getName() + " not able to join more lobbies !");
            send(Commands.LOBBY_JOIN, params, sender);
            return;
        }

        Room room = getParentExtension().getParentZone().getRoomById(params.getInt("id"));
        Integer privacy = room.getVariable("pri").getIntValue();
        if( privacy == 0 )
        {
            try {
                getApi().joinRoom(sender, room, null, false, null);
            } catch (SFSJoinRoomException e) {
                e.printStackTrace();
            }
            params.putInt("response", JOINED);
        }
        else if( privacy == 1 )
        {
            Game game = ((Game) sender.getSession().getProperty("core"));
            ISFSArray messages = room.getVariable("msg").getSFSArrayValue();

            for (int i = messages.size()-1; i >= 0; i--)
                if( messages.getSFSObject(i).getShort("m") == MessageTypes.M41_CONFIRM_JOIN && messages.getSFSObject(i).getInt("o") == game.player.id && !messages.getSFSObject(i).containsKey("pr") )
                    return;

            SFSObject msg = new SFSObject();
            msg.putShort("m", (short) MessageTypes.M41_CONFIRM_JOIN);
            msg.putInt("u", (int) Instant.now().getEpochSecond());
            msg.putInt("o", game.player.id);
            msg.putText("on", game.player.nickName);
            messages.addSFSObject(msg);

            getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, room.getUserList(), room, false);
            //send(Commands.LOBBY_PUBLIC_MESSAGE, msg, room.getUserList());
            params.putInt("response", REQUEST_SENT);
        }
        else
        {
            params.putInt("response", NOT_ALLOWED);
        }
        send(Commands.LOBBY_JOIN, params, sender);
    }
}