package analyse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.MutableGraph;
import model.Noeud;
import model.Relation;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import static guru.nidi.graphviz.model.Factory.*;

public class GraphVizMain {

	public static void main(String[] args) throws IOException {

		
		/*------------------------------------------------------------------------*/
		MutableGraph graphDeDependances = mutGraph("graphe de dépendances").setDirected(true);
		//graphDeDependances.add(mutNode(name))
		
		System.out.println("Begin Analysis");

		// Parsing arguments using JCommander
		Arguments arguments = new Arguments();
		boolean isParsed = arguments.parseArguments(args);

		// if there was a problem parsing the arguments then the program is terminated.
		if(!isParsed)
			return;
		
		// Parsed Arguments
		String experiment_source_code = arguments.getSource();
		String experiment_output_filepath = arguments.getTarget();
		
		// Load project (APP_SOURCE only, no TEST_SOURCE for now)
		Launcher launcher = null;
		if(arguments.isMavenProject() ) {
			launcher = new MavenLauncher(experiment_source_code, MavenLauncher.SOURCE_TYPE.APP_SOURCE); // requires M2_HOME environment variable
		}else {
			launcher = new Launcher();
			launcher.addInputResource(experiment_source_code + "/src");
		}
		
		// Setting the environment for Spoon
		Environment environment = launcher.getEnvironment();
		environment.setCommentEnabled(true); // represent the comments from the source code in the AST
		environment.setAutoImports(true); // add the imports dynamically based on the typeReferences inside the AST nodes.
//		environment.setComplianceLevel(0); // sets the java compliance level.
		
		System.out.println("Run Launcher and fetch model.");
		launcher.run(); // creates model of project
		CtModel model = launcher.getModel(); // returns the model of the project

		
		
		//Nombre de classes de l'application
		List<CtClass> classList = model.getElements(new TypeFilter<CtClass>(CtClass.class));
		//System.out.println(classList.get(0).getSimpleName());
		
		
		
	/***************CONSTRUCTION DU GRAPH************************************/
		

		Map<String, CtClass> mapClasses = new HashMap<String, CtClass>();
		
		
		for(CtClass cls : classList) {
			mapClasses.put(cls.getSimpleName(), cls);
		}
		
		
		
		HashMap<String, Integer> maprelation = new HashMap<String, Integer>();
		int poidsTotal = 0;
		
		List<Relation> lisRelation = new ArrayList<Relation>();
		
		for(CtClass class1 : classList) {
			//graphDeDependances.add(mutNode(class1.getSimpleName()));
			Set<CtMethod> methods = class1.getMethods();
			//AMODIFIER POUR VOIR APRES DIFF
			String nomClass1 = class1.getSimpleName();
			for(CtMethod method1 : methods) {
				//System.out.println(class1.getSimpleName()+"======================="+method.getSimpleName());
				String nomMethod1 = method1.getSimpleName();
				for(CtInvocation invocation : method1.getElements(new TypeFilter<CtInvocation>(CtInvocation.class))){
					String nomMethod2 = invocation.getExecutable().getSimpleName();
					String nomClass2 = "";
					if(invocation.getExecutable().isStatic()) {
						nomClass2 = invocation.getTarget().toStringDebug();
						//nomClass2 = split(nomClass2,".");
						
					}else {
						nomClass2 = invocation.getTarget().getType().toString();
						
					}
					String[] tab = nomClass2.split("\\.");
					if(tab.length > 0) {
						nomClass2 = tab[tab.length -1];
					}
						
					if((!nomClass1.equals(nomClass2)) && (mapClasses.containsKey(nomClass2.trim())) ) {
						String cle = nomClass1+"="+nomClass2;
						String cle2 = nomClass2+"="+nomClass1;
						if(maprelation.containsKey(cle)) {
							int poids = maprelation.get(cle);
							maprelation.put(cle, poids+1);
						}else if(maprelation.containsKey(cle2)) {
							int poids = maprelation.get(cle2);
							maprelation.put(cle2, poids+1);
						}
						
						else {
							maprelation.put(cle, 1);
						}
						poidsTotal++;
					}
				}	
			}
		}
		
		
		PrintWriter pr = new PrintWriter(new FileWriter("sortie-de-graphe.dot")); 
		
		pr.println("graph G {");

		
		for(String cle : maprelation.keySet()) {
			String nomCls1 = cle.split("=")[0];
			String nomCls2 = cle.split("=")[1];
			
			int poids = maprelation.get(cle);
			double p = (double) poids/poidsTotal;
			
			lisRelation.add(new Relation(nomCls1, nomCls2, p));
			pr.println(nomCls1+" -- "+nomCls2+" [label=\" "+p+" \"]; ");
			
		}

		
		
		pr.println("}");
		pr.close();
		
		List<Noeud> listNoeud = new ArrayList<Noeud>();
		for(Relation r : lisRelation) {
			Noeud n1 = listNoeud.stream().filter(no -> no.getClasses().contains(r.getClass1())).findAny().orElse(null);
			Noeud n2 = listNoeud.stream().filter(no -> no.getClasses().contains(r.getClass2())).findAny().orElse(null);
			
			if(n1 == null) {
				Noeud node = new Noeud();
				node.addClasse(r.getClass1());
				listNoeud.add(node);
			}
			
			if(n2 == null) {
				Noeud node = new Noeud();
				node.addClasse(r.getClass2());
				listNoeud.add(node);
			}
		}
		
		
		int siz = listNoeud.size();
		while (siz > 1) {
			System.out.println("JE RENTRE  ");
			double poidsMax = 0;
			
			
			int i = 0;
			int j = 0;
		
			for(Relation r : lisRelation) {
				if(r.getPoids() > poidsMax) {
					poidsMax = r.getPoids();
					j = i;
				}
				i++;
			}
			
			
			
			try {
				Relation relationMax = lisRelation.get(j);
				lisRelation.remove(j);
				
				List<String> clss1  = listNoeud.stream().filter(no -> no.getClasses().contains(relationMax.getClass1())).findFirst().get().getClasses();
				List<String> clss  = listNoeud.stream().filter(no -> no.getClasses().contains(relationMax.getClass2())).findFirst().get().getClasses();
				
				for(String n : clss) {
					clss1.add(n);
				}
				
				Noeud n2 = listNoeud.stream().filter(no -> no.getClasses().contains(relationMax.getClass2())).findAny().orElse(null);
				listNoeud.remove(n2);
				
				siz = listNoeud.size();
				
			}catch (Exception e) {
				System.out.println("  ");
			}
			
			
		}
		
		
		
		
		try {
			Graphviz.fromGraph(graphDeDependances).width(7500)/*.height(5000)*/.render(Format.PNG).toFile(new File("./graphe_de_dependances.png"));
			Graphviz.fromGraph(graphDeDependances).width(7500)/*.height(5000)*/.render(Format.DOT).toFile(new File("./graphe_de_dependances.dot"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
}
