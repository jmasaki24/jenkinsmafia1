package BestBotv3;
import battlecode.common.*;

import java.util.ArrayList;

public class Unit extends Robot {

    Navigation nav;

    public static MapLocation hqLoc;
    public static MapLocation EHqLoc = new MapLocation(-3, -3);
    public static ArrayList<MapLocation> soupLocations = new ArrayList<>();

    public static ArrayList<MapLocation> hqLocations = new ArrayList<>();
    public static ArrayList<MapLocation> ehqLocations = new ArrayList<>();
    public static ArrayList<MapLocation> amazonLocations = new ArrayList<>();
    public static ArrayList<MapLocation> designSchoolLocations = new ArrayList<>();
    public static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
    public static ArrayList<MapLocation> vaporatorLocations = new ArrayList<>();



    public Unit(RobotController r) {
        super(r);
        nav = new Navigation(rc);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        findHQ();
    }

    public void findHQ() throws GameActionException {
        if (hqLoc == null) {
            comms.updateBuildingLocations();
            for (MapLocation location : hqLocations){
                hqLoc = location;
                System.out.println("I got the HQ location from the hqLocations list");
            }
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                    System.out.println("I saw the HQ because I'm near to it");
                }
            }
            if (hqLoc == null) {
                // if still null, search the blockchain
                System.out.println("HQ loc still null bruh");
            }
        }
    }

    void findEHQ() throws GameActionException {
        if (EHqLoc.x < 0 || EHqLoc.y < 0) {
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam().opponent()) {
                    EHqLoc = robot.location;
                    System.out.println("Sending Enemy Location");
                    comms.broadcastBuildingCreation(RobotType.HQ, EHqLoc, rc.getTeam().opponent());
                }
            }
            if (EHqLoc.x < 0 || EHqLoc.y < 0) {
                // if still null, search the blockchain
                System.out.println("Ehq loc still null");
            }
        }
    }
}

//
//    public void getSoupLocations() throws GameActionException {
//        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
//            int[] mess = tx.getMessage();
//            if(mess[0] == comms.teamSecret && mess[1] == 2){
//                soupLocations.add(new MapLocation(mess[2], mess[3]));
//            }
//        }
//    }
//}