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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ManJav on 9/30/2018.
 */
public class BattleBot
{
    SFSExtension ext;
    BattleRoom battleRoom;
    BattleField battleField;
   // int sampleTime;
    //int timeFactor;
    int battleRatio = 0;
    SFSObject chatPatams;
    private int lastCardIndexUsed = 0;

    public BattleBot(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
       // timeFactor = Math.min(8, Math.max(2, 10 - battleField.difficulty ) );

        chatPatams = new SFSObject();
        chatPatams.putDouble("ready", battleField.now + 15000);

        ext.trace("p-point:" + battleField.games.get(0).player.resources.get(ResourceType.R2_POINT), "b-point:"+ battleField.games.get(1).player.resources.get(ResourceType.R2_POINT), " winRate:" + battleField.games.get(0).player.resources.get(ResourceType.R16_WIN_RATE), "difficulty:" + battleField.difficulty);
    }

    public void update()
    {
        //int _sampleTime = (int) (battleField.getDuration() % timeFactor);
        //ext.trace("update", battleField.getDuration(), _sampleTime, sampleTime, timeFactor);
        //if(timeFactor == 1 || ( _sampleTime == 0 && _sampleTime != sampleTime ) )
            summonCard();

        //this.battleRatio = battleRoom.endCalculator.scores[0] / battleRoom.endCalculator.scores[1];
       // sampleTime = _sampleTime;

        cover();
        updateChatProcess();
        //updateFightingProcess();
    }

    /**
     * cover for defence main places
     */
    void cover(){
    }

    void summonCard()
    {
        int cardType = battleField.decks.get(1).get(lastCardIndexUsed);
        double xPosition = Math.random() * BattleField.WIDTH;
        double yPosition = 0;

        Map<Integer, Double> healths = new HashMap();
        Map<Integer, Double> ys = new HashMap();
        Iterator<Map.Entry<Object, Unit>> iterator = battleField.units._map.entrySet().iterator();
        Unit unit;
        double health;
        while( iterator.hasNext() )
        {
            unit = iterator.next().getValue();
            health = unit.health * (unit.side == 1 ? 1 : -1);
            int key = (int) (unit.x % 100);
            healths.put(key, (healths.containsKey(key) ? healths.get(key) : 0) + health);
            ys.put(key, unit.y);
        }

        health = 0;
        Map.Entry<Integer, Double> step;
        Iterator<Map.Entry<Integer, Double>> healthsIterator = healths.entrySet().iterator();
        while( healthsIterator.hasNext() )
        {
            step = healthsIterator.next();
            if( health < step.getValue())
            {
                health = step.getValue();
                xPosition = step.getKey() * 100;
                yPosition = BattleField.HEIGHT - ys.get(step.getKey());
            }
        }

        xPosition = BattleField.WIDTH - Math.min(BattleField.WIDTH - BattleField.PADDING, Math.max(BattleField.PADDING, xPosition));
        int id = battleRoom.summonUnit(1, cardType, xPosition,
        BattleField.HEIGHT * 0.66 + Math.random() * (CardTypes.isSpell(cardType) ? yPosition : BattleField.HEIGHT * 0.33) - BattleField.PADDING );
        if( id >= 0 )
        {
            //ext.trace("summonCard  type:", cardType, "id:", id, lastCardIndexUsed, battleField.games.get(0).player.cards.exists(cardType), xPosition );
            lastCardIndexUsed = lastCardIndexUsed == 3 ? 0 : lastCardIndexUsed + 1;
        }
    }

    void chatStarting(int battleRatio)
    {
        if( battleField.map.isOperation() || battleField.games.get(0).player.inTutorial() )
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
        if( chatPatams.getDouble("ready") > battleField.now || Math.random() < 0.2 )
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