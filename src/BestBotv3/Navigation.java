package BestBotv3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Map;

public class Navigation {
    RobotController rc;
    MapLocation lastTarget;
    int closestIveEverBeen = 888888888;
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

    boolean tryFly(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && isOnMap(dir)) {
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

    void bugPath(MapLocation target) throws GameActionException {

        //Specify Long Term Target
        if (lastTarget == null){
            lastTarget = target;
        } else{
            if (target != lastTarget){
                closestIveEverBeen = 888888888;
            }
        }
        MapLocation myLoc = rc.getLocation();

        //Remember the closest Ive Ever Been
        if (closestIveEverBeen == 888888888){
            closestIveEverBeen = myLoc.distanceSquaredTo(target);
        }

        int closest = closestIveEverBeen;
        Direction closestDir = Direction.CENTER;

        for (Direction dis: Util.directions){
            if (rc.canMove(dis)){
                MapLocation potentialLoc = myLoc.add(dis);
                if (closest < potentialLoc.distanceSquaredTo(target)){
                    closest = potentialLoc.distanceSquaredTo(target);
                    closestDir = dis;
                }
            }
        }
        if (closest > closestIveEverBeen){
            Direction dirToTarget = myLoc.directionTo(target);
            if (rc.canMove(dirToTarget)){
                rc.move(dirToTarget);
            } else {
                int count = 0;
                while(!rc.canMove(dirToTarget) && count <= 8){
                    dirToTarget = dirToTarget.rotateRight();
                    count++;
                }
                if (dirToTarget == myLoc.directionTo(target)){
                    Direction randomDir = Util.randomDirection();
                    if (rc.canMove(randomDir)){
                        rc.move(randomDir);
                    }
                } else {
                    rc.move(dirToTarget);
                }
            }
        } else{
            if (rc.canMove(closestDir))
                rc.move(closestDir);
        }
    }

    //Is a copy of goTo that ignores water
    boolean flyTo(Direction dir) throws GameActionException {

        // if dir is north, order would be N, NW, NE, W, E, SW, SE, S
        Direction[] fuzzyNavDirectionsInOrder = { dir, dir.rotateLeft(), dir.rotateRight(),
                dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(), dir.rotateRight().rotateRight().rotateRight(),
                dir.opposite(),
        };

        Direction moveToward = fuzzyNavDirectionsInOrder[0];
        for (int i = 0; i < 8; i ++) {
            moveToward = fuzzyNavDirectionsInOrder[i];
            if (tryFly(moveToward)) {
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

    // still a copy of goTo but for flying
    boolean flyTo(MapLocation destination) throws GameActionException {
        return flyTo(rc.getLocation().directionTo(destination));
    }
}