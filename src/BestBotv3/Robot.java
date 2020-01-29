package BestBotv3;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.Map;


public class Robot {
    RobotController rc;
    Communications comms;
    Navigation nav;
    final int ARBITRARY_SOUP_NUMBER_LMAO = 650;

    int turnCount = 0;
    MapLocation myLoc;


    final int teamSecret = -817280;
    final int MINERID = -887;
    final int DRONEID = -142;
    final int LANDSCAPERID = -242;

    ArrayList<Integer> miners_ids_us = new ArrayList<Integer>();
    ArrayList<Integer> landscapers_ids_us = new ArrayList<Integer>();
    ArrayList<Integer> drones_ids_us = new ArrayList<Integer>();

    public Robot(RobotController r) {
        this.rc = r;
        comms = new Communications(rc);
        myLoc = rc.getLocation();
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
        myLoc = rc.getLocation();
//        comms.updateBuildingLocations();
        updateUnitCounts();
    }

    // THIS METHOD REQUIRES THAT THE ROBOT CAN SEE ALL TILES ADJACENT TO SOUP!!!!!!!!!!!!!!!!!!!!!!!!!!
    // if a tile adjacent to soup exists and is not flooded, it is accessible
    boolean isSoupInMoat(MapLocation soupLoc) throws GameActionException {
        surroundingLocs = new ArrayList<MapLocation>();

        for (int i = 0; i<8; i++){
            if (rc.onTheMap(soupLoc.add(Util.directions[i]))) {
                surroundingLocs.add(soupLoc.add(Util.directions[i]));
            }
        }

        for (MapLocation loc: surroundingLocs) {
            if (rc.canSenseLocation(loc) && !rc.senseFlooding(loc)) { // in theory should not need the canSense
                return true;
            }
        }

        return false;
    }

    // THIS METHOD REQUIRES THAT THE ROBOT CAN SEE ALL TILES ADJACENT TO SOUP!!!!!!!!!!!!!!!!!!!!!!!!!!
    // this method could be done MUCH MUCH better, I think. sorry in advance.
    // if a tile adjacent to soup is not flooded, it is accessible
    // returns true if soup is accessible 
    // TODO: 1/21/2020 Figure out if there is a direct path to the soup or not - matt
    // This no longer produces null pointer exceptions for miners with soup on the edge i think.
    public static ArrayList<MapLocation> surroundingLocs;
    public boolean isSoupAccessible(MapLocation soupLoc) throws GameActionException {
        for (int i = 0; i<8; i++){
            if (       soupLoc.add(Util.directions[i]).x <= rc.getMapWidth()
                    && soupLoc.add(Util.directions[i]).x >= 0
                    && soupLoc.add(Util.directions[i]).y <= rc.getMapHeight()
                    && soupLoc.add(Util.directions[i]).y >= 0){
                surroundingLocs.add(soupLoc.add(Util.directions[i]));
            }
        }

        boolean isAccessible = false;
        for (MapLocation loc : surroundingLocs) {


            // if tile is not flooded, doesn't have a building, then true

            // if there is flooding, don't bother checking for a building. saves bytecode
            if (nav.isOnMap(myLoc.directionTo(loc)) && !rc.senseFlooding(loc)) {
                boolean tileHasABuilding = false;
                RobotInfo robotOnLoc = rc.senseRobotAtLocation(loc);
                if (robotOnLoc != null) {
                    switch (robotOnLoc.type) {
                        case HQ:
                        case DESIGN_SCHOOL:
                        case FULFILLMENT_CENTER:
                        case NET_GUN:
                        case REFINERY:
                        case VAPORATOR:
                            tileHasABuilding = true;
                            break;
                        default: break;
                    }
                }

                // if there is no flooding and no building, than you can stand on that tile.
                // if you can stand on ONE adj. tile, then it is accessible
                isAccessible = isAccessible || !tileHasABuilding ;
            }

            // if its accessible, don't bother checking all of the other tiles.
            if (isAccessible) {
                return isAccessible;
            }
        }
        return isAccessible;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }
        return false;
    }

    void broadcastUnitCreation(RobotInfo botInfo) throws GameActionException {
        int SENTID;
        switch (botInfo.type) {
            case COW:                     SENTID = 1;                break;
            case DELIVERY_DRONE:          SENTID = DRONEID;          break;
            case LANDSCAPER:              SENTID = LANDSCAPERID;     break;
            case MINER:                   SENTID = MINERID;          break;
            default:                      SENTID = 0;                break;
        }

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = SENTID;
        message[2] = botInfo.ID;
        message[3] = message[0] + message[1] + message[2];
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            // System.out.println("new building! type: " + SENTID + "Location:" + loc);
        }

    }

    void updateUnitCounts() throws GameActionException {
        if (RobotPlayer.turnCount == 1) {
            // System.out.println("turncount 1 in updateBuildingLoc");
            for (int i = 1; i < rc.getRoundNum(); i++) {   // This could also start at round num and go downwards instead of starting from scratch. Might be better that way. - MZ
//                // System.out.println("crawl chain round " + i);
                getUnitCreationInBlock(i);
                // System.out.println("Im searching all the rounds before I was created");
            }
        } else {
            // System.out.println("Currently updating building locations. Round: " + RobotPlayer.turnCount);
            getUnitCreationInBlock(rc.getRoundNum() - 1);
        }
    }
    void getUnitCreationInBlock (int roundNum) throws GameActionException {
        for (Transaction tx : rc.getBlock(roundNum)) {
            int[] mess = tx.getMessage();
            if (mess[0] == teamSecret && (mess[0] + mess[1] + mess[2] == mess[3])) {
                // System.out.print("Possible new building? Type: " + mess[1] + " at [" + mess[2] + ", " + mess[3] + "].");
                switch (mess[1]) {
                    case MINERID:
                        if (!miners_ids_us.contains(mess[2])) {
                            System.out.println("m_total: " + miners_ids_us.size());
                            miners_ids_us.add(mess[2]);
                        }
                        break;
                    case LANDSCAPERID:
                        if (!landscapers_ids_us.contains(mess[2])) {
                            System.out.println("l_total: " + landscapers_ids_us.size());
                            landscapers_ids_us.add(mess[2]);
                        }
                        break;
                    case DRONEID:
                        if (!drones_ids_us.contains(mess[2])) {
                            System.out.println("d_total: " + drones_ids_us.size());
                            drones_ids_us.add(mess[2]);
                        }
                        break;
                    default:
                        break;
                }


            }
        }
    }
}