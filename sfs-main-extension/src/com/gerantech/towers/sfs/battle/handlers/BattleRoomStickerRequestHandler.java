package com.gerantech.towers.sfs.battle.handlers;

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
        trace(params.getDump());
        for (User u : getParentExtension().getParentRoom().getPlayersList())
            if (!u.isNpc() && u.getId()!=sender.getId())
                send("ss", params, u);
    }
}