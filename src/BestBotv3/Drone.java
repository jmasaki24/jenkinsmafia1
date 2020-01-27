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

    boolean onStrike = false;
    boolean onMission = false;
    boolean onHelpMission = false;
    boolean onBootMission = false;
    RobotInfo targetEnemy = null;
    RobotInfo targetLandscaper = null;
    RobotInfo targetBootBot = null;
    boolean findANewBot = false;
    boolean iBroadcastedWaterLoc = false;

    boolean findANewEnemyBot = false;
    boolean findANewHelpBot = false;
    boolean findANewBootBot = false;



    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.updateAttackerDir(enemyDir);
        onStrike = rc.getRoundNum() > 1447;

        // Water Locations isnt updated right now
        if (!iBroadcastedWaterLoc) {
            for (Direction dir : Util.directions) {
                if (rc.canSenseLocation(myLoc.add(dir))) {
                    if (rc.senseFlooding(myLoc.add(dir))) {
                        comms.broadcastWaterLocation(myLoc.add(dir));
                        iBroadcastedWaterLoc = true;
                    }
                }
            }
        }


        //If we can swarm
        if (onStrike){
            System.out.println("Going to EHQ");
            goToEHQ();
            getNearbyEnemies();
            if (targetEnemy != null){
                pickupEnemy();
            }
            disposeOfScum();
        } else{
            // Enemy Detection
            RobotInfo[] nearbyEnemies = getNearbyEnemies();

            //Landscaper Detection
            RobotInfo[] nearbyLandscapers = getNearbyLandscapers();

            // Setting Standby Location

            if (standbyLocation == null) {
                if (hqLoc != null){
                    standbyLocation = hqLoc;
                }
            }

            //State your Mission:
            if (onMission){
                System.out.println("I am getting rid of the enemy!");
            }
            if (onHelpMission){
                System.out.println("I'm helping to build the wall!");
            }
            if (onBootMission){
                System.out.println("I'm booting!");
            }
            
            // If my task is to remove the enemy
            if (onMission){
                if (hqLoc != null){
                    //if I have the scum, look for a place to dispose them
                    disposeOfScum();


                    // If I see an enemy, go pick them up
                    if (targetEnemy != null) {
                        pickupEnemy();
                    }
                    // HQ has seen an enemy bot
                    else if (enemyDir.size() != 0){
                        nav.flyTo(hqLoc.add(enemyDir.get(0))); // Goes to enemies
                    }
                }
            }

            //If I'm helping landscapers:
            else if (onHelpMission){

                // If drone is holding friendly landscaper, put him back on the wall
                if (rc.isCurrentlyHoldingUnit()){
                    getLandscaperToWall();
                }

                //If I see a landscaper not on the wall, pick him up
                if (targetLandscaper != null){
                    pickupTargetLandscaper();
                }

                //If HQ sees a landscaper, go to help it
                else if (helpDir != null){
                    if (helpDir.size() != 0){
                        nav.flyTo(hqLoc.add(helpDir.get(helpDir.size()-1)));
                    }
                }
            }
           // BOOT

           else if (onBootMission){
                            // I need to boot this hot
              if (rc.isCurrentlyHoldingUnit()) {
                bootBot();
              }
                            // I see a bot that needs to be BOOTED
              else if (targetBootBot != null) {
                  pickUpBootBot();
              }
                            // HQ has seen a bot that needs to be BOOTED
              }


            else {
                // Standby if we are not on a mission
                if (standbyLocation != null) {
                    nav.flyTo(standbyLocation);
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
            rc.setIndicatorDot(loc,200,200,200);
        }


        //Find the enemy hq
        if(EHqLoc.x > 0 || EHqLoc.y > 0){
            // System.out.println("Found ENEMY HQ");
            if (myLoc.distanceSquaredTo(EHqLoc) > 5){
                // System.out.println("Going to ENEMY HQ:" + EHqLoc);
                nav.swarm(myLoc.directionTo(EHqLoc),myLoc,EHqLoc);
            } else{
                // System.out.println("Standing my gound at ENEMY HQ");
                for (Direction dir: Util.directions){
                    tryBuild(RobotType.NET_GUN,dir);
                }
                shouldMove = false;
            }
        }


        if(myLoc.distanceSquaredTo(potentialHQ[hqToCheck]) > 5){
            // System.out.println("Going to a potential HQ:" + potentialHQ);
            if(shouldMove)
                nav.swarm(myLoc.directionTo(potentialHQ[hqToCheck]),myLoc,potentialHQ[hqToCheck]);
            rc.setIndicatorLine(myLoc,potentialHQ[hqToCheck],0,230,0);
        } else{
            // System.out.println("Nothing Here at potential HQ:" + potentialHQ);
            hqToCheck += 1;
        }

    }

    public RobotInfo[] getNearbyEnemies(){
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo robot : nearbyEnemyRobots) {
            if ((robot.type.equals(RobotType.MINER) || robot.type.equals(RobotType.LANDSCAPER))){
                // If its on opponent team
                onMission = true;
                targetEnemy = robot;
                break;
            }
        }
        return nearbyEnemyRobots;
    }

    public RobotInfo[] getNearbyLandscapers(){
        RobotInfo[] nearbyLandscapers = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robot : nearbyLandscapers) {
            if ((robot.type.equals(RobotType.LANDSCAPER))){
                if (hqLoc != null){
                    if (robot.location.distanceSquaredTo(hqLoc) > 2){
                        // If its on opponent team
                        targetLandscaper = robot;
                        onHelpMission = true;
                        break;
                    }
                }
            }
        }
        return nearbyLandscapers;
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
                        break;
                    } else{
                        targetBootBot = null;
                    }
                } else{
                    targetBootBot = null;
                }
            }
        }
    }


    public void getLandscaperToWall() throws GameActionException{
        for (Direction dir: Util.directions){
            if (myLoc.add(dir).distanceSquaredTo(hqLoc) <= 2){
                if (rc.canDropUnit(dir)){
                    rc.dropUnit(dir);
                    targetLandscaper = null;
                    onHelpMission = false;
                    helpDir = null;
                    findANewBot = true;
                }
            }
        }
        //Good fuzzy nav
        if (!nav.tryFly(myLoc.directionTo(hqLoc))){
            nav.tryFly(Util.randomDirection());
        }
    }

    public void pickupTargetLandscaper() throws GameActionException{
        if (hqLoc != null){
            // System.out.println("My target landscaper is " + targetLandscaper.location.distanceSquaredTo(hqLoc) + " away from hq");
            // System.out.println("Target id: #" + targetLandscaper.ID);
            if (myLoc.distanceSquaredTo(targetLandscaper.location) < 3){ // If target is close
                if (targetLandscaper.location.distanceSquaredTo(hqLoc) > 2) { // if target not on wall
                    if (rc.canPickUpUnit(targetLandscaper.ID)) {
                        rc.pickUpUnit(targetLandscaper.ID);
                        // System.out.println("I picked up a landscaper! #" + targetLandscaper.ID);
                    }
                }
                // Target is on wall, mission accomplished
                else{
                    targetLandscaper = null;
                    onHelpMission = false;
                }
            } else{ //target out of range, going in for the rescue
                nav.flyTo(targetLandscaper.location);
            }
        } else {
            nav.flyTo(targetLandscaper.location);
        }
    }

    public void pickupEnemy() throws GameActionException{
        // And I'm there
        if (myLoc.distanceSquaredTo(targetEnemy.location) <= 2) {
            //And the bot is not on the wall
            if (targetEnemy.location.distanceSquaredTo(hqLoc) > 3)
                if (rc.canPickUpUnit(targetEnemy.ID)) {
                    rc.pickUpUnit(targetEnemy.ID);
                } else {
                    // System.out.println("Can't pickup Landscape #" + targetEnemy.ID);
                }
        }
        // If I'm not close enough get closer
        else {
            nav.flyTo(targetEnemy.location);
        }
    }

    public void disposeOfScum() throws GameActionException{
        if (rc.isCurrentlyHoldingUnit()){
            for (Direction dir: Util.directions){
                if (rc.senseFlooding(myLoc.add(dir))){
                    rc.dropUnit(dir);
                    targetEnemy = null;
                    onMission = false;
                    enemyDir = null;
                } else{
                    if (waterLocation.size() != 0){
                        nav.flyTo(waterLocation.get(waterLocation.size()-1));
                    } else{
                        nav.flyTo(Util.randomDirection());
                    }
                }
            }
        }
    }

    public void bootBot() throws GameActionException{
      for (Direction dir : Util.directions) {
          if (myLoc.add(dir).distanceSquaredTo(hqLoc) > 2) {
              if (rc.canDropUnit(dir)) {
                  rc.dropUnit(dir);
                  onBootMission = false;
                  bootDir = null;
                  findANewBootBot = true;
                  nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
                  System.out.println("I dropped our booted miner somewhere!");
                  targetBootBot = null;


              }
          } else {
              nav.droneGoTo(myLoc.directionTo(hqLoc).opposite());
          }
      }
    }

   public void pickUpBootBot() throws GameActionException{
     // I am there
     if (myLoc.distanceSquaredTo(targetBootBot.location) <= 2) {
         if (rc.canPickUpUnit(targetBootBot.ID)) {
             rc.pickUpUnit(targetBootBot.ID);
             System.out.println("I picked this unit up to boot it: " + targetBootBot.ID);
         } else {
             System.out.println("I cant pick up unit to boot it: " + targetBootBot.ID);
         }
     }
     // I'm not there yet
     else {
         nav.droneGoTo(targetBootBot.location);
         System.out.println("I'm going to the boot bot");
     }
   }
}
