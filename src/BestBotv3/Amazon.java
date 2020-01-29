package BestBotv3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

//Todo: set a hard limit of landscapers to make
//Todo: Create Broadcast Design School Creation

public class Amazon extends Building {
    private boolean shouldMakeBuilders;
    private int typeOfDrone = 0;

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

        if (numDrones < 2 && rc.getTeamSoup() > RobotType.REFINERY.cost) {
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.DELIVERY_DRONE,dir)) {
                    numDrones++;
                    System.out.println("hi " + numDrones);

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

        if(numDrones == 1 || numDrones == 2){
            typeOfDrone = 1;
        }
        else if(numDrones == 3){
            typeOfDrone = 3;
        }
        else if(numDrones < 30){
            typeOfDrone = 4;
        }
        else{
            typeOfDrone = 5;
        }

        comms.broadcastTypeOfDrone(typeOfDrone, rc.getRoundNum());

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
