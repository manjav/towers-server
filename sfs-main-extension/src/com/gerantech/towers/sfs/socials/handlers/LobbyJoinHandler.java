package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.data.LobbyData;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import java.time.Instant;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyJoinHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game) sender.getSession().getProperty("core"));

        // if you found lobby by member means member already joint to a lobby
        LobbyData data = LobbyUtils.getInstance().getDataByMember(game.player.id);
        if( data != null )
        {
            params.putInt("response", MessageTypes.JOIN_LOBBY_MULTI_LOBBY_ILLEGAL);
            params.putText("lobby", data.getName());
            send(Commands.LOBBY_JOIN, params, sender);
            trace(sender.getName() + " already joined in " + data.getName() + ".");
            return;
        }

        // finding lobby data
        LobbyData lobbyData = LobbyUtils.getInstance().getDataById(params.getInt("id"));
        Room lobby = LobbyUtils.getInstance().getLobby(lobbyData);
        if( lobbyData.getPrivacy() == 0 )
        {
            LobbyUtils.getInstance().join(lobby, sender);
            params.putInt("response", MessageTypes.RESPONSE_SUCCEED);
        }
        else if( lobbyData.getPrivacy() == 1 )
        {
            ISFSArray messages = lobbyData.getMessages();
            for (int i = messages.size()-1; i >= 0; i--)
            {
                if( messages.getSFSObject(i).getShort("m") == MessageTypes.M41_CONFIRM_JOIN && messages.getSFSObject(i).getInt("o") == game.player.id && !messages.getSFSObject(i).containsKey("pr") )
                {
                    params.putInt("response", MessageTypes.RESPONSE_ALREADY_SENT);
                    send(Commands.LOBBY_JOIN, params, sender);
                    return;
                }
            }

            SFSObject msg = new SFSObject();
            msg.putShort("m", (short) MessageTypes.M41_CONFIRM_JOIN);
            msg.putInt("u", (int) Instant.now().getEpochSecond());
            msg.putInt("o", game.player.id);
            msg.putUtfString("on", game.player.nickName);
            try {
                lobby.getExtension().handleClientRequest(Commands.LOBBY_PUBLIC_MESSAGE, sender, msg);
            } catch (SFSException e) { e.printStackTrace(); }

            // getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, lobby.getUserList(), lobby, false);
            //send(Commands.LOBBY_PUBLIC_MESSAGE, msg, room.getUserList());
            params.putInt("response", MessageTypes.RESPONSE_SENT);
        }
        else
        {
            params.putInt("response", MessageTypes.RESPONSE_NOT_ALLOWED);
        }
        send(Commands.LOBBY_JOIN, params, sender);
    }
}