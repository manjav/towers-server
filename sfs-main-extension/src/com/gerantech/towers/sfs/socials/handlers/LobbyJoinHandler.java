package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
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
        LobbyData lobbyData = LobbyUtils.getInstance().getDataById(params.getInt("id"));
        if( lobbyData.isFull() && !game.player.admin )
        {
            sendResponse( MessageTypes.RESPONSE_NOT_ALLOWED, lobbyData.getName(), params, sender);
            return;
        }

        // if you found lobby by member means member already joint to a lobby
        LobbyData data = game.player.admin ? null : LobbyUtils.getInstance().getDataByMember(game.player.id);
        if( data != null )
        {
            sendResponse( MessageTypes.JOIN_LOBBY_MULTI_LOBBY_ILLEGAL, data.getName(), params, sender);
            return;
        }

        int response = -1;
        Room lobby = LobbyUtils.getInstance().getLobby(lobbyData);
        if( lobbyData.getPrivacy() == 0 || game.player.admin )
        {
            LobbyUtils.getInstance().join(lobby, sender);
            response = MessageTypes.RESPONSE_SUCCEED;
        }
        else if( lobbyData.getPrivacy() == 1 )
        {
            ISFSArray messages = lobbyData.getMessages();
            for (int i = messages.size()-1; i >= 0; i--)
            {
                if( messages.getSFSObject(i).getShort("m") == MessageTypes.M41_CONFIRM_JOIN && messages.getSFSObject(i).getInt("o") == game.player.id && !messages.getSFSObject(i).containsKey("pr") )
                {
                    sendResponse(MessageTypes.RESPONSE_ALREADY_SENT, lobbyData.getName(), params, sender);
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

            response = MessageTypes.RESPONSE_SENT;
        }
        else
        {
            response = MessageTypes.RESPONSE_NOT_ALLOWED;
        }
        sendResponse(response, lobbyData.getName(), params, sender);
    }

    private void sendResponse(int response, String name, ISFSObject params, User sender)
    {
        params.putInt("response", response);
        params.putText("lobby", name);
        send(Commands.LOBBY_JOIN, params, sender);
        trace("Sender:", sender.getName() + "  Response: " + response + "  Lobby: " + name);
    }
}