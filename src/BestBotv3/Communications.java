package BestBotv3;

import battlecode.common.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class Communications {
    RobotController rc;

    final int teamSecret = -817280;

    final int HQID = 982;
    final int EHQID = 382;

    final int AMAZONID = 101;
    final int DESIGNSCHOOLID = 202;
    final int REFINERYID = 303;
    final int VAPORATORID = 404;
    final int NETGUNID = 505;

    final ArrayList<Integer> BuildingIDs = new ArrayList<Integer>(Arrays.asList(HQID,EHQID,AMAZONID,DESIGNSCHOOLID,REFINERYID,VAPORATORID));

    final int SOUPID = 312;
    final int WATERID = 820;
    final int ATTACKERID = 555;

    final int DRONEDUTY = 345;
    final int DRONEDEFENSE = 12;
    final int DRONEHELPMINER = 30;
    final int DRONEATTACK = 450;

    int[] lastSpoofedMessage;

//    static final String[] messageType = {
//        "HQ loc",
//        "design school created",
//        "soup location",
//    };

    public Communications(RobotController r) {
        rc = r;
    }

//    public void sendHqLoc(MapLocation loc) throws GameActionException {
//        int[] message = new int[7];
//        message[0] = teamSecret;
//        message[1] = HQID;
//        message[2] = loc.x; // x coord of HQ
//        message[3] = loc.y; // y coord of HQ
//        if (rc.canSubmitTransaction(message, 3))
//            rc.submitTransaction(message, 3);
//    }

//    public void sendEHqLoc(MapLocation loc) throws GameActionException {
//        int[] message = new int[7];
//        message[0] = teamSecret;
//        message[1] = EHQID;
//        message[2] = loc.x; // x coord of HQ
//        message[3] = loc.y; // y coord of HQ
//        if (rc.canSubmitTransaction(message, 3))
//            rc.submitTransaction(message, 3);
//    }

    public MapLocation getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[0] == teamSecret && mess[1] == HQID){
                     System.out.println("found hqLoc in chain");
                    return new MapLocation(mess[2], mess[3]);
                }
            }
        }
        return null;
    }
//
//    public MapLocation getEHqLocFromBlockchain() throws GameActionException {
//        for (int i = 1; i < rc.getRoundNum(); i++){
//            for(Transaction tx : rc.getBlock(i)) {
//                int[] mess = tx.getMessage();
//                if(mess[0] == teamSecret && mess[1] == EHQID){
//                    return new MapLocation(mess[2], mess[3]);
//                }
//            }
//        }
//        return null;
//    }

// ------------------------------------------------------------------------------------------------ //
// ------------------------------------ ATTACKER COMMUNICATION ------------------------------------ //
    public void broadcastAttackerInfo(int AttackerID, Direction dir) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = ATTACKERID; // id for updateAttackerDir
        message[2] = AttackerID; // ID of attacking bot
        message[3] = directionToNumber(dir); // direction number
        message[4] = message[0] + message[1] + message[2] + message[3];
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    public void updateAttackerDir(ArrayList<Direction> enemyDir) throws GameActionException {
        // if its just been created, go through all of the blocks and transactions to find attackers directions
        if (RobotPlayer.turnCount == 1) {
            // System.out.println("turncount 1 in updateAttackerDirection");
            for (int i = 1; i < rc.getRoundNum(); i++) {
                getAttackersInBlock(enemyDir, 1);
            }
        } else {
            getAttackersInBlock(enemyDir, rc.getRoundNum() - 1);
        }
    }
    public void getAttackersInBlock(ArrayList<Direction> enemyDir, int roundNum) throws GameActionException {
        for(Transaction tx : rc.getBlock(roundNum)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == ATTACKERID && (mess[4] == mess[0] + mess[1] + mess[2] + mess[3])) {
                 System.out.println("Theres an attacker with ID of " + mess[2] + ", and a direction from HQ of " + mess[3] + "!!!!");
                enemyDir.add(numberToDirection(mess[3]));
            }
        }
    }

    public Direction numberToDirection (int d){
        Direction dir;
        switch(d){
            case 1:             dir = Direction.NORTH;          break;
            case 2:             dir = Direction.NORTHEAST;      break;
            case 3:             dir = Direction.EAST;           break;
            case 4:             dir = Direction.SOUTHEAST;      break;
            case 5:             dir = Direction.SOUTH;          break;
            case 6:             dir = Direction.SOUTHWEST;      break;
            case 7:             dir = Direction.WEST;           break;
            case 8:             dir = Direction.NORTHWEST;      break;
            default:            dir = Direction.EAST;           break;
        }
        return dir;
    }
    public int directionToNumber(Direction d){
        int type = 0;
        switch(d){
            case NORTH:                 type = 1;       break;
            case NORTHEAST:             type = 2;       break;
            case EAST:                  type = 3;       break;
            case SOUTHEAST:             type = 4;       break;
            case SOUTH:                 type = 5;       break;
            case SOUTHWEST:             type = 6;       break;
            case WEST:                  type = 7;       break;
            case NORTHWEST:             type = 8;       break;
            default:                    type = 3;       break; // No reason its east, I just figured that if it breaks, then hopefully the enemy is east
        }
        return type;
    }

// ------------------------------------------------------------------------------------------------ //
// ------------------------------------ WATER COMMUNICATION ------------------------------------ //
    public void broadcastWaterLocation(MapLocation loc ) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = WATERID;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        message[4] = message[0] + message[1] + message[2] + message[3];
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            // System.out.println("new water!" + loc);
        }
    }
    public ArrayList<MapLocation> updateWaterLocations(ArrayList<MapLocation> waterLocations) throws GameActionException {
        // if its just been created, go through all of the blocks and transactions to find soup
        if (RobotPlayer.turnCount == 1) {
            // System.out.println("turncount 1 in updatewaterloc");
            for (int i = 1; i < rc.getRoundNum(); i++) {
                waterLocations = getWaterLocInBlock(waterLocations, i);
            }
        } else {
            waterLocations = getWaterLocInBlock(waterLocations, rc.getRoundNum() - 1);
        }
        return waterLocations;
    }
    public ArrayList<MapLocation> getWaterLocInBlock(ArrayList<MapLocation> waterLocations, int roundNum) throws GameActionException {
        for(Transaction tx : rc.getBlock(roundNum)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == WATERID && (mess[4] == mess[0] + mess[1] + mess[2] + mess[3])){
                // TODO: don't add duplicate locations
                // System.out.println("heard water at [" + mess[2] + ", " + mess[3] + "]");
                waterLocations.add(new MapLocation(mess[2], mess[3]));
            }
        }
        return waterLocations;
    }

// ------------------------------------------------------------------------------------------------ //
// ------------------------------------ SOUP COMMUNICATION ------------------------------------ //
    public void broadcastSoupLocation(MapLocation loc ) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = SOUPID;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        message[4] = message[0] + message[1] + message[2] + message[3];
        if (!Unit.soupLocations.contains(new MapLocation (loc.x, loc.y)) && rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            // System.out.println("new soup!" + loc);
        }
    }
    public void updateSoupLocations(ArrayList<MapLocation> soupLocations) throws GameActionException {
        // if its just been created, go through all of the blocks and transactions to find soup
        if (RobotPlayer.turnCount <= 1) {
            // System.out.println("turncount 1 in updatesouploc");
            for (int i = 1; i < rc.getRoundNum(); i++) {
                getSoupLocInBlock(soupLocations, i);
            }
        } else {
            getSoupLocInBlock(soupLocations, rc.getRoundNum() - 1);
        }
    }
    public void getSoupLocInBlock(ArrayList<MapLocation> soupLocations, int roundNum) throws GameActionException {
        for(Transaction tx : rc.getBlock(roundNum)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == SOUPID  && (mess[4] == mess[0] + mess[1] + mess[2] + mess[3])){
                if (!Unit.soupLocations.contains(new MapLocation (mess[2], mess[3]))) {
                    // System.out.println("heard NEW soup at [" + mess[2] + ", " + mess[3] + "]");
                    soupLocations.add(new MapLocation(mess[2], mess[3]));
                }
            }
        }
    }

// ------------------------------------------------------------------------------------------------ //
// ------------------------------------ BUILDING COMMUNICATION ------------------------------------ //
    // One default if its on our team
    public void broadcastBuildingCreation(RobotType type, MapLocation loc) throws GameActionException {
        // System.out.println("broadcast building creation");
        int SENTID;
        switch (type) {
            case FULFILLMENT_CENTER:         SENTID = AMAZONID;         break;
            case DESIGN_SCHOOL:              SENTID = DESIGNSCHOOLID;   break;
            case HQ:                         SENTID = HQID;             break;
            case NET_GUN:                    SENTID = NETGUNID;         break;
            case REFINERY:                   SENTID = REFINERYID;       break;
            case VAPORATOR:                  SENTID = VAPORATORID;      break;
            default:                         SENTID = 0;                break;
        }

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = SENTID;
        message[2] = loc.x; // x coord of unit
        message[3] = loc.y; // y coord of unit
        message[4] = message[0] + message[1] + message[2] + message[3];
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            // System.out.println("new building! type: " + SENTID + "Location:" + loc);
        }
    }

    //amazon broadcasts the type of drone it just made in the previous round
    public void broadcastTypeOfDrone(int numOfDrones, int roundNum) throws GameActionException{
        int SENTID = 0;
        switch(numOfDrones){
            case 1:             SENTID = DRONEDEFENSE; break; //defensive drones
            case 3:             SENTID = DRONEHELPMINER; break; //transport drone
            case 5:             SENTID = DRONEATTACK; break; //attacking drones
        }

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = DRONEDUTY;
        message[2] = roundNum;
        message[3] = SENTID;

        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }

    //drones that are just created trying to access their role
    public int getDroneDuty(int roundNum) throws GameActionException{
        for(Transaction tx: rc.getBlock(roundNum)){
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == DRONEDUTY && mess[2] == roundNum){
                switch(mess[3]){
                    case DRONEDEFENSE: return 1;
                    case DRONEHELPMINER: return 3;
                    case DRONEATTACK: return 5;
                }
            }
        }
        return 0;
    }


    // Other for potential enemies/enemy HQ that is team specific
    public void broadcastBuildingCreation(RobotType type, MapLocation loc, Team team) throws GameActionException {
        // System.out.println("broadcast building creation");
        int SENTID;
        switch (type) {
            case FULFILLMENT_CENTER:         SENTID = AMAZONID;         break;
            case DESIGN_SCHOOL:              SENTID = DESIGNSCHOOLID;   break;
            case HQ:                         SENTID = HQID;             break;
            case NET_GUN:                    SENTID = NETGUNID;         break;
            case REFINERY:                   SENTID = REFINERYID;       break;
            case VAPORATOR:                  SENTID = VAPORATORID;      break;
            default:                         SENTID = 0;                break;
        }
        if (team == rc.getTeam().opponent()){
            SENTID = EHQID;
        }

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = SENTID;
        message[2] = loc.x; // x coord of unit
        message[3] = loc.y; // y coord of unit
        message[4] = message[0] + message[1] + message[2] + message[3];
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            // System.out.println("new building! type: " + SENTID + "Location:" + loc);
        }
    }

    public void updateBuildingLocations() throws GameActionException {
        if (RobotPlayer.turnCount == 1) {
            // System.out.println("turncount 1 in updateBuildingLoc");
            for (int i = 1; i < rc.getRoundNum(); i++) {   // This could also start at round num and go downwards instead of starting from scratch. Might be better that way. - MZ
//                // System.out.println("crawl chain round " + i);
                getBuildingLocInBlock(i);
                // System.out.println("Im searching all the rounds before I was created");
            }
        } else {
            // System.out.println("Currently updating building locations. Round: " + RobotPlayer.turnCount);
            getBuildingLocInBlock(rc.getRoundNum() - 1);
        }
    }
    public void getBuildingLocInBlock(int roundNum) throws GameActionException {
        ArrayList<MapLocation> buildingLocations = null;
        for (Transaction tx : rc.getBlock(roundNum)) {
            int[] mess = tx.getMessage();
            if (mess[0] == teamSecret && BuildingIDs.contains(mess[1])  && (mess[4] == mess[0] + mess[1] + mess[2] + mess[3])) {
                // System.out.print("Possible new building? Type: " + mess[1] + " at [" + mess[2] + ", " + mess[3] + "].");
                switch(mess[1]){
                    case HQID:  // bruh findHQ is in Unit
                        break;
                    case EHQID:
                        buildingLocations = Unit.ehqLocations;
                        break;
                    case DESIGNSCHOOLID:
                        buildingLocations = Unit.designSchoolLocations;
                        break;
                    case AMAZONID:
                        buildingLocations = Unit.amazonLocations;
                        break;
                    case REFINERYID:
                        buildingLocations = Unit.refineryLocations;
                        break;
                    case VAPORATORID:
                        buildingLocations = Unit.vaporatorLocations;
                        break;
                    default:
                        break;
                }

                if (buildingLocations != null){
                    if (!buildingLocations.contains(new MapLocation(mess[2], mess[3]))) {
                        buildingLocations.add(new MapLocation(mess[2], mess[3]));
                        // System.out.println("New building. Type: " + mess[1] + ".");
                    } else {
                        // System.out.println("Already seen this building. Type " + mess[1]);
                    }
                }

//                switch (mess[1]) {
//                    case 101:
//                        if (!designSchoolLocations.contains(buildingLoc)) {
//                            designSchoolLocations.add(buildingLoc);
//                            // System.out.println("yes, new school");
//                        } else {
//                            // System.out.println("no, school");
//                        }
//                        break;
//                    case 202:
//                        if (!amazonLocations.contains(buildingLoc)) {
//                            amazonLocations.add(buildingLoc);
//                            // System.out.println("yes, new amazon");
//                        } else {
//                            // System.out.println("no, amazon");
//                        }
//                        break;
//                    case 303:
//                        if (!refineryLocations.contains(buildingLoc)) {
//                            refineryLocations.add(buildingLoc);
//                            // System.out.println("yes, new refinery");
//                        } else {
//                            // System.out.println("no, refinery");
//                        }
//                        break;
//                    case 404:
//                        if (!vaporatorLocations.contains(buildingLoc)) {
//                            vaporatorLocations.add(buildingLoc);
//                            // System.out.println("yes, new vaporator");
//                        }
//                        // System.out.println("no, vaporator");
//                        break;
//                    default:
//                        // System.out.println("idk?!?");
//                        break;
            }

        }
    }

// ------------------------------------------------------------------------------------------------ //
// ------------------------------------ OTHER STUFF ------------------------------------ //

    public void jamEnemyComms() throws GameActionException {
        boolean sentMessage = false;
        //for the last 3 turns
        for (int i = 1; i <= 3; i++){
            for (Transaction tx : rc.getBlock(RobotPlayer.turnCount - i)){
                int[] message = tx.getMessage();
                if (((message[0] != teamSecret) && !sentMessage) && lastSpoofedMessage != message){
                    if (rc.canSubmitTransaction(message,1)){
                        rc.submitTransaction(message,1);
                        lastSpoofedMessage = message;
                        sentMessage = true;
                    }
                }
            }
        }

        if (lastSpoofedMessage != null){
            if (!sentMessage){
                if (rc.canSubmitTransaction(lastSpoofedMessage,1)){
                    rc.submitTransaction(lastSpoofedMessage,1);
                }
            }
        }

        // System.out.println(RobotPlayer.turnCount);
    }
}
