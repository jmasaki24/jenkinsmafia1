package BestBotv3;

import battlecode.common.*;

import java.util.ArrayList;

// so our defensive drone has a few states
// 1. not doing anything. go near standbyLocation
// 2. sees a target (i.e. has a targetEnemyBot). go to targetEnemyBot's location
// 3. is carrying a bot. go to water?
// 4. Sees a landscaper that needs help
// 5. Drops landscaper off on wall



public class Drone extends Unit{

    // Vars
    boolean shouldMove = true;
    int hqToCheck = 0;
    MapLocation[] potentialHQ;
    MapLocation standbyLocation;
    public ArrayList<Direction> enemyDir = new ArrayList<>();
    public ArrayList<Direction> helpDir = new ArrayList<>();
    public ArrayList<Direction> bootDir = new ArrayList<>();
    public ArrayList<MapLocation> waterLocation = new ArrayList<>();



    public Drone(RobotController r) {
        super(r);
    }

    boolean onEnemyMission = false;
    boolean onHelpMission = false;
    boolean onBootMission = false;

    RobotInfo targetEnemyBot = null;
    RobotInfo targetHelpBot = null;
    RobotInfo targetBootBot = null;

    boolean findANewEnemyBot = false;
    boolean findANewHelpBot = false;
    boolean findANewBootBot = false;


    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.updateAttackerDir(enemyDir);

        // Water Locations isnt updated right now
        // comms.updateWaterLocations(waterLocation);

        // goToEHQ works, but first we need a defensive drone.
        // gotoEHQ();

        if (RobotPlayer.turnCount > 10) { //  Wait until turn 11 to start doing stuff.

            // Unit Detection
            getNearbyEnemyBots();
            getNearbyLandscapers();
            getNearbyBootMiners();

            //State your Mission:
            if (onEnemyMission){
                System.out.println("I am getting rid of the enemy!");
            }
            if (onHelpMission){
                System.out.println("I'm helping to build the wall!");
            }
            if (onBootMission){
                System.out.println("I am on a BOOT mission");
            }


// KILL
            // We have a bot of some sort and have been told to kill it
            if (rc.isCurrentlyHoldingUnit() && onEnemyMission) {
                // First try to drop if in the water.
                for (Direction dir : Util.directions) {
                    if (rc.senseFlooding(myLoc.add(dir))) {
                        rc.dropUnit(dir);
                        targetEnemyBot = null;
                        onEnemyMission = false;
                        enemyDir = null;
                        System.out.println("Dropped");
                        break;
                    }
                    // If I can't, go to some water
                    else {
                        if (waterLocation.size() != 0) {
                            nav.droneGoTo(waterLocation.get(waterLocation.size() - 1));
                            System.out.println("Going to known water");
                            break;
                        } else {
                            nav.droneGoTo(Util.randomDirection());
                            System.out.println("Randomly going to water");
                            break;
                        }
                    }
                }
            }
            // I see a bot that is an enemy and want to pick it up
            else if (targetEnemyBot != null) {
                // I am at the bot
                if (myLoc.distanceSquaredTo(targetEnemyBot.location) <= 2) {
                    if (rc.canPickUpUnit(targetEnemyBot.ID)) {
                        rc.pickUpUnit(targetEnemyBot.ID);
                        System.out.println("I picked this unit up: " + targetEnemyBot.ID);
                    } else {
                        System.out.println("I cannot pick up this unit: " + targetEnemyBot.ID);
                    }
                }
                // I'm not there yet
                else {
                    nav.droneGoTo(targetEnemyBot.location);
                    System.out.println("I am going towards the bot I saw");
                }
            }
            // HQ has seen a bot
            else if (enemyDir.size() != 0) {
                nav.droneGoTo(hqLoc.add(enemyDir.get(enemyDir.size() - 1)));
                System.out.println("I am going to the bot the HQ saw");
            }



// BOOT
            // Does it need to find a new bot to BOOT?
            else if (findANewBootBot) {
                //Good fuzzy nav
                if (!nav.droneGoTo(myLoc.directionTo(hqLoc).opposite())){
                    nav.droneGoTo(Util.randomDirection());
                }
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
                            targetBootBot = null;
                            onBootMission = false;
                            bootDir = null;
                            findANewBootBot = true;
                            nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
                            System.out.println("I dropped the miner in the water!");
                            break;
                        }
                    } else {
                        nav.droneGoTo(hqLoc);
                        break;
                    }
                }
            }
            // I see a bot that needs to be BOOTED
            else if (targetBootBot != null) {
                // I am there
                if (myLoc.distanceSquaredTo(targetBootBot.location) <= 2) {
                    if (rc.canPickUpUnit(targetBootBot.ID)) {
                        rc.pickUpUnit(targetBootBot.ID);
                        System.out.println("I picked this unit up: " + targetBootBot.ID);
                    } else {
                        System.out.println("I cant pick up unit: " + targetBootBot.ID);
                    }
                }
                // I'm not there yet
                else {
                    nav.droneGoTo(targetBootBot.location);
                    System.out.println("I'm going to the boot bot");
                }
            }
            // HQ has seen a bot that needs to be BOOTED
//            else if (bootDir.size() != 0) {
//                nav.droneGoTo(hqLoc.add(bootDir.get(bootDir.size() - 1)));
//            } // Boot dir doesnt do anything right now.


// HELP
            // Does it need to find a new bot?
            else if (findANewHelpBot) {
                //Good fuzzy nav
                if (!nav.droneGoTo(myLoc.directionTo(hqLoc).opposite())){
                    nav.droneGoTo(Util.randomDirection());
                }
                if (myLoc.distanceSquaredTo(hqLoc) > 4) {
                    findANewHelpBot = false;
                }
            }
            // If its holding a unit, can it drop it on the wall? If not, go the HQ.
            else if (rc.isCurrentlyHoldingUnit() && onHelpMission) {
                for (Direction dir : Util.directions) {
                    if (myLoc.add(dir).distanceSquaredTo(hqLoc) <= 2 && myLoc.add(dir).distanceSquaredTo(hqLoc) > 0) {
                        if (rc.canDropUnit(dir)) {
                            rc.dropUnit(dir);
                            targetHelpBot = null;
                            onHelpMission = false;
                            helpDir = null;
                            findANewHelpBot = true;
                            nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
                            System.out.println("Help drop sucessful. Dropped unit: " + targetHelpBot.ID);
                            break;
                        } else {
                            System.out.println("Can't help drop it here");
                            if (!nav.droneGoTo(myLoc.directionTo(hqLoc).rotateRight())) {
                                nav.droneGoTo(myLoc.directionTo(hqLoc).rotateLeft());
                                break;
                            }
                            break;
                        }
                    }
                }
            }
            // I see a bot that needs help
            else if (targetHelpBot != null) {
                // I am there
                if (myLoc.distanceSquaredTo(targetHelpBot.location) <= 2) {
                    if (rc.canPickUpUnit(targetHelpBot.ID)) {
                        rc.pickUpUnit(targetHelpBot.ID);
                        System.out.println("I picked this unit up: " + targetHelpBot.ID);
                    } else {
                        System.out.println("I can't pick this unit up: " + targetHelpBot.ID);
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
                    for (Direction dir : Direction.allDirections()) {
                        if (myLoc.add(dir).distanceSquaredTo(hqLoc) >= 8 && myLoc.add(dir).distanceSquaredTo(hqLoc) <= 10) {
                            nav.droneGoTo(dir);
                        } else if (myLoc.distanceSquaredTo(hqLoc) < 8) {
                            nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
                        } else if (myLoc.distanceSquaredTo(hqLoc) > 10) {
                            nav.droneGoTo(myLoc.directionTo(hqLoc));
                        }
                    }
                }
            }
        }
    }


    // ----------------------------------------------- METHODS SECTION ---------------------------------------------- \\

    public void goToEHQ() throws GameActionException {
        //Define internal variables
        shouldMove = true;

        //Determine if we know enemy HQ Location
        findEHQ();

        //Define every potential EHQ location
        MapLocation[] potentialHQ = new MapLocation[] {new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (hqLoc.y) - 1),
                new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (rc.getMapHeight() - hqLoc.y) - 1),
                new MapLocation((hqLoc.x) - 1                   , (rc.getMapHeight() - hqLoc.y) - 1)};

        //Mark the potential loc with dots
        for (MapLocation loc: potentialHQ){
            rc.setIndicatorDot(loc,0,200,200);
        }

        //If we are a drone with an even robot id (random number... gets called ~50% of the time) -cam
        if (rc.getID()%2 == 0){ //Essentially makes half the drones swarm and half not
            //if I do swarm

            //Find the enemy hq
            if(EHqLoc.x > 0 || EHqLoc.y > 0){
                System.out.println("Found ENEMY HQ");
                if (myLoc.distanceSquaredTo(EHqLoc) > 5){
                    System.out.println("Going to ENEMY HQ:" + EHqLoc);
                    nav.droneGoTo(myLoc.directionTo(EHqLoc));

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
                    nav.droneGoTo(myLoc.directionTo(potentialHQ[hqToCheck]));
                rc.setIndicatorLine(myLoc,potentialHQ[hqToCheck],0,230,0);
            } else{
                System.out.println("Nothing Here at potential HQ:" + potentialHQ);
                hqToCheck += 1;
            }
        }

    }

    public void getNearbyEnemyBots(){
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo robot : nearbyEnemyRobots) {
            if ((robot.type.equals(RobotType.MINER) || robot.type.equals(RobotType.LANDSCAPER))){
                // If its on opponent team
                onEnemyMission = true;
                targetEnemyBot = robot;
                break;
            }
        }
    }

    public void getNearbyLandscapers(){
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
    }

    public void getNearbyBootMiners(){
        RobotInfo[] nearbyBootMiners = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robot : nearbyBootMiners) {
            if (robot.type.equals(RobotType.MINER)){
                // If its a miner on our team
                if (hqLoc != null){
                    // Only go for the robots that are too close to the HQ
                    if (robot.location.distanceSquaredTo(hqLoc) <= 2){
                        onBootMission = true;
                        targetBootBot = robot;
                    } else{
                        targetBootBot = null;
                    }
                } else{
                    targetBootBot = null;
                }
            }
        }
    }


}
