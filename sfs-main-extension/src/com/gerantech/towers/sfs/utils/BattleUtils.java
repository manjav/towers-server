package com.gerantech.towers.sfs.utils;

import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.arenas.Arena;
import com.hazelcast.util.RandomPicker;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.variables.SFSUserVariable;
import com.smartfoxserver.v2.entities.variables.UserVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ManJav on 9/23/2017.
 */
public class BattleUtils
{
    private final SFSExtension ext;
    private static AtomicInteger roomId = new AtomicInteger();
    public static int arenaDivider = 5;

    public BattleUtils()
    {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }

    public static BattleUtils getInstance()
    {
        return new BattleUtils();
    }


    public void join(User user, Room theRoom, String spectatedUser)
    {
        Player player = ((Game)user.getSession().getProperty("core")).player;
        ext.trace("---------=========<<<<  JOIN user:"+user.getName()+" theRoom:"+theRoom.getName()+" spectatedUser:"+spectatedUser+" >>>>==========---------");
        List<UserVariable> vars = new ArrayList();
        vars.add(new SFSUserVariable("name", player.nickName));
        vars.add(new SFSUserVariable("point", player.get_point()));
        vars.add(new SFSUserVariable("spectatedUser", spectatedUser));
        ext.getApi().setUserVariables(user, vars, true, true);

        try
        {
            ext.getApi().joinRoom(user, theRoom, null, spectatedUser!="", null);
        }
        catch (SFSJoinRoomException e)
        {
            e.printStackTrace();
        }
    }

    public Room make(User owner, boolean isQuest, int index, int friendlyMode, boolean hasExtraTime)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.battle.BattleRoom");

        Game game = ((Game)owner.getSession().getProperty("core"));
        ext.trace("---------=========<<<<  MAKE owner:", owner.getName(), "index:", index, "isQuest:", isQuest, "friendlyMode:", friendlyMode, " >>>>==========---------");

        Map<Object, Object> roomProperties = new HashMap<>();

        if( !isQuest )
        {
            Arena arena = game.arenas.get(game.player.get_arena(game.player.get_point()));
            List<String> fields = game.fieldProvider.battles.getKeyRange(arena.index * 100, (arena.index + 1) * 100);
            index = game.fieldProvider.battles.get(fields.get(RandomPicker.getInt(0, fields.size()))).index;
            Double arenaIndex =  Math.min(BattleUtils.arenaDivider, Math.floor(arena.index/2)*2);
            roomProperties.put("arena", arenaIndex.intValue());// ===> is temp
            roomProperties.put("appVersion", game.appVersion);// ===> is temp
        }

        roomProperties.put("isQuest", isQuest);
        roomProperties.put("index", index);
        if( hasExtraTime )
            roomProperties.put("hasExtraTime", true);
        if( friendlyMode > 0 )
            roomProperties.put("isFriendly", true);

        String pref = isQuest ? "q_" : "b_";
        if( friendlyMode > 0 )
            pref = friendlyMode == 1 ? "fl_" : "fb_";

        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setMaxSpectators(10);
        rs.setDynamic(true);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
        rs.setRoomProperties( roomProperties );
        rs.setName( pref + index+ "__" + roomId.getAndIncrement() );
        rs.setMaxUsers(isQuest?1:2);
        rs.setGroupId(isQuest?"quests":"battles");
        rs.setExtension(res);

        try {
            return ext.getApi().createRoom(ext.getParentZone(), rs, owner);
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        }
        return null;
    }


}