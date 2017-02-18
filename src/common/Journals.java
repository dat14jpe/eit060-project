package common;

import java.util.Map;
import java.util.Set;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

public class Journals {
    // Each record has a unique ID; to ensure uniqueness across time
    // and deletions, we store all previously used IDs.
    private Set<String> usedIds;
    private SecureRandom rand;
    
    // All journals, indexed by their unique IDs.
    private Map<String, Journal> journals;
    
    // Lists of journal IDs indexed by patient/nurse/doctor "name".
    private Map<String, ArrayList<String>> personJournals;
    
    // Lists of journal IDs indexed by divisions.
    private Map<String, ArrayList<String>> divisionJournals;
    
    // Save to file.
    public void save(String fileName) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(fileName);
        for (Map.Entry<String, Journal> entry : journals.entrySet()) {
            out.println(entry.getKey() + " " + entry.getValue().encode());
        }
        out.flush();
        out.close();
    }
    
    // Load from file.
    public static Journals load(String fileName) throws IOException {
        Journals journals = new Journals();
        try (BufferedReader in = new BufferedReader(new FileReader(fileName))) {
            String line;
            while (null != (line = in.readLine())) {
                int space = line.indexOf(' ');
                String id = line.substring(0, space);
                Journal journal = Journal.decode(line.substring(space + 1));
                journals.add(journal, id);
            }
        }
        return journals;
    }

    public Journals() {
        usedIds = new HashSet<>();
        rand = new SecureRandom();
        journals = new HashMap<>();
        personJournals = new HashMap<>();
        divisionJournals = new HashMap<>();
    }
    
    // Returns a list of all division names.
    public ArrayList<String> getDivisions() {
        ArrayList<String> divisions = new ArrayList<String>();
        for (Map.Entry<String, ArrayList<String>> entry : divisionJournals.entrySet()) {
            divisions.add(entry.getKey());
        }
        return divisions;
    }
    
    // Returns a list of the unique journal IDs of all journals
    // associated with the input individual (by name).
    // Returns null if there are no such journal IDs.
    public ArrayList<String> get(Individual i) {
        // - create copy of list? No, we probably needn't bother
        System.out.println("Returning by name " + i.getName());
        return personJournals.get(i.getName());
    }
    
    // Returns a list of journal IDs associated with a given division.
    // Returns null if no such IDs exist.
    public ArrayList<String> getByDivision(String division) {
        return divisionJournals.get(division);
    }
    
    // Retrieves a journal with a specific unique ID.
    // Returnes null if there is no journal with the given ID.
    public Journal get(String id) {
        return journals.get(id);
    }

    // Adds a journal to the database.
    // Return value: newly-generated unique ID of the journal.
    public String add(Journal journal) {
        String id = generateId();
        add(journal, id);
        return id;
    }
    
    private void add(Journal journal, String id) {
        journals.put(id, journal);
        addIndex(personJournals, journal.getPatient(), id);
        addIndex(personJournals, journal.getNurse(), id);
        addIndex(personJournals, journal.getDoctor(), id);
        addIndex(divisionJournals, journal.getDivision(), id);
    }
    
    // Removes a journal (and all of its individual-to-journal associations).
    // Returns true if there was a journal by this ID.
    public boolean remove(String id) {
        Journal journal = journals.get(id);
        if (null == journal) {
            return false;
        }
        journals.remove(id);
        removeIndex(personJournals, journal.getPatient(), id);
        removeIndex(personJournals, journal.getNurse(), id);
        removeIndex(personJournals, journal.getDoctor(), id);
        removeIndex(divisionJournals, journal.getDivision(), id);
        return true;
    }
    
    // Add a journal to a name-indexed list of associated journals.
    // Create journal list if it doesn't exist for this name.
    private void addIndex(Map<String, ArrayList<String>> index, String individual, String id) {
        if (!index.containsKey(individual)) {
            System.out.println("Adding index for " + individual + " (id " + id + ")");
            ArrayList<String> list = new ArrayList<String>();
            list.add(id);
            index.put(individual, list);
        } else {
            index.get(individual).add(id);
        }
    }
    
    // Removes a journal from an individual's associated list of journals.
    // Also removes the entire journal list for this individual, if it becomes empty.
    private void removeIndex(Map<String, ArrayList<String>> index, String individual, String id) {
        if (!index.containsKey(individual)) {
            return;
        }
        index.get(individual).remove(id);
        if (0 == index.get(individual).size()) {
            index.remove(individual);
        }
    }
    
    // Generate 8-character (base64-encoded) unique ID.
    private String generateId() {
        byte[] randomBytes = new byte[6];
        String id;
        // - to do: should probably have a maximum number of iterations
        // (but what happens if/when the limit is exceeded?)
        do {
            rand.nextBytes(randomBytes);
            id = Base64.getEncoder().encodeToString(randomBytes);
        } while (usedIds.contains(id));
        usedIds.add(id);
        return id;
    }
}
