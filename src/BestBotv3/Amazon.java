package BestBotv3;

import battlecode.common.*;


//Todo: set a hard limit of landscapers to make
//Todo: Create Broadcast Design School Creation

public class Amazon extends Building {
    private boolean shouldMakeBuilders;
    private int typeOfDrone = 0;

    private int DEFENSE = 1;
    private int MINEHELP = 3;
    private int ATTACK = 5;

    private int droneID;

    public Amazon(RobotController r) {
        super(r);
    }

    int numDrones = 0;
    public void takeTurn() throws GameActionException {
        super.takeTurn();

        RobotInfo[] newDrones = rc.senseNearbyRobots(3, rc.getTeam());
        for(RobotInfo r: newDrones){
            if(r.getType() == RobotType.DELIVERY_DRONE){
                droneID = r.getID();

            }
        }

        if (turnCount == 1) {
            // I would love to do this, but instead we're just gonna broadcast from Miner atm. -jm
//            comms.broadcastBuildingCreation(RobotType.FULFILLMENT_CENTER, myLoc);
        }

        if (numDrones < 2 && rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost + 2) {
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.DELIVERY_DRONE,dir)) {
                    numDrones++;

                    RobotInfo justCreatedBot = rc.senseRobotAtLocation(myLoc.add(dir));
                    if (justCreatedBot != null) {
                        broadcastUnitCreation(justCreatedBot);
                    } else {
                        System.out.println("NULL EXCEPTION! nuts!");
                    }
                }
            }
        }

        if (rc.getTeamSoup() > ARBITRARY_SOUP_NUMBER_LMAO) {
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.DELIVERY_DRONE,dir)) {
                    numDrones++;
                    RobotInfo justCreatedBot = rc.senseRobotAtLocation(myLoc.add(dir));
                    if (justCreatedBot != null) {
                        broadcastUnitCreation(justCreatedBot);
                    } else {
                        System.out.println("NULL EXCEPTION! nuts!");
                    }
                }
            }
        }

        if(numDrones == 1 || numDrones == 2){
            typeOfDrone = DEFENSE;
        }
        else if(numDrones == 3){
            typeOfDrone = MINEHELP;
        }
        else if(numDrones < 30){
            typeOfDrone = DEFENSE;
        }
        else{
            typeOfDrone = ATTACK;
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
