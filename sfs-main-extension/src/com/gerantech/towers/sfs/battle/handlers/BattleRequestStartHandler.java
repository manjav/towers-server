package com.gerantech.towers.sfs.battle.handlers;
import com.gerantech.towers.sfs.handlers.LoginEventHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.battle.BattleField;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.socials.Challenge;
import com.gt.utils.BattleUtils;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.util.List;

public class BattleRequestStartHandler extends BaseClientRequestHandler
{
    private int type;
    private int mode;
    private int index;
    private int league;
    private int friendlyMode;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        int now = (int)Instant.now().getEpochSecond();
        if( now < LoginEventHandler.UNTIL_MAINTENANCE )
        {
            params.putInt("umt", LoginEventHandler.UNTIL_MAINTENANCE - now);
            send(Commands.BATTLE_START, params, sender);
            return;
        }

        this.index = params.getInt("index");
        if( params.containsKey("spectatedUser") )
        {
            Room room = getParentExtension().getParentZone().getRoomById(index);
            if( room != null )
                BattleUtils.getInstance().join(sender, room, params.getText("spectatedUser"));
            return;
        }

        Game game = (Game)sender.getSession().getProperty("core");
        this.friendlyMode = params.containsKey("friendlyMode") ? params.getInt("friendlyMode") : 0;
        this.league = game.player.get_arena(0);
        this.type = Challenge.getType(index);
        this.mode = Challenge.getMode(index);

        if( !game.player.has(Challenge.getRunRequiements(index)) )
        {
            params.putInt("response", MessageTypes.RESPONSE_NOT_ENOUGH_REQS);
            send(Commands.BATTLE_START, params, sender);
            return;
        }
        this.joinUser(sender);
    }
 
	private void joinUser(User user)
    {
        Room room = null;
        int joinedRoomId = (Integer) user.getSession().getProperty("joinedRoomId");
        if( joinedRoomId > -1 )
            room = getParentExtension().getParentZone().getRoomById(joinedRoomId);
        else
            room = findWaitingBattleRoom(user);

        BattleUtils bu = BattleUtils.getInstance();
        if( room == null )
            room = bu.make(user, mode, type, friendlyMode);

        bu.join(user, room, "");
    }

    private Room findWaitingBattleRoom(User user)
    {
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("battles");
        Room room = null;
        try
        {
            for(int r=0; r<rList.size(); r++)
            {
                room = rList.get(r);
                if( room.isFull() )
                    continue;
                if( ((int)room.getProperty("friendlyMode")) > 0 )
                    continue;
                if( (int) room.getProperty("league") != league )
                    continue;
                if( (int) room.getProperty("state") == BattleField.STATE_0_WAITING )
                    continue;
                if( !room.containsProperty("mode") || (int) room.getProperty("mode") != mode )
                    continue;
                return room;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            if( room != null )
                trace("friendlyMode:" + room.getProperty("friendlyMode"),  "state:" + room.getProperty("state"), "league:" + room.getProperty("league"));
        }
        return null;
    }
}