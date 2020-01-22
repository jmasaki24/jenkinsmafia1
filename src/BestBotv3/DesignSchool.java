package BestBotv3;

import battlecode.common.*;

//Todo: set a hard limit of landscapers to make

public class DesignSchool extends Building {
    private static MapLocation hqLoc;

    public DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if (rc.getTeamSoup()>=(2*RobotType.LANDSCAPER.cost)){
            for (Direction dir : Util.directions) {
                tryBuild(RobotType.LANDSCAPER, dir);
            }
        }
    }
}
