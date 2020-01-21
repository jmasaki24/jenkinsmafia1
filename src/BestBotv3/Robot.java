package BestBotv3;

import battlecode.common.*;

public class Robot {
    RobotController rc;
    Communications comms;

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
    }

    // THIS METHOD REQUIRES THAT THE ROBOT CAN SEE ALL TILES ADJACENT TO SOUP!!!!!!!!!!!!!!!!!!!!!!!!!!
    // this method could be done MUCH MUCH better, I think. sorry in advance.
    // if a tile adjacent to soup is not flooded, it is accessible
    // returns true if soup is accessible
    public boolean isSoupAccessible(MapLocation soupLoc) throws GameActionException {
        MapLocation[] surroundingLocs = {
                soupLoc.add(Util.directions[0]), soupLoc.add(Util.directions[1]),
                soupLoc.add(Util.directions[2]), soupLoc.add(Util.directions[3]),
                soupLoc.add(Util.directions[4]), soupLoc.add(Util.directions[5]),
                soupLoc.add(Util.directions[6]), soupLoc.add(Util.directions[7]),
        };

        boolean isAccessible = false;
        for (MapLocation loc : surroundingLocs) {

            // if tile is not flooded, doesn't have a building, then true

            // if there is flooding, don't bother checking for a building. saves bytecode
            if (!rc.senseFlooding(loc)) {
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