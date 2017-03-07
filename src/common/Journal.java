package common;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class Journal {
    private String patient, nurse, doctor, division;
    private String medicalData;

    public Journal(String patient, String nurse, String doctor, String division, String medicalData) {
        // (patient/nurse/doctor are all assumed to be unique;
        // they needn't be (solely) names, but could also include,
        // for example, social security numbers)
        this.patient = patient;
        this.nurse = nurse;
        this.doctor = doctor;
        this.division = division;
        this.medicalData = medicalData;
    }

    public String getPatient() {
        return patient;
    }

    public String getNurse() {
        return nurse;
    }

    public String getDoctor() {
        return doctor;
    }

    public String getDivision() {
        return division;
    }

    public String getMedicalData() {
        return medicalData;
    }
    
    public void setMedicalData(String s) {
        medicalData = s;
    }

    public boolean equals(Journal j) {
        return patient.equals(j.patient) && nurse.equals(j.nurse) && doctor.equals(j.doctor)
                && division.equals(j.division) && medicalData.equals(j.medicalData);
    }

    // Encode as base64-encoded string storage format. Includes medical data.
    public String encode() {
        // Seeing as this format isn't likely to change, we do not encode the
        // field names, but rather only the values themselves.
        StringBuilder sb = new StringBuilder();
        encode(sb, true);
        return sb.toString();
    }
    public void encode(StringBuilder sb, boolean includeMedicalData) {
        encodeValue(sb, patient);
        encodeValue(sb, nurse);
        encodeValue(sb, doctor);
        encodeValue(sb, division);
        if (includeMedicalData) {
            encodeValue(sb, medicalData);
        }
        sb.setLength(sb.length() - 1);
    }
    
    private static String separator = ",";

    private void encodeValue(StringBuilder sb, String s) {
        sb.append(Base64.getEncoder().encodeToString(s.getBytes()));
        sb.append(separator);
    }

    // Decode from base64-encoded string storage format.
    // Returns null if the input is malformed.
    static public Journal decode(String input) {
        String[] params = input.split(separator);
        if (params.length < 4) { // medical data is optional
            return null;
        }
        try {
            String patient = decodeValue(params[0]);
            String nurse = decodeValue(params[1]);
            String doctor = decodeValue(params[2]);
            String division = decodeValue(params[3]);
            String medicalData = params.length >= 5 ? decodeValue(params[4]) : "";
            return new Journal(patient, nurse, doctor, division, medicalData);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    static private String decodeValue(String s) throws UnsupportedEncodingException {
        return new String(Base64.getDecoder().decode(s.getBytes()), "UTF-8");
    }
}
