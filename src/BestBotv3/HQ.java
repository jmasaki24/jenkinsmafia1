package BestBotv3;

import battlecode.common.*;

import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.Arrays;
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
            broadcastNearbySoupLocations();
        } else if (turnCount == 30) {
            // wait until 30th turn to send nearby water locations cuz who knows how much soup we'll have
            broadcastNearbyWaterLocations();
        }

        //Every 3 turns repeat messages.
        if (turnCount > 3 && turnCount % 3 == 2) {
            comms.jamEnemyComms();
        }

        if (numMiners < MINER_LIMIT && rc.getTeamSoup() > RobotType.MINER.cost + 2) {
            for (Direction dir : Util.directions)
                if (tryBuild(RobotType.MINER, dir)) {
                    numMiners++;
                    RobotInfo justCreatedBot = rc.senseRobotAtLocation(myLoc.add(dir));
                    if (justCreatedBot != null) {
                        broadcastUnitCreation(justCreatedBot);
                    } else {
                        System.out.println("NULL EXCEPTION! nuts!");
                    }
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

    void broadcastNearbySoupLocations() throws GameActionException {
        comms.broadcastBuildingCreation(RobotType.HQ, myLoc);
        // System.out.println("I broadcasted my location");
        MapLocation[] nearbySoupLocations = rc.senseNearbySoup(13);
        // TODO: 1/19/2020 if the soup is surrounded by water or elevated land, miner will be fucked
        ArrayList<MapLocation> soupInHQRadius = new ArrayList<MapLocation>(Arrays.asList(nearbySoupLocations));
        for (MapLocation soupLoc : soupInHQRadius) {
            comms.broadcastSoupLocation(soupLoc);
        }

        if (soupInHQRadius.size() < 10) {
            MapLocation[] moreSoupLocations = rc.senseNearbySoup(29);
            ArrayList<MapLocation> moreSoupInRadius = new ArrayList<MapLocation>();
            for (MapLocation soupLoc: moreSoupLocations) {
                if (!soupInHQRadius.contains(soupLoc)) {
                    soupInHQRadius.add(soupLoc);
                    comms.broadcastSoupLocation(soupLoc);
                }
            }
        }
    }





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
