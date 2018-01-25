package com.gerantech.towers.sfs.battle.bots;

import com.gt.towers.battle.BattleField;
import com.gt.towers.utils.lists.IntList;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.extensions.SFSExtension;

/**
 * Created by ManJav on 1/25/2018.
 */
public class Bot
{
    public BotActions action;
    public IntList sources;
    public float sourcesPowers;
    public int target;

    public int dangerousPoint = -1;
    protected final SFSExtension extension;
    protected final BattleField battleField;

    public Bot(BattleField battleField)
    {
        action =  BotActions.INIT;
        this.battleField = battleField;
        extension = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }

    public BotActions doAction()
    {
        return action;
    }
}

