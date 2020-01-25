package BestBotv3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Navigation {
    RobotController rc;

    // state related only to navigation should go here

    public Navigation(RobotController r) {
        rc = r;
    }
    
    //Attempts to move in a given direction
    boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir)) && isOnMap(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    boolean isOnMap(Direction dir){
        // Basically if the coordinates are less than dimension and greater than 0
        if (rc.getLocation().add(dir).x <= rc.getMapWidth() && rc.getLocation().add(dir).x >= 0 && rc.getLocation().add(dir).y <= rc.getMapHeight() && rc.getLocation().add(dir).y >= 0){
            return true;
        } else{
            return false;
        }
    }

    // tries to move in the general direction of dir
    boolean goTo(Direction dir) throws GameActionException {

        // if dir is north, order would be N, NW, NE, W, E, SW, SE, S
        Direction[] fuzzyNavDirectionsInOrder = { dir, dir.rotateLeft(), dir.rotateRight(),
                dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(), dir.rotateRight().rotateRight().rotateRight(),
                dir.opposite(),
        };

        Direction moveToward = fuzzyNavDirectionsInOrder[0];
        for (int i = 0; i < 8; i ++) {
            moveToward = fuzzyNavDirectionsInOrder[i];
            if (tryMove(moveToward)) {
                return true;
            }
        }
//
//        for (Direction d : toTry){
//            if(tryMove(d))
//                return true;
//        }
        return false;
    }
    // navigate towards a particular location
    boolean goTo(MapLocation destination) throws GameActionException {
        return goTo(rc.getLocation().directionTo(destination));
    }


    // Same as above, but doesnt care about flooding
    boolean droneGoTo(Direction dir) throws GameActionException {

        // if dir is north, order would be N, NW, NE, W, E, SW, SE, S
        Direction[] fuzzyNavDirectionsInOrder = { dir, dir.rotateLeft(), dir.rotateRight(),
                dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(), dir.rotateRight().rotateRight().rotateRight(),
                dir.opposite(),
        };

        Direction moveToward = fuzzyNavDirectionsInOrder[0];
        for (int i = 0; i < 8; i ++) {
            moveToward = fuzzyNavDirectionsInOrder[i];
            if (tryDroneMove(moveToward)) {
                return true;
            }
        }
//
//        for (Direction d : toTry){
//            if(tryDroneMove(d))
//                return true;
//        }
        return false;
    }
    boolean droneGoTo(MapLocation destination) throws GameActionException {
        return droneGoTo(rc.getLocation().directionTo(destination));
    }
    boolean tryDroneMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && isOnMap(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}