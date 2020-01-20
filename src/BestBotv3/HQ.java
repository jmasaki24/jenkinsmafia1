package BestBotv3;

import battlecode.common.*;

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
    public int numMiners = 0;

    // why is this static? idk. might be helpful later. -jm
    public static final int MINER_LIMIT = 4;

    public HQ(RobotController r) throws GameActionException {
        super(r);
        comms.sendHqLoc(rc.getLocation());
    }


    public void takeTurn() throws GameActionException {
        super.takeTurn();
        int numSoupNearby = 0;
        if(turnCount == 1) {
            comms.sendHqLoc(rc.getLocation());
            MapLocation[] nearbySoupLocations = rc.senseNearbySoup();
            if (nearbySoupLocations.length > 0) {
                for (MapLocation nearbySoup : nearbySoupLocations) {
                    // TODO: 1/19/2020 if the soup is surrounded by water, miner will be fucked
                    if (numSoupNearby < 20) { // don't want to spend all the soup broadcasting locs
                        comms.broadcastSoupLocation(nearbySoup);
                        numSoupNearby++;
                    }
                }
            }
        }

        if(numMiners < MINER_LIMIT) {
            for (Direction dir : Util.directions)
                if(tryBuild(RobotType.MINER, dir)){
                    numMiners++;
                }
        }

        //Request a school next to base
        boolean seeDesignSchool = false;
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.HQ.sensorRadiusSquared,rc.getTeam());
        for (RobotInfo robot:robots){
            if(robot.type == RobotType.DESIGN_SCHOOL){
                seeDesignSchool = true;
            }
        }
        if(!seeDesignSchool){
            if(rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.MINER.cost){
                tryBuild(RobotType.MINER,Direction.SOUTHWEST);
            }
        }
        if (seeDesignSchool && rc.getRoundNum() > 300){
            for (Direction dir: Util.directions){
                tryBuild(RobotType.MINER,Util.randomDirection());
            }
        }
    }
}