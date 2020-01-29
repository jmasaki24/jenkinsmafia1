package usqualbot;

import battlecode.common.*;
import java.util.ArrayList;


public class Robot {
    RobotController rc;
    Communications comms;
    Navigation nav;
    final int ARBITRARY_SOUP_NUMBER_LMAO = 750;

    int turnCount = 0;
    MapLocation myLoc;


    public Robot(RobotController r) {
        this.rc = r;
        comms = new Communications(rc);
        myLoc = rc.getLocation();
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
        myLoc = rc.getLocation();
//        comms.updateBuildingLocations();
    }

    // THIS METHOD REQUIRES THAT THE ROBOT CAN SEE ALL TILES ADJACENT TO SOUP!!!!!!!!!!!!!!!!!!!!!!!!!!
    // this method could be done MUCH MUCH better, I think. sorry in advance.
    // if a tile adjacent to soup is not flooded, it is accessible
    // returns true if soup is accessible 
    // TODO: 1/21/2020 Figure out if there is a direct path to the soup or not - matt
    // This no longer produces null pointer exceptions for miners with soup on the edge i think.
    public static ArrayList<MapLocation> surroundingLocs;
    public boolean isSoupAccessible(MapLocation soupLoc) throws GameActionException {
        for (int i = 0; i<8; i++){
            if (       soupLoc.add(Util.directions[i]).x <= rc.getMapWidth()
                    && soupLoc.add(Util.directions[i]).x >= 0
                    && soupLoc.add(Util.directions[i]).y <= rc.getMapHeight()
                    && soupLoc.add(Util.directions[i]).y >= 0){
                surroundingLocs.add(soupLoc.add(Util.directions[i]));
            }
        }

        boolean isAccessible = false;
        for (MapLocation loc : surroundingLocs) {


            // if tile is not flooded, doesn't have a building, then true

            // if there is flooding, don't bother checking for a building. saves bytecode
            if (nav.isOnMap(myLoc.directionTo(loc)) && !rc.senseFlooding(loc)) {
                boolean tileHasABuilding = false;
                RobotInfo robotOnLoc = rc.senseRobotAtLocation(loc);
                if (robotOnLoc != null) {
                    switch (robotOnLoc.type) {
                        case HQ:
                        case DESIGN_SCHOOL:
                        case FULFILLMENT_CENTER:
                        case NET_GUN:
                        case REFINERY:
                        case VAPORATOR:
                            tileHasABuilding = true;
                            break;
                        default: break;
                    }
                }

                // if there is no flooding and no building, than you can stand on that tile.
                // if you can stand on ONE adj. tile, then it is accessible
                isAccessible = isAccessible || !tileHasABuilding ;
            }

            // if its accessible, don't bother checking all of the other tiles.
            if (isAccessible) {
                return isAccessible;
            }
        }
        return isAccessible;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }
        return false;
    }
}