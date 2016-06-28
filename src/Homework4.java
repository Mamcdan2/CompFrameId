import java.io.InputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

public class Homework4 {

	public static void main(String[] args) {
		Model model = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open("first_dataN.n3");
		if (in == null){
			throw new IllegalArgumentException("File not found");
		}

		//model.read takes an input stream (here in), a base (here none), and the kind of input (language)
		//NOTE: want to specify the language because the default is RDF/XML
		model.read(in,null, "Turtle");

		//Adding John Jones to the file (here instead of by hand so I can practice with jena)
		Resource r = model.createResource("http://somewhere/JohnJones/");
		Property fullName = model.createProperty(model.getNsPrefixURI("vCard"), "FN");
		Property name = model.createProperty(model.getNsPrefixURI("vCard"), "N");
		Property family = model.createProperty(model.getNsPrefixURI("vCard"), "Family");
		Property given = model.createProperty(model.getNsPrefixURI("vCard"), "Given");

		r.addProperty(fullName, "Johnny Jones");
		r.addProperty(name, 
				model.createResource().addProperty(family, "Jones")
				.addProperty(given, "John"));
		//Output the current n3 code to check that john got added right
		model.write(System.out, "N3");

		//Setting up SPARQL query for full names of the Joneses
		StringBuffer qString = new StringBuffer();
		qString.append(
				"PREFIX vcard: <"+ model.getNsPrefixURI("vCard")+">"+
				"SELECT ?x "+
						"WHERE { "
						+ "?y vcard:Family \"Jones\" . "
						+ "?z vcard:N ?y ."
						+ "?z vcard:FN ?x .}");

		Query query = QueryFactory.create(qString.toString());
		QueryExecution qx = QueryExecutionFactory.create(query, model);
		try{
			ResultSet rs = qx.execSelect();
			while(rs.hasNext()){
				System.out.println(rs.nextSolution().get("?x").toString());
			}
		}
		finally{ qx.close();}
		
		//Setting up SPARQL query for family name of all Johns
		
		StringBuffer q = new StringBuffer();
		q.append(
				"PREFIX vcard: <"+ model.getNsPrefixURI("vCard")+">"+
				"SELECT ?x "+
						"WHERE { "
						+ "?y vcard:Given \"John\" . "
						+ "?y vcard:Family ?x .}");

		QueryExecution newQx = QueryExecutionFactory.create(QueryFactory.create(q.toString()), model);
		try{
			ResultSet rs = newQx.execSelect();
			while(rs.hasNext()){
				System.out.println(rs.nextSolution().get("?x").toString());
			}
		}
		finally{ newQx.close();}
	}

}