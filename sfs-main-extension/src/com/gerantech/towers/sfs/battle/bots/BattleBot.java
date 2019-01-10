package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.fieldes.FieldData;
import com.gt.towers.battle.units.Unit;
import com.gt.towers.constants.CardTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.StickerType;
import com.gt.towers.scripts.ScriptEngine;
import com.gt.towers.utils.CoreUtils;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

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
    double lastHelpTime = 0;
    int battleRatio = 0;
    SFSObject chatPatams;

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
        if( battleField.state < BattleField.STATE_2_STARTED && (battleField.games.get(0).player.get_battleswins() < 3 || Math.random() < 0.3) )
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
        if( lastHelpTime == 0 )
            lastHelpTime = battleField.now + SUMMON_DELAY * 2;
        if( lastSummonTime > battleField.now )
            return;
        Unit playerHeader = null, botHeader = null;
        double x = BattleField.WIDTH * Math.random();
        for( Map.Entry<Object, Unit> entry : battleField.units._map.entrySet() )
        {
            if( !CardTypes.isTroop((Integer) entry.getKey()) )
                continue;
            if( entry.getValue().side == 0 )
            {
                // top of player troops
                if( playerHeader == null || playerHeader.y > entry.getValue().y )
                    playerHeader = entry.getValue();
            }
            else
            {
                // bottom of bot troops
                if( botHeader == null || botHeader.y < entry.getValue().y )
                    botHeader = entry.getValue();
            }
        }

        double y = Math.random() * (BattleField.HEIGHT * 0.3);

        int cardType;
        if( playerHeader == null )
        {
            cardType = battleField.decks.get(1).queue_get(0);
            if( CardTypes.isSpell(cardType) && battleField.field.type == FieldData.TYPE_TOUCHDOWN )
                return;

            if( cardType == 109 )
                return;

            if( battleField.elixirBar.get(1) < CoreUtils.clamp(battleField.difficulty * 0.7, 4, 9.5) )// waiting for more elixir to create waves
                return;
        }
        else
        {
            int cardIndex = getCandidateCardIndex(playerHeader.card.type);

            double random = (Math.random() > 0.5 ? 33 : -33) * Math.random();
            x = Math.max(BattleField.PADDING, Math.min(BattleField.WIDTH - BattleField.PADDING, playerHeader.x + random));
           // ext.trace("playerHeader:"+ playerHeader.card.type, "x:"+ x, "y:"+ y, "e:"+ battleField.elixirBar.get(1), "ratio:" + battleRoom.endCalculator.ratio());
            cardType = battleField.decks.get(1).queue_get(cardIndex);

            if( CardTypes.isSpell(cardType) || playerHeader.y < BattleField.HEIGHT * 0.4 )// drop spell
                y = playerHeader.y - (CardTypes.isTroop(playerHeader.card.type) ? 200 : 0);
            else if( cardType == 109 && botHeader != null )//summon healer for covering
                y = botHeader.y - 300;

            // fake stronger bot
            if( battleField.games.get(0).player.get_battleswins() > 4 && lastHelpTime < battleField.now && !CardTypes.isSpell(cardType) && playerHeader.y < BattleField.HEIGHT * 0.3 )
            {
                ext.trace("help:", battleField.elixirBar.get(1), battleField.difficulty * 0.3);
                battleField.elixirBar.set(1, battleField.elixirBar.get(1) + battleField.difficulty * 0.3 );
                lastHelpTime = battleField.now + SUMMON_DELAY * 2;
            }
        }

        // when battlefield is empty
        if( botHeader == null && cardType == 109 )// skip spells and healer
            return;

        int id = battleRoom.summonUnit(1, cardType, x, y);
        if( id >= 0 )
        {
            //ext.trace("summonCard  type:", cardType, "id:", id, lastCardIndexUsed, battleField.games.get(0).player.cards.exists(cardType), xPosition );
            lastSummonTime = battleField.now + SUMMON_DELAY;
            return;
        }

        // fake stronger bot
        if( battleField.games.get(0).player.get_battleswins() > 3 )
            battleField.elixirSpeeds.set(1, battleRoom.endCalculator.ratio() > 1 ? 1 + battleField.difficulty * 0.04 : 1);
    }

    int getCandidateCardIndex(int type)
    {
        haxe.root.Array candidates = (haxe.root.Array) ScriptEngine.get(-3, type, 0);
        int len = candidates.length;
        for (int i = 0; i < len; i++)
        {
            int index = battleField.decks.get(1).queue_indexOf((int) candidates.__get(i));
            //ext.trace("queue_indexOf", i, candidates.__get(i), index);
            if( index > 0 && index < 4 )
                return  index;
        }
        return 0;
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