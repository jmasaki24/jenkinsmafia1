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
    ArrayList<MapLocation> soupLocations = new ArrayList<MapLocation>();

    public Miner(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        //Update Stuff
        updateBuildingLocations();
        comms.updateSoupLocations(soupLocations);
        checkIfSoupGone();

        //Build 1 school when summoned into a specific position by HQ and after move away
        System.out.println(turnCount);
        if(turnCount <= 11){
            System.out.println(hqLoc.distanceSquaredTo(myLoc));
            if(myLoc.directionTo(hqLoc) == Direction.NORTHEAST && myLoc.distanceSquaredTo(hqLoc) == 2){
                if (tryBuild(RobotType.DESIGN_SCHOOL,Direction.NORTH)) {
                    System.out.println("built school");
                    comms.broadcastBuildingCreation(RobotType.DESIGN_SCHOOL, myLoc.add(Direction.NORTH));
                }
            }
        }

        // run away from hq if you see a design school
        boolean schoolExists = false;



        // Better to deposit soup while you can
        for (Direction dir : Util.directions) {
            if (rc.canDepositSoup(dir)) {
                rc.depositSoup(dir, rc.getSoupCarrying());
                System.out.println("Deposited soup into new refinery");
            }
        }

        // then, try to mine soup in all directions
        for (Direction dir : Util.directions)
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (hqLoc.distanceSquaredTo(soupLoc) > 10) {
                    if (tryBuild(RobotType.REFINERY, Util.randomDirection())) {
                        comms.broadcastBuildingCreation(RobotType.REFINERY, rc.adjacentLocation(dir.opposite()));
                    }
                }
                if(soupLocations.size() == 0) {
                    comms.broadcastSoupLocation(soupLoc);
                } else{
                    if (!soupLocations.contains(soupLoc)) {
                        comms.broadcastSoupLocation(soupLoc);
                    }
                }


            }

        //lastly, move

        // if at soup limit, go to nearest refinery or hq.
        //      if there is a design school, hq is no longer part of the nearest refineries.
        // if hq or refinery is far away, build a refinery.
        // if there are soupLocations, go to soup
        // finally, move away from other miners
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            System.out.println("I'm full of soup");


            //find closest refinery (including hq, should change that tho since HQ will become unreachable)
            MapLocation closestRefineryLoc = hqLoc;

            //Find Closest Refinery
            if (refineryLocations.size() != 0) {
                for (MapLocation refinery : refineryLocations) {
                    if (myLoc.distanceSquaredTo(refinery) < myLoc.distanceSquaredTo(closestRefineryLoc)) {
                        closestRefineryLoc = refinery;
                    }
                }
            }

            // a refinery, they just sit there and wait for passive soup income.

            // how far away is enough to justify a new refinery?
            if (rc.getLocation().distanceSquaredTo(closestRefineryLoc) > 35) {
                if(!tryBuild(RobotType.REFINERY, Util.randomDirection())){ // if a new refinery can't be built go back to hq
                    System.out.println("moved towards HQ");
                    nav.goTo(closestRefineryLoc);
                    rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
                }
            } else {
                System.out.println("moved towards HQ");
                nav.goTo(closestRefineryLoc);
                rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
            }
        }

        else {
            if (soupLocations.size() > 0) {
                System.out.println("I'm moving to soupLocation");

                MapLocation nearestSoupLoc = soupLocations.get(0);

                for (MapLocation loc : soupLocations) {
                    if (myLoc.distanceSquaredTo(loc) < myLoc.distanceSquaredTo(nearestSoupLoc)) {
                        nearestSoupLoc = loc;
                    }
                }

                // if you can see all tiles adjacent to soupLoc
                if (myLoc.distanceSquaredTo(nearestSoupLoc) < 20) {
                    if (isSoupAccessible(nearestSoupLoc)) {

                    }
                }

                nav.goTo(soupLocations.get(0));
            } else {
                System.out.println("I'm searching for soup, moving away from other miners");
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared,rc.getTeam());
                MapLocation nextPlace = rc.getLocation();
                for (RobotInfo robot:robots){
                    if (robot.type == RobotType.MINER){
                        nextPlace = nextPlace.add(rc.getLocation().directionTo(robot.location).opposite());
                    }
                }
                if(robots.length == 0){
                    nextPlace.add(Util.randomDirection());
                }
                System.out.println("Trying to go: " + rc.getLocation().directionTo(nextPlace));
                if(nextPlace != rc.getLocation()){
                    nav.goTo(rc.getLocation().directionTo(nextPlace));
                } else{
                    nav.goTo(Util.randomDirection());
                }
            }
        }
    }

    // this method could be done MUCH MUCH better, I think. sorry in advance.
    public boolean isSoupAccessible(MapLocation soupLoc) throws GameActionException {
        MapLocation[] surroundingLocs = {
            soupLoc.add(Util.directions[0]), soupLoc.add(Util.directions[1]),
            soupLoc.add(Util.directions[2]), soupLoc.add(Util.directions[3]),
            soupLoc.add(Util.directions[4]), soupLoc.add(Util.directions[5]),
            soupLoc.add(Util.directions[6]), soupLoc.add(Util.directions[7]),
        };

        boolean isAccessible = false;
        for (MapLocation loc : surroundingLocs) {
            isAccessible = isAccessible || !rc.senseFlooding(loc);
        }
        return isAccessible;
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

    void checkIfSoupGone() throws GameActionException {
        if (soupLocations.size() > 0) {
            MapLocation targetSoupLoc = soupLocations.get(0);
            if (rc.canSenseLocation(targetSoupLoc)
                    && rc.senseSoup(targetSoupLoc) == 0) {
                System.out.println("soup is gone");
                soupLocations.remove(0);
            }
        }
    }
}
