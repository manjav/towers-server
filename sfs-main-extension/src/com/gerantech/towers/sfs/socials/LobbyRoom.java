package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.inbox.InboxUtils;
import com.gerantech.towers.sfs.socials.handlers.*;
import com.gerantech.towers.sfs.utils.BattleUtils;
import com.gerantech.towers.sfs.utils.DBUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.time.Instant;


/**
 * Created by ManJav on 8/25/2017.
 */
public class LobbyRoom extends SFSExtension
{
    private Room lobby;
    public void init()
    {
        lobby = getParentRoom();

        addEventHandler(SFSEventType.USER_JOIN_ROOM, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, LobbyRoomServerEventsHandler.class);

        addRequestHandler(Commands.LOBBY_PUBLIC_MESSAGE, PublicMessageHandler.class);
        addRequestHandler(Commands.LOBBY_INFO, LobbyInfoHandler.class);
        addRequestHandler(Commands.LOBBY_EDIT, LobbyEditHandler.class);
        addRequestHandler(Commands.LOBBY_MODERATION, LobbyModerationHandler.class);
    }

    public void handleClientRequest(String requestId, User sender, ISFSObject params)
    {
        if( requestId.equals(Commands.LOBBY_PUBLIC_MESSAGE) )
            organizeMessage(sender, params);
        else if( requestId.equals(Commands.LOBBY_INFO) && !params.containsKey("nomsg"))
             params.putSFSArray("messages", messageQueue());

        super.handleClientRequest(requestId, sender, params);
        LobbyUtils.getInstance().setActivenessVariable (lobby, getActiveness() + 1);
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
        else if( mode == MessageTypes.M30_FRIENDLY_BATTLE )
        {

            // cancel previous requested battle by owner
            ISFSObject message = getMyRequestedBattle(params, game.player);
            if (message != null) {
                params.putShort("st", (short) 3); // cancel this new battle request
                message.putShort("st", (short) 3); // cancel previous battle request
                Room room = getParentZone().getRoomById(message.getInt("bid"));
                if (room != null)
                    getApi().leaveRoom(sender, room);
                return;
            }

            // join to an available battle
            message = getAvailableBattle(params);
            if (message != null)
            {
                Room room = getParentZone().getRoomById(params.getInt("bid")); // battle room
                if (room != null)
                {
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

            // make new lobby battle
            BattleUtils battleUtils = BattleUtils.getInstance();
            Room room = battleUtils.make(sender, false, 0, 1, false);
            lobby.setProperty(room.getName(), true);
            battleUtils.join(sender, room, "");
            params.putInt("bid", room.getId());
        }
        else if ( mode == MessageTypes.M20_DONATE )
        {
            trace("\n\t..::DONATION::..\n");
            params.putInt("did", 1000);
            params.getDump();

            //check card request or card donate. state=0 --> Request | state=1 --> Donate
            if ( params.getShort("st") == 0 ) // Request Card
            {
                if ( checkPreviousCardRequest(params) )
                    return;
                messages.addSFSObject(params);
            }
            else if ( params.getShort("st") == 1 ) // Donate Card
            {
                getRequestedCardNum(params);
            }
            return;
        }
        else if (MessageTypes.isConfirm(mode))
        {
            int confirmIndex = getRelatedConfirm(messages, params);
            if (confirmIndex > -1 || params.containsKey("pr")) {
                if (confirmIndex > -1) {
                    replyRequest(game, params);
                    messages.getSFSObject(confirmIndex).putShort("pr", params.getShort("pr"));
                }
                return;
            }
        }
        messages.addSFSObject(params);
    }

    private void getRequestedCardNum(ISFSObject params)
    {
        try {
            DBUtils db = new DBUtils();
            SFSArray resources = db.getResources(params.getInt("i"));
            trace("player resources:");
            resources.getDump();
            /*for (int i = 0; i < resources.size(); i++) {
                for (int j = 0; j < resources.getSFSArray(i).size(); j++) {
                }
            }*/
        } catch (SFSException e) {
            e.printStackTrace();
        }
    }

    private Boolean checkPreviousCardRequest(ISFSObject params) // returns true if previous card req exists
    {
        ISFSArray messages = messageQueue();
        for (int i = messages.size()-1; i > 0; i--)
        {
            ISFSObject message = messages.getSFSObject(i);
            Boolean isDonate = ( message.getShort("m") == MessageTypes.M20_DONATE );
            if ( isDonate && message.getShort("st") == 0 && message.getInt("i").equals(params.getInt("i")) )
            {
                trace("ERROR-donation: There is a previous requests ...");
                return true;
            }
        }
        return false;
    }

    private void updateCardNum(int playerId, int cardType, int value)
    {
        DBUtils db = DBUtils.getInstance();
    }

    private int getRelatedConfirm(ISFSArray messages, ISFSObject params)
    {
        int msgSize = messages.size();
        for (int i = msgSize - 1; i >= 0; i--)
            if( MessageTypes.isConfirm(messages.getSFSObject(i).getShort("m")) && messages.getSFSObject(i).getInt("o").equals(params.getInt("o")) )
                return i;
        return -1;
    }

    private ISFSObject  getMyRequestedBattle(ISFSObject params, Player player)
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
    private ISFSObject getAvailableDonation(ISFSObject params)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        for (int i = msgSize-1; i >=0; i--)
            if (messages.getSFSObject(i).getShort("m") == MessageTypes.M20_DONATE && params.getShort("st") == 0 && messages.getSFSObject(i).getShort("st") == 0 && messages.getSFSObject(i).getInt("did").equals(params.getInt("did")))
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
        ISFSObject msg = new SFSObject();
        msg.putUtfString("t", "");
        msg.putShort("m", mode);
        msg.putText("s", subject);
        msg.putText("o", object);
        msg.putShort("p", permissionId);
        messageQueue().addSFSObject(msg);
        super.handleClientRequest(Commands.LOBBY_PUBLIC_MESSAGE, null, msg);
    }

    private ISFSArray messageQueue ()
    {
        return lobby.getVariable("msg").getSFSArrayValue();
    }

    private int getActiveness ()
    {
        return lobby.getVariable("act").getIntValue();
    }


    private boolean replyRequest(Game game, ISFSObject params)
    {
        boolean accepted = params.getShort("pr") == MessageTypes.M16_COMMENT_JOIN_ACCEPT;
        if ( accepted )
        {
            if ( !LobbyUtils.getInstance().addUser(lobby, params.getInt("o")) )
                return false;

            // join online users
            User targetUser = getParentZone().getUserByName(params.getInt("o").toString());
            if ( targetUser != null )
            {
                try {
                    getApi().joinRoom(targetUser, lobby);
                } catch (SFSJoinRoomException e) { e.printStackTrace(); }
            }
        }

        String msg = "درخواست عضویتت در دهکده " + lobby.getName() + (accepted ? " پذیرفته شد. " : " رد شد. ");
        InboxUtils.getInstance().send(accepted?MessageTypes.M50_URL:MessageTypes.M0_TEXT, msg, game.player.nickName, game.player.id, params.getInt("o"), "towers://open?controls=screen&type=socialScreen");
        sendComment(params.getShort("pr"), game.player.nickName, params.getText("on"), (short)-1);// mode = join
        return true;
    }
}