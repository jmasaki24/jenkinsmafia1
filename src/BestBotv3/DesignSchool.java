package BestBotv3;

import battlecode.common.*;

//Todo: set a hard limit of landscapers to make

public class DesignSchool extends Building {
    private static MapLocation hqLoc;
    private int numLandscapers = 0;

    public DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (turnCount == 1){
            comms.broadcastBuildingCreation(RobotType.DESIGN_SCHOOL, myLoc);
        }

        if (numLandscapers < 9){
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.LANDSCAPER, dir)){
                    numLandscapers++;
                }
            }
        }
    }
}
