/**
 * @file AddImmPortStudy.java
 * @author Edison Ong
 * @since Nov 13, 2019
 * @version 1.0
 * @comment 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 */
public class AddImmPortStudy {
	/*
	 * args4j options declaration
	 */
	@Option(
			name = "-s",
			usage = "Immport study accession.",
			required = true,
			aliases = {"--study_id"}
			)
	protected String studyID;
	@Option(
			name = "-i",
			usage = "VIO OWL input file.",
			required = true,
			aliases = {"--input_vio"}
			)
	protected String inputVIO;
	@Option(
			name = "-o",
			usage = "VIO OWL output file.",
			required = true,
			aliases = {"--output_vio"}
			)
	protected String outputVIO;
	@Option(
			name = "-u",
			usage = "DB username.",
			required = false
			)
	protected String username;
	@Option(
			name = "-p",
			usage = "DB password.",
			required = false
			)
	protected String password;
	
	public static final String BASE_IRI_STR = "http://purl.obolibrary.org/obo/";
	public static final String ONTOLOGY_IRI_STR = "http://purl.obolibrary.org/obo/vo/vio-immport.owl";
	
	public static final String STUDY_IRI_STR = BASE_IRI_STR + "OBI_0000066";
	public static final String SUBJECT_IRI_STR = BASE_IRI_STR + "OPMI_humansubject";
	public static final String BIOSAMPLE_IRI_STR = BASE_IRI_STR + "OBI_0100051";
	public static final String EXPSAMPLE_IRI_STR = BASE_IRI_STR + "OPMI_experimentsample";
	public static final String GSM_IRI_STR = BASE_IRI_STR + "OPMI_0000386";
	
	public static final String PARTICIPATES_IN_IRI_STR = BASE_IRI_STR + "RO_0000056";
	public static final String VACCINATED_WITH_IRI_STR = BASE_IRI_STR + "VO_vaccinatedwith";
	public static final String HAS_AGE_IRI_STR = BASE_IRI_STR + "OPMI_hasage";
	public static final String PART_OF_IRI_STR = BASE_IRI_STR + "BFO_0000050";
	public static final String IS_ABOUT_IRI_STR = BASE_IRI_STR + "IAO_0000136";
	
	protected OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	protected OWLDataFactory dataFactory = manager.getOWLDataFactory();
	protected OWLOntology ontology;
	ImmPortSQL immportSQL;
	
	protected static final Logger logger = LoggerFactory
			.getLogger(AddImmPortStudy.class);
	
	public static void main( String[] args ) {
		new AddImmPortStudy().run( args );
	}
	
	public void run( String[] args ) {
		CmdLineParser parser = new CmdLineParser( this );
		
		// Load arguments
		try {
			parser.parseArgument( args );
		} catch( CmdLineException e ) {
			logger.error( "Incorrect arguments" );
            parser.printUsage( System.err );
            System.exit( -1 );
		}
		
		// Load VIO
		try {
			logger.info( "Loading ontology from document: " + inputVIO );
			this.ontology = this.manager.loadOntologyFromOntologyDocument( new File ( inputVIO ) );
		} catch ( OWLOntologyCreationException e ) {
			logger.error( "Fail to load ontology from document: " + inputVIO, e );
			System.exit( -1 );
		}
		logger.info( "Loaded ontology");
		
		if ( username == null && password == null ) {
			java.io.Console console = System.console();
			String username = console.readLine( "DB username: " );
			String password = new String(console.readPassword( "DB password" ) );
		}
		
		this.immportSQL = new ImmPortSQL( username, password );
		
		this.addStudyInstance( studyID );
		
		ArrayList<String> subjectIDs = immportSQL.getAllHumanSubjectFromStudy( studyID );
		
		for ( String subjectID : subjectIDs ) {
			this.addSubjectInstance( subjectID, studyID );
			
			ArrayList<String> biosampleIDs = immportSQL.getAllBiosampleFromHumanSubjectInStudy( subjectID, studyID );
			for ( String biosampleID : biosampleIDs ) {
				this.addBiosampleInstance( biosampleID, subjectID );
				
				ArrayList<String> expsampleIDs = immportSQL.getAllExpsampleFromBiosample( biosampleID );
				for ( String expsampleID : expsampleIDs ) {
					this.addExpsampleInstance( expsampleID, biosampleID );
				}
			}
		}
		
		try {
			this.manager.setOntologyDocumentIRI( this.ontology, IRI.create( ONTOLOGY_IRI_STR ) );
			this.manager.saveOntology( this.ontology, new FileOutputStream( outputVIO ) );
		} catch (OWLOntologyStorageException e) {
			logger.error( "Fail to store ontology " + inputVIO, e );
			System.exit( -1 );
		} catch (FileNotFoundException e) {
			logger.error( "Fail to find output document: " + inputVIO, e );
			System.exit( -1 );
		}
	}
		
	protected void addStudyInstance( String instanceID ) {
		IRI instanceIRI =  IRI.create( BASE_IRI_STR + "VIO_" + instanceID );
		
		OWLNamedIndividual instance = dataFactory.getOWLNamedIndividual( instanceIRI );
		
		OWLClass parent = dataFactory.getOWLClass( IRI.create( STUDY_IRI_STR ) );
		OWLAxiom axiom1 = dataFactory.getOWLClassAssertionAxiom( parent, instance );
		manager.applyChange( new AddAxiom( ontology, axiom1 ) );

		
		OWLAnnotationProperty labelProperty = dataFactory.getOWLAnnotationProperty(
				OWLRDFVocabulary.RDFS_LABEL.getIRI() );
		OWLAnnotation labelAnnotation = dataFactory.getOWLAnnotation(
				labelProperty, dataFactory.getOWLLiteral( "study " + instanceID ) );
		OWLAxiom axiom2 = dataFactory.getOWLAnnotationAssertionAxiom( instanceIRI, labelAnnotation );
		manager.applyChange( new AddAxiom( ontology, axiom2 ) );
		
		logger.info( String.format( 
				"generated new instance 'study %s' to class 'investigation(OBI_0000066)'", instanceID ) );

	}
	
	protected void addSubjectInstance( String subjectID, String studyID ) {
		IRI subjectIRI =  IRI.create( BASE_IRI_STR + "VIO_" + subjectID );
		IRI studyIRI =  IRI.create( BASE_IRI_STR + "VIO_" + studyID );
		
		OWLNamedIndividual subject = dataFactory.getOWLNamedIndividual( subjectIRI );
		OWLNamedIndividual study = dataFactory.getOWLNamedIndividual( studyIRI );
		
		OWLClass parent = dataFactory.getOWLClass( IRI.create( SUBJECT_IRI_STR ) );
		OWLAxiom axiom1 = dataFactory.getOWLClassAssertionAxiom( parent, subject );
		manager.applyChange( new AddAxiom( ontology, axiom1 ) );
		
		OWLAnnotationProperty labelProperty = dataFactory.getOWLAnnotationProperty(
				OWLRDFVocabulary.RDFS_LABEL.getIRI() );
		OWLAnnotation labelAnnotation = dataFactory.getOWLAnnotation(
				labelProperty, dataFactory.getOWLLiteral( "human subject " + subjectID ) );
		OWLAxiom axiom2 = dataFactory.getOWLAnnotationAssertionAxiom( subjectIRI, labelAnnotation );
		manager.applyChange( new AddAxiom( ontology, axiom2 ) );
		
		OWLObjectProperty participatesIn = dataFactory.getOWLObjectProperty( IRI.create( PARTICIPATES_IN_IRI_STR ) );
		OWLAxiom axiom3 = dataFactory.getOWLObjectPropertyAssertionAxiom(participatesIn, subject, study );
		manager.applyChange( new AddAxiom( ontology, axiom3 ) );
		
		String vaccineID = this.immportSQL.getVaccineFromSubject( subjectID );
		if ( vaccineID.startsWith( "VO" ) ) {
			IRI vaccineIRI = IRI.create( BASE_IRI_STR + vaccineID );
			OWLClass vaccine = dataFactory.getOWLClass( vaccineIRI );
			OWLObjectProperty vaccinatedWith = dataFactory.getOWLObjectProperty( IRI.create( VACCINATED_WITH_IRI_STR ) );
			OWLObjectSomeValuesFrom vaccinatedWithValue = dataFactory.getOWLObjectSomeValuesFrom( vaccinatedWith, vaccine );
			OWLAxiom axiom4 = dataFactory.getOWLClassAssertionAxiom( vaccinatedWithValue, subject );
			manager.applyChange( new AddAxiom( ontology, axiom4 ) );
		}
		
		String age = this.immportSQL.getAgeFromSubject( subjectID );
		OWLDataProperty hasAge = dataFactory.getOWLDataProperty( IRI.create( HAS_AGE_IRI_STR ) );
		OWLDataPropertyAssertionAxiom axiom5 = dataFactory.getOWLDataPropertyAssertionAxiom( hasAge, subject, (int) Double.parseDouble( age ) );
		manager.applyChange( new AddAxiom( ontology, axiom5 ) );
		
		logger.info( String.format( 
				"generated new instance 'human subject %s' to class 'human subject(OPMI_humansubject)'", subjectID ) );

	}
	
	protected void addBiosampleInstance( String biosampleID, String subjectID ) {
		IRI biosampleIRI =  IRI.create( BASE_IRI_STR + "VIO_" + biosampleID );
		IRI subjectIRI =  IRI.create( BASE_IRI_STR + "VIO_" + subjectID );
		
		OWLNamedIndividual biosample = dataFactory.getOWLNamedIndividual( biosampleIRI );
		OWLNamedIndividual subject = dataFactory.getOWLNamedIndividual( subjectIRI );
		
		OWLClass parent = dataFactory.getOWLClass( IRI.create( BIOSAMPLE_IRI_STR ) );
		OWLAxiom axiom1 = dataFactory.getOWLClassAssertionAxiom( parent, biosample );
		manager.applyChange( new AddAxiom( ontology, axiom1 ) );
		
		OWLAnnotationProperty labelProperty = dataFactory.getOWLAnnotationProperty(
				OWLRDFVocabulary.RDFS_LABEL.getIRI() );
		OWLAnnotation labelAnnotation = dataFactory.getOWLAnnotation(
				labelProperty, dataFactory.getOWLLiteral( "biosample " + biosampleID ) );
		OWLAxiom axiom2 = dataFactory.getOWLAnnotationAssertionAxiom( biosampleIRI, labelAnnotation );
		manager.applyChange( new AddAxiom( ontology, axiom2 ) );
		
		OWLObjectProperty partOf = dataFactory.getOWLObjectProperty( IRI.create( PART_OF_IRI_STR ) );
		OWLAxiom axiom3 = dataFactory.getOWLObjectPropertyAssertionAxiom( partOf, biosample, subject );
		manager.applyChange( new AddAxiom( ontology, axiom3 ) );

		logger.info( String.format( 
				"generated new instance 'biosample %s' to class 'specimen(OBI_0100051)'", biosampleID ) );

	}
	
	protected void addExpsampleInstance( String expsampleID, String biosampleID ) {
		IRI expsampleIRI =  IRI.create( BASE_IRI_STR + "VIO_" + expsampleID );
		IRI biosampleIRI =  IRI.create( BASE_IRI_STR + "VIO_" + biosampleID );
		
		OWLNamedIndividual expsample = dataFactory.getOWLNamedIndividual( expsampleIRI );
		OWLNamedIndividual biosample = dataFactory.getOWLNamedIndividual( biosampleIRI );
		
		OWLClass parent = dataFactory.getOWLClass( IRI.create( EXPSAMPLE_IRI_STR ) );
		OWLAxiom axiom1 = dataFactory.getOWLClassAssertionAxiom( parent, expsample );
		manager.applyChange( new AddAxiom( ontology, axiom1 ) );

		
		OWLAnnotationProperty labelProperty = dataFactory.getOWLAnnotationProperty(
				OWLRDFVocabulary.RDFS_LABEL.getIRI() );
		OWLAnnotation labelAnnotation = dataFactory.getOWLAnnotation(
				labelProperty, dataFactory.getOWLLiteral( "expsample " + expsampleID ) );
		OWLAxiom axiom2 = dataFactory.getOWLAnnotationAssertionAxiom( expsampleIRI, labelAnnotation );
		manager.applyChange( new AddAxiom( ontology, axiom2 ) );
		
		OWLObjectProperty partOf = dataFactory.getOWLObjectProperty( IRI.create( PART_OF_IRI_STR ) );
		OWLAxiom axiom3 = dataFactory.getOWLObjectPropertyAssertionAxiom( partOf, expsample, biosample );
		manager.applyChange( new AddAxiom( ontology, axiom3 ) );
		
		String gsmID = this.immportSQL.getGSMFromExpsample( expsampleID );
		if ( gsmID != null ) {
			OWLNamedIndividual gsm = dataFactory.getOWLNamedIndividual( IRI.create( BASE_IRI_STR + gsmID ) );
			OWLClass gsmClass = dataFactory.getOWLClass( IRI.create( GSM_IRI_STR ) );
			OWLAxiom axiom4 = dataFactory.getOWLClassAssertionAxiom( gsmClass, gsm );
			manager.applyChange( new AddAxiom( ontology, axiom4 ) );
			
			OWLObjectProperty isAbout = dataFactory.getOWLObjectProperty( IRI.create( IS_ABOUT_IRI_STR ) );
			OWLAxiom axiom5 = dataFactory.getOWLObjectPropertyAssertionAxiom( isAbout, gsm, expsample );
			manager.applyChange( new AddAxiom( ontology, axiom5 ) );
		}
		
		logger.info( String.format( 
				"generated new instance 'expsample %s' to class 'experiment sample(OPMI_experimentsample)'", expsampleID ) );

	}
}
