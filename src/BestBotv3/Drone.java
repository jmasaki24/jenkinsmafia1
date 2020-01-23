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
    public ArrayList<Direction> helpDir = new ArrayList<>();
    public ArrayList<MapLocation> waterLocation = new ArrayList<>();



    public Drone(RobotController r) {
        super(r);
    }

    MapLocation standbyLocation;
    boolean onMission = false;
    boolean onHelpMission = false;
    RobotInfo targetBot = null;
    RobotInfo targetHelpBot = null;

    boolean findANewBot = false;


    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.updateAttackerDir(enemyDir);
        comms.updateWaterLocations(waterLocation);

        // goToEHQ works, but first we need a defensive drone.
        // gotoEHQ();

        // Enemy Detection
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo robot : nearbyEnemyRobots) {
            if ((robot.type.equals(RobotType.MINER) || robot.type.equals(RobotType.LANDSCAPER))){
                // If its on opponent team
                onMission = true;
                targetBot = robot;
                break;
            }
        }
        RobotInfo[] nearbyLandscapers = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robot : nearbyLandscapers) {
            if ((robot.type.equals(RobotType.LANDSCAPER))){
                // If its on opponent team
                onHelpMission = true;
                targetHelpBot = robot;
                break;
            }
        }

        // Setting Standby Location
        if (turnCount == 1) {
            if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top left
                standbyLocation = new MapLocation(hqLoc.x + 4, hqLoc.y - 4);
            } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y > (rc.getMapHeight() / 2)) { // top right
                standbyLocation = new MapLocation(hqLoc.x - 4, hqLoc.y - 4);
            } else if (hqLoc.x < (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom left
                standbyLocation= new MapLocation(hqLoc.x + 4, hqLoc.y + 4);
            } else if (hqLoc.x > (rc.getMapWidth() / 2) && hqLoc.y < (rc.getMapHeight() / 2)) { // bottom right
                standbyLocation = new MapLocation(hqLoc.x - 4, hqLoc.y + 4);
            } else {
                standbyLocation = myLoc;
            }
        }


        // If its holding a unit, sense if its near flooding and drop. If not, move randomly.
        if (rc.isCurrentlyHoldingUnit() && onMission){
            for (Direction dir: Util.directions){
                if (rc.senseFlooding(myLoc.add(dir))){
                    rc.dropUnit(dir);
                    targetBot = null;
                    onMission = false;
                    enemyDir = null;
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
        }


        // Does it need to find a new bot?
        else if (findANewBot){
            nav.goTo(myLoc.directionTo(hqLoc).opposite());
            if (myLoc.distanceSquaredTo(hqLoc) > 4){
                findANewBot = false;
            }
        }
        // If its holding a unit, can it drop it on the wall? If not, go the HQ.
        else if (rc.isCurrentlyHoldingUnit() && onHelpMission){
            for (Direction dir: Util.directions){
                if (myLoc.add(dir).distanceSquaredTo(hqLoc) <=2 && myLoc.add(dir).distanceSquaredTo(hqLoc) > 0){
                    if (rc.canDropUnit(dir)){
                        rc.dropUnit(dir);
                        targetHelpBot = null;
                        onHelpMission = false;
                        helpDir = null;
                        findANewBot = true;
                    }
                } else {
                    nav.goTo(hqLoc);
                }
            }
        }
        // I see a bot that needs help
        else if (targetHelpBot != null){
            // I am there
            if (myLoc.distanceSquaredTo(targetHelpBot.location) <=2){
                if (rc.canPickUpUnit(targetHelpBot.ID)){
                    rc.pickUpUnit(targetHelpBot.ID);
                    System.out.println("I should have picked this unit up" + targetHelpBot.ID);
                } else {
                    System.out.println("dude");
                }
            }
            // I'm not there yet
            else{
                nav.goTo(targetHelpBot.location);
            }
        }
        // HQ has seen a bot
        else if (helpDir.size() != 0){
            nav.goTo(hqLoc.add(helpDir.get(helpDir.size()-1)));
        }


        // Standby as last resort
        else if (myLoc.distanceSquaredTo(standbyLocation) > 2){
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
