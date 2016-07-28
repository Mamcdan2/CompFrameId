import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;


import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

public class FingerprintMatching {

	public static void main(String[] args) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open("FingerprintIdSitNew.n3");
		if (in == null){
			throw new IllegalArgumentException("File not found");
		}

		model.read(in,null, "Turtle");

		ArrayList<Double> similarities = new ArrayList<Double>();
		ArrayList<String> ids = new ArrayList<String>();
		//Setting up SPARQL query to print out record numbers and fp similarity measures
		StringBuffer qString = new StringBuffer();
		qString.append(
				"PREFIX sitterms: <"+ model.getNsPrefixURI("sitterms")+">"+
						"PREFIX biom: <"+ model.getNsPrefixURI("biom")+">"+
						"PREFIX recterms: <"+ model.getNsPrefixURI("recterms")+">"+
						"SELECT ?num ?sim "+
						"WHERE { "
						+ "?infon sitterms:simMeasure ?sim ."
						+"?infon sitterms:fpRecorded ?fp ."
						+"?rec biom:hasFpImage ?fp ."
						+"?num recterms:hasRecord ?rec ."
						+"}");

		Query query = QueryFactory.create(qString.toString());
		QueryExecution qx = QueryExecutionFactory.create(query, model);
		try{
			ResultSet rs = qx.execSelect();
			while(rs.hasNext()){
				QuerySolution sol = rs.nextSolution();
				System.out.print(sol.get("?num").toString()+", ");
				System.out.println(sol.get("?sim").toString());
				similarities.add(Double.valueOf(sol.get("?sim").toString()));
				String id = sol.get("?num").toString();
				ids.add(id.substring(id.indexOf('#')+1));
			}
		}
		finally{ qx.close();}

		//adding another item to the list for all
		similarities.add(0.0);
		ids.add("All");

		//Modified mass function:
		//Sigmoid function then
		//Threshold for mass: similarity<.5 => 0

		Double[] sim = similarities.toArray(new Double[similarities.size()]);
		String[] recordNums = ids.toArray(new String[ids.size()]);
		Double total = 0.0;
		for(int i=0; i<sim.length;i++){
			//NEED TO SHIFT THE SIGMOID FUNCTION
				sim[i]=1/(1+Math.pow(Math.E, 5*(-1*sim[i])+1));
				if (sim[i]<=.5){sim[i] = 0.0;}
				System.out.print(recordNums[i] + ", ");
				System.out.println(sim[i]);
				total+=sim[i];
			
		}

		//If the total is more than 1, normalize
		if (total>1){
			for(int i=0; i<sim.length;i++){
				sim[i]/=total;
			}
		}
		//Otherwise,remaining mass goes to all
		else{
			sim[sim.length-1] = 1.0 - total;
		}

		PrintWriter w0 = new PrintWriter("unmodified_fp.txt");
		for(int i=0; i<sim.length;i++){
			w0.print(recordNums[i]);
			w0.print(" \t");
			w0.println(String.format("%.3f", sim[i]));
		}
		w0.close();
		
		/*
		 * Quick outline of following mass measures for the fpsit
		 * 
		 *  modification happens *after* both the mass function and normalization
		 *
		 * Situation: s1: id-situation
		 * Officer: sitterms:fpAnalyst
		 * Objects: recorded fp, observed fp
		 * 
		 * Situation: s3*: fingerprint taking
		 * This one has different situations for each fp: modify individual masses
		 * Officer: sitterms:fpTakingOfficer
		 * Objects: recorded fp
		 * 
		 * Situation: s4: fingerprint left
		 * No attached infons at this time
		 * Need to check on copy of observed fp
		 * This situation is the same for all suspects and therefore
		 * 		 should really be a weight on the piece of evidence,
		 * 		 not on the individual suspects
		 * (Isn't it convenient that we aren't including evidence weights yet when we don't have this info yet)
		 * 
		 */
		//Looping through each suspect and modifying them based on their fp files
		/*
		 * A quick thought:
		 * If I were to organize the code the way you'd organize your thoughts
		 * The first query would be to pull out the name of the fp file for the particular suspect from their record
		 * (biom:hasFpImage)
		 * THEN you'd go to all infons that include that fp file
		 * (And see if there's an officer involved, which requires either regexs or knowledge of the RDF)
		 */

		for(int i=0; i<recordNums.length-1; i++){
			String idNum = recordNums[i];

			//SITUATION 1 MASS MODIFICATION for analyst ratings
			StringBuffer s1qString = new StringBuffer();
			s1qString.append(
					"PREFIX sitterms: <"+ model.getNsPrefixURI("sitterms")+">"+
							"PREFIX lawterms: <"+ model.getNsPrefixURI("lawterms")+">"+
							"PREFIX biom: <"+ model.getNsPrefixURI("biom")+">"+
							"PREFIX recterms: <"+ model.getNsPrefixURI("recterms")+">"+
							"PREFIX insys: <"+ model.getNsPrefixURI("insys")+">"+
							"SELECT ?rel "+
							"WHERE { "
							+"insys:"+idNum + " recterms:hasRecord ?rec ."
							+"?rec biom:hasFpImage ?fp ."
							+"?infon sitterms:fpRecorded ?fp ."
							+"?infon sitterms:fpAnalyst ?officer ."
							+"?officer lawterms:Reliability ?rel ."
							+"}");

			Query s1query = QueryFactory.create(s1qString.toString());
			QueryExecution s1qx = QueryExecutionFactory.create(s1query, model);
			try{
				ResultSet rs = s1qx.execSelect();
				while(rs.hasNext()){
					QuerySolution sol = rs.nextSolution();
					sim[sim.length-1] += sim[i]*(1 - Double.valueOf(sol.get("?rel").toString()));
					sim[i] *= Double.valueOf(sol.get("?rel").toString());
				}
			}
			finally{ s1qx.close();}

			//SITUATION 3* MASS MODIFICATION
			StringBuffer s3qString = new StringBuffer();
			s3qString.append(
					"PREFIX sitterms: <"+ model.getNsPrefixURI("sitterms")+">"+
							"PREFIX lawterms: <"+ model.getNsPrefixURI("lawterms")+">"+
							"PREFIX biom: <"+ model.getNsPrefixURI("biom")+">"+
							"PREFIX recterms: <"+ model.getNsPrefixURI("recterms")+">"+
							"PREFIX insys: <"+ model.getNsPrefixURI("insys")+">"+
							"SELECT ?rel "+
							"WHERE { "
							+"?infon sitterms:fpSubject insys:"+idNum + " ."
							+"?infon sitterms:fpTakingOfficer ?officer ."
							+"?officer lawterms:Reliability ?rel ."
							+"}");

			Query s3query = QueryFactory.create(s3qString.toString());
			QueryExecution s3qx = QueryExecutionFactory.create(s3query, model);
			try{
				ResultSet rs = s3qx.execSelect();
				while(rs.hasNext()){
					QuerySolution sol = rs.nextSolution();
					sim[sim.length-1] += sim[i]*(1 - Double.valueOf(sol.get("?rel").toString()));
					sim[i] *= Double.valueOf(sol.get("?rel").toString());
				}
			}
			finally{ s1qx.close();}
		}

		PrintWriter w = new PrintWriter("fp_from_sit.txt");
		for(int i=0; i<sim.length;i++){
			w.print(recordNums[i]);
			w.print(" \t");
			w.println(String.format("%.3f", sim[i]));
		}
		w.close();


		//Coming up with masses for the photo id situation

		Model model2 = ModelFactory.createDefaultModel();
		InputStream in2 = FileManager.get().open("PhotoIdSit.n3");
		if (in2 == null){
			throw new IllegalArgumentException("File not found");
		}

		model2.read(in2,null, "Turtle");

		ArrayList<Double> photoSimilarities = new ArrayList<Double>();
		ArrayList<String> photoIds = new ArrayList<String>();
		//Setting up SPARQL query to print out record numbers and photo similarity measures
		StringBuffer qString2 = new StringBuffer();
		qString2.append(
				"PREFIX sitterms: <"+ model2.getNsPrefixURI("sitterms")+">"+
						"PREFIX biom: <"+ model2.getNsPrefixURI("biom")+">"+
						"PREFIX recterms: <"+ model2.getNsPrefixURI("recterms")+">"+
						"SELECT ?num ?sim "+
						"WHERE { "
						+ "?infon sitterms:simPicMeasure ?sim ."
						+"?infon sitterms:picRecorded ?fp ."
						+"?rec biom:hasFacialImage ?fp ."
						+"?num recterms:hasRecord ?rec ."
						+"}");

		Query query2 = QueryFactory.create(qString2.toString());
		QueryExecution qx2 = QueryExecutionFactory.create(query2, model2);
		try{
			ResultSet rs = qx2.execSelect();
			while(rs.hasNext()){
				QuerySolution sol = rs.nextSolution();
				System.out.print(sol.get("?num").toString()+", ");
				System.out.println(sol.get("?sim").toString());
				photoSimilarities.add(Double.valueOf(sol.get("?sim").toString()));
				String photoId = sol.get("?num").toString();
				photoIds.add(photoId.substring(photoId.indexOf('#')+1));
			}
		}
		finally{ qx2.close();}

		//adding another item to the list for all
		photoSimilarities.add(0.0);
		photoIds.add("All");

		//Modified mass function:
		//Threshold for mass: similarity<.5 => 0
		//similarity>.5 => sigmoid function

		Double[] photoSim = photoSimilarities.toArray(new Double[photoSimilarities.size()]);
		String[] suspectNums = photoIds.toArray(new String[photoIds.size()]);
		Double total2 = 0.0;
		for(int i=0; i<photoSim.length;i++){
			if (photoSim[i]<=.5){photoSim[i] = 0.0;}
			else{
				photoSim[i]=1/(1+Math.pow(Math.E, (-1*photoSim[i])));
				total2+=photoSim[i];
			}
		}
		//If the total is more than 1, normalize
		if (total2>1){
			for(int i=0; i<photoSim.length;i++){
				photoSim[i]/=total2;
			}
		}
		//Otherwise,remaining mass goes to all
		else{
			photoSim[photoSim.length-1] = 1.0 - total;
		}

		/*
		 * Quick outline of following mass measures for the photo situation
		 * 
		 * Again, need to discuss *when* to modify
		 * 
		 * Situation: s2: id-situation
		 * Officer: sitterms:picAnalyst
		 * Objects: mugshot, group photo
		 * 
		 * Situation: s6*: mugshot taking
		 * Officer: lawterms:AdminOfficer
		 * Objects: mugshot
		 * 
		 * Situation: s5: group photo
		 * We're currently assuming no copying, transfers, etc.
		 * So the group photo (evidence-level mass change) has no chain of custody
		 * 
		 */

		for(int i=0; i<suspectNums.length; i++){
			String idNum = suspectNums[i];
			//System.out.println(idNum);

			//Situation 2 Mass Modification
			StringBuffer s2qString = new StringBuffer();
			s2qString.append(
					"PREFIX sitterms: <"+ model.getNsPrefixURI("sitterms")+">"+
							"PREFIX lawterms: <"+ model.getNsPrefixURI("lawterms")+">"+
							"PREFIX biom: <"+ model.getNsPrefixURI("biom")+">"+
							"PREFIX recterms: <"+ model.getNsPrefixURI("recterms")+">"+
							"PREFIX insys: <"+ model.getNsPrefixURI("insys")+">"+
							"SELECT ?rel "+
							"WHERE { "
							+"insys:"+idNum + " recterms:hasRecord ?rec ."
							+"?rec biom:hasFacialImage ?pic ."
							+"?infon sitterms:picRecorded ?pic ."
							+"?infon sitterms:picAnalyst ?officer ."
							+"?officer lawterms:Reliability ?rel ."
							+"}");

			Query s2query = QueryFactory.create(s2qString.toString());
			QueryExecution s2qx = QueryExecutionFactory.create(s2query, model2);
			try{
				ResultSet rs = s2qx.execSelect();
				while(rs.hasNext()){
					QuerySolution sol = rs.nextSolution();
					//System.out.println(sol.get("?rel").toString());
					photoSim[photoSim.length-1] += photoSim[i]*(1 - Double.valueOf(sol.get("?rel").toString()));
					photoSim[i] *= Double.valueOf(sol.get("?rel").toString());
				}
			}
			finally{ s2qx.close();}

			//Situation 6* mass modifications

			StringBuffer s6qString = new StringBuffer();
			s6qString.append(
					"PREFIX sitterms: <"+ model.getNsPrefixURI("sitterms")+">"+
							"PREFIX lawterms: <"+ model.getNsPrefixURI("lawterms")+">"+
							"PREFIX biom: <"+ model.getNsPrefixURI("biom")+">"+
							"PREFIX recterms: <"+ model.getNsPrefixURI("recterms")+">"+
							"PREFIX insys: <"+ model.getNsPrefixURI("insys")+">"+
							"SELECT ?rel "+
							"WHERE { "
							+"insys:"+idNum + " recterms:hasRecord ?rec ."
							+"?rec biom:hasFacialImage ?pic ."
							+"?infon sitterms:mugRecorded ?pic ."
							+"?infon sitterms:mugTakingOfficer ?officer ."
							+"?officer lawterms:Reliability ?rel ."
							+"}");

			Query s6query = QueryFactory.create(s6qString.toString());
			QueryExecution s6qx = QueryExecutionFactory.create(s6query, model2);
			try{
				ResultSet rs = s6qx.execSelect();
				while(rs.hasNext()){
					QuerySolution sol = rs.nextSolution();
					//System.out.println(sol.get("?rel").toString());
					photoSim[photoSim.length-1] += photoSim[i]*(1 - Double.valueOf(sol.get("?rel").toString()));
					photoSim[i] *= Double.valueOf(sol.get("?rel").toString());
				}
			}
			finally{ s6qx.close();}
		}

		PrintWriter w2 = new PrintWriter("photo_from_sit.txt");
		for(int i=0; i<photoSim.length;i++){
			w2.print(suspectNums[i]);
			w2.print(" \t");
			w2.println(String.format("%.3f", photoSim[i]));
		}
		w2.close();

		//Using jython to call methods from the ds python code on our fp data

		PythonInterpreter interpreter = new PythonInterpreter();
		interpreter.execfile("ds1.py");
		PyObject makeMass = interpreter.get("make_mass");
		PyObject outMeasures = interpreter.get("out_measures");
		PyObject combine = interpreter.get("combine");

		PyObject massFp = makeMass.__call__(new PyString("fp_from_sit.txt"));
		PyObject printData = outMeasures.__call__(massFp);
		PyObject massPhoto = makeMass.__call__(new PyString("photo_from_sit.txt"));
		PyObject printData2 = outMeasures.__call__(massPhoto);

		//Now to combine the masses: currently using Dempster's Rule
		PyObject combinedMass = combine.__call__(massFp, massPhoto);
		PyObject printCombined = outMeasures.__call__(combinedMass);


		interpreter.close();

		//model.write(System.out, "N3");
	}
}
