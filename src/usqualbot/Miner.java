package usqualbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

/*
 * WHAT DOES THE MINER DO (in order)
 * update stuff
 * build 1 school when summoned by HQ
 * IF SCHOOL IN RADIUS THEN RUN AWAY!!!!!!!!!!!
 * try deposit soup
 * try mine soup
 * move
 */

/*
 * STATE-MANAGEMENT - think of each bot being in a certain 'state' at any point in time.
 *   Use takeTurn() to define the states, and then use methods to define the behavior in that state.
 *
 * MINER STATE(s) in order of priority/precedence
  0.update stuff (also crawls chain on turnCount==1)
  1.INITIALIZATION (turnCount == 1)
  2.Don't get drowned.
  3.if you can see all tiles in a 5x5 around soup or a refinery:
    A. if it's hq, presence of landscapers also define accessibility.
    B. if it's not accessible, remove it from locations arraylist.
  4.if <= 2 squared away from a landscaper, run away.
  5.if there are no amazons, build one in suitable location.
  6.if there is an amazon and no design school, build a design school in suitable location.
  8.try to mine and deposit in all directions.
  7.if at soup limit:
    A. if there is a refinery:
      a. if it's not too far away, go to it.
      b. if it's too far away and there's more than 50 soup:
        1. build one if you can.
      c. if it's too far away and there's less than 50 soup:
        1. go to it anyway. (don't need miners sitting around, waiting for passive soup income)
    B. if there is no refinery, build it.
  9.if
  *
1/21/2020 I'll finish this later. -jm.

 */

public class Miner extends Unit {

    final int DISTANCESQUARED_BETWEEN_REFINERIES = 30;
    boolean iBroadcastedWaterLoc = false; //Use for only sending 1 water loc per miner(We lose from overspending)
    boolean isLandscaperNearby = false;
    boolean hqRemovedFromRefineryLocations = false;
    // array and not an arraylist!
    MapLocation[] recentlyVisitedLocations = new MapLocation[7];
    // ALL OTHER "...Locations" ArrayList<MapLocation> ARE IN Unit.java!!!!!!!!!!!!!!!!!!!!


    public Miner(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        initializeUpdateAndBroadcast();

        tryCheckIfSoupOrRefineryIsGone();

        // if (near water()) { find higherGround }

        checkForLandscapersNearby();
        if (isLandscaperNearby && myLoc.distanceSquaredTo(hqLoc) < 8) {
            refineryLocations.remove(hqLoc);
            runAwayFromHQ();
        }

        buildAmazonAndSchoolIfAppropriate();

        tryDepositAndMineAllDirections();

        //the following is just for fun :) -jm
        if (rc.getTeamSoup() > ARBITRARY_SOUP_NUMBER_LMAO - 14) {
            buildAVaporatorUpHigh();
        }

        // MOVEMENT
                // if at soup limit, go to nearest refinery
                //      if there is a design school, hq is no longer part of the nearest refineries.
                // if there are soupLocations, go to nearest soup
                // else, move away from other miners

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            // System.out.println("I'm full of soup, refineTime");

            buildRefineryIfAppropriate();

            //find closest refinery (including hq, should change that tho since HQ will become unreachable)
            if (refineryLocations.size() > 0) {
                MapLocation closestRefineryLoc = findClosestRefinery();
                minerGoTo(closestRefineryLoc);
                rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
            } else {
                // if you can't build a refinery, and there are no refineries, it's because you're too close to HQ
                nav.tryMove(myLoc.directionTo(hqLoc).opposite());
            }
        }
        else {
            if (soupLocations.size() > 0) {
                minerGoToNearestSoup();
            } else {
                searchForSoup();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------- //
    // ----------------------------------------------- BEHAVIOR METHODS -------------------------------------------- //

    public void initializeUpdateAndBroadcast() throws GameActionException {
        comms.updateBuildingLocations();
        comms.updateSoupLocations(soupLocations);
        recentlyVisitedLocations[turnCount%7] = myLoc;

        if (turnCount == 1) {
            // System.out.println("adding hq to refineries");
            refineryLocations.add(hqLoc);   // since hq is technically a refinery
        }

        // the following is in the event the chain is spammed or something, idk.
        if (hqLoc == null){
            System.out.println("Hq didnt broadcast its location well");
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
            for (RobotInfo robot : nearbyRobots) {
                if (robot.type.equals(RobotType.HQ)) {
                    hqLoc = robot.location;
                }
            }
        }

        if (!iBroadcastedWaterLoc) {
            for (Direction dir : Util.directions) {
                if (rc.canSenseLocation(myLoc.add(dir))) {
                    if (rc.senseFlooding(myLoc.add(dir))) {
                        comms.broadcastWaterLocation(myLoc.add(dir));
                        iBroadcastedWaterLoc = true;
                    }
                }
            }
        }
    }

    void tryCheckIfSoupOrRefineryIsGone() throws GameActionException {
        if (refineryLocations.size() > 0) {
            checkIfRefineryGone(findClosestRefinery());
        }
        if (soupLocations.size() > 0) {
            checkIfSoupGone(findClosestSoup());
        }
    }

    // basically, goes in the direction of the center of the map
    void checkForLandscapersNearby() throws GameActionException {
        RobotInfo[] nearbyTeammates = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
        if (nearbyTeammates.length > 0) {
            for (RobotInfo bot : nearbyTeammates) {
                if (bot.type.equals(RobotType.LANDSCAPER)) {
                    isLandscaperNearby = true;
                    break;
                } else {
                    isLandscaperNearby = false;
                }
            }
        }
    }

    void runAwayFromHQ() throws GameActionException {
        System.out.println("Run awayyyyyyyy");

        if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top left
            minerGoTo(new MapLocation(myLoc.x + 4, myLoc.y - 4));
        } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top right
            minerGoTo(new MapLocation(myLoc.x - 4, myLoc.y - 4));
        } else if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom left
            minerGoTo(new MapLocation(myLoc.x + 4, myLoc.y + 4));
        } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom right
            minerGoTo(new MapLocation(myLoc.x - 4, myLoc.y + 4));
        } // else.. idk?!?!?
    }

    void buildAmazonAndSchoolIfAppropriate() throws GameActionException {
        if (amazonLocations.size() == 0 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 5) {
            if (myLoc.distanceSquaredTo(hqLoc) > 2) {
                System.out.println("Trybuild amazon");
                if (tryBuild(RobotType.FULFILLMENT_CENTER, myLoc.directionTo(hqLoc).opposite())) {
                    comms.broadcastBuildingCreation(RobotType.FULFILLMENT_CENTER, myLoc.add(myLoc.directionTo(hqLoc).opposite()));
                }
            }
        } else if (designSchoolLocations.size() == 0) {
            if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && myLoc.distanceSquaredTo(hqLoc) < 13  && myLoc.distanceSquaredTo(hqLoc) >= 2) {
                // System.out.println("No design schools yet, gotta build one");
                if (tryBuild(RobotType.DESIGN_SCHOOL, myLoc.directionTo(hqLoc).opposite())) {
                    // System.out.println("built school");
                    comms.broadcastBuildingCreation(RobotType.DESIGN_SCHOOL, myLoc.add(myLoc.directionTo(hqLoc).opposite()));
                }
            } else {
                // System.out.println("There are no design schools, but we dont have enough money to make one");
            }
        } else {
            // funcAfterBuildSchool();
        }
    }

    void tryDepositAndMineAllDirections() throws GameActionException {
        // TODO: 1/26/2020 Which do we want to use? Limit*0.8 or limit-7 -mz
        if (rc.getSoupCarrying() >= RobotType.MINER.soupLimit - 7) {
            for (Direction dir : Util.directions) {
                if (rc.canDepositSoup(dir)) {
                    rc.depositSoup(dir, rc.getSoupCarrying());
                    // System.out.println("Deposited soup into refinery");
                }
            }
        }

        // then, try to mine soup in all directions
        for (Direction dir : Util.directions) {
            if (tryMine(dir)) {
                // System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = myLoc.add(dir);
                if (!soupLocations.contains(soupLoc)) {
                    comms.broadcastSoupLocation(soupLoc);
                }
            }
        }

    }

    void buildAVaporatorUpHigh() throws GameActionException {
        int locX = myLoc.x - 1;
        int locY = myLoc.y - 1;
        MapLocation checkThisMapLoc = myLoc;

        for (int i = 0; i <= 2; i++) {
            locX += i;
            for (int j = 0; j <= 2; j++) {
                locY += j;
                checkThisMapLoc = new MapLocation(locX, locY);
                if (rc.onTheMap(checkThisMapLoc) && rc.canSenseLocation(checkThisMapLoc)) {
                    if (rc.senseElevation(checkThisMapLoc) > 8 && vaporatorLocations.size() < 20) {
                        if (tryBuild(RobotType.VAPORATOR, myLoc.directionTo(checkThisMapLoc))) {
                            System.out.println("build vap " + checkThisMapLoc);
                            comms.broadcastBuildingCreation(RobotType.VAPORATOR, checkThisMapLoc);
                            return;
                        }
                    }
                }
            }
        }
    }

    void buildRefineryIfAppropriate() throws GameActionException {
        // if there are no refineries, you get a pass to build a refinery earlier.
        // just not in the direction of HQ.
        if (refineryLocations.size() == 0 && myLoc.distanceSquaredTo(hqLoc) >= 4) {
            for (Direction dir: Util.directions) {
                if (!dir.equals(myLoc.directionTo(hqLoc))
                        && !dir.equals(myLoc.directionTo(hqLoc).rotateLeft())
                        && !dir.equals(myLoc.directionTo(hqLoc).rotateRight()) ) {
                    System.out.println("trybuild refinery away from hq");
                    if (tryBuild(RobotType.REFINERY, dir)) {
                        comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc.add(dir));
                        break;
                    }
                }
            }
        } else if (refineryLocations.size() == 0) {
//            nav.tryMove(myLoc.directionTo(hqLoc).opposite()); already called elsewhere
        } else {
            MapLocation closestRefinery = findClosestRefinery();
            // if further than 10, tries to build in all directions. breaks loop when it can.
            if (myLoc.distanceSquaredTo(closestRefinery) > DISTANCESQUARED_BETWEEN_REFINERIES) {
                for (Direction dir : Util.directions) {
                    System.out.println("trybuild refinery, far away");
                    if (tryBuild(RobotType.REFINERY, dir)) {
                        comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc.add(dir));
                        break;
                    }
                }
            }
        }
    }

    public void minerGoToNearestSoup() throws GameActionException {

        MapLocation nearestSoupLoc = findClosestSoup();

        // TODO: 1/20/2020 make miner sense soup, and add to soupLocations if said sensed soup is accessible
        // if a tile adjacent to soup is not flooded, it is accessible
        // if we can see around soupLoc, check if accessible
//        if (myLoc.distanceSquaredTo(nearestSoupLoc) < 20) {
//            while(!isSoupAccessible(nearestSoupLoc)) {
//                nearestSoupLoc = findClosestSoup();
//            }
//        }
        // System.out.println("I'm moving to soupLocation " + nearestSoupLoc);

        rc.setIndicatorLine(rc.getLocation(), nearestSoupLoc, 255, 0, 255);
        minerGoTo(nearestSoupLoc);

    }

    public void searchForSoup() throws GameActionException {

        MapLocation[] nearbySoup = rc.senseNearbySoup();
        if (nearbySoup.length > 0) {
            MapLocation closestSoup = nearbySoup[0];
            for (MapLocation soupLoc : nearbySoup) {
                if (myLoc.distanceSquaredTo(soupLoc) < myLoc.distanceSquaredTo(closestSoup)) {
                    closestSoup = soupLoc;
                }
            }
            minerGoTo(closestSoup);
        }

        // System.out.println("I'm searching for soup, moving away from other miners");
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
        MapLocation nextPlace = myLoc;
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.MINER) {
                nextPlace = nextPlace.add(myLoc.directionTo(robot.location).opposite());
            }
        }
        if (robots.length == 0) {
            nextPlace.add(Util.randomDirection());
        }
        // System.out.println("Trying to go: " + rc.getLocation().directionTo(nextPlace));
        if (nextPlace != rc.getLocation()) {
            minerGoTo(rc.getLocation().directionTo(nextPlace));

        } else {
            minerGoTo(Util.randomDirection());
        }
    }

    // ------------------------------------------------------------------------------------------------------------ //
    // ----------------------------------------- HELPER METHODS --------------------------------------------------- //


    // MAKE SURE SOUPLOCATIONS.SIZE > 0 WHEN USING THIS METHOD
    public MapLocation findClosestSoup() throws GameActionException {
        MapLocation nearestSoupLoc = soupLocations.get(0);

        // Find Closest Soup Location if theres more than 1
        if (soupLocations.size() > 0) { // This might be fixed because intelij thinks that its always greater than 0, not really sure. - Matt 1/21
            for (MapLocation loc : soupLocations) {
                if (myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(nearestSoupLoc)) {
                    nearestSoupLoc = loc;
                }
            }
        }
        return nearestSoupLoc;
    }

    // MAKE SURE REFINERYLOCATIONS.SIZE > 0 WHEN USING THIS METHOD
    public MapLocation findClosestRefinery() throws GameActionException {
        MapLocation closestRefineryLoc = refineryLocations.get(0);

        //Find Closest Refinery if there's more than 1
        if (refineryLocations.size() > 0) {
            for (MapLocation refinery : refineryLocations) {
                if (myLoc.distanceSquaredTo(refinery) < myLoc.distanceSquaredTo(closestRefineryLoc)) {
                    closestRefineryLoc = refinery;
                }
            }
        }
        return closestRefineryLoc;
    }


    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    void checkIfSoupGone(MapLocation loc) throws GameActionException {
        if (soupLocations.size() > 0) {
            // System.out.println("check soup" + loc);
//            MapLocation targetSoupLoc = soupLocations.get(0);
            if (rc.canSenseLocation(loc)
                    && rc.senseSoup(loc) == 0) {
                // System.out.println("soup at " + loc + "is gone");
                soupLocations.remove(loc);
            } /*else {
                if (myLoc.distanceSquaredTo(loc) < 20 *//*&& !isSoupAccessible(loc)*//*) {
                    // System.out.println("soup at " + loc + "is gone");
                    soupLocations.remove(loc);
                }
            }*/
        }
    }

    void checkIfRefineryGone(MapLocation loc) throws GameActionException {
        if (refineryLocations.size() > 0) {
            if (rc.canSenseLocation(loc)) {
                RobotInfo refinery = rc.senseRobotAtLocation(loc);
                // NullPointerException if the only refinery gets destroyed
                if (refinery != null && !rc.senseRobotAtLocation(loc).type.equals(RobotType.REFINERY)
                        && !rc.senseRobotAtLocation(loc).type.equals(RobotType.HQ)) {
                    // System.out.println("refinery at " + loc + "is gone");
                    refineryLocations.remove(loc);
                }
            }
        }
    }

    // fuzzy nav, except it won't go to a place it has visited in the last ten rounds
    boolean minerGoTo(Direction dir) throws GameActionException {

        // if dir is north, order would be N, NW, NE, W, E, SW, SE, S
        Direction[] fuzzyNavDirectionsInOrder = { dir, dir.rotateLeft(), dir.rotateRight(),
                dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(), dir.rotateRight().rotateRight().rotateRight(),
                dir.opposite(),
        };

        MapLocation moveTowardLocation = myLoc;
        Direction moveToward = fuzzyNavDirectionsInOrder[0];
        for (int i = 0; i < 8; i ++) {
            boolean shouldIMoveThere = true;
            moveToward = fuzzyNavDirectionsInOrder[i];
            moveTowardLocation = myLoc.add(moveToward);

            for (int j = 0; j < recentlyVisitedLocations.length; j++) {
                if (moveTowardLocation.equals(recentlyVisitedLocations[j])) {
                    System.out.println("recently visited " + moveTowardLocation);
                    shouldIMoveThere = false;
                    break;
                }
            }

             System.out.println("move " + fuzzyNavDirectionsInOrder[i] + "? " + shouldIMoveThere);

            if (shouldIMoveThere) {
                if (nav.tryMove(moveToward)) {
                    System.out.println("moved toward " + moveToward);
                    return true;
                }
            }
        }
//
//        for (Direction d : toTry){
//            if(tryMove(d))
//                return true;
//        }
        return false;
    }

    // navigate towards a particular location
    boolean minerGoTo(MapLocation destination) throws GameActionException {
        return minerGoTo(rc.getLocation().directionTo(destination));
    }
    
    void funcAfterBuildSchool() throws GameActionException {
        // System.out.println("There are design schools");
        boolean landscapersNearby = false;
        RobotInfo[] nearbyTeammates = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
        if (nearbyTeammates.length > 0) {
            for (RobotInfo bot : nearbyTeammates) {
                if (bot.type.equals(RobotType.LANDSCAPER)) {
                    landscapersNearby = true;
                    break;
                }
            }
        }

        if (landscapersNearby) {
            if (!hqRemovedFromRefineryLocations) {
                // System.out.println("remove hq from refineries");
                refineryLocations.remove(hqLoc);
                hqRemovedFromRefineryLocations = true;
            }
            // need to build refinery ASAP
            if (refineryLocations.size() == 0) {
                // System.out.println("need to build refinery asap");
                buildRefineryIfAppropriate();
            } else if (myLoc.distanceSquaredTo(findClosestRefinery()) > DISTANCESQUARED_BETWEEN_REFINERIES) {
                for (Direction dir: Util.directions) {
                    if (!dir.equals(myLoc.directionTo(hqLoc))
                            && !dir.equals(myLoc.directionTo(hqLoc).rotateLeft())
                            && !dir.equals(myLoc.directionTo(hqLoc).rotateRight()) ) {
                        if (rc.getTeamSoup() >= RobotType.REFINERY.cost + 5 && tryBuild(RobotType.REFINERY, dir)) {
                            comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc.add(dir));
                        }
                    }
                }
            }

            // : 1/20/2020 gotta try and make it move away from HQ
            if (myLoc.distanceSquaredTo(hqLoc) < 6) {
//                    runAwayyyyyy();
            }
        }
    }


}
