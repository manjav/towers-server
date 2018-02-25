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
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;

import java.time.Instant;

/**
 * Created by ManJav on 8/25/2017.
 */
public class LobbyRoom extends BaseLobbyRoom
{
   // @Override
    public void init()
    {
        super.init();
        addEventHandler(SFSEventType.USER_JOIN_ROOM, LobbyRoomServerEventsHandler.class);
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, LobbyRoomServerEventsHandler.class);
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
        LobbyUtils.getInstance().setActivenessVariable (lobby, getActiveness() + 1);
    }

 //   @Override
    protected void organizeMessage(User sender, ISFSObject params, boolean alreadyAdd)
    {

        super.organizeMessage(sender, params, false);
        removeExpiredDonateRequest(); // removes any previous donation message if its time has passed
        if( mode == MessageTypes.M30_FRIENDLY_BATTLE ) {
            trace("\n\t..::BATTLE::..");
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

                    params.putUtfString("o", game.player.nickName);
                    message.putUtfString("o", game.player.nickName);

                    params.putInt("i", message.getInt("i"));
                    params.putUtfString("s", message.getUtfString("s"));

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
            trace("\n\n\t..::DONATION::..", params.getDump());

            // check card request or card donate. state=0 --> Request | state=1 --> Donate
            if ( params.getShort("st") == 0 ) // Request Card
            {
                if ( getMyRequestedDonate(game.player) )
                {
                    trace("There is already a donation request from player:", params.getInt("i"));
                    return;
                }
                else
                {
                    int dueTime = (int) (Instant.now().getEpochSecond() + game.player.DONATION_TIMER);
                    params.putInt("dt", dueTime);
                    trace("DONATION: requester:", params.getInt("i"), "| card type:", params.getShort("ct"));
                }
            }
            else if ( params.getShort("st") == 1 ) // Donate Card
            {
                if ( params.getInt("dt") > (int) Instant.now().getEpochSecond()) {
                    DBUtils db = new DBUtils();
                    Short cardType = params.getShort("ct");

                    if (db.donateCard(cardType, params.getInt("i"), params.getInt("r"))) {
                        db.donorRewards(params.getShort("ct"), params.getInt("i"));
                        trace("DONATION: donorId:", params.getInt("i"), "| requester:", params.getInt("r"), "| card type:", cardType);
                    } else {
                        trace("DONATION ERROR!");
                    }
                }
                else
                {
                    trace("Timer finished cannot donate more!");
                }
                return;
            }
            else if ( params.getShort("st") == 2 )
            {
                trace("Card limit reached");
                return;
            }
            else if ( params.getShort("st") == 3 )
            {
                trace("Timer finished!");
                return;
            }
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

    public void removeExpiredDonateRequest()
    {
        trace("\t..::checkMessageTimer::..");
        int msgSize = messages.size();
        for (int i = msgSize - 1; i > 0 ; i--) {
            ISFSObject message = messages.getSFSObject(i);
            if (message.getShort("m") == MessageTypes.M20_DONATE && message.containsKey("dt")) {
                int remainingDonateTimer = calcRemainingDonateTimer( message.getInt("dt") );
                if (remainingDonateTimer < 0 && message.getShort("st") != 3)
                {
                    message.putShort("st", (short) 3);
                    messages.removeElementAt(i);
                    trace(i, ": canceled & removed element at", i);
                }
                else if (remainingDonateTimer > 0)
                    trace(i, ": due time not reached! remaining time: '", remainingDonateTimer, "' seconds");
                else if (message.getShort("st") == 3)
                {
                    trace(i, ": Already canceled!");
                    messages.removeElementAt(i);
                }
            }
            else
                trace(i, ": messageType:", messages.getSFSObject(i).getShort("m"));
        }
    }
    public int calcRemainingDonateTimer(int dueTime)
    {
        int now = (int) Instant.now().getEpochSecond();
        return dueTime - now;
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
        msg.putUtfString("s", subject);
        msg.putUtfString("o", object);
        msg.putShort("p", permissionId);
        messageQueue().addSFSObject(msg);
        super.handleClientRequest(Commands.LOBBY_PUBLIC_MESSAGE, null, msg);
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
        InboxUtils.getInstance().send(accepted?MessageTypes.M50_URL:MessageTypes.M0_TEXT, msg, game.player.nickName, game.player.id, params.getInt("o"), "towers://open?controls=tabs&dashTab=3&socialTab=0");
        sendComment(params.getShort("pr"), game.player.nickName, params.getUtfString("on"), (short)-1);// mode = join
        return true;
    }

    private Boolean  getMyRequestedDonate(Player player)
    {
        ISFSArray messages = messageQueue();
        int msgSize = messages.size();
        ISFSObject message;
        for (int i = msgSize-1; i >=0; i--)
        {
            message = messages.getSFSObject(i);
            if( message.getShort("m") == MessageTypes.M20_DONATE && message.getShort("st") == 0 && message.getInt("i") == player.id )
                return  true;
        }
        return false;
    }
}