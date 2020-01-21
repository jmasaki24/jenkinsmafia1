package BestBotv3;

import battlecode.common.*;

public class Landscaper extends Unit {

    public Landscaper(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        System.out.println(myLoc.x + " " + myLoc.y);

        if(rc.getDirtCarrying() == 0){
            tryDig();
        }

        //Wait 15 turns to build
        if (turnCount > 50) {
            for (int i = 0; i < 8; i++){ //8 times per turn
                MapLocation bestPlaceToBuildWall = null;
                // find best place to build
                if (hqLoc != null) {
                    int lowestElevation = 9999999;
                    for (Direction dir : Util.directions) {
                        MapLocation tileToCheck = hqLoc.add(dir);
                        if (myLoc.distanceSquaredTo(tileToCheck) < 4
                                && rc.canDepositDirt(myLoc.directionTo(tileToCheck))) {
                            if (rc.senseElevation(tileToCheck) < lowestElevation) {
                                lowestElevation = rc.senseElevation(tileToCheck);
                                bestPlaceToBuildWall = tileToCheck;
                            }
                        }
                    }
                }

                if (Math.random() < 0.4) {
                    // build the wall
                    if (bestPlaceToBuildWall != null) {
                        rc.depositDirt(myLoc.directionTo(bestPlaceToBuildWall));
                        rc.setIndicatorDot(bestPlaceToBuildWall, 0, 255, 0);
                        System.out.println("building a wall");
                    }
                }
            }
        }

        // otherwise try to get to the hq
        if(rc.onTheMap(hqLoc)){
            System.out.println("Can See hq");

            //Runs from the school
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared,rc.getTeam());
            MapLocation nextPlace = myLoc;
            for (RobotInfo robot:robots){
                if (robot.type == RobotType.DESIGN_SCHOOL){
                    nextPlace = nextPlace.add(myLoc.directionTo(robot.location).opposite());
                }
            }
            if(nextPlace == myLoc){
                nextPlace = nextPlace.add(Util.randomDirection());
            }
            if(nextPlace != myLoc) {
                if(myLoc.add(myLoc.directionTo(nextPlace)).distanceSquaredTo(hqLoc) < 3) { //Only move in directions where you end up on the wall
                    System.out.println("Going to next wall location" + myLoc.add(myLoc.directionTo(nextPlace)).distanceSquaredTo(hqLoc));
                    nav.tryMove(myLoc.directionTo(nextPlace));
                }
            }
            //Else move random (uses move limits to not go random every line)
            Direction rand = Util.randomDirection();
            if (myLoc.add(rand).distanceSquaredTo(hqLoc) < 3){ //Only move in directions where you end up on the wall
                System.out.println("Moving Random within distance of hq" + myLoc.add(rand).distanceSquaredTo(hqLoc));
                nav.tryMove(rand);
            }
        } else {
            System.out.println("Can't see hq");
            nav.tryMove(Util.randomDirection());
        }
    }

    boolean tryDig() throws GameActionException {
        Direction dir = Util.randomDirection();
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            rc.setIndicatorDot(myLoc.add(dir), 255, 0, 0);
            return true;
        }
        return false;
    }
}

