package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.handlers.*;
import com.gerantech.towers.sfs.utils.BattleUtils;
import com.gerantech.towers.sfs.utils.NPCTools;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
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
    private Room lobby;
    public void init() {
        lobby = getParentRoom();
        lobby.setProperty("queue", new SFSArray());

        addEventHandler(SFSEventType.USER_JOIN_ROOM, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, LobbyRoomServerEventsHandler.class);

        addRequestHandler(Commands.LOBBY_PUBLIC_MESSAGE, PublicMessageHandler.class);
        addRequestHandler(Commands.LOBBY_INFO, LobbyInfoHandler.class);
        addRequestHandler(Commands.LOBBY_KICK, LobbyKickHandler.class);
    }

    public void handleClientRequest(String requestId, User sender, ISFSObject params)
    {
        if( requestId.equals(Commands.LOBBY_PUBLIC_MESSAGE) )
        {
            //trace("in", params.getDump());
            organizeMessage(sender, params);
            /*ISFSArray messages = messageQueue();
            int msgSize = messages.size();
            for (int i = 0; i <msgSize; i++) {
                ISFSObject msg =  messages.getSFSObject(i);
                trace(i, msg.containsKey("m")?msg.getShort("m"):"", msg.containsKey("st")?msg.getShort("st"):"", msg.containsKey("i")?msg.getInt("i"):"", msg.containsKey("bid") ? msg.getInt("bid") : "");
            }
            trace("out", params.getDump());*/
        }
        else if( requestId.equals(Commands.LOBBY_INFO) )
        {
             params.putSFSArray("messages", messageQueue());
        }

        super.handleClientRequest(requestId, sender, params);
    }

    private void organizeMessage(User sender, ISFSObject params)
    {
        // Add time and user-id to message
        Game game = ((Game) sender.getSession().getProperty("core"));
        if( !params.containsKey("m") )
            params.putShort("m", (short) MessageTypes.M0_TEXT);
        params.putInt("u", (int) Instant.now().getEpochSecond());
        params.putInt("i", game.player.id);
        params.putText("s", game.player.nickName);

        Short mode = params.getShort("m");
        ISFSArray messages = messageQueue();
        // Max 30 len message queue
        while( messages.size() > 30 )
            messages.removeElementAt(0);

        if( mode == MessageTypes.M0_TEXT )
        {
            // Merge messages from a sender
            ISFSObject last = messages.size() > 0 ? messages.getSFSObject(messages.size() - 1) : null;
            if( last != null && last.getShort("m") == MessageTypes.M0_TEXT && last.getInt("i") == game.player.id )
            {
                params.putUtfString("t", last.getUtfString("t") + "\n" + params.getUtfString("t"));
                messages.removeElementAt(messages.size() - 1);
            }
        }
        else if( mode == MessageTypes.M30_FRIENDLY_BATTLE ) {

            // cancel requested battle by owner
            ISFSObject message = getMyRequestedBattle(params, game.player);
            if (message != null) {
                params.putShort("st", (short) 3);
                message.putShort("st", (short) 3);
                Room room = getParentZone().getRoomById(message.getInt("bid"));
                if (room != null)
                    getApi().leaveRoom(sender, room);
                return;
            }

            // join to an available battle
            message = getAvailableBattle(params);
            if (message != null) {
                Room room = getParentZone().getRoomById(params.getInt("bid"));
                if (room != null) {
                    BattleUtils.getInstance().join(sender, room, "");

                    params.putText("o", game.player.nickName);
                    message.putText("o", game.player.nickName);

                    params.putInt("i", message.getInt("i"));
                    params.putText("s", message.getText("s"));

                    params.putShort("st", (short) 1);
                    message.putShort("st", (short) 1);
                }

                return;
            }

            // spectate started battle
            message = getStartedBattle(params);
            if (message != null) {
                Room room = getParentZone().getRoomById(params.getInt("bid"));
                if (room != null)
                    BattleUtils.getInstance().join(sender, room, game.player.nickName);
                return;
            }

            // request new battle
            if (params.getShort("st") > 0)
                return;

            BattleUtils battleUtils = BattleUtils.getInstance();
            Room room =  battleUtils.make(sender, false, 0, 1, false);
            lobby.setProperty(room.getName(), true);
            battleUtils.join(sender, room, "");
            params.putInt("bid", room.getId());

            //lobby.setProperty("queue", messages);
        }
        messages.addSFSObject(params);
    }

    private ISFSObject getMyRequestedBattle(ISFSObject params, Player player)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        ISFSObject message;
        for (int i = msgSize-1; i >=0; i--)
        {
            message = messages.getSFSObject(i);
            if( message.getShort("m") == MessageTypes.M30_FRIENDLY_BATTLE && message.getShort("st") == 0 && message.getInt("i") == player.id )
                return  message;
        }
        return null;
    }
    private ISFSObject getAvailableBattle(ISFSObject params)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        for (int i = msgSize-1; i >=0; i--)
            if (messages.getSFSObject(i).getShort("m") == MessageTypes.M30_FRIENDLY_BATTLE && params.getShort("st") == 0 && messages.getSFSObject(i).getShort("st") == 0 && messages.getSFSObject(i).getInt("bid").equals(params.getInt("bid")))
                return messages.getSFSObject(i);

        return null;
    }
    private ISFSObject getStartedBattle(ISFSObject params)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        for (int i = msgSize-1; i >=0; i--)
            if( messages.getSFSObject(i).getShort("m") == MessageTypes.M30_FRIENDLY_BATTLE && params.getShort("st") == 1 && messages.getSFSObject(i).getShort("st") == 1 && messages.getSFSObject(i).getInt("bid").equals(params.getInt("bid")))
                return messages.getSFSObject(i);
        return null;
    }

    public void sendComment(short mode, String subject, String object, short permissionId)
    {
        ISFSObject params = new SFSObject();
        params.putUtfString("t", "");
        params.putShort("m", mode);
        params.putText("s", subject);
        params.putText("o", object);
        params.putShort("p", permissionId);
        messageQueue().addSFSObject(params);
        super.handleClientRequest(Commands.LOBBY_PUBLIC_MESSAGE, null, params);
    }

    private ISFSArray messageQueue ()
    {
        return (ISFSArray) lobby.getProperty("queue");
    }
}