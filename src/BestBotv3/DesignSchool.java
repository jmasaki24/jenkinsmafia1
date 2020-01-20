package BestBotv3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

//Todo: set a hard limit of landscapers to make

public class DesignSchool extends Building {
    private boolean shouldMakeBuilders;

    public DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        
        if (rc.getTeamSoup()>=(3*RobotType.LANDSCAPER.cost)){
            shouldMakeBuilders = true;
        }
        if (shouldMakeBuilders){
            for (Direction dir: Util.directions){
                tryBuild(RobotType.LANDSCAPER,dir);
            }
        }
    }
}
