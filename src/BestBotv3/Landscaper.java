package BestBotv3;

import battlecode.common.*;

import java.util.Map;

public class Landscaper extends Unit {

    public Landscaper(RobotController r) {
        super(r);
    }

    //Vars
    MapLocation bestPlaceToBuildWall;

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        // System.out.println(myLoc.x + " " + myLoc.y);


        if(rc.getDirtCarrying() == 0){
            //While we haven't digged, we should keep digging
            boolean digged = false;
            while(!digged){
                digged = DontDigTheWall();
            }
        }

        //DONT HAVE TO WAIT TO BUILD
        if (turnCount > 0) {
            for (int i = 0; i < 8; i++){ //8 times per turn
                bestPlaceToBuildWall = null;
                // find best place to build
                findBestPlaceToBuild();

                // build the wall
                if (bestPlaceToBuildWall != null) {
                    rc.depositDirt(myLoc.directionTo(bestPlaceToBuildWall));
                    rc.setIndicatorDot(bestPlaceToBuildWall, 0, 255, 0);
                    // System.out.println("building a wall");
                }
            }
        }

        // otherwise try to get to the hq
        if(rc.onTheMap(hqLoc)){
            // System.out.println("Can See hq");
            if (myLoc.distanceSquaredTo(hqLoc) > 2){
                nav.goTo(hqLoc.add(myLoc.directionTo(hqLoc)));
            } else { // In the circle
                if (!nav.tryMove(myLoc.directionTo(hqLoc).rotateLeft())){
                    Direction toHQ = myLoc.directionTo(hqLoc);
                    Direction next = Direction.CENTER;
                    switch (toHQ){
                        case NORTH:         next = Direction.WEST;      break;
                        case EAST:          next = Direction.NORTH;     break;
                        case SOUTH:         next = Direction.EAST;      break;
                        case WEST:          next = Direction.SOUTH;     break;
                        default:            next = Direction.CENTER;    break;
                    }
                    nav.tryMove(next);
                }
            }
            //Else move random (uses move limits to not go random every line)
            Direction rand = Util.randomDirection();
            if (myLoc.add(rand).distanceSquaredTo(hqLoc) < 3){ //Only move in directions where you end up on the wall
                // System.out.println("Moving Random within distance of hq" + myLoc.add(rand).distanceSquaredTo(hqLoc));
                nav.tryMove(rand);
            }
        } else { //If we don't see the hq
            // System.out.println("Can't see hq");
            nav.tryMove(Util.randomDirection());
        }
    }

    // ----------------------------------------------- METHODS SECTION ---------------------------------------------- \\

    boolean DontDigTheWall() throws GameActionException {
        Direction randomDir = Util.randomDirection();
        // System.out.println("Direction Chosen" + randomDir);
        // System.out.println("Distance to HQ" + myLoc.add(randomDir).distanceSquaredTo(hqLoc));
        if (myLoc.add(randomDir).distanceSquaredTo(hqLoc) > 2){
            if (rc.canDigDirt(randomDir)) {
                rc.digDirt(randomDir);
                rc.setIndicatorDot(myLoc.add(randomDir) ,0,0,250);
                return true;
            }
        }
        return false;
    }

    void findBestPlaceToBuild() throws GameActionException {
        if (hqLoc != null) {
            int lowestElevation = 9999999;
            for (Direction dir : Util.directions) {
                MapLocation tileToCheck = hqLoc.add(dir);
                if (myLoc.distanceSquaredTo(tileToCheck) < 2
                        && rc.canDepositDirt(myLoc.directionTo(tileToCheck))) {
                    if (rc.senseElevation(tileToCheck) < lowestElevation) {
                        lowestElevation = rc.senseElevation(tileToCheck);
                        bestPlaceToBuildWall = tileToCheck;
                    }
                }
            }
        }
    }
}

