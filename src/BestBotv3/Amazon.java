package BestBotv3;

import battlecode.common.*;

import java.util.ArrayList;

//Todo: set a hard limit of landscapers to make
//Todo: Create Broadcast Design School Creation

public class Amazon extends Building {
    private boolean shouldMakeBuilders;

    public Amazon(RobotController r) {
        super(r);
    }

    int numDrones = 0;
    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (turnCount == 1) {
            // I would love to do this, but instead we're just gonna broadcast from Miner atm. -jm
//            comms.broadcastBuildingCreation(RobotType.FULFILLMENT_CENTER, myLoc);
        }

        if (numDrones >= 12){
            comms.broadcastBuildingCreation(RobotType.COW, myLoc);
            System.out.println("Block!");
        }
        if ((numDrones < 2 && rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost) || rc.getTeamSoup() > (RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost + 30)) {
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.DELIVERY_DRONE,dir)) {
                    numDrones++;
                }
            }
        }

        if (rc.getTeamSoup() > ARBITRARY_SOUP_NUMBER_LMAO) {
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.DELIVERY_DRONE,dir)) {
                    numDrones++;
                }
            }
        }

//        if (rc.getTeamSoup()>=(6*RobotType.DELIVERY_DRONE.cost)){
//            shouldMakeBuilders = true;
//        }
//        if (shouldMakeBuilders){
//            for (Direction dir: Util.directions){
//                tryBuild(RobotType.DELIVERY_DRONE,dir);
//            }
//        }
    }
}
