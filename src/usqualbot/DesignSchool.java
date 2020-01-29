package usqualbot;

import battlecode.common.*;

//Todo: set a hard limit of landscapers to make

public class DesignSchool extends Building {
    private static MapLocation hqLoc;
    private final int landscaperLimit = 9;
    private int numLandscapers = 0;

    public DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (turnCount == 1){
            // this happens in Miner.java instead. wasn't working here.
//            comms.broadcastBuildingCreation(RobotType.DESIGN_SCHOOL, myLoc);
        }

        comms.updateBuildingLocations();

        if (Unit.refineryLocations.size() >= 1) {
            System.out.println("two+ refineries");
        }

        if (numLandscapers < 8){
            if (rc.getTeamSoup() > RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost){
                for (Direction dir : Util.directions) {
                    if (tryBuild(RobotType.LANDSCAPER, dir)){
                        numLandscapers++;
                    }
                }
            }
        }
    }
}
