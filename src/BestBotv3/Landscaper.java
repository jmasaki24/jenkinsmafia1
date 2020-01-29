package BestBotv3;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Landscaper extends Unit {

    public Landscaper(RobotController r) {
        super(r);
    }

    //Vars
    MapLocation bestPlaceToBuildWall;
    boolean isSmallWallBuilder = false;
    boolean isHashtagBuilder = false;

    ArrayList<MapLocation> possibleDigLocations = new ArrayList<MapLocation>();

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        System.out.println("hqloc is " + hqLoc);
        System.out.println("turncount " + turnCount);
        if (turnCount == 1) {
            if (landscapers_ids_us.size() <= 8 ) {
                isSmallWallBuilder = true;
            } else {
                isHashtagBuilder = true;
            }
        }
        if (myLoc.distanceSquaredTo(hqLoc) <= 2) {
            isSmallWallBuilder = true;
            isHashtagBuilder = false;
        }

        if (turnCount == 3) {
            possibleDigLocations.add(new MapLocation(hqLoc.x - 2, hqLoc.y));
            possibleDigLocations.add(new MapLocation(hqLoc.x, hqLoc.y + 2));
            possibleDigLocations.add(new MapLocation(hqLoc.x + 2, hqLoc.y));
            possibleDigLocations.add(new MapLocation(hqLoc.x, hqLoc.y - 2));
            possibleDigLocations.removeIf(loc -> !rc.onTheMap(loc));
            for (MapLocation loc: possibleDigLocations) {
                System.out.println("dig: " + loc);
            }
        } else if (turnCount == 15) {
            hashtagLocations.add(new MapLocation((hqLoc.x - 2), (hqLoc.y + 1)));
            hashtagLocations.add(new MapLocation((hqLoc.x - 1), (hqLoc.y + 2)));
            hashtagLocations.add(new MapLocation((hqLoc.x + 1), (hqLoc.y + 2)));
            hashtagLocations.add(new MapLocation((hqLoc.x + 2), (hqLoc.y + 1)));
            hashtagLocations.add(new MapLocation((hqLoc.x + 2), (hqLoc.y - 1)));
            hashtagLocations.add(new MapLocation((hqLoc.x + 1), (hqLoc.y - 2)));
            hashtagLocations.add(new MapLocation((hqLoc.x - 1), (hqLoc.y - 2)));
            hashtagLocations.add(new MapLocation((hqLoc.x - 2), (hqLoc.y - 1)));
            hashtagLocations.removeIf(loc -> !rc.onTheMap(loc));
            for (MapLocation loc: possibleDigLocations) {
                System.out.println("hshtg: " + loc);
            }
        }


        unburyHQ();

        if (isSmallWallBuilder) {
            doSmallWallBuilderStuff();
        } else {
            doHashtagWallBuilderStuff();
        }




        System.out.print(hqLoc);
//      otherwise try to get to the hq
        if (rc.canMove(myLoc.directionTo(hqLoc))){
            rc.move(myLoc.directionTo(hqLoc));
        }
}

    // ----------------------------------------------- METHODS SECTION ---------------------------------------------- \\

    void doSmallWallBuilderStuff() throws GameActionException {
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
                digged = digInCardinalDirections();
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
    }

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

    ArrayList<MapLocation> hashtagLocations = new ArrayList<MapLocation>();
    void doHashtagWallBuilderStuff() throws GameActionException {
        for (MapLocation loc: hashtagLocations) {
            if (rc.canSenseLocation(loc)) {
                RobotInfo botmaybe = rc.senseRobotAtLocation(loc);
                if (botmaybe != null && botmaybe.type.equals(RobotType.LANDSCAPER)) {
                    hashtagLocations.remove(loc);
                }
            }
        }

        if (hashtagLocations.size() > 0) {
            MapLocation closestHashLoc = hashtagLocations.get(0);
            for (MapLocation loc: hashtagLocations) {
                if (myLoc.distanceSquaredTo(closestHashLoc) > myLoc.distanceSquaredTo(loc)) {
                    closestHashLoc = loc;
                }
            }

            System.out.println("hshtg: " + closestHashLoc);
            nav.goTo(closestHashLoc);
        } else {
            nav.goTo(hqLoc);
        }

    }

    void unburyHQ() throws GameActionException {
        if (hqLoc.distanceSquaredTo(myLoc) < 3){
            if (rc.canDigDirt(myLoc.directionTo(hqLoc))){
                rc.digDirt(myLoc.directionTo(hqLoc));
            }
        }
    }

    // made to replace DontDigTheWall()
    boolean digInCardinalDirections() throws GameActionException {
        for (MapLocation digLoc : possibleDigLocations) {
            if (myLoc.distanceSquaredTo(digLoc) <= 2) {
                Direction dirToDigSpot = rc.getLocation().directionTo(digLoc);
                if (rc.canDigDirt(dirToDigSpot)) {
                    rc.digDirt(dirToDigSpot);
                    rc.setIndicatorDot(myLoc.add(dirToDigSpot) ,0,0,250);
                    return true;
                }
            }
        }

        return true;


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

