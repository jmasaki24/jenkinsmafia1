package rebecca;
import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    static Direction[] allDirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    // from lectureplayer, could be deleted
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};


    static final int HQID = 0;
    static final int DESIGNSCHOOL = 123;
    static int turnCount; // number of turns since creation
    static int numMiners = 0;
    static int numDesignSchools = 0;
    static int numLandscapers = 0;
    static int lastCheckedBlock = 0;
    static boolean shouldMakeBuilders = false;
    static final int NOTHINGID = 404;
    static final MapLocation closestRefineryLoc = null;
    static MapLocation lastSeenWater = new MapLocation(-5,-5);
    static MapLocation m1, m2, m3; //3 possible locations for enemy HQ
    static int mapHeight; //height of the map
    static int mapWidth; //width of the map
    static MapLocation myLoc; //robot's location atm
    static MapLocation ourHQ; //the location for our HQ
    static MapLocation enemyHQ; //final enemy location
    static ArrayList<MapLocation> soupLocations = new ArrayList<>();
    static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
    static ArrayList<MapLocation> designSchoolLocations = new ArrayList<>();
    static ArrayList<MapLocation> vaporatorLocations = new ArrayList<>();
    static ArrayList<MapLocation> amazonLocations = new ArrayList<>();
    static int EHQID = 3867; //code for block chain
    static boolean findEQ = false; //checks if final enemy hq has been found

    // used in blockchain transactions
    static final int teamSecret = 1211212211;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        rebecca.RobotPlayer.rc = rc;

        mapHeight = rc.getMapHeight(); //set the map's width and height
        mapWidth = rc.getMapWidth();

        turnCount = 0;

        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            turnCount++;
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                findHQ();
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    //case REFINERY:           runRefinery();          break;
                    //case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    //case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }

    }

    static void runDesignSchool() throws GameActionException {
        if (rc.getTeamSoup()>=(4*RobotType.LANDSCAPER.cost)){
            shouldMakeBuilders = true;
        }
        if (shouldMakeBuilders){
            for (Direction dir: directions){
                tryBuild(RobotType.LANDSCAPER,dir);
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions) { //build drones
            tryBuild(RobotType.DELIVERY_DRONE, dir);
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        m1 = getEHqLocFromBlockchain(); //gets the enemy's hq location from block chain

        //gotten from the block chain
        if(m1 != null){
            System.out.println("DRONE: " + m1.x + ", " + m1.y); //prints the location the drone is flying to

            //checks if drone can sense the hq
            if(rc.canSenseLocation(m1)){
                RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                for(RobotInfo r: robots){
                    if(r.type == RobotType.HQ){ //check if HQ is actual at the location
                        System.out.println("ENEMYHQ FOUND");
                        System.out.println("EHQ: " + m1.x + ", " + m1.y);
                        sendEHqLoc(m1); //send it to block chain
                        //make the drone move away so that it doesn't get shot down by net gun
                        Direction awayEHQ = rc.getLocation().directionTo(m1).opposite();
                        tryMove(awayEHQ);
                        findEQ = true; //so that it doesn't fly back
                    }
                }
            }
            else if(findEQ == false){ //hasn't found hq yet so keep flying to enemy hq
                Direction toEHQ = rc.getLocation().directionTo(m1);
                tryMove(toEHQ);
            }
            else{ //fly away
                Direction awayEHQ = rc.getLocation().directionTo(m1).opposite();
                tryMove(awayEHQ);
            }
        }

        boolean moved = false;
        if(rc.senseFlooding(rc.getLocation())){
            lastSeenWater = rc.getLocation();
        }
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robot = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            RobotInfo[] cows = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, Team.NEUTRAL);
            //Combines the two arrays into one
            RobotInfo[] robots = new RobotInfo[robot.length + cows.length];
            System.arraycopy(robot, 0, robots, 0, robot.length);
            System.arraycopy(cows, 0, robots, robot.length, cows.length);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
            tryMove(randomDirection());
        } else {
            if(rc.getLocation() == lastSeenWater){
                for (Direction dir: directions){
                    if (tryMove(dir)){
                        rc.dropUnit(Direction.CENTER.opposite());
                    }
                }
            } else{
                if(lastSeenWater.x < 0 || lastSeenWater.y < 0){
                    tryMove(randomDirection());
                } else{
                    if(!tryMove(rc.getLocation().directionTo(lastSeenWater))){
                        tryMove(randomDirection());
                    }
                }
            }
        }
    }


    static void runLandscaper() throws GameActionException {
        if(rc.getDirtCarrying() == 0){
            tryDig();
        }

        MapLocation bestPlaceToBuildWall = null;
        // find best place to build
        if(ourHQ != null) {
            int lowestElevation = 9999999;
            for (Direction dir : directions) {
                MapLocation tileToCheck = ourHQ.add(dir);
                if(rc.getLocation().distanceSquaredTo(tileToCheck) < 4
                        && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
                    if (rc.senseElevation(tileToCheck) < lowestElevation) {
                        lowestElevation = rc.senseElevation(tileToCheck);
                        bestPlaceToBuildWall = tileToCheck;
                    }
                }
            }
        }

        if (Math.random() < 0.4){
            // build the wall
            if (bestPlaceToBuildWall != null) {
                rc.depositDirt(rc.getLocation().directionTo(bestPlaceToBuildWall));
                rc.setIndicatorDot(bestPlaceToBuildWall, 0, 255, 0);
                System.out.println("building a wall");
            }
        }

        // otherwise try to get to the hq
        if(ourHQ != null){
            goTo(ourHQ);
        } else {
            tryMove(randomDirection());
        }
    }

//    static void definitelyMove() throws GameActionException {
//        definitelyMove(0);
//    }
//
//    static void definitelyMove(int count) throws GameActionException {
//        Direction dir = randomDirection();
//        int distance = myLoc.add(dir).distanceSquaredTo(hqLoc);
//        System.out.println(distance);
//        if(distance <= 8){
//            if ((dir != Direction.EAST && dir != Direction.NORTHEAST) && dir != Direction.SOUTHEAST){
//                if(!tryMove(dir)){
//                    if(count < 10){
//                        definitelyMove(count + 1);
//                    }
//                }
//            }
//        }
//        if(count < 10)
//            definitelyMove(count + 1);
//    }

    static void definitelyDigDirt() throws GameActionException {
        definitelyDigDirt(0);
    }

    static void definitelyDigDirt(int count) throws GameActionException {
        Direction dir = randomAllDirection();
        int distance = myLoc.add(dir).distanceSquaredTo(ourHQ);
        if ((distance > 4 && (distance < 9) && rc.canDepositDirt(dir))) {
            if(rc.canDepositDirt(dir)){
                rc.depositDirt(dir);
            } else{
                if (count < 10)
                    definitelyDigDirt(count + 1);
            }
        } else {
            if (rc.canDigDirt(dir)) {
                rc.digDirt(dir);
            } else{
                if (count < 10)
                    definitelyDigDirt(count + 1);
            }
        }
    }

    static void findHQ() throws GameActionException {
        if (ourHQ == null) {
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    ourHQ = robot.location;
                }
            }
            if(ourHQ == null) {
                // if still null, search the blockchain
                getHqLocFromBlockchain();
            }
        }
        //HQ now shoots drones
        RobotInfo targets[] = rc.senseNearbyRobots(RobotType.NET_GUN.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo target : targets) {
            if (rc.canShootUnit(target.ID)) {
                rc.shootUnit(target.ID);
            }
        }
    }

    static void runHQ() throws GameActionException {
        if(turnCount == 1) {
            sendHqLoc(rc.getLocation());
        }
        if(numMiners < 15) {
            for (Direction dir : directions)
                if(tryBuild(RobotType.MINER, dir)){
                    numMiners++;
                }
        }

        //Request a school next to base
        boolean seeDesignSchool = false;
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.HQ.sensorRadiusSquared,rc.getTeam());
        for (RobotInfo robot:robots){
            if(robot.type == RobotType.DESIGN_SCHOOL){
                seeDesignSchool = true;
            }
        }
        if(!seeDesignSchool){
            if(rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.MINER.cost){
                tryBuild(RobotType.MINER,Direction.SOUTHWEST);
            }
        }
        if (seeDesignSchool && rc.getRoundNum() > 300){
            for (Direction dir: directions){
                tryBuild(RobotType.MINER,randomDirection());
            }
        }
    }

    static void possibleHQ() {

        m1 = ourHQ.translate(0, (mapHeight - 2*ourHQ.y));
        m2 = ourHQ.translate((mapWidth - 2*ourHQ.x), 0);
        m3 = ourHQ.translate((mapWidth - 2*ourHQ.x), (mapHeight - 2*ourHQ.y));

        RobotInfo[] sensem1 = rc.senseNearbyRobots(m1, -1, rc.getTeam().opponent());
        for(RobotInfo robo: sensem1) {
            if(robo.type == RobotType.HQ) {
                enemyHQ = robo.location;
                System.out.println("The enemy HQ is at (" + enemyHQ.x + ", " + enemyHQ.y + ")");
            }
        }

        RobotInfo[] sensem2 = rc.senseNearbyRobots(m2, -1, rc.getTeam().opponent());
        for(RobotInfo robo: sensem2) {
            if(robo.type == RobotType.HQ) {
                enemyHQ = robo.location;
                System.out.println("The enemy HQ is at (" + enemyHQ.x + ", " + enemyHQ.y + ")");
            }
        }
        RobotInfo[] sensem3 = rc.senseNearbyRobots(m3, -1, rc.getTeam().opponent());
        for(RobotInfo robo: sensem3) {
            if(robo.type == RobotType.HQ) {
                enemyHQ = robo.location;
                System.out.println("The enemy HQ is at (" + enemyHQ.x + ", " + enemyHQ.y + ")");
            }
        }



    }

    static void runDroneAfterHQ() throws GameActionException {
        boolean moved = false;
        /*if(rc.senseFlooding(rc.getLocation())){
            lastSeenWater = rc.getLocation();
        }*/
        Team enemy = rc.getTeam().opponent();
        Direction toEHQ = (rc.getLocation()).directionTo(enemyHQ);

        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robot = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            RobotInfo[] cows = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, Team.NEUTRAL);
            //Combines the two arrays into one
            RobotInfo[] robots = new RobotInfo[robot.length + cows.length];
            System.arraycopy(robot, 0, robots, 0, robot.length);
            System.arraycopy(cows, 0, robots, robot.length, cows.length);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
            tryMove(toEHQ);
        } else {
            if(rc.getLocation() == enemyHQ){
                for (Direction dir: directions){
                    if (tryMove(dir)){
                        rc.dropUnit(Direction.CENTER.opposite());
                    }
                }
            } else{
                if(lastSeenWater.x < 0 || lastSeenWater.y < 0){
                    tryMove(toEHQ);
                } else{
                    if(!tryMove(rc.getLocation().directionTo(enemyHQ))){
                        tryMove(randomDirection());
                    }
                }
            }
        }

    }

    static void senseHQ() throws GameActionException {
        //System.out.println("hello");
        RobotInfo[] robott = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robo : robott) {
            if (robo.type == RobotType.HQ) {
                ourHQ = robo.location;
                System.out.println("Our HQ is at (" + ourHQ.x + ", " + ourHQ.y + ")");
                possibleHQ();
                runDroneAfterHQ();
            }
        }

    }

    static void runMiner() throws GameActionException {
        updateUnitLocations();
        updateSoupLocations();
        checkIfSoupGone();


        /*if(amazonLocations.size() == 0){
            if(tryBuild(RobotType.FULFILLMENT_CENTER, Direction.CENTER) == true){
                rc.buildRobot(RobotType.FULFILLMENT_CENTER, Direction.CENTER);
                broadcastUnitCreation(RobotType.FULFILLMENT_CENTER, rc.getLocation());
            }
        }

        if(tryBuild(RobotType.FULFILLMENT_CENTER, Direction.CENTER) == true){
            rc.buildRobot(RobotType.FULFILLMENT_CENTER, Direction.CENTER);
            broadcastUnitCreation(RobotType.FULFILLMENT_CENTER, rc.getLocation());
        }*/
        if(amazonLocations.size() == 0){
            for (Direction dir: directions){
                System.out.println("build amazon");
                tryBuild(RobotType.FULFILLMENT_CENTER,randomDirection());
            }
        }



        //Build vaporators late game
        /*if(rc.getRoundNum() > 780){
            boolean seeHQ = false;
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot: robots){
                if (robot.type == RobotType.HQ){
                    seeHQ = true;
                    ourHQ = robot.getLocation();
                    System.out.println("HQ = (" + ourHQ.x + ", " + ourHQ.y);

                    m1 = ourHQ.translate(0, (mapHeight - 2*ourHQ.y - 1));
                    m2 = ourHQ.translate((mapWidth - 2*ourHQ.x - 1), 0);
                    m3 = ourHQ.translate((mapWidth - 2*ourHQ.x - 1), (mapHeight - 2*ourHQ.y - 1));

                    System.out.println("1 = (" + m1.x + ", " + m1.y);
                    System.out.println("2 = (" + m2.x + ", " + m2.y);
                    System.out.println("3 = (" + m3.x + ", " + m3.y);

                    sendEHqLoc(m1);
                    sendEHqLoc(m2);
                    sendEHqLoc(m3);

                }
            }
            if(seeHQ)
                tryBuild(RobotType.VAPORATOR,randomDirection());
            goTo((ourHQ));
        }*/


        if(m1 == null){
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot: robots){
                if (robot.type == RobotType.HQ){
                    ourHQ = robot.getLocation();
                    System.out.println("HQ = (" + ourHQ.x + ", " + ourHQ.y);

                    m3 = ourHQ.translate(0, (mapHeight - 2*ourHQ.y - 1));
                    m1 = ourHQ.translate((mapWidth - 2*ourHQ.x - 1), 0);
                    m2 = ourHQ.translate((mapWidth - 2*ourHQ.x - 1), (mapHeight - 2*ourHQ.y - 1));

                    System.out.println("1 = (" + m1.x + ", " + m1.y);
                    //System.out.println("2 = (" + m2.x + ", " + m2.y);
                    //System.out.println("3 = (" + m3.x + ", " + m3.y);

                    sendEHqLoc(m1);
                    //sendEHqLoc(m2);
                    //sendEHqLoc(m3);

                }
            }
        }

        if(rc.getRoundNum() > 281){
            System.out.println("second try1 = (" + m1.x + ", " + m1.y);
            //System.out.println("second tyr2 = (" + m2.x + ", " + m2.y);
            //System.out.println("second try3 = (" + m3.x + ", " + m3.y);

            sendEHqLoc(m1);
            //sendEHqLoc(m2);
            //sendEHqLoc(m3);
        }


        myLoc = rc.getLocation();

        //Build 1 school when summoned into a specific position by HQ
        //System.out.println(turnCount);
        if(turnCount <= 13){
            System.out.println(ourHQ.distanceSquaredTo(myLoc));
            if(myLoc.directionTo(ourHQ) == Direction.NORTHEAST && myLoc.distanceSquaredTo(ourHQ) == 2){
                System.out.println("Trybuild school");
                tryBuild(RobotType.DESIGN_SCHOOL,Direction.NORTH);
            }
        }
        // Better to deposit soup while you can
        for (Direction dir : directions) {
            if (rc.canDepositSoup(dir)) {
                rc.depositSoup(dir, rc.getSoupCarrying());
                System.out.println("Deposited soup into new refinery");
            }
        }

        // then, try to mine soup in all directions
        for (Direction dir : directions)
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (ourHQ.distanceSquaredTo(soupLoc) > 15) {
                    if (tryBuild(RobotType.REFINERY, randomDirection())) {
                        broadcastUnitCreation(RobotType.REFINERY, rc.adjacentLocation(dir.opposite()));
                    }
                }
                if (!soupLocations.contains(soupLoc)) {
                    broadcastSoupLocation(soupLoc);
                }
            }

        //lastly, move

        // if at soup limit, go to nearest refinery or hq.
        // if hq or refinery is far away, build a refinery.
        // if there are less than MINERLIMIT miners, tell hq to pause building miners????
        /*if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            System.out.println("I'm full of soup");


            //find closest refinery (including hq, should change that tho since HQ will become unreachable)
            MapLocation closestRefineryLoc = ourHQ;

            //Find Closest Refinery
            if (refineryLocations.size() != 0) {
                for (MapLocation refinery : refineryLocations) {
                    if (myLoc.distanceSquaredTo(refinery) < myLoc.distanceSquaredTo(closestRefineryLoc)) {
                        closestRefineryLoc = refinery;
                    }
                }
            }

            // a refinery, they just sit there and wait for passive soup income.

            // how far away is enough to justify a new refinery?
            if (rc.getLocation().distanceSquaredTo(closestRefineryLoc) > 35) {
                if(!tryBuild(RobotType.REFINERY, randomDirection())){ // if a new refinery can't be built go back to hq
                    System.out.println("moved towards HQ");
                    goTo(closestRefineryLoc);
                    rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
                }
            } else {
                System.out.println("moved towards HQ");
                goTo(closestRefineryLoc);
                rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);

            }
        }

        else {
            if (soupLocations.size() > 0) {
                System.out.println("I'm moving to soupLocation[0]");
                goTo(soupLocations.get(0));
            } else {
                System.out.println("I'm searching for soup, moving away from other miners");
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared,rc.getTeam());
                MapLocation nextPlace = rc.getLocation();
                for (RobotInfo robot:robots){
                    if (robot.type == RobotType.MINER){
                        nextPlace = nextPlace.add(rc.getLocation().directionTo(robot.location).opposite());
                    }
                }
                if(robots.length == 0){
                    nextPlace.add(randomDirection());
                }
                System.out.println("Trying to go: " + rc.getLocation().directionTo(nextPlace));
                if(nextPlace != rc.getLocation()){
                    goTo(rc.getLocation().directionTo(nextPlace));
                } else{
                    goTo(randomDirection());
                }
            }
        }*/


    }

    // tries to move in the general direction of dir
    // THIS IS WHERE I WORK ON PATHFINDING - jm
    static boolean goTo(Direction dir) throws GameActionException {

        // while, I cannot move, rotate the direction to the right.
        for (int i = 0; i < 8; i++) {
            if (!tryMove(dir)) {
                dir = dir.rotateRight();
            } else {
                return true;
            }
        }
        return false;

//        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
//        for (Direction d : toTry){
//            if(tryMove(d))
//                return true;
//        }
//        return false;
    }

    // navigate towards a particular location
    static boolean goTo(MapLocation destination) throws GameActionException {
        return goTo(rc.getLocation().directionTo(destination));
    }

    static void checkIfSoupGone() throws GameActionException {
        if (soupLocations.size() > 0) {
            MapLocation targetSoupLoc = soupLocations.get(0);
            if (rc.canSenseLocation(targetSoupLoc)
                    && rc.senseSoup(targetSoupLoc) == 0) {
                soupLocations.remove(0);
            }
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static Direction randomAllDirection() {
        return allDirections[(int) (Math.random() * allDirections.length)];
    }

    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            rc.setIndicatorDot(rc.getLocation().add(dir), 255, 0, 0);
            return true;
        }
        return false;
    }

    /**
     * Attempts to move in every direction
     */
    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            broadcastUnitCreation(type, rc.getLocation().add(dir));
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }



    public static void broadcastSoupLocation(MapLocation loc ) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 2;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("new soup!" + loc);
        }
    }

    public static void updateSoupLocations() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == 2){
                System.out.println("heard about a tasty new soup location");
                soupLocations.add(new MapLocation(mess[2], mess[3]));
            }
        }
    }

    public static void sendHqLoc(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = HQID;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3))
            rc.submitTransaction(message, 3);
    }

    public static void getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[0] == teamSecret && mess[1] == HQID){
                    System.out.println("found the HQ!");
                    ourHQ = new MapLocation(mess[2], mess[3]);
                }
            }
        }
    }

    public static void sendEHqLoc(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = EHQID;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3))
            rc.submitTransaction(message, 3);
    }

    public static MapLocation getEHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[0] == teamSecret && mess[1] == EHQID){
                    return new MapLocation(mess[2], mess[3]);
                }
            }
        }
        return null;
    }

    public static void broadcastUnitCreation(RobotType type, MapLocation loc) throws GameActionException {
        int typeNumber;
        switch (type) {
            case COW:                     typeNumber = 1;     break;
            case DELIVERY_DRONE:          typeNumber = 2;     break;
            case DESIGN_SCHOOL:           typeNumber = 3;     break;
            case FULFILLMENT_CENTER:      typeNumber = 4;     break;
            case HQ:                      typeNumber = 5;     break;
            case LANDSCAPER:              typeNumber = 6;     break;
            case MINER:                   typeNumber = 7;     break;
            case NET_GUN:                 typeNumber = 8;     break;
            case REFINERY:                typeNumber = 9;     break;
            case VAPORATOR:               typeNumber = 10;    break;
            default:                      typeNumber = 0;     break;
        }

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 4;
        message[2] = loc.x; // x coord of unit
        message[3] = loc.y; // y coord of unit
        message[4] = typeNumber;
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("new refinery!" + loc);
        }
        if(rc.canSubmitTransaction(message, 5)){
            rc.submitTransaction(message, 5);
        }
    }

    public static void updateUnitLocations() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == 4){
                System.out.println("heard about a new unit");
                switch (mess[4]) {
                    case 3:     designSchoolLocations.add(new MapLocation(mess[2], mess[3]));   break;
                    case 4:     amazonLocations.add(new MapLocation(mess[2], mess[3]));         break;
                    case 9:     refineryLocations.add(new MapLocation(mess[2], mess[3]));       break;
                    case 10:    vaporatorLocations.add(new MapLocation(mess[2], mess[3]));      break;
                    default: break;
                }
            }
        }
    }
}
