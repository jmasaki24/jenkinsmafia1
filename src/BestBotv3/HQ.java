package BestBotv3;

import battlecode.common.*;

import java.rmi.MarshalledObject;
import java.util.Map;
/*
 * FIRST FEW ROUNDS STRATEGY
 * Build 4 miners, start refining soup in HQ
 * Build Amazon, drone. Drone is on standby (hover near HQ, wait for enemy units)
 * Build design school
 * Build refinery (if there isn’t already one)
 * build 2 landscapers
 */

public class HQ extends Shooter {
    //HQ variables
    public int numMiners = 0;
    public final int MINER_LIMIT = 4;

    public HQ(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int numSoupNearby = 0;

        // on first turn send nearbySoupLocations
        if (turnCount == 1) {
            comms.broadcastBuildingCreation(RobotType.HQ, myLoc);
            // System.out.println("I broadcasted my location");
            MapLocation[] nearbySoupLocations = rc.senseNearbySoup();
            if (nearbySoupLocations.length > 0) {
                for (MapLocation nearbySoup : nearbySoupLocations) {
                    // System.out.println("hq sees soup " + nearbySoup);
                    // TODO: 1/19/2020 if the soup is surrounded by water or elevated land, miner will be fucked
                    if (numSoupNearby < 10) { // don't want to spend all the soup broadcasting locs
                        if (myLoc.distanceSquaredTo(nearbySoup) < 32 /*&& isSoupAccessible(nearbySoup)*/) {
                            comms.broadcastSoupLocation(nearbySoup);
                            numSoupNearby++;
                        } else {
                            numSoupNearby++;
                        }
                    }
                }
            }
        }

        // wait until 30th turn to send nearby water locations cuz who knows how much soup we'll have
        else if (turnCount == 30) {
            broadcastNearbyWaterLocations();
        }

        //Every 3 turns repeat messages.
        if (turnCount > 3 && turnCount % 3 == 2) {
            comms.jamEnemyComms();
        }

        if (numMiners < MINER_LIMIT) {
            for (Direction dir : Util.directions)
                if (tryBuild(RobotType.MINER, dir)) {
                    numMiners++;
                }
        }

        // just cuz :)
//        if (rc.getTeamSoup() > 450) {
//            for (Direction dir : Util.directions) {
//                if (tryBuild(RobotType.MINER, dir)) {
//                    numMiners++;
//                }
//            }
//        }

        //Request a school next to base
        boolean seeDesignSchool = false;
        int numAttackers = 0; // to prevent us from using all of our soup on attacker broadcasts
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.HQ.sensorRadiusSquared);
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.DESIGN_SCHOOL && robot.getTeam() == rc.getTeam()) {
                seeDesignSchool = true;
            } else if ((robot.type == RobotType.MINER || robot.type == RobotType.LANDSCAPER) && robot.getTeam() == rc.getTeam().opponent()){
                if (numAttackers > 6) {
                    return;
                }
                comms.broadcastAttackerInfo(robot.ID, myLoc.directionTo(robot.location));
                numAttackers++;
            }
        }


//        if (!seeDesignSchool) {
//            if (rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.MINER.cost) {
//                tryBuild(RobotType.MINER, Direction.SOUTHWEST);
//            }
//        }
//        if (seeDesignSchool && rc.getRoundNum() > 300){
//            for (Direction dir: Util.directions){
//                tryBuild(RobotType.MINER,Util.randomDirection());
//            }
//        }
    }


    // ----------------------------------------------- METHODS SECTION ---------------------------------------------- \\

    // go from top row to bottom row, left to right
    void broadcastNearbyWaterLocations() throws GameActionException {
        int checkThisX = myLoc.x;
        int checkThisY = myLoc.y;
        MapLocation checkThisLoc = myLoc;


        // FIRST ROW
        checkThisX = myLoc.x - 3;
        checkThisY = myLoc.y + 6;

    }



}
