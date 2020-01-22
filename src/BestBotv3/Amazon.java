package BestBotv3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

//Todo: set a hard limit of landscapers to make
//Todo: Create Broadcast Design School Creation

public class Amazon extends Building {
    private boolean shouldMakeBuilders;

    public Amazon(RobotController r) {
        super(r);
    }

    int numDrones = 1;
    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (numDrones <= 2) {
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
