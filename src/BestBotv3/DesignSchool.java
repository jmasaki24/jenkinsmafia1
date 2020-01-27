package BestBotv3;

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

        int buildLandscaperSoupLimit = RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost + 5;

        // once we build a landscaper miners won't use hq as a refinery, so we should make sure we enough soup
        //      to make a refinery once we make a landscaper. after that it's all good.
        // + 5 is for broadcast stuff.
        if (Unit.refineryLocations.size() >= 1) {
            System.out.println("one+ refineries");
            buildLandscaperSoupLimit = RobotType.LANDSCAPER.cost + 5;
        }

        if (numLandscapers < 8){
            if (rc.getTeamSoup() > buildLandscaperSoupLimit){
                for (Direction dir : Util.directions) {
                    if (tryBuild(RobotType.LANDSCAPER, dir)){
                        numLandscapers++;
                    }
                }
            }
        }
    }
}
