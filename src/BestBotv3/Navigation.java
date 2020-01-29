package BestBotv3;

import battlecode.common.*;

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
    void tryMove(Direction dir) throws GameActionException {
        bugPath(dir);
    }

    void tryFly(Direction dir) throws GameActionException {
        bugPath(dir);
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
    void goTo(Direction dir) throws GameActionException {
        bugPath(dir);
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
            if (rc.getType() != RobotType.DELIVERY_DRONE) {
                if (rc.canMove(dis) && !rc.senseFlooding(myLoc.add(dis))){
                    MapLocation potentialLoc = myLoc.add(dis);
                    if (closest < potentialLoc.distanceSquaredTo(target)){
                        closest = potentialLoc.distanceSquaredTo(target);
                        closestDir = dis;
                    }
                }
            } else {
                if (rc.canMove(dis)){
                    MapLocation potentialLoc = myLoc.add(dis);
                    if (closest < potentialLoc.distanceSquaredTo(target)){
                        closest = potentialLoc.distanceSquaredTo(target);
                        closestDir = dis;
                    }
                }
            }

        }
        if (closest > closestIveEverBeen){
            Direction dirToTarget = myLoc.directionTo(target);
            if (rc.getType() != RobotType.DELIVERY_DRONE) {
                if (rc.canMove(dirToTarget) && !rc.senseFlooding(myLoc.add(dirToTarget))) {
                    rc.move(dirToTarget);
                }
            } else {
                if (rc.canMove(dirToTarget)){
                    rc.move(dirToTarget);
                }
            }
            int count = 0;
            if (rc.getType() != RobotType.DELIVERY_DRONE) {
                while ((!rc.canMove(dirToTarget) && count <= 8) && !rc.senseFlooding(myLoc.add(dirToTarget))) {
                    dirToTarget = dirToTarget.rotateRight();
                    count++;
                }
            } else {
                while (!rc.canMove(dirToTarget) && count <= 8) {
                    dirToTarget = dirToTarget.rotateRight();
                    count++;
                }
            }
            if (dirToTarget == myLoc.directionTo(target)){
                Direction randomDir = Util.randomDirection();
                if (rc.canMove(randomDir)){
                    rc.move(randomDir);
                }
            } else {
                rc.move(dirToTarget);
            }
        } else{
            if (rc.canMove(closestDir)) {
                rc.move(closestDir);
            }
        }
    }

    void bugPath(Direction dir) throws GameActionException {
        bugPath(rc.getLocation().add(dir));
    }

//    //Is a copy of goTo that ignores water
//    boolean flyTo(Direction dir) throws GameActionException {
//
//        // if dir is north, order would be N, NW, NE, W, E, SW, SE, S
//        Direction[] fuzzyNavDirectionsInOrder = { dir, dir.rotateLeft(), dir.rotateRight(),
//                dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(),
//                dir.rotateLeft().rotateLeft().rotateLeft(), dir.rotateRight().rotateRight().rotateRight(),
//                dir.opposite(),
//        };
//
//        Direction moveToward = fuzzyNavDirectionsInOrder[0];
//        for (int i = 0; i < 8; i ++) {
//            moveToward = fuzzyNavDirectionsInOrder[i];
//            if (tryFly(moveToward)) {
//                return true;
//            }
//        }
////
////        for (Direction d : toTry){
////            if(tryMove(d))
////                return true;
////        }
//        return false;
//    }

    //Is a copy of goTo that ignores water
    void flyTo(Direction dir) throws GameActionException {
        bugPath(dir);
    }

    // navigate towards a particular location
    void goTo(MapLocation destination) throws GameActionException {
        bugPath(rc.getLocation().directionTo(destination));
    }

    // still a copy of goTo but for flying
    void flyTo(MapLocation destination) throws GameActionException {
        bugPath(rc.getLocation().directionTo(destination));
    }
}