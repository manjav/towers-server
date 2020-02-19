package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 7/28/2017.
 */
public class BattleRoomStickerRequestHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
try {
        BattleRoom battleRoom = (BattleRoom) getParentExtension().getParentRoom().getExtension();
        battleRoom.sendSticker(sender, params);
} catch (Exception | Error e) { e.printStackTrace(); }
    }
}