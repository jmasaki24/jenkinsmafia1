package BestBotv3;

import battlecode.common.*;

import java.util.ArrayList;

// so our defensive drone has a few states
// 1. not doing anything. go near standbyLocation
// 2. sees a target (i.e. has a targetBot). go to targetBot's location
// 3. is carrying a bot. go to water?
// 4. Sees a landscaper that needs help
// 5. Drops landscaper off on wall



public class Drone extends Unit{

    // Vars
    boolean shouldMove = true;
    int hqToCheck = 0;
    MapLocation[] potentialHQ;
    public ArrayList<Direction> enemyDir = new ArrayList<>();
    public ArrayList<Direction> helpDir = new ArrayList<>();
    public ArrayList<Direction> bootDir = new ArrayList<>();
    public ArrayList<MapLocation> waterLocation = new ArrayList<>();



    public Drone(RobotController r) {
        super(r);
    }

    boolean onMission = false;
    boolean onHelpMission = false;
    boolean onBootMission = false;
    RobotInfo targetBot = null;
    RobotInfo targetHelpBot = null;
    RobotInfo targetBootBot = null;

    boolean findANewBot = false;
    boolean findANewBootBot = false;


    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.updateAttackerDir(enemyDir);

        // Water Locations isnt updated right now
        // comms.updateWaterLocations(waterLocation);

        // goToEHQ works, but first we need a defensive drone.
        // gotoEHQ();


        if (RobotPlayer.turnCount > 10) { //  Wait until turn 11 to start doing stuff.
            // If its holding a unit, sense if its near flooding and drop. If not, move randomly.
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
                if (robot.type.equals(RobotType.LANDSCAPER)){
                    // If its a landscaper on our team

                    if (hqLoc != null){
                        // If we do know where the HQ is, dont move the robots that are in the right place
                        if (robot.location.distanceSquaredTo(hqLoc) > 2){
                            onHelpMission = true;
                            targetHelpBot = robot;
                        }
                    } else {
                        // If we dont know where the HQ is, just dont bother.
                        onHelpMission = true;
                        targetHelpBot = robot;
                        break;
                    }
                }
            }

            RobotInfo[] nearbyBootMiners = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam());
            for (RobotInfo robot : nearbyBootMiners) {
                if (robot.type.equals(RobotType.MINER)){
                    // If its a miner on our team
                    if (hqLoc != null){
                        // Only go for the robots that are too close to the HQ
                        if (robot.location.distanceSquaredTo(hqLoc) <= 2){
                            System.out.println("Theres a boot bot near HQ");
                            onBootMission = true;
                            targetBootBot = robot;
                        } else{
                            targetBootBot = null;
                            System.out.println("No boot bot near HQ");
                        }
                    } else{
                        targetBootBot = null;
                        System.out.println("No boot bot cause no HQ");
                    }
                }
            }


// KILL
            if (rc.isCurrentlyHoldingUnit() && onMission) {
                for (Direction dir : Util.directions) {
                    if (rc.senseFlooding(myLoc.add(dir))) {
                        rc.dropUnit(dir);
                        targetBot = null;
                        onMission = false;
                        enemyDir = null;
                    } else {
                        if (waterLocation.size() != 0) {
                            nav.droneGoTo(waterLocation.get(waterLocation.size() - 1));
                        } else {
                            rc.move(Util.randomDirection());
                        }
                    }
                }
            }
            // I see a bot
            else if (targetBot != null) {
                // I am there
                if (myLoc.distanceSquaredTo(targetBot.location) <= 2) {
                    if (rc.canPickUpUnit(targetBot.ID)) {
                        rc.pickUpUnit(targetBot.ID);
                        System.out.println("I should have picked this unit up" + targetBot.ID);
                    } else {
                        System.out.println("dude");
                    }

                }
                // I'm not there yet
                else {
                    nav.droneGoTo(targetBot.location);
                }
            }
            // HQ has seen a bot
            else if (enemyDir.size() != 0) {
                nav.droneGoTo(hqLoc.add(enemyDir.get(enemyDir.size() - 1)));
            }


// BOOT
            // Does it need to find a new bot to BOOT?
            else if (findANewBootBot) {
                nav.droneGoTo(myLoc.directionTo(hqLoc).opposite().rotateRight());
                if (myLoc.distanceSquaredTo(hqLoc) > 4) {
                    findANewBootBot = false;
                }
            }
            // If its holding a booted miner, can it get rid of it? If not, go away from HQ.
            else if (rc.isCurrentlyHoldingUnit() && onBootMission) {
                for (Direction dir : Util.directions) {
                    if (myLoc.add(dir).distanceSquaredTo(hqLoc) > 2) {
                        if (rc.canDropUnit(dir)) {
                            rc.dropUnit(dir);
                            System.out.println("I dropped the miner in the water!");
                            targetBootBot = null;
                            onBootMission = false;
                            bootDir = null;
                            findANewBootBot = true;
                            nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
                        }
                    } else {
                        nav.droneGoTo(hqLoc);
                    }
                }
            }
            // I see a bot that needs to be BOOTED
            else if (targetBootBot != null) {
                // I am there
                if (myLoc.distanceSquaredTo(targetBootBot.location) <= 2) {
                    if (rc.canPickUpUnit(targetBootBot.ID)) {
                        rc.pickUpUnit(targetBootBot.ID);
                        System.out.println("I should have picked this unit up" + targetBootBot.ID);
                    } else {
                        System.out.println("dude cant boot");
                    }
                }
                // I'm not there yet
                else {
                    nav.droneGoTo(targetBootBot.location);
                }
            }
            // HQ has seen a bot that needs to be BOOTED
//            else if (bootDir.size() != 0) {
//                nav.droneGoTo(hqLoc.add(bootDir.get(bootDir.size() - 1)));
//            } // Boot dir doesnt do anything right now.


// HELP
            // Does it need to find a new bot?
            else if (findANewBot) {
                nav.droneGoTo(myLoc.directionTo(hqLoc).opposite().rotateRight());
                if (myLoc.distanceSquaredTo(hqLoc) > 4) {
                    findANewBot = false;
                }
            }
            // If its holding a unit, can it drop it on the wall? If not, go the HQ.
            else if (rc.isCurrentlyHoldingUnit() && onHelpMission) {
                for (Direction dir : Util.directions) {
                    if (myLoc.add(dir).distanceSquaredTo(hqLoc) <= 2 && myLoc.add(dir).distanceSquaredTo(hqLoc) > 0) {
                        if (rc.canDropUnit(dir)) {
                            rc.dropUnit(dir);
                            System.out.println("I dropped the unit!");
                            targetHelpBot = null;
                            onHelpMission = false;
                            helpDir = null;
                            findANewBot = true;
                            nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
                        }
                    } else {
                        nav.droneGoTo(hqLoc);
                    }
                }
            }
            // I see a bot that needs help
            else if (targetHelpBot != null) {
                // I am there
                if (myLoc.distanceSquaredTo(targetHelpBot.location) <= 2) {
                    if (rc.canPickUpUnit(targetHelpBot.ID)) {
                        rc.pickUpUnit(targetHelpBot.ID);
                        System.out.println("I should have picked this unit up" + targetHelpBot.ID);
                    } else {
                        System.out.println("dude");
                    }
                }
                // I'm not there yet
                else {
                    nav.droneGoTo(targetHelpBot.location);
                }
            }
            // HQ has seen a bot
//            else if (helpDir.size() != 0) {
//                nav.droneGoTo(hqLoc.add(helpDir.get(helpDir.size() - 1)));
//            }   // Help dir doesnt do anything right now.


// STANDBY
            // Setting Standby Location to constantly change
            else if (hqLoc != null){
                if (myLoc.distanceSquaredTo(hqLoc) <= 10 && myLoc.distanceSquaredTo(hqLoc) >= 8){
                    for (Direction dir : Direction.allDirections()){
                        if (myLoc.add(dir).distanceSquaredTo(hqLoc)>=8 && myLoc.add(dir).distanceSquaredTo(hqLoc) <=10 && dir != myLoc.directionTo(hqLoc).rotateRight()){
                            nav.droneGoTo(dir);
                        }
                    }
                } else if (myLoc.distanceSquaredTo(hqLoc) < 8){
                    nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
                } else if (myLoc.distanceSquaredTo(hqLoc) > 10){
                    nav.droneGoTo(myLoc.directionTo(hqLoc));
                }
            } else{
                findHQ();
            }
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
