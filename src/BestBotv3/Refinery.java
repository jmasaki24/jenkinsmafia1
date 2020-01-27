package BestBotv3;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;


//Commented to try to commit improved support drone to master take 2
public class Refinery extends Building {
    public Refinery(RobotController r) {
        super(r);
    }


    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (turnCount == 1) {
            // this happens in Miner.java instead. wasn't working here.
//            comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc);
        }
    }
}
