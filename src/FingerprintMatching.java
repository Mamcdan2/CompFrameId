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

		model.read(in,null, "N3");

		ArrayList<Double> similarities = new ArrayList<Double>();
		ArrayList<String> ids = new ArrayList<String>();
		//Setting up SPARQL query to print out record numbers and fp similarity measures
		StringBuffer qString = new StringBuffer();
		qString.append(
				"PREFIX sitterms: <"+ model.getNsPrefixURI("sitterms")+">"+
						"PREFIX biom: <"+ model.getNsPrefixURI("biom")+">"+
						"SELECT ?num ?sim "+
						"WHERE { "
						+ "?infon sitterms:simMeasure ?sim ."
						+"?infon sitterms:fpRecorded ?fp ."
						+"?num biom:hasFpImage ?fp ."
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

		//This is my first attempt at a mass function: similarity<.5 => 0 similarity>.5 => sim-.5 remaining mass => all
		//adding another item to the list for all
		similarities.add(0.0);
		ids.add("All");
		Double[] sim = similarities.toArray(new Double[similarities.size()]);
		String[] recordNums = ids.toArray(new String[ids.size()]);
		Double total = 0.0;
		for(int i=0; i<sim.length;i++){
			if (sim[i]<=.5){sim[i] = 0.0;}
			else{
				sim[i]-=.5;
				total+=sim[i];
			}
		}
		//If the total is more than 1, normalize
		if (total>1){
			for(int i=0; i<sim.length;i++){
				sim[i]/=total;
			}
		}
		//Otherwise,remaining mass goes to 1
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
		
		//Using jython to call methods from the ds python code on our fp data
		
		PythonInterpreter interpreter = new PythonInterpreter();
		interpreter.execfile("ds1.py");
		PyObject makeMass = interpreter.get("make_mass");
		PyObject outMeasures = interpreter.get("out_measures");

		PyObject massFp = makeMass.__call__(new PyString("fp_from_sit.txt"));
		PyObject printData = outMeasures.__call__(massFp);
		
		//Probably want to add some code doing comparison or whatever here
		interpreter.close();

		//model.write(System.out, "N3");
	}
}
