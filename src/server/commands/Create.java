package server.commands;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import common.Individual;
import common.Journal;
import common.Journals;
import server.AuditLog;

public class Create extends Command {
    private String patient, nurse, medicalData;
    
    public Create(String input) throws Exception {
        String[] params = input.split(" ");
        if (params.length < 4) {
            throw new Exception("Too few parameters.");
        }
        
        // Format: patient, nurse, and medical data,
        // in that order and base64-encoded.
        // Doctor and division are retrieved from individual information.
        try {
            patient = decodeParameter(params[1]);
            nurse = decodeParameter(params[2]);
            medicalData = decodeParameter(params[3]);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // - to do: handle format errors in a sensible way
    }

    public boolean hasPermission(Individual i, Journals journals) {
        // Only doctors can create new records.
        return Individual.DOCTOR == i.getType();
    }

    // Return value: unique ID of the newly created journal.
    public String execute(Individual i, Journals journals) {
        String doctor = i.getName(), division = i.getUnit();
        Journal journal = new Journal(patient, nurse, doctor, division, medicalData);
        String id = journals.add(journal);
        AuditLog.log(i, "created record " + id);
        return id;
    }
    
    private String decodeParameter(String paramBase64) throws UnsupportedEncodingException {
        return new String(Base64.getDecoder().decode(paramBase64), "UTF-8");
    }
}
