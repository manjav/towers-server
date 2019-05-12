package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.handlers.LoginEventHandler;
import com.gt.BBGRoom;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.battle.BattleField;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.socials.Challenge;
import com.gt.utils.BattleUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            BBGRoom room = BattleUtils.getInstance().rooms.get(this.index);
            if( room != null )
                BattleUtils.getInstance().join(room, sender, params.getText("spectatedUser"));
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
        BBGRoom room;
        BattleUtils bu = BattleUtils.getInstance();
        int joinedRoomId = (Integer) user.getSession().getProperty("joinedRoomId");
        if( joinedRoomId > -1 )
            room = bu.rooms.get(joinedRoomId);
        else
            room = findWaitingBattleRoom(user);

        if( room == null )
            room = bu.make((Class) getParentExtension().getParentZone().getProperty("battleClass"), user, mode, type, friendlyMode);

        bu.join(room, user, "");
    }

    private BBGRoom findWaitingBattleRoom(User user)
    {
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        ConcurrentHashMap<Integer, BBGRoom> battles = BattleUtils.getInstance().rooms;
        trace("alive battles " + battles.size());
        BBGRoom room = null;
        for(Map.Entry<Integer, BBGRoom> entry : battles.entrySet())
        {
            room = entry.getValue();
            trace(room.toString());
            if( room.isFull() )
                continue;
            if( room.getPropertyAsInt("friendlyMode") > 0 )
                continue;
            if( room.getPropertyAsInt("league") != league )
                continue;
            if( room.getPropertyAsInt("state") != BattleField.STATE_0_WAITING )
                continue;
            if( room.getPropertyAsInt("mode") != mode )
                continue;
            return room;
        }
        return null;
    }
}