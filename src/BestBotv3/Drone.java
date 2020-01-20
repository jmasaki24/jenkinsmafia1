package BestBotv3;

import battlecode.common.*;

// so our defensive drone has a few states
// 1. not doing anything. go near standbyLocation
// 2. sees a target (i.e. has a targetBot). go to targetBot's location
// 3. is carrying a bot. go to water?

public class Drone extends Unit{

    //Vars
    boolean shouldMove = true;
    int hqToCheck = 0;
    MapLocation[] potentialHQ;

    public Drone(RobotController r) {
        super(r);
    }

    boolean onMission = false;
    RobotInfo targetBot = null;

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        // goToEHQ works, but first we need a defensive drone.
        // gotoEHQ();

//        MapLocation standbyLocation;
//
//        if (turnCount == 1) {
//            if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top left
//                standbyLocation = new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
//            } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top right
//
//                standbyLocation = new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
//            } else if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom left
//
//                standbyLocation= new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
//            } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom right
//
//                standbyLocation = new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
//            } else {
//                standbyLocation = myLoc;
//            }
//        }
//        if (!onMission && myLoc.distanceSquaredTo(standbyLocation) < 4 ) {
//            nav.goTo(standbyLocation);
//        }
//
//        if (targetBot != null && onMission) {
//            if (rc.canPickUpUnit(targetBot.ID)) {
//                rc.pickUpUnit(targetBot.ID);
//            }
//        }
//
//        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
//        for (RobotInfo robot : nearbyRobots) {
//            if (robot.type.equals(RobotType.MINER)) {
//                onMission = true;
//                targetBot = robot;
//                nav.goTo(robot.location);
//            }
//        }
    }

    void goToEHQ() throws GameActionException {
        shouldMove = true;

        findEHQ();

        MapLocation[] potentialHQ = new MapLocation[] {new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (hqLoc.y) - 1),
                new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (rc.getMapHeight() - hqLoc.y) - 1),
                new MapLocation((hqLoc.x) - 1                   , (rc.getMapHeight() - hqLoc.y) - 1)};
        for (MapLocation loc: potentialHQ){
            rc.setIndicatorDot(loc,0,200,200);
        }


        if (rc.getID()%2 == 0){
            if(EHqLoc.x > 0 || EHqLoc.y > 0){
                System.out.println("Found ENEMY HQ");
                if (myLoc.distanceSquaredTo(EHqLoc) > 5){
                    System.out.println("Going to ENEMY HQ:" + EHqLoc);
                    nav.tryMove(myLoc.directionTo(EHqLoc));
                } else{
                    System.out.println("Standing my gound at ENEMY HQ");
                    for (Direction dir: Util.directions){
                        tryBuild(RobotType.NET_GUN,dir);
                    }
                    shouldMove = false;
                }
            }

            if(myLoc.distanceSquaredTo(potentialHQ[hqToCheck]) > 5){
                System.out.println("Going to a potential HQ:" + potentialHQ);
                if(shouldMove)
                    nav.tryMove(myLoc.directionTo(potentialHQ[hqToCheck]));
                rc.setIndicatorLine(rc.getLocation(),potentialHQ[hqToCheck],0,230,0);
            } else{
                System.out.println("Nothing Here at potential HQ:" + potentialHQ);
                hqToCheck += 1;
            }
        }

    }

}
