Branch of the computational framework for identity to deal with numerical data.

The semantic web files are unchanged except for the following:
- FingerprintIdSitNew.n3 -- modified to have multiple suspects and officers, with reliability values for the officers
- PhotoIdSit.n3 -- modified in the same ways as the fingerprint situation
- LawEnforcement.rdfs -- the property of reliability is added

The java file FingerprintMatching.java and its surrounding package contain the SPARQL and java code for handling masses.

ds1.py is the latest python code for Dempster-Shafer Theory, which includes a number of combination rules and
outputs of belief and plausibility given masses. Some of its methods are called in FingerprintMatching.java using jython.

The following .txt files are intermediary files that both give a human-readable output of masses and 
act as input for the python files:
- photo_from_sit.txt
- fp_from_sit.txt
- unmodified_fp.txt
