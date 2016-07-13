import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.python.core.PyInstance;
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
		//Threshold for mass: similarity<.5 => 0
		//similarity>.5 => sigmoid function
		
		Double[] sim = similarities.toArray(new Double[similarities.size()]);
		String[] recordNums = ids.toArray(new String[ids.size()]);
		Double total = 0.0;
		for(int i=0; i<sim.length;i++){
			if (sim[i]<=.5){sim[i] = 0.0;}
			else{
				sim[i]=1/(1+Math.pow(Math.E, (-1*sim[i])));
				System.out.println(sim[i]);
				total+=sim[i];
			}
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

		PrintWriter w = new PrintWriter("fp_from_sit.txt");
		for(int i=0; i<sim.length;i++){
			w.print(recordNums[i]);
			w.print(" \t");
			w.println(String.format("%.3f", sim[i]));
		}
		w.close();
		
		//Now doing the same stuff to the photo id situation
		
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
				"PREFIX sitterms: <"+ model.getNsPrefixURI("sitterms")+">"+
						"PREFIX biom: <"+ model.getNsPrefixURI("biom")+">"+
						"PREFIX recterms: <"+ model.getNsPrefixURI("recterms")+">"+
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
				System.out.println(photoSim[i]);
				total2+=photoSim[i];
			}
		}
		//If the total is more than 1, normalize
		if (total>1){
			for(int i=0; i<photoSim.length;i++){
				photoSim[i]/=total;
			}
		}
		//Otherwise,remaining mass goes to all
		else{
			photoSim[photoSim.length-1] = 1.0 - total;
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
		//PyObject printData = outMeasures.__call__(massFp);
		PyObject massPhoto = makeMass.__call__(new PyString("photo_from_sit.txt"));
		//PyObject printData2 = outMeasures.__call__(massPhoto);
		
		//Now to combine the masses: currently using Dempster's Rule
		PyObject combinedMass = combine.__call__(massFp, massPhoto);
		PyObject printCombined = outMeasures.__call__(combinedMass);
		
		
		interpreter.close();

		//model.write(System.out, "N3");
	}
}
