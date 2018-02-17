package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Place;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.StickerType;
import com.gt.towers.constants.TroopType;
import com.gt.towers.utils.PathFinder;
import com.gt.towers.utils.lists.IntList;
import com.gt.towers.utils.lists.PlaceList;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.*;

/**
 * Created by ManJav on 1/25/2018.
 */
public class BattleBot {

    public ISFSArray offenders;
    public int dangerousPoint = -1;

    protected float sourcesPowers;
    protected float targetHealth;
    protected final SFSExtension extension;
    protected final BattleRoom battleRoom;
    protected final BattleField battleField;

    private int battleRatio;
    private int sampleTime;
    private int timeFactor;
    private double lastStickerTime;
    private PlaceList allPlaces;
    private Timer chatTimer;
    private final Map<Integer, ScheduledPlace> fighters;

    public BattleBot(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        this.battleField = battleRoom.battleField;
        extension = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
        fighters = new HashMap();
        timeFactor = Math.max(1, 8 - battleField.difficulty );
        extension.trace("winStreak: " + battleField.places.get(0).game.player.resources.get(ResourceType.WIN_STREAK) + " difficulty " + battleField.difficulty + " timeFactor " + timeFactor);

        allPlaces = battleField.getPlacesByTroopType(TroopType.NONE, true);
    }

    public void doAction()
    {
        int _sampleTime = (int) Math.floor(battleField.now % timeFactor);

        if( _sampleTime == 0 && _sampleTime != sampleTime )
            tryAction();
 //       else
 //           extension.trace("now", battleField.now, "timeFactor", timeFactor, "_sampleTime", _sampleTime, "sampleTime", sampleTime);

        sampleTime = _sampleTime;
        /*if ( !battleField.map.isQuest && Math.random() < 0.002 && !stickerStarted ) {
            stickerStarted = true;
            return action = BotActions.START_STICKER;
        }*/
    }

    void tryAction()
    {
        /**
         * transform for defence main places
         */
        if( dangerousPoint > -1 )
        {
            Place dangerousPlace = battleField.places.get(dangerousPoint);
            if( dangerousPlace.mode > 0 && dangerousPlace.building.get_population() < battleField.deckBuildings.get(4).building.troopsCount )
                dangerousPlace.building.transform(battleField.deckBuildings.get(4).building);
            dangerousPoint = -1;
        }

        /**
         * transform and fight troops to empty places
         */
        int step = allPlaces.size() - 1;
        int weakestPlace = -1;
        float mostPriority = 1000;
        List <Integer> samePriorities = new ArrayList();
        while (step >= 0)
        {
            if( isNeighbor(allPlaces.get(step)) )
            {
                float pr = priority(allPlaces.get(step));
                if( mostPriority >= pr )
                {
                    if( mostPriority == pr )
                        samePriorities.add(step);
                    else
                        samePriorities.clear();

                    if( samePriorities.size() == 0)
                        samePriorities.add(step);

                    mostPriority = pr;
                }
            }
            step --;
        }
        // random place
        if( samePriorities.size() > 0 )
            weakestPlace = samePriorities.get((int) Math.floor(Math.random() * samePriorities.size()));
        if( weakestPlace > -1 )
            fightToPlace(allPlaces.get(weakestPlace));
    }

    void fightToPlace(Place target)
    {
        sourcesPowers = 0;
        targetHealth = estimateHealth(target);
        IntList fightersCandidates = new IntList();

        addFighters(target, fightersCandidates);

        extension.trace("target: " + target.index, "size: " + fighters.size(), " sourcesPowers: " + sourcesPowers, " targetHealth: " + targetHealth);
        if( fighters.size() > 0 && (sourcesPowers >= targetHealth || battleField.getPlacesByTroopType(TroopType.T_1, true).size() <= 1) )
            scheduleFighters(target);
        else
            fighters.clear();
    }

    void addFighters(Place place, IntList fightersCandidates)
    {
        if( fightersCandidates.indexOf(place.index) > -1 || sourcesPowers >= targetHealth * 1.1 )
            return;
        fightersCandidates.push(place.index);

        if (place.building.troopType == TroopType.T_1 && !fighters.containsKey(place.index))
        {
            Place card = battleField.deckBuildings.get(4);
            double placePower = place.building.getPower();
            double cardPower = estimateCardPower(card);
            //extension.trace(place.index, "placePower", placePower, "cardPower", cardPower, sourcesPowers);

            if( placePower >= cardPower )
            {
                sourcesPowers += placePower;
                fighters.put(place.index, new ScheduledPlace(place));
            }
            else if( place.building.transform(card.building) )
            {
                sourcesPowers += cardPower;
                fighters.put(place.index, new ScheduledPlace(place));
            }
        }

        // transform neighbors
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        int step = placeLinks.size() - 1;
        while (step >= 0)
        {
            addFighters(placeLinks.get(step), fightersCandidates);
            step --;
        }
    }

    void scheduleFighters(Place target)
    {
        double maxDelay = 0;
        Set<Map.Entry<Integer, ScheduledPlace>> fightersEntry = fighters.entrySet();
        for (Map.Entry<Integer, ScheduledPlace> entry : fightersEntry)
        {
            ScheduledPlace sPlace = entry.getValue();
            sPlace.place.fightTime = (int) (PathFinder.getDistance(sPlace.place, target) * sPlace.place.building.troopSpeed);
            if( maxDelay < sPlace.place.fightTime )
                maxDelay = sPlace.place.fightTime;
        }

        for (Map.Entry<Integer, ScheduledPlace> entry : fightersEntry)
        {
            ScheduledPlace sPlace = entry.getValue();
           // extension.trace("fight", sPlace.place.index + " -> target: " + target.index, "delay:", maxDelay, sPlace.fightTime, maxDelay - sPlace.fightTime + sPlace.place.building.deployTime);
            sPlace.timer = new Timer();
            sPlace.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if( sPlace.place.building.troopType == TroopType.T_1 )
                    {
                        SFSArray _fighters = new SFSArray();
                        _fighters.addInt(sPlace.place.index);
                        battleRoom.fight(_fighters, target.index, true);
                    }
                    sPlace.dispose();
                    fighters.remove(sPlace.place.index);
                    //extension.trace("remove", sPlace.place.index, "fighters.size: " + fighters.size());
                }
            }, (long) (maxDelay - sPlace.place.fightTime + sPlace.place.building.deployTime));
        }
    }



    private void startChating(int battleRatio)
    {
        if( battleField.map.isQuest )
            return;

        //extension.trace(this.battleRatio, battleRatio);
        if( chatTimer == null && battleRatio != this.battleRatio && battleField.now - lastStickerTime > 10 && Math.random() < 0.2 && battleField.now > battleField.startAt + 30 )
        {
            SFSObject stickerParams = new SFSObject();
            stickerParams.putInt("t", StickerType.getRandomStart(battleRatio));
            stickerParams.putInt("tt", 1);
            chatTimer = new Timer();
            chatTimer.schedule(new TimerTask() {
                @Override
                public void run()
                {
                    battleRoom.sendSticker(null, stickerParams);
                    lastStickerTime = battleField.now;
                    chatTimer.cancel();
                    chatTimer = null;
                }
            }, (long) (Math.random() * 1000 + 500));
        }
        this.battleRatio = battleRatio;
    }

    public void answerChat(ISFSObject params)
    {
        if( chatTimer == null && Math.random() > 0.5 )
        {
            int answer = StickerType.getRandomAnswer( params.getInt("t") );
            if( answer > -1 )
            {
                ISFSObject stickerParams = new SFSObject();
                stickerParams.putInt("t", answer);
                stickerParams.putInt("tt", 1);
                stickerParams.putInt("wait", 0);

                chatTimer = new Timer();
                chatTimer.schedule(new TimerTask() {
                    @Override
                    public void run()
                    {
                        battleRoom.sendSticker(null, stickerParams);
                        chatTimer.cancel();
                        chatTimer = null;
                    }
                }, (long) (Math.random() * 2500 + 1500));
            }
        }
    }

    // tools
    boolean isNeighbor(Place place)
    {
        if( place.building.troopType == TroopType.T_1 )
            return false;
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        return placeLinks.size() > 0;
    }

    double estimateCardPower(Place place) {
        return place.building.troopsCount * place.building.troopPower;
    }

    float estimateHealth(Place place) {
        return (place.building.get_population() + place.building.get_health());
    }

    float priority(Place place)
    {
        //if (robotCastle != null && place.getLinks(TroopType.T_1).indexOf(robotCastle) > -1)
        //    return 0.1f;
        if( battleField.now > battleField.getTime(2) && place.mode > 0 )
            return 0.1f;
        return estimateHealth(place) / (place.mode + 1);
    }
}