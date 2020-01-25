package BestBotv3;

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
  0. INITIALIZATION (turnCount == 1)
    - crawl blockchain for locations.
  1.update everything from current block.
  2.if you can see all tiles in a 5x5 around soup or a refinery:
    A. if it's hq, presence of landscapers also define accessibility.
    B. if it's not accessible, remove it from locations arraylist.
  3.if <= 2 squared away from a landscaper, run away.
  4.if there are no refineries, build one.
  5.if there are no amazons, build one in suitable location.
  6.if there is an amazon and no design school, build a design school in suitable location.
  7.if carrying limit-7 soup, try to deposit in all directions.
  8.if at soup limit:
    A. if there is a refinery:
      a. if it's not too far away, go to it.
      b. if it's too far away and there's more than 50 soup:
        1. build one if you can.
      c. if it's too far away and there's less than 50 soup:
        1. go to it anyway. (don't need miners sitting around, waiting for passive soup income)
    B. if there is no refinery, build it.

  9.if
  * next is movement shii
1/21/2020 I'll finish this later. -jm.

 */

public class Miner extends Unit {

    int numDesignSchools = 0;
    int distanceToMakeRefinery = 50;

    // ALL "...Locations" ArrayList<MapLocation> ARE IN Unit.java!!!!!!!!!!!!!!!!!!!!


    public Miner(RobotController r) {
        super(r);
    }

    boolean hqRemovedFromRefineryLocations = false;

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // 0. INITIALIZATION
        if (turnCount == 1) {
            System.out.println("adding hq to refineries");
            refineryLocations.add(hqLoc);   // since hq is technically a refinery
        }

        // 1.update everything from current block.
        comms.updateBuildingLocations();
        comms.updateSoupLocations(soupLocations);

        // 2. if you can see all tiles in a 5x5 around soup or a refinery:
        // TODO: 1/22/2020 the accessible part of these methods. right now they just check if it's there
        if (soupLocations.size() > 0) {
            checkIfSoupGone(findClosestSoup());
        }
        if (refineryLocations.size() > 0) {
            checkIfRefineryGone(findClosestRefinery());
        }

        // TODO: 1/22/2020 determine whether the hq stuff can just be put in turnCount==1 state with senseNearbyRobots
        if (hqLocations.size() == 0 && hqLoc == null){
            comms.broadcastBuildingCreation(RobotType.HQ, hqLoc);
            System.out.println("Hq didnt broadcast its location well");
        } else {
            System.out.println("HQ has been broadcasted and I recieved. Its at " + hqLoc);
        }

        // TODO: 1/21/2020 How can we make the miners sense water anywhere in their field of vision? -matt
        // see
//        for (Direction dir : Util.directions) {
//            if (rc.senseFlooding(myLoc.add(dir))) {
//                comms.broadcastWaterLocation(myLoc.add(dir));
//            }
//        }




        // 3.if <= 2 squared away from a landscaper, run away.
        // 4.if there are no refineries, build one.
        // 5.if there are no amazons, build one in suitable location.
        // 6.if there is an amazon and no design school, build a design school in suitable location.
        buildAmazonThenSchoolThenCheckForLandscapersAndRunAway();


        // TODO: 1/20/2020 somehow trying to deposit and refine in all directions slows down mining when miner is next to hq
        // 7.if carrying limit-7 soup, try to deposit in all directions.
        if (rc.getSoupCarrying() >= RobotType.MINER.soupLimit-7) {
            // Better to deposit soup while you can
            for (Direction dir : Util.directions) {
                if (rc.canDepositSoup(dir)) {
                    rc.depositSoup(dir, rc.getSoupCarrying());
                    System.out.println("Deposited soup into refinery");
                }
            }
        }
            // then, try to mine soup in all directions
        for (Direction dir : Util.directions) {
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = myLoc.add(dir);
                if (!soupLocations.contains(soupLoc)) {
                    comms.broadcastSoupLocation(soupLoc);
                }
            }
        }


        // if closest refinery is far away, build a refinery.

        // MOVEMENT

                // if at soup limit, go to nearest refinery
                //      if there is a design school, hq is no longer part of the nearest refineries.
                // if there are soupLocations, go to nearest soup
                // else, move away from other miners

        // 8.if at soup limit:
        //  A. if there is a refinery:
        //   a. if it's not too far away, go to it.
        //   b. if it's too far away and there's more than 50 soup:
        //     1. build one if you can.
        //   c. if it's too far away and there's less than 50 soup:
        //     1. go to it anyway. (don't need miners sitting around, waiting for passive soup income)
        //  B. if there is no refinery, build it.

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            System.out.println("I'm full of soup, refineTime");

            // TODO: 1/20/2020 need to sit still when there isn't enough soup
            buildRefineryIfAppropriate();

            //find closest refinery (including hq, should change that tho since HQ will become unreachable)
            if (refineryLocations.size() > 0) {
                MapLocation closestRefineryLoc = findClosestRefinery();
                nav.goTo(closestRefineryLoc);
                rc.setIndicatorLine(myLoc, closestRefineryLoc, 255, 0, 255);
            }
            // else, just sit there?
        }
        else {
            if (soupLocations.size() > 0) {
                goToNearestSoup();
            } else {
                searchForSoup();
            }
        }
    }





    // ----------------------------------------------- METHODS SECTION ---------------------------------------------- \\

    // TODO: 1/22/2020 make this method smaller (to an extent), and clearer.
    void buildAmazonThenSchoolThenCheckForLandscapersAndRunAway() throws GameActionException {

        if (amazonLocations.size() == 0 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 5) {
            if (myLoc.distanceSquaredTo(hqLoc) > 2) {
                System.out.println("Trybuild amazon");
                if (tryBuild(RobotType.FULFILLMENT_CENTER, myLoc.directionTo(hqLoc).opposite())) {
                    comms.broadcastBuildingCreation(RobotType.FULFILLMENT_CENTER, myLoc.add(myLoc.directionTo(hqLoc).opposite()));
                }
            }
        } else if (designSchoolLocations.size() == 0){
            if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && myLoc.distanceSquaredTo(hqLoc) < 9) {
                System.out.println("No design schools yet, gotta build one");
                if (tryBuild(RobotType.DESIGN_SCHOOL, myLoc.directionTo(hqLoc).opposite())) {
                    System.out.println("built school");
                    comms.broadcastBuildingCreation(RobotType.DESIGN_SCHOOL, myLoc.add(myLoc.directionTo(hqLoc).opposite()));
                }
            } else {
                System.out.println("There are no design schools, but we dont have enough money to make one");
            }
        } else {
            System.out.println("There are design schools");
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
                    System.out.println("remove hq from refineries");
                    refineryLocations.remove(hqLoc);
                    hqRemovedFromRefineryLocations = true;
                }
                // need to build refinery ASAP
                if (refineryLocations.size() == 0) {
                    System.out.println("need to build refinery asap");
                    buildRefineryIfAppropriate();
                }
                if (myLoc.distanceSquaredTo(findClosestRefinery()) > distanceToMakeRefinery) {
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

                // TODO: 1/20/2020 gotta try and make it move away from HQ
                if (myLoc.distanceSquaredTo(hqLoc) < 6) {
                    runAwayyyyyy();
                }
            }
        }
    }


    public void goToNearestSoup() throws GameActionException {
        MapLocation nearestSoupLoc = findClosestSoup();

        // TODO: 1/20/2020 make miner sense soup, and add to soupLocations if said sensed soup is accessible
        // if a tile adjacent to soup is not flooded, it is accessible
        // if we can see around soupLoc, check if accessible
//        if (myLoc.distanceSquaredTo(nearestSoupLoc) < 20) {
//            while(!isSoupAccessible(nearestSoupLoc)) {
//                nearestSoupLoc = findClosestSoup();
//            }
//        }
        System.out.println("I'm moving to soupLocation " + nearestSoupLoc);

        rc.setIndicatorLine(myLoc, nearestSoupLoc, 255, 0, 255);
        nav.goTo(nearestSoupLoc);
    }

    public void searchForSoup() throws GameActionException {
        System.out.println("I'm searching for soup, moving away from other miners");
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
        System.out.println("Trying to go: " + myLoc.directionTo(nextPlace));
        if (nextPlace != myLoc) {
            nav.goTo(myLoc.directionTo(nextPlace));
        } else {
            nav.goTo(Util.randomDirection());
        }
    }

    // builds a refinery if there are none or if we are far enough away from the closest one (which includes hq)
    // if near hq, don't build in the direction of hq
    public void buildRefineryIfAppropriate() throws GameActionException {

        // if near hq, don't build in the direction of hq
        if ((myLoc.distanceSquaredTo(hqLoc) < 8) && refineryLocations.size() == 0) {
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
        }
        // if no refineries (i.e. there is a school and we can't use hq) build one asap
        if (refineryLocations.size() == 0) {
            for (Direction dir : Util.directions) {
                System.out.println("trybuild refinery");
                if (tryBuild(RobotType.REFINERY, dir)) {
                    comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc.add(dir));
                    break;
                }
            }
        } else { // findClosestRefinery REQUIRES that there be at least 1 refinery
            MapLocation closestRefinery = findClosestRefinery();
            // if further than 10, tries to build in all directions. breaks loop when it can.
            if (myLoc.distanceSquaredTo(closestRefinery) > 20) {
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
//            MapLocation targetSoupLoc = soupLocations.get(0);
            if (rc.canSenseLocation(loc)
                    && rc.senseSoup(loc) == 0) {
                System.out.println("soup at " + loc + "is gone");
                soupLocations.remove(loc);
            } /*else {
                if (myLoc.distanceSquaredTo(loc) < 20 *//*&& !isSoupAccessible(loc)*//*) {
                    System.out.println("soup at " + loc + "is gone");
                    soupLocations.remove(loc);
                }
            }*/
        }
    }
    void checkIfRefineryGone(MapLocation loc) throws GameActionException {
        if (refineryLocations.size() > 0) {
//            MapLocation targetSoupLoc = soupLocations.get(0);
            if (rc.canSenseLocation(loc)
                    && (!rc.senseRobotAtLocation(loc).type.equals(RobotType.REFINERY)
                    && !rc.senseRobotAtLocation(loc).type.equals(RobotType.HQ))) {
                System.out.println("refinery at " + loc + "is gone");
                refineryLocations.remove(loc);
            }
        }
    }

    // basically, goes in the direction of the center of the map
    void runAwayyyyyy() throws GameActionException {
        System.out.println("Run awayyyyyyyy");

        if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top left
            nav.goTo(new MapLocation(myLoc.x + 4, myLoc.y - 4));
        } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top right
            nav.goTo(new MapLocation(myLoc.x + 4, myLoc.y - 4));
        } else if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom left
            nav.goTo(new MapLocation(myLoc.x + 4, myLoc.y - 4));
        } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom right
            nav.goTo(new MapLocation(myLoc.x + 4, myLoc.y - 4));
        } // else.. idk?!?!?
    }
}
