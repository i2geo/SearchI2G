package net.i2geo.onto.tasks;

import net.i2geo.onto.GeoSkillsAccess;

import java.io.File;

public class ConsistencyCheck {

    public static void main(String[] args) throws Exception {
        GeoSkillsAccess gs =
                new GeoSkillsAccess(new File(args[0]).toURI().toASCIIString());
        gs.open();
    }

}
