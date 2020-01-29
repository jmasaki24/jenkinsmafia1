package BestBotv3;

import battlecode.common.*;

import java.util.ArrayList;

// so our defensive drone has a few states
// 1. not doing anything. go near standbyLocation
// 2. sees a target (i.e. has a targetEnemy). go to targetEnemy's location
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
    public ArrayList<MapLocation> waterLocation = new ArrayList<>();
    boolean justCreated = true;
    int duty = 0;

    private int DEFENSE = 1;
    private int MINEHELP = 3;
    private int ATTACK = 5;

    boolean checkUpperR = false;
    boolean checkUpperL = false;
    boolean checkBottomR = false;
    boolean checkBottomL = false;

    boolean gotFromBlock = false;

    int fourthWidth = rc.getMapWidth()/4;
    int threefourthWidth = rc.getMapWidth()*3/4;
    int fourthHeight = rc.getMapHeight()/4;
    int threefourthHeigh = rc.getMapHeight()*3/4;

    public Drone(RobotController r) {
        super(r);
    }

    boolean onMission = false;
    boolean onHelpMission = false;
    RobotInfo targetEnemy = null;
    RobotInfo targetLandscaper = null;

    RobotInfo targetMiner = null;
    MapLocation soupLoc = null;

    //boolean onCowMission = false;
    boolean iBroadcastedWaterLoc = false;
    boolean findANewBot = false;

    boolean specificdrone = false;
    RobotInfo targetMinerAtHQ = null;

    boolean helperDrone = false;



    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.updateAttackerDir(enemyDir);


        if(turnCount == 1){ //get duty from block chain because it's just created!
            System.out.println("round num passing in" + (rc.getRoundNum()-1));
            duty = comms.getDroneDuty(rc.getRoundNum() - 1); //updated from amazon previous round
        }

        //runs specialization method
        if(duty == DEFENSE){
            System.out.println("YOO im on defense"); //im guessing continue in this method if its on defense
            if(rc.getRoundNum() > 300 && rc.getRoundNum() < 350){
                helperDrone = true;
            }
        }
        else if(duty == MINEHELP){
            System.out.println("Time to help those miners get the soup");
            //getMinersToHigherSoup(); //help move miner to higher soup
            helperDrone = true;
        }
        else if(duty == ATTACK){ //attack --> maybe call gotoEHQ???
            System.out.println("letss gooo offense");
        }

        /*if(rc.isCurrentlyHoldingUnit() == false){
            moveMinerAwayFromHQ();
        }*/

        // Water Locations isnt updated right now
        // comms.updateWaterLocations(waterLocation);

        // goToEHQ works, but first we need a defensive drone.
        // gotoEHQ();

        waterLocation = comms.updateWaterLocations(waterLocation);

        //If we have water, get water locs
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

        // EXAMPLE of how to use drones_ids_us
//        if (turnCount == 1) {
//            if (drones_ids_us.size() == 2) {
//                specificdrone = true;
//            }
//        }
//
//        if (specificdrone) {
//            nav.flyTo(new MapLocation(31, 16));
//        }
        if(!helperDrone){
            if(rc.isCurrentlyHoldingUnit() == false){
                // Enemy Detection
                RobotInfo[] nearbyEnemies = getNearbyEnemies();

                //Landscaper Detection
                RobotInfo[] nearbyLandscapers = getNearbyLandscapers();
            }

            //wont get cow unless it doesnt have a mission already, makes sure other mission gets priority
            if(onMission == false && onHelpMission == false && targetMinerAtHQ == null && !rc.isCurrentlyHoldingUnit()){
                //finding cows near HQ
                getNearbyCows();
            }
        }


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
        if(helperDrone){
            System.out.println("moving miner");
        }

        // If my task is to remove the enemy
        if (onMission){
            if (hqLoc != null){
                //if I have the scum, look for a place to dispose them
                disposeOfScum();


                // If I see an enemy, go pick them up
                if (targetEnemy != null) {
                    System.out.println("drone" + targetEnemy.location);
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
        else if(helperDrone){
            getMinersToHigherSoup();
        }
        else {
            // Standby if we are not on a mission
            if (standbyLocation != null) {
                nav.flyTo(standbyLocation);
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
                // System.out.println("Found ENEMY HQ");
                if (myLoc.distanceSquaredTo(EHqLoc) > 5){
                    // System.out.println("Going to ENEMY HQ:" + EHqLoc);
                    nav.tryFly(myLoc.directionTo(EHqLoc));
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
                    nav.tryFly(myLoc.directionTo(potentialHQ[hqToCheck]));
                rc.setIndicatorLine(myLoc,potentialHQ[hqToCheck],0,230,0);
            } else{
                // System.out.println("Nothing Here at potential HQ:" + potentialHQ);
                hqToCheck += 1;
            }
        }

    }

    public void getMinersToHigherSoup() throws GameActionException{
        if(targetMiner == null){
            MapLocation possibleMiner = comms.getMinerLocationForDrone(rc.getRoundNum());
            if(possibleMiner.x > 0 && possibleMiner.y > 0){
                nav.flyTo(possibleMiner);
                gotFromBlock = true;
            }
        }
        System.out.println("yo");
       if(!rc.isCurrentlyHoldingUnit() && soupLoc == null){
           System.out.println("pickup");
           RobotInfo[] nearbyMiners = getNearbyMiners(); //check if miners are nearby

           if(targetMiner != null){ //pick up miner
               pickupTargetMiner();
           }
       }

       if(rc.isCurrentlyHoldingUnit() && soupLoc == null){
           findHighSoup();
       }

       if(soupLoc != null){
           System.out.println("lets go get the soup");
           if(rc.getLocation().distanceSquaredTo(soupLoc) < 3){
               if(rc.isCurrentlyHoldingUnit()){
                   if(!rc.senseFlooding(rc.getLocation())){
                       for(Direction d: Util.directions){
                           if(rc.canDropUnit(d)){
                               rc.dropUnit(d);
                               soupLoc = null;
                               targetMiner = null;
                               onHelpMission = false;
                               nav.flyTo(hqLoc);
                           }
                       }
                   }
                   else if(Math.random() > 0.25){
                       nav.flyTo(rc.getLocation().translate(1, 0));
                   }
                   else{
                       nav.flyTo(rc.getLocation().translate(0, 1));
                   }
               }
           }
           else{
               nav.flyTo(soupLoc);
           }
       }
    }

    public RobotInfo[] getNearbyMiners() throws GameActionException{
        RobotInfo[] nearbyMiners = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam());
        for(RobotInfo robot: nearbyMiners){
            if(robot.type.equals(RobotType.MINER)){
                targetMiner = robot; //will try to pick this guy up
                return nearbyMiners;
            }
        }
        comms.broadCastMinerHelpFromDrone();
        nav.flyTo(new MapLocation(15,15));
        return nearbyMiners;
    }

    public void findHighSoup() throws GameActionException{
        System.out.println("123");
        MapLocation[] soups = rc.senseNearbySoup();
        for(MapLocation m: soups){
            int soupContent = rc.senseSoup(m);
            if(rc.senseElevation(m) > 4 && soupContent > 100){ //hardcoded at 3, not sure what to change it to
                soupLoc = m; //target soup location to move to
                System.out.println("345");
                return;
            }
        }
        System.out.println("234");
        if(checkUpperL == false){
            if(rc.getLocation().distanceSquaredTo(new MapLocation(fourthWidth, threefourthHeigh)) < 5){
                checkUpperL = true;
            }
            nav.flyTo(new MapLocation(fourthWidth, threefourthHeigh));
        }
        else if(checkUpperR == false){
            if(rc.getLocation().distanceSquaredTo(new MapLocation(threefourthWidth, threefourthHeigh)) < 5){
                checkUpperR = true;
            }
            nav.flyTo(new MapLocation(threefourthWidth, threefourthHeigh));
        }
        else if(checkBottomR == false){
            if(rc.getLocation().distanceSquaredTo(new MapLocation(threefourthWidth, fourthHeight)) < 5){
                checkBottomR = true;
            }
            nav.flyTo(new MapLocation(threefourthWidth, fourthHeight));
        }
        else if(checkBottomL == false){
            if(rc.getLocation().distanceSquaredTo(new MapLocation(fourthWidth, fourthHeight)) < 5){
                checkBottomL = true;
            }
            nav.flyTo(new MapLocation(fourthWidth, fourthHeight));
        }
        else{
            nav.flyTo(new MapLocation(15, 7));
        }
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

    public void getNearbyCows(){
        if(hqLoc == null){
            return;
        }
        //don't care, not near HQ
        /*if(!myLoc.isWithinDistanceSquared(hqLoc, rc.getCurrentSensorRadiusSquared())){
            return;
        }*/
        RobotInfo[] nearbyCows = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared);
        for(RobotInfo robot: nearbyCows){
            if(robot.type.equals(RobotType.COW)){
                System.out.println("I sense cow");
                onMission = true;
                targetEnemy = robot;
                break;
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
                        System.out.println("I picked up a landscaper! #" + targetLandscaper.ID);
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

    public void pickupTargetMiner() throws GameActionException {
        if(myLoc.distanceSquaredTo(targetMiner.location) < 3){ //if its close then pick itup
            if(rc.canPickUpUnit(targetMiner.ID)){
                rc.pickUpUnit(targetMiner.ID);
            }
        }
        else{
            nav.flyTo(targetMiner.location); //fly to location
        }
    }


    //not working properly yet
    /*public void moveMinerAwayFromHQ() throws GameActionException{
        if(hqLoc != null){
            if(rc.canSenseLocation(hqLoc)){
                RobotInfo[] miners = rc.senseNearbyRobots(-1, rc.getTeam());
                for(RobotInfo r: miners){
                    if(r.getType() == RobotType.MINER){
                        if(r.getLocation().distanceSquaredTo(hqLoc) < 3 && rc.senseSoup(r.getLocation()) < 5){
                            targetMinerAtHQ = r;
                            onMission = false;
                            onHelpMission = false;
                            if(rc.canPickUpUnit(targetMinerAtHQ.ID)){
                                rc.pickUpUnit(targetMinerAtHQ.ID);
                                System.out.println("moving away from hq with miner");
                            }
                            findHighSoup();
                            if(soupLoc != null){
                                if(rc.getLocation().distanceSquaredTo(soupLoc) < 5){
                                    if(rc.isCurrentlyHoldingUnit()){
                                        rc.dropUnit(Util.randomDirection());
                                        targetMinerAtHQ = null;
                                    }
                                }
                                else{
                                    nav.flyTo(soupLoc);
                                }
                            }
                        }
                    }
                }
            }
        }

    }*/
}
