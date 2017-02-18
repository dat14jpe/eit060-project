package common;

import java.util.Base64;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

// - Maybe make interface/abstract class, and have classes for different
// types? Or maybe that's just overkill.

public class Individual {
    public static final int INVALID = 0, PATIENT = 1, NURSE = 2, DOCTOR = 3, GOVERNMENT = 4;

    public static String typeString(int type) {
        switch (type) {
        case PATIENT:
            return "Patient";
        case NURSE:
            return "Nurse";
        case DOCTOR:
            return "Doctor";
        case GOVERNMENT:
            return "Government";
        default:
            return "Invalid";
        }
    }

    private String name, organization, unit;
    private int type;

    // dn is the individual's distinguished name (from the certificate).
    public Individual(String dn) throws InvalidNameException {
        // Find name, organization, and unit.
        LdapName ln = new LdapName(dn);
        String cn = null, o = null, ou = null;
        for (Rdn rdn : ln.getRdns()) {
            switch (rdn.getType().toUpperCase()) {
            case "CN":
                cn = (String) rdn.getValue();
                break;
            case "O":
                o = (String) rdn.getValue();
                break;
            case "OU":
                ou = (String) rdn.getValue();
                break;
            }
        }
        if (null == cn || null == o || null == ou) {
            throw new InvalidNameException("Malformed distinguished name");
        }

        name = cn;
        organization = o;
        unit = ou;

        // - The hardcoded strings in here should probably be moved
        // to some kind of configuration file (at least if this were
        // more than a small school project).
        switch (o.toLowerCase()) {
        case "nurses":
            type = NURSE;
            break;
        case "doctors":
            type = DOCTOR;
            break;
        case "patients":
            type = PATIENT;
            break;
        case "government":
            type = GOVERNMENT;
            break;
        default:
            throw new InvalidNameException("Unknown organization \"" + o + "\".");
        }
    }

    private Individual(int t, String n, String o, String ou) {
        type = t;
        name = n;
        organization = o;
        unit = ou;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getOrganization() {
        return organization;
    }

    public String getUnit() {
        return unit;
    }

    public String encode() {
        Base64.Encoder encoder = Base64.getEncoder();
        return type + " " + encoder.encodeToString(name.getBytes()) + " "
                + encoder.encodeToString(organization.getBytes()) + " " + encoder.encodeToString(unit.getBytes());
    }

    public static Individual decode(String s) {
        String[] values = s.split(" "); // - should be at least 4 elements long
        return new Individual(Integer.parseInt(values[0]), decodeValue(values[1]), decodeValue(values[2]), decodeValue(values[3]));
    }
    
    private static String decodeValue(String s) {
        return new String(Base64.getDecoder().decode(s));
    }
}
