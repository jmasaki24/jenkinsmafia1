package BestBotv3;

import battlecode.common.*;
import BestBotv3.*;

import java.util.ArrayList;

//Long term goals
//TODO: The goal of this bot is to be as simple and clean as possible to enable future versions to be even better
//Todo: Comment any uncommented piece of code to ensure we know what each section generally does

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount; // number of turns since creation


    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        Robot me = null;

        turnCount = 1;

        switch (rc.getType()) {
            case HQ:                 me = new HQ(rc);           break;
            case MINER:              me = new Miner(rc);        break;
            case REFINERY:           me = new Refinery(rc);     break;
            case VAPORATOR:          me = new Vaporator(rc);    break;
            case DESIGN_SCHOOL:      me = new DesignSchool(rc); break;
            case FULFILLMENT_CENTER: me = new Amazon(rc);     break;
            case LANDSCAPER:         me = new Landscaper(rc);   break;
            case DELIVERY_DRONE:     me = new Drone(rc);         break;
            case NET_GUN:            me = new Shooter(rc);      break;
            default: me = new Robot(rc); break;
        }

        while(true) {
            try {
                me.takeTurn();
                turnCount++;
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception"); // darn
                e.printStackTrace();
            }
        }
    }
}
