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

        unburyHQ();

        // System.out.println(myLoc.x + " " + myLoc.y);
        if(rc.onTheMap(hqLoc)){
            System.out.println("Can See hq");
            //Else move random (uses move limits to not go random every line)
            Direction rand = Util.randomDirection();
            if (myLoc.add(rand).distanceSquaredTo(hqLoc) < 3){ //Only move in directions where you end up on the wall
                System.out.println("Moving Random within distance of hq" + myLoc.add(rand).distanceSquaredTo(hqLoc));
                randMoveIfNoDesignSchool(rand);
            }
        } else {
            System.out.println("Can't see hq");
            nav.tryMove(Util.randomDirection());
        }

        if(rc.getDirtCarrying() == 0){
            //While we haven't digged, we should keep digging
            boolean digged = false;
            while(!digged){
                digged = DontDigTheWall();
            }
        }

        //DONT HAVE TO WAIT TO BUILD
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


        System.out.print(hqLoc);
//      otherwise try to get to the hq
        if (rc.canMove(myLoc.directionTo(hqLoc))){
            rc.move(myLoc.directionTo(hqLoc));
        }
}

    // ----------------------------------------------- METHODS SECTION ---------------------------------------------- \\

    void randMoveIfNoDesignSchool(Direction rand) throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
        boolean isThereADesignSchool = false;
        int numLandscapersNearby = 0;
        for (RobotInfo robot : nearbyRobots) {
            if (robot.type.equals(RobotType.LANDSCAPER)) {
                numLandscapersNearby++;
            } else if (robot.type.equals(RobotType.DESIGN_SCHOOL)) {
                isThereADesignSchool = true;
                break;
            }
        }

        // don't move so often!!! causes slower build -jm
        if (Math.random() < 0.6) {
            nav.tryMove(rand);
        }

//        if (isThereADesignSchool && numLandscapersNearby > 5) {
//            System.out.println("i want to move!!");
//            nav.tryMove(rand);
//        }
    }

    void unburyHQ() throws GameActionException {
        if (hqLoc.distanceSquaredTo(myLoc) < 3){
            if (rc.canDigDirt(myLoc.directionTo(hqLoc))){
                rc.digDirt(myLoc.directionTo(hqLoc));
            }
        }
    }

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

