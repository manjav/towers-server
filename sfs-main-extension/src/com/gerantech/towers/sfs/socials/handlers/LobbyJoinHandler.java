package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyJoinHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game) sender.getSession().getProperty("core"));
        if( LobbyUtils.getInstance().getLobby(game.player.id) != null )
        {
            params.putInt("response", MessageTypes.JOIN_LOBBY_MULTI_LOBBY_ILLEGAL);
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

            // reset weekly battles
            try {
                getParentExtension().getParentZone().getDBManager().executeUpdate("UPDATE resources SET count= 0 WHERE type=1204 AND count != 0 AND player_id = " + game.player.id, new Object[]{});
            } catch (SQLException e) { e.printStackTrace(); }

            IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
            game.player.resources.set(ResourceType.BATTLES_COUNT_WEEKLY, 0);
            RankData rd = new RankData(game.player.id, game.player.nickName,  game.player.get_point(), 0);
            if( users.containsKey(game.player.id) )
                users.replace(game.player.id, rd);
            else
                users.put(game.player.id, rd);

            params.putInt("response", MessageTypes.RESPONSE_SUCCEED);
        }
        else if( privacy == 1 )
        {
            ISFSArray messages = room.getVariable("msg").getSFSArrayValue();

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
            messages.addSFSObject(msg);

            getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, room.getUserList(), room, false);
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