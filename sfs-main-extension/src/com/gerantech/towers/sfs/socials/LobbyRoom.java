package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.socials.handlers.LobbyEditHandler;
import com.gerantech.towers.sfs.socials.handlers.LobbyInfoHandler;
import com.gerantech.towers.sfs.socials.handlers.LobbyModerationHandler;
import com.gerantech.towers.sfs.socials.handlers.LobbyRoomServerEventsHandler;
import com.gt.BBGRoom;
import com.gt.Commands;
import com.gt.data.LobbySFS;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.gt.utils.BanUtils;
import com.gt.utils.BattleUtils;
import com.gt.utils.InboxUtils;
import com.gt.utils.LobbyUtils;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

import java.time.Instant;

/**
 * Created by ManJav on 8/25/2017.
 */
public class LobbyRoom extends BaseLobbyRoom
{
    private int savedAt;

    // @Override`
    public void init()
    {
        super.init();
        savedAt = (int) Instant.now().getEpochSecond();
        addEventHandler(SFSEventType.USER_JOIN_ROOM, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_DISCONNECT, LobbyRoomServerEventsHandler.class);
        addRequestHandler(Commands.LOBBY_INFO, LobbyInfoHandler.class);
        addRequestHandler(Commands.LOBBY_EDIT, LobbyEditHandler.class);
        addRequestHandler(Commands.LOBBY_MODERATION, LobbyModerationHandler.class);
    }

   // @Override
    public void handleClientRequest(String requestId, User sender, ISFSObject params)
    {
        if( requestId.equals(Commands.LOBBY_INFO) && !params.containsKey("nomsg") )
            params.putSFSArray("messages", messageQueue() );
        super.handleClientRequest(requestId, sender, params);
    }

 //   @Override
    protected void organizeMessage(User sender, ISFSObject params, boolean alreadyAdd)
    {
        super.organizeMessage(sender, params, false);
        if( MessageTypes.isBattle(mode) )
        {
            BattleUtils battleUtils = BattleUtils.getInstance();
            // cancel requested battle by owner
            int battleMsgIndex = getMyRequestedBattle(params, game.player);
            if( battleMsgIndex > -1 )
            {
                params.putInt("st", 3);
                messages.getSFSObject(battleMsgIndex).putInt("st", 3);
                BBGRoom room = battleUtils.rooms.get(messages.getSFSObject(battleMsgIndex).getInt("bid"));
                if( room != null )
                    battleUtils.remove(room);
                messages.removeElementAt(battleMsgIndex);
                return;
            }

            // join to an available battle
            ISFSObject message = getAvailableBattle(params);
            if( message != null )
            {
                BBGRoom room = battleUtils.rooms.get(params.getInt("bid"));
                if( room != null)
                {
                    battleUtils.join(room, sender, "");

                    params.putUtfString("o", game.player.nickName);
                    message.putUtfString("o", game.player.nickName);

                    params.putInt("i", message.getInt("i"));
                    params.putUtfString("s", message.getUtfString("s"));

                    params.putInt("st", 1);
                    message.putInt("st", 1);
                }
                return;
            }

            // spectate started battle
            message = getStartedBattle(params);
            if( message != null )
            {
                BBGRoom room = battleUtils.rooms.get(params.getInt("bid"));
                if( room != null )
                    BattleUtils.getInstance().join(room, sender, game.player.nickName);
                return;
            }

            // request new battle
            if( params.getInt("st") > 0 )
                return;

            BBGRoom room = battleUtils.make((Class) getParentZone().getProperty("battleClass"), sender, params.containsKey("bt")?1:0, 0, 1);
            lobby.setProperty(room.getName(), true);
            battleUtils.join(room, sender, "");
            params.putInt("bid", room.getId());
        }
        else if( MessageTypes.isConfirm(mode) )
        {
            int confirmIndex = getRelatedConfirm(messages, params);
            if( confirmIndex > -1 || params.containsKey("pr") )
            {
                if( confirmIndex > -1 )
                {
                    if( replyRequest(game, params) )
                        messages.removeElementAt(confirmIndex);
                    messages.getSFSObject(confirmIndex).putInt("pr", params.getInt("pr"));
                }
                return;
            }
        }
        messages.addSFSObject(params);
        data.setMessages(messages);

        // save lobby messages every 30 seconds
        if( savedAt < params.getInt("u") - 30 )
        {
            LobbyUtils.getInstance().save(data, null, null, -1, -1, -1, -1, null, data.getMessagesBytes());
            savedAt = params.getInt("u");
        }
    }

    private int getRelatedConfirm(ISFSArray messages, ISFSObject params)
    {
        int msgSize = messages.size();
        for (int i = msgSize - 1; i >= 0; i--)
            if( MessageTypes.isConfirm(messages.getSFSObject(i).getInt("m")) && messages.getSFSObject(i).getInt("o").equals(params.getInt("o")) )
                return i;
        return -1;
    }

    private int getMyRequestedBattle(ISFSObject params, Player player)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        ISFSObject message;
        for (int i = msgSize-1; i >=0; i--)
        {
            message = messages.getSFSObject(i);
            if( MessageTypes.isBattle(message.getInt("m")) && message.getInt("st") == 0 && message.getInt("i") == player.id )
                return i;
        }
        return -1;
    }
    private ISFSObject getAvailableBattle(ISFSObject params)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        for (int i = msgSize-1; i >=0; i--)
            if (MessageTypes.isBattle(messages.getSFSObject(i).getInt("m")) && params.getInt("st") == 0 && messages.getSFSObject(i).getInt("st") == 0 && messages.getSFSObject(i).getInt("bid").equals(params.getInt("bid")))
                return messages.getSFSObject(i);

        return null;
    }
    private ISFSObject getStartedBattle(ISFSObject params)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        for (int i = msgSize-1; i >=0; i--)
            if( MessageTypes.isBattle(messages.getSFSObject(i).getInt("m")) && params.getInt("st") == 1 && messages.getSFSObject(i).getInt("st") == 1 && messages.getSFSObject(i).getInt("bid").equals(params.getInt("bid")))
                return messages.getSFSObject(i);
        return null;
    }

    public void sendComment(int mode, Player subject, String object, int permissionId)
    {
        if( subject.admin )
            return;

        ISFSObject msg = new SFSObject();
        msg.putUtfString("t", "");
        msg.putInt("m", mode);
        msg.putUtfString("s", subject.nickName);
        msg.putUtfString("o", object);
        msg.putInt("p", permissionId);
        //messageQueue().addSFSObject(msg);
        handleClientRequest(Commands.LOBBY_PUBLIC_MESSAGE, null, msg);
    }

    /*private int getActiveness ()
    {
        return lobby.getVariable("act").getIntValue();
    }*/

    public LobbySFS getData()
    {
        return (LobbySFS) lobby.getProperty("data");
    }

    private boolean replyRequest(Game game, ISFSObject params)
    {
        boolean accepted = params.getInt("pr") == MessageTypes.M16_COMMENT_JOIN_ACCEPT;
        if( accepted )
        {
            if ( !LobbyUtils.getInstance().addUser(getData(), params.getInt("o")) )
                return false;

            // join online users
            LobbyUtils.getInstance().join(lobby, getParentZone().getUserByName(params.getInt("o").toString()));
        }

        String msg = "درخواست عضویتت در دهکده " + lobby.getName() + (accepted ? " پذیرفته شد. " : " رد شد. ");
        InboxUtils.getInstance().send(accepted?MessageTypes.M50_URL:MessageTypes.M0_TEXT, msg, BanUtils.SYSTEM_ID, params.getInt("o"), "towers://open?controls=tabs&dashTab=3&socialTab=0");
        sendComment(params.getInt("pr"), game.player, params.getUtfString("on"), -1);// mode = join
        return true;
    }

    @Override
    protected ISFSArray messageQueue ()
    {
        return getData().getMessages();
    }
}