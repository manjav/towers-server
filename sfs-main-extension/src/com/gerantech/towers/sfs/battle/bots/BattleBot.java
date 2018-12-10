package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.units.Unit;
import com.gt.towers.constants.CardTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.StickerType;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by ManJav on 9/30/2018.
 */
public class BattleBot
{
    static final int SUMMON_DELAY = 3000;
    SFSExtension ext;
    BattleRoom battleRoom;
    BattleField battleField;
    double lastSummonTime = 0;
    int battleRatio = 0;
    SFSObject chatPatams;
    private int lastCardIndexUsed = 0;

    public BattleBot(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();

        chatPatams = new SFSObject();
        chatPatams.putDouble("ready", battleField.now + 15000);

        ext.trace("p-point:" + battleField.games.get(0).player.resources.get(ResourceType.R2_POINT), "b-point:"+ battleField.games.get(1).player.resources.get(ResourceType.R2_POINT), " winRate:" + battleField.games.get(0).player.resources.get(ResourceType.R16_WIN_RATE), "difficulty:" + battleField.difficulty);
    }

    public void update()
    {
        if( battleField.state < BattleField.STATE_2_STARTED && Math.random() < 0.3 )
            return;
        summonCard();
        updateChatProcess();
    }

    void summonCard()
    {
        if( battleField.games.get(0).player.get_battleswins() < 1 )
            return;

        if( lastSummonTime == 0 )
            lastSummonTime = battleField.now + SUMMON_DELAY;
        if( lastSummonTime > battleField.now )
            return;
        lastSummonTime = battleField.now + SUMMON_DELAY;
        int cardType = battleField.decks.get(1).get(lastCardIndexUsed);
        if( CardTypes.isSpell(cardType) )
        {
            lastCardIndexUsed = lastCardIndexUsed == 7 ? 0 : lastCardIndexUsed + 1;
            return;
        }

        Iterator<Map.Entry<Object, Unit>> iterator = battleField.units._map.entrySet().iterator();
        Unit unit, pioneer = null;
        double x = BattleField.WIDTH * Math.random(), y = 0
                ;
        while( iterator.hasNext() )
        {
            unit = iterator.next().getValue();
            if( unit.side != 0 )
                continue;
            if( y > unit.y )
            {
                pioneer = unit;
                y = pioneer.y;
            }
        }

        double random = (Math.random() > 0.5 ? 1 : -1) * (Math.random() * BattleField.PADDING);
        if( pioneer != null )
            x = pioneer.x + random;
        y = Math.random() * (BattleField.HEIGHT * 0.3);
        int id = battleRoom.summonUnit(1, cardType, Math.max(BattleField.PADDING, Math.min(BattleField.WIDTH - BattleField.PADDING, x)), y);
        if( id >= 0 )
        {
            //ext.trace("summonCard  type:", cardType, "id:", id, lastCardIndexUsed, battleField.games.get(0).player.cards.exists(cardType), xPosition );
            lastCardIndexUsed = lastCardIndexUsed == 7 ? 0 : lastCardIndexUsed + 1;
        }
    }

    void chatStarting(int battleRatio)
    {
        if( battleField.field.isOperation() || battleField.games.get(0).player.inTutorial() )
            return;

        // verbose bot threshold
        if( chatPatams.getDouble("ready") > battleField.now || Math.random() > 0.1 )
            return;

        //ext.trace(this.battleRatio, battleRatio);
        if( battleRatio != this.battleRatio )
        {
            chatPatams.putInt("t", StickerType.getRandomStart(battleRatio, battleField.games.get(0)));
            chatPatams.putInt("tt", 1);
            chatPatams.putDouble("ready", battleField.now + Math.random() * 2500 + 500);
        }
        this.battleRatio = battleRatio;
    }

    public void chatAnswering(ISFSObject params)
    {
        if( chatPatams.getDouble("ready") > battleField.now || Math.random() < 0.4 )
            return;

        int answer = StickerType.getAnswer( params.getInt("t") );
        if( answer <= -1 )
            return;

        chatPatams.putInt("t", answer);
        chatPatams.putInt("tt", 1);
        chatPatams.putInt("wait", 0);
        chatPatams.putDouble("ready", battleField.now + Math.random() * 2500 + 2500);
    }

    void updateChatProcess()
    {
        if( chatPatams.getDouble("ready") > battleField.now || !chatPatams.containsKey("t") )
            return;

        battleRoom.sendSticker(null, chatPatams);
        chatPatams.removeElement("t");
        chatPatams.putDouble("ready", battleField.now + 10000);
    }
}