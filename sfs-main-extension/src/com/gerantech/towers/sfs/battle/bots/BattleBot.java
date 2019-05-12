package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Player;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.GameObject;
import com.gt.towers.battle.units.Unit;
import com.gt.towers.constants.CardTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.StickerType;
import com.gt.towers.scripts.ScriptEngine;
import com.gt.towers.socials.Challenge;
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
    static final private int SUMMON_DELAY = 3000;
    static final private boolean DEBUG_MODE = false;
    private Player player;
    private SFSExtension ext;
    private BattleRoom battleRoom;
    private BattleField battleField;
    private double lastSummonTime = 0;
    private double lastHelpTime = 0;
    private float battleRatio = 0;
    private SFSObject chatParams;
    private int defaultIndex = 0;

    public BattleBot(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();

        chatParams = new SFSObject();
        chatParams.putDouble("ready", battleField.now + 15000);

        player = battleField.games.__get(0).player;
        trace("p-point:" + player.getResource(ResourceType.R2_POINT), "b-point:"+ battleField.games.__get(1).player.getResource(ResourceType.R2_POINT), " winRate:" + player.getResource(ResourceType.R16_WIN_RATE), "difficulty:" + battleField.difficulty);
    }

    public void reset()
    {
        lastSummonTime = 0;
    }

    public void update()
    {
        if( battleField.state < BattleField.STATE_2_STARTED && (player.get_battleswins() < 3 || Math.random() < 0.3) )
            return;
        summonCard();
        updateChatProcess();
    }

    private void summonCard()
    {
        if( player.get_battleswins() < 1 )
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
            if( !CardTypes.isTroop((Integer) entry.getKey()) || entry.getValue().state < GameObject.STATE_2_MORTAL )
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
                if( botHeader == null || botHeader.y < entry.getValue().y || botHeader.health > entry.getValue().health )
                    botHeader = entry.getValue();
            }
        }

        double y = Math.random() * (BattleField.HEIGHT * 0.3);

        int cardType;
        if( playerHeader == null )
        {
            cardType = battleField.decks.get(1).queue_get(defaultIndex);
            if( CardTypes.isSpell(cardType) && battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
            {
                skipCard(cardType);
                return;
            }

            if( cardType == 109 )
            {
                skipCard(cardType);
                return;
            }
            if( (double)battleField.elixirUpdater.bars.__get(1) < CoreUtils.clamp(battleField.difficulty * 0.7, 4, 9.5) )// waiting for more elixir to create waves
            {
                trace("wait for", battleField.elixirUpdater.bars.__get(1), CoreUtils.clamp(battleField.difficulty * 0.7, 4, 9.5), battleField.difficulty);
                return;
            }
        }
        else
        {
            int cardIndex = getCandidateCardIndex(playerHeader.card.type);

            double random = (Math.random() > 0.5 ? 33 : -33) * Math.random();
            x = Math.max(BattleField.PADDING, Math.min(BattleField.WIDTH - BattleField.PADDING, playerHeader.x + random));
           // trace("playerHeader:"+ playerHeader.card.type, "x:"+ x, "y:"+ y, "e:"+ battleField.elixirBar.get(1), "ratio:" + battleRoom.endCalculator.ratio());
            cardType = battleField.decks.get(1).queue_get(cardIndex);

            if( CardTypes.isSpell(cardType) || playerHeader.y < BattleField.HEIGHT * 0.4 )// drop spell
            {
                if( CardTypes.isSpell(cardType) )
                    trace("isSpell", cardType);
                y = playerHeader.y - (CardTypes.isTroop(playerHeader.card.type) && playerHeader.state == GameObject.STATE_4_MOVING ? 200 : 0);
            }
            else if( cardType == 109 )
            {
                if( botHeader == null )
                {
                    skipCard(cardType);
                    return;
                }
                y = botHeader.y - 300;//summon healer for covering
            }

            // fake stronger bot
            if( player.get_battleswins() > 4 && lastHelpTime < battleField.now && !CardTypes.isSpell(cardType) && playerHeader.y < BattleField.HEIGHT * 0.3 )
            {
                trace("help:", battleField.elixirUpdater.bars.__get(1), battleField.difficulty * 0.3);
                battleField.elixirUpdater.bars.__set(1, (double)battleField.elixirUpdater.bars.__get(1) + battleField.difficulty * 0.3 );
                lastHelpTime = battleField.now + SUMMON_DELAY * 2;
            }
        }

        // when battlefield is empty
        if( botHeader == null && cardType == 109 )// skip spells and healer
            return;

        if( defaultIndex  != 0 )
            defaultIndex = 0;

        int id = battleRoom.summonUnit(1, cardType, x, y);
        if( id >= 0 )
        {
            //trace("summonCard  type:", cardType, "id:", id, lastCardIndexUsed, player.cards.exists(cardType), xPosition );
            lastSummonTime = battleField.now + SUMMON_DELAY;
            return;
        }
//
//        // fake stronger bot
//        if( player.get_battleswins() > 3 )
//            battleField.elixirSpeeds.__set(1, battleRoom.endCalculator.ratio() > 1 ? 1 + battleField.difficulty * 0.04 : 1);
    }

    private void skipCard(int cardType)
    {
        defaultIndex ++;
        if( defaultIndex >= battleField.decks.get(1).keys().length )
            defaultIndex = 0;
        trace("skipCard", cardType, "index:", defaultIndex);
    }

    private int getCandidateCardIndex(int type)
    {
        haxe.root.Array candidates = (haxe.root.Array) ScriptEngine.get(-3, type, 0);
        int len = candidates.length;
        for (int i = defaultIndex; i < len; i++)
        {
            int index = battleField.decks.get(1).queue_indexOf((int) candidates.__get(i));
            //trace("queue_indexOf", i, candidates.__get(i), index);
            if( index > 0 && index < 4 )
                return index;
        }
        return 0;
    }

    public void chatStarting(float battleRatio)
    {
        if( battleField.field.isOperation() || player.inTutorial() )
            return;

        // verbose bot threshold
        if( chatParams.getDouble("ready") > battleField.now || Math.random() > 0.1 )
            return;

        //trace(this.battleRatio, battleRatio);
        if( battleRatio != this.battleRatio )
        {
            chatParams.putInt("t", StickerType.getRandomStart(battleRatio, battleField.games.__get(0)));
            chatParams.putInt("tt", 1);
            chatParams.putDouble("ready", battleField.now + Math.random() * 2500 + 1500);
        }
        this.battleRatio = battleRatio;
    }

    public void chatAnswering(ISFSObject params)
    {
        if( chatParams.getDouble("ready") > battleField.now || Math.random() < 0.4 )
            return;

        int answer = StickerType.getAnswer( params.getInt("t") );
        if( answer <= -1 )
            return;

        chatParams.putInt("t", answer);
        chatParams.putInt("tt", 1);
        chatParams.putInt("wait", 0);
        chatParams.putDouble("ready", battleField.now + Math.random() * 2500 + 2500);
    }

    private void updateChatProcess()
    {
        if( chatParams.getDouble("ready") > battleField.now || !chatParams.containsKey("t") )
            return;

        battleRoom.sendSticker(null, chatParams);
        chatParams.removeElement("t");
        chatParams.putDouble("ready", battleField.now + 10000);
    }
    private void trace(Object... args)
    {
        if( DEBUG_MODE )
            trace(args);
    }
}