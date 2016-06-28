import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/*
 * Commenting the hell out of this because the java docs aren't connected to eclipse
 * This is the starter code written by Dr. Esterline probably in the JenaStart.ppt slides
 * Imports are different than anticipated, possibly because I downloaded Jena 3 rather than Jena 2.whatever
 * To get imports to work, first include all jars in the Apache-Jena folder in the buildpath
 * Then just type in the things you're looking for (model in this case) and do default imports
 * org.apache.jena.whatever
 * 
 * Also, apparently jena is pronounced "yay-na"
 * 
 */

public class FirstJena {

	public static void main(String[] args) {
		//RDF Model is a set of statements. Can create resources (uri nodes, bnodes), properties
		Model m = ModelFactory.createDefaultModel();
		String ns = "http://example/test/";
		//Input for createResource is uri
		Resource r = m.createResource(ns + "r");
		//input is either uri or a uri split up into namespace, localname
		Property p = m.createProperty(ns, "p");
		
		r.addProperty(p, "hello world", XSDDatatype.XSDstring);
		
		//First argument is where to write to (file or out?)
		//Second argument is type of notation: some possibilities are Turtle, RDF/XML, N3
		m.write(System.out, "Turtle");
	}

}
