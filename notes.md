
# EIT060 - Projekt 2: notes

Certificate format: We use the DN (distinguished name) to differentiate 
between the different user types (patient, nurse, doctor, and government
agency) and to determine a nurse's or a doctor's hospital division.

DN fields used:

* **CN:** unique user ID (might be SSN; should at least be more than just a potentially non-unique "name").
* **O:** organization name (either "Doctors", "Nurses", "Patients", or "Government").
* **OU:** organization unit (division name for doctors and nurses).
