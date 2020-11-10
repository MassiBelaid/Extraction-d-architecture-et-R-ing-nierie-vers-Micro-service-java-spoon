package analyse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
		

		Map<String, CtMethod> mapMethds = new HashMap<String, CtMethod>();
		
		
		for(CtClass class1 : classList) {
			//graphDeDependances.add(mutNode(class1.getSimpleName()));
			Set<CtMethod> methods = class1.getMethods();
			//AMODIFIER POUR VOIR APRES DIFF
			for(CtMethod method : methods){
				mapMethds.put(method.getSimpleName().trim(), method);
				//System.out.println(method.getSimpleName());
				}
		}
		
		
		
		HashMap<String, Integer> maprelation = new HashMap<String, Integer>();
		
		for(CtClass class1 : classList) {
			//graphDeDependances.add(mutNode(class1.getSimpleName()));
			Set<CtMethod> methods = class1.getMethods();
			//AMODIFIER POUR VOIR APRES DIFF
			for(CtMethod method : methods) {
				for(CtInvocation invocation : method.getElements(new TypeFilter<CtInvocation>(CtInvocation.class))){
					String mthName = invocation.toString();
					String[ ] mthNames = mthName.split("\\.");
					if(mthNames.length > 1) {
						mthName = mthNames[1];
					}else if(mthNames.length > 0){
						mthName = mthNames[0];
					}
					mthName = mthName.split("\\(")[0];
				
					//System.out.println(mthName);
					if(mapMethds.containsKey(mthName)) {
						CtMethod ctMethod2 = mapMethds.get(mthName);
						CtClass class2 = ctMethod2.getParent(CtClass.class);
						
						
						if(!class1.getSimpleName().contentEquals(class2.getSimpleName())) {
							String cle = class1.getSimpleName()+"="+class2.getSimpleName();
							if(maprelation.containsKey(cle)) {
								int poids = maprelation.get(cle);
								maprelation.put(cle, poids+1);
							}else {
								maprelation.put(cle, 1);
							}

					}}
				}	
			}
		}
		
		
		PrintWriter pr = new PrintWriter(new FileWriter("sortie-de-graphe.dot")); 
		
		pr.println("digraph G {");
		
		for(String cle : maprelation.keySet()) {
			String nomCls1 = cle.split("=")[0];
			String nomCls2 = cle.split("=")[1];
			
			int poids = maprelation.get(cle);
			
			System.out.println(nomCls1+" ==> "+nomCls2+" Métrique : "+poids);
			
			pr.println(nomCls1+" -> "+nomCls2+" [label=\" "+poids+" \"]; ");
			
			graphDeDependances.add(mutNode(nomCls1).addLink(mutNode(nomCls2)));
		}
		
		
		pr.println("}");
		pr.close();
		
		
		try {
			Graphviz.fromGraph(graphDeDependances).width(7500)/*.height(5000)*/.render(Format.PNG).toFile(new File("./graphe_de_dependances.png"));
			Graphviz.fromGraph(graphDeDependances).width(7500)/*.height(5000)*/.render(Format.DOT).toFile(new File("./graphe_de_dependances.dot"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
