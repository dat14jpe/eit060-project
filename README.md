# EIT060 project

## Program usage
Both server and client must be run from the directory containing the `certificates/` directory. The server must be started before the client.

## Certificate format
We use the DN (distinguished name) to differentiate 
between the different user types (patient, nurse, doctor, and government
agency) and to determine a nurse's or a doctor's hospital division.

DN fields used:

* **CN:** unique user ID (might be SSN; should at least be more than just a potentially non-unique "name").
* **O:** organization name (either "Doctors", "Nurses", "Patients", or "Government").
* **OU:** organization unit (division name for doctors and nurses).

The shell script certificates/genkeystore.sh can be used to generate new client certificates (in keystores).
Usage:

```
genkeystore.sh certFileName commonName type division
```

Example (for how to re-generate the included example file DAlice):

```
genkeystore.sh DAlice "Alice Alicesson" Doctors Div1
```
