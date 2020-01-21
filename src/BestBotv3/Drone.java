package BestBotv3;

import battlecode.common.*;

import java.util.ArrayList;

// so our defensive drone has a few states
// 1. not doing anything. go near standbyLocation
// 2. sees a target (i.e. has a targetBot). go to targetBot's location
// 3. is carrying a bot. go to water?



public class Drone extends Unit{

    // Vars
    boolean shouldMove = true;
    int hqToCheck = 0;
    MapLocation[] potentialHQ;
    public ArrayList<Direction> enemyDir = new ArrayList<>();
    public ArrayList<MapLocation> waterLocation = new ArrayList<>();



    public Drone(RobotController r) {
        super(r);
    }

    MapLocation standbyLocation;
    boolean onMission = false;
    RobotInfo targetBot = null;

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.updateAttackerDir(enemyDir);
        comms.updateWaterLocations(waterLocation);

        // goToEHQ works, but first we need a defensive drone.
        // gotoEHQ();

        // Enemy Detection
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo robot : nearbyRobots) {
            if ((robot.type.equals(RobotType.MINER) || robot.type.equals(RobotType.LANDSCAPER)) && robot.getTeam() == rc.getTeam().opponent()) {
                // If its on opponent team
                onMission = true;
                targetBot = robot;
                break;
            }
        }

        // Setting Standby Location
        if (turnCount == 1) {
            if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top left
                standbyLocation = new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
            } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top right
                standbyLocation = new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
            } else if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom left

                standbyLocation= new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
            } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom right

                standbyLocation = new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
            } else {
                standbyLocation = myLoc;
            }
        }


        // If its holding a unit, sense if its near flooding and drop. If not, move randomly.
        if (rc.isCurrentlyHoldingUnit()){
            for (Direction dir: Util.directions){
                if (rc.senseFlooding(myLoc.add(dir))){
                    rc.dropUnit(dir);
                } else{
                    if (waterLocation.size() != 0){
                        nav.goTo(waterLocation.get(waterLocation.size()-1));
                    } else{
                        rc.move(Util.randomDirection());
                    }
                }
            }
        }
        // I see a bot
        else if (targetBot != null){
            // I am there
            if (myLoc.distanceSquaredTo(targetBot.location) <=2){
                if (rc.canPickUpUnit(targetBot.ID)){
                    rc.pickUpUnit(targetBot.ID);
                    System.out.println("I should have picked this unit up" + targetBot.ID);
                } else {
                    System.out.println("dude");
                }

            }
            // I'm not there yet
            else{
                nav.goTo(targetBot.location);
            }
        }
        // HQ has seen a bot
        else if (enemyDir.size() != 0){
            nav.goTo(hqLoc.add(enemyDir.get(enemyDir.size()-1)));
        } else if (myLoc.distanceSquaredTo(standbyLocation) > 2){
            nav.goTo(standbyLocation);
        }
    }







    // ----------------------------------------------- METHODS SECTION ---------------------------------------------- \\

    public void goToEHQ() throws GameActionException {
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
                rc.setIndicatorLine(myLoc,potentialHQ[hqToCheck],0,230,0);
            } else{
                System.out.println("Nothing Here at potential HQ:" + potentialHQ);
                hqToCheck += 1;
            }
        }

    }

}
