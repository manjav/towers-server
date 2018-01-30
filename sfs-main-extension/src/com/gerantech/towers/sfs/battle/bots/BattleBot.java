package com.gerantech.towers.sfs.battle.bots;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Place;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.TroopType;
import com.gt.towers.utils.PathFinder;
import com.gt.towers.utils.lists.IntList;
import com.gt.towers.utils.lists.PlaceList;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.*;

/**
 * Created by ManJav on 1/25/2018.
 */
public class BattleBot {

    public int dangerousPoint = -1;

    protected float sourcesPowers;
    protected float targetHealth;

    protected final SFSExtension extension;
    protected final BattleRoom battleRoom;
    protected final BattleField battleField;

    private int sampleTime;
    private int timeFactor;
    private PlaceList allPlaces;
    private Place robotCastle;
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
        PlaceList castles = battleField.getPlacesByMode(2, TroopType.T_0);
        robotCastle = castles.size() > 0 ? battleField.getPlacesByMode(2, TroopType.T_1).get(0) : null;
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
        int placeTargetIndex = -1;
        while (step >= 0) {
            if (allPlaces.get(step).building.troopType != TroopType.T_1 && isNeighbor(allPlaces.get(step))) {
                if (placeTargetIndex == -1 || piorarity(allPlaces.get(step)) < piorarity(allPlaces.get(placeTargetIndex)))
                    placeTargetIndex = step;
            }
            step--;
        }
        if (placeTargetIndex > -1)
            fightToPlace(allPlaces.get(placeTargetIndex));
    }

    void fightToPlace(Place target)
    {
        sourcesPowers = 0;
        targetHealth = estimateHealth(target);
        IntList fightersCandidates = new IntList();

        addFighters(target, fightersCandidates);

        extension.trace("target: " + target.index, "size: " + fighters.size(), " sourcesPowers: " + sourcesPowers, " targetHealth: " + targetHealth);
        if (fighters.size() > 0 && (sourcesPowers >= targetHealth || battleField.getPlacesByTroopType(TroopType.T_1, true).size() <= 1))
            scheduleFighters(target);
        else
            fighters.clear();
    }

    void addFighters(Place place, IntList fightersCandidates)
    {
        if (fightersCandidates.indexOf(place.index) > -1 || sourcesPowers >= targetHealth * 1.1)
            return;
        fightersCandidates.push(place.index);

        if (place.building.troopType == TroopType.T_1 && !fighters.containsKey(place.index))
        {
            Place card = battleField.deckBuildings.get(4);
            double placePower = place.building.getPower();
            double cardPower = estimateCardPower(card);
            extension.trace(place.index, "placePower", placePower, "cardPower", cardPower, sourcesPowers);

            if( placePower >= cardPower )
            {
                sourcesPowers += placePower;
                fighters.put(place.index, new ScheduledPlace(place));
            }
            else if( place.building.transform(card.building) )
            {
                sourcesPowers += cardPower;
                fighters.put(place.index, new ScheduledPlace(place));
                extension.trace("fsdfsdfs");
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
        for (Map.Entry<Integer, ScheduledPlace> entry : fightersEntry) {
            ScheduledPlace sPlace = entry.getValue();
            sPlace.fightTime = PathFinder.getDistance(sPlace.place, target) * sPlace.place.building.troopSpeed;
            if (maxDelay < sPlace.fightTime)
                maxDelay = sPlace.fightTime;
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
            }, (long) (maxDelay - sPlace.fightTime + sPlace.place.building.deployTime));
        }
    }

    // tools
    boolean isNeighbor(Place place) {
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        return placeLinks.size() > 0;
    }

    double estimateCardPower(Place place) {
        return place.building.troopsCount * place.building.troopPower;
    }

    float estimateHealth(Place place) {
        return (place.building.get_population() + place.building.get_health());
    }

    float piorarity(Place place) {
        if (robotCastle != null && place.getLinks(TroopType.T_1).indexOf(robotCastle) > -1)
            return 0.1f;
        return estimateHealth(place) / (place.mode + 1);
    }
}