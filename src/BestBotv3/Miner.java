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
public class Miner extends Unit {

    int numDesignSchools = 0;

    // ALL "...Locations" ArrayList<MapLocation> ARE IN Unit.java!!!!!!!!!!!!!!!!!!!!


    public Miner(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (turnCount == 1) {
            System.out.println("adding hq to refineries");
            refineryLocations.add(hqLoc);   // since hq is technically a refinery
        }

        //Update Stuff
        updateBuildingLocations();
        comms.updateSoupLocations(soupLocations);
        comms.updateAmazonLocations(amazonLocations);
        if (soupLocations.size() > 0) {
            checkIfSoupGone(findClosestSoup());
        }
        if (refineryLocations.size() > 0) {
            checkIfRefineryGone(findClosestRefinery());
        }

        if (designSchoolLocations.size() > 0) {
            System.out.println("remove hq from refineries");
            refineryLocations.remove(hqLoc);

            // need to build refinery ASAP
            if (myLoc.distanceSquaredTo(hqLoc) > 2) {
                for (Direction dir: Util.directions) {
                    if (!dir.equals(myLoc.directionTo(hqLoc))
                            && !dir.equals(myLoc.directionTo(hqLoc).rotateLeft())
                            && !dir.equals(myLoc.directionTo(hqLoc).rotateRight()) ) {
                        tryBuild(RobotType.REFINERY, dir);
                        comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc.add(dir));
                    }
                }
            }

            // TODO: 1/20/2020 gotta try and make it move away from HQ
            if (myLoc.distanceSquaredTo(hqLoc) < 5) {
                runAwayyyyyy();
            }
        }

        // Build 1 amazon, then build school.
        System.out.println(turnCount);
        if(!comms.updateAmazonLocations(amazonLocations) && rc.getTeamSoup()>=155){
            if(myLoc.distanceSquaredTo(hqLoc) > 0){
                System.out.println("Trybuild amazon");
                if (tryBuild(RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(hqLoc).opposite())){
                    comms.broadcastBuildingCreation(RobotType.FULFILLMENT_CENTER,rc.getLocation().add(rc.getLocation().directionTo(hqLoc).opposite()));
                }
            }
        } else if(designSchoolLocations.size() == 0){
            if(myLoc.directionTo(hqLoc) == Direction.NORTHEAST && myLoc.distanceSquaredTo(hqLoc) == 2){
                if (tryBuild(RobotType.DESIGN_SCHOOL,Direction.NORTH)) {
                    System.out.println("built school");
                    comms.broadcastBuildingCreation(RobotType.DESIGN_SCHOOL, myLoc.add(Direction.NORTH));
                }
            }
        }

        // run away from hq if you see a design school
        boolean schoolExists = false;

        // TODO: 1/20/2020 somehow trying to deposit and refine in all directions slows down mining when miner is next to hq

        // Better to deposit soup while you can
        for (Direction dir : Util.directions) {
            if (rc.canDepositSoup(dir)) {
                rc.depositSoup(dir, rc.getSoupCarrying());
                System.out.println("Deposited soup into refinery");
            }
        }

        // then, try to mine soup in all directions
        for (Direction dir : Util.directions) {
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (!soupLocations.contains(soupLoc)) {
                    comms.broadcastSoupLocation(soupLoc);
                }
            }
        }

        // if closest refinery is far away, build a refinery.

        //lastly, move

        // if at soup limit, go to nearest refinery
        //      if there is a design school, hq is no longer part of the nearest refineries.
        // if there are soupLocations, go to nearest soup
        // else, move away from other miners
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            System.out.println("I'm full of soup, refineTime");

            // TODO: 1/20/2020 need to sit still when there isn't enough soup 
            buildRefineryIfAppropriate();

            //find closest refinery (including hq, should change that tho since HQ will become unreachable)
            MapLocation closestRefineryLoc = findClosestRefinery();
            nav.goTo(closestRefineryLoc);
            rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);

        } else {
            if (soupLocations.size() > 0) {
                goToNearestSoup();
            } else {
                searchForSoup();
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

        rc.setIndicatorLine(rc.getLocation(), nearestSoupLoc, 255, 0, 255);
        nav.goTo(nearestSoupLoc);
    }

    public void searchForSoup() throws GameActionException {
        System.out.println("I'm searching for soup, moving away from other miners");
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
        MapLocation nextPlace = rc.getLocation();
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.MINER) {
                nextPlace = nextPlace.add(rc.getLocation().directionTo(robot.location).opposite());
            }
        }
        if (robots.length == 0) {
            nextPlace.add(Util.randomDirection());
        }
        System.out.println("Trying to go: " + rc.getLocation().directionTo(nextPlace));
        if (nextPlace != rc.getLocation()) {
            nav.goTo(rc.getLocation().directionTo(nextPlace));
        } else {
            nav.goTo(Util.randomDirection());
        }
    }

    // builds a refinery if there are none or if we are far enough away from the closest one (which includes hq)
    public void buildRefineryIfAppropriate() throws GameActionException {
        System.out.println("building refinery");

        if (refineryLocations.size() == 0) {
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.REFINERY, dir)) {
                    comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc.add(dir));
                    break;
                }
            }
        } else {
            MapLocation closestRefinery = findClosestRefinery();
            // if further than 10, tries to build in all directions. breaks loop when it can.
            if (myLoc.distanceSquaredTo(closestRefinery) > 13) {
                for (Direction dir : Util.directions) {
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
        for (MapLocation loc : soupLocations) {
            if (myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(nearestSoupLoc)) {
                nearestSoupLoc = loc;
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
            }
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
