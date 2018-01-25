package com.gerantech.towers.sfs.battle.bots;

import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Place;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.TroopType;
import com.gt.towers.utils.lists.IntList;
import com.gt.towers.utils.lists.PlaceList;

/**
 * Created by ManJav on 1/25/2018.
 */
public class BattleBot extends Bot
{
    float accessPoint;
    PlaceList allPlaces;
    Place robotCastle;

    public BattleBot(BattleField battleField)
    {
        super(battleField);
        accessPoint = (float)Math.floor(battleField.startAt % 3);
        extension.trace("winStreak: " + battleField.places.get(0).game.player.resources.get(ResourceType.WIN_STREAK) + " difficulty " + battleField.difficulty);

        allPlaces = battleField.getPlacesByTroopType(TroopType.NONE, true);
        PlaceList castles = battleField.getPlacesByMode(2, TroopType.T_0);
        robotCastle = castles.size() > 0 ? battleField.getPlacesByMode(2, TroopType.T_1).get(0) : null;
    }

    @Override
    public BotActions doAction()
    {
        int seed = (int)Math.floor(battleField.now % 5);
        if ( seed == accessPoint && ( action.equals(BotActions.NO_CHANCE) || action.equals(BotActions.INIT) ) )
            return action = tryAction();

        /*if ( !battleField.map.isQuest && Math.random() < 0.002 && !stickerStarted )
        {
            stickerStarted = true;
            return action = BotActions.START_STICKER;
        }*/
        return action = BotActions.NO_CHANCE;
    }

    BotActions tryAction()
    {
        BotActions ret = BotActions.NO_CHANCE;

        /**
         * transform for defence main places
         */
        if ( dangerousPoint > -1 )
        {
            Place dangerousPlace = battleField.getPlaceByIndex(dangerousPoint);
            if( dangerousPlace.mode > 0 )
                dangerousPlace.building.transform(battleField.deckBuildings.get(4).building);
            dangerousPoint = -1;
        }

        /**
         * transform and fight troops to empty places
         */
        int step = allPlaces.size() - 1;
        int placeTargetIndex = -1;
        while ( step >= 0 )
        {
            if ( allPlaces.get(step).building.troopType != TroopType.T_1 && isNeighbor(allPlaces.get(step)) )
            {
                if( placeTargetIndex == -1 || estimateHealth(allPlaces.get(step)) < estimateHealth(allPlaces.get(placeTargetIndex)) )
                    placeTargetIndex = step;
            }
            step --;
        }
        //extension.trace("placeTargetIndex", placeTargetIndex, allPlaces.get(placeTargetIndex).index);
        if ( placeTargetIndex > -1 )
            ret = fightToPlace(allPlaces.get(placeTargetIndex));

        return ret;
    }

    BotActions fightToPlace(Place target)
    {
        sourcesPowers = 0;
        sources = new IntList();
        IntList fightersList = new IntList();
        IntList transformersList = new IntList();
        boolean transformed = false;
        float estimaestimatedTargetHealth = estimateHealth(target);

        addFighters(target, fightersList, estimaestimatedTargetHealth);
        if( addTransformers(target, transformersList) )
            transformed = true;

        //extension.trace("target: , " + target.index , " sourcesPowers: " + sourcesPowers , "  estimaestimatedTargetHealth: " + estimaestimatedTargetHealth);
        if ( sources.size() > 0 && ( sourcesPowers >= estimaestimatedTargetHealth || battleField.getPlacesByTroopType(TroopType.T_1, true).size() <= 1 ) )
        {
            this.target = target.index;
            return BotActions.FIGHT;
        }

        if ( transformed )
            return BotActions.TRANSFORM;

        return BotActions.NO_CHANCE;
    }

    void addFighters(Place place, IntList fightersList, float estimaestimatedTargetHealth)
    {
        if ( fightersList.indexOf(place.index) > -1 || sourcesPowers >= estimaestimatedTargetHealth / 0.6 )
            return;

        fightersList.push(place.index);
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        int step = placeLinks.size() - 1;
        while ( step >= 0 )
        {
            if( placeLinks.get(step).hasTroop() )
            {
                sourcesPowers += estimateTroopsPower(placeLinks.get(step));
                sources.push(placeLinks.get(step).index);
            }
            addFighters(placeLinks.get(step), fightersList, estimaestimatedTargetHealth);
            step --;
        }
    }

    boolean addTransformers( Place place, IntList transformersList)
    {
        if ( transformersList.indexOf(place.index) > -1 )
            return false;

        boolean ret = false;
        if( place.building.get_population() < battleField.deckBuildings.get(5).building.troopsCount )
            ret = place.building.transform(battleField.deckBuildings.get(5).building) || ret;

        transformersList.push(place.index);

        // transform neighbors
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        int step = placeLinks.size() - 1;
        while ( step >= 0 )
        {
            ret = addTransformers(placeLinks.get(step), transformersList) || ret;
            step --;
        }
        return ret;
    }

    // tools
    boolean isNeighbor(Place place)
    {
        PlaceList placeLinks = place.getLinks(TroopType.T_1);
        return placeLinks.size() > 0;
    }
    double estimateTroopsPower(Place place)
    {
        return place.building.get_population() * place.building.troopPower;
    }
    float estimateHealth(Place place)
    {
        if ( robotCastle != null && place.getLinks(TroopType.T_1).indexOf(robotCastle) > -1 )
            return 0.1f;
        return (place.building.get_population() + place.building.get_health()) / (place.mode + 1) ;
    }
}