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
        if (turnCount == 1) {
            comms.broadcastBuildingCreation(RobotType.REFINERY, myLoc);
        }
    }
}
