import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file ImmPortSQL.java
 * @author Edison Ong
 * @since Nov 13, 2019
 * @version 1.0
 * @comment 
 */

/**
 * 
 */
public class ImmPortSQL {
	
	protected Connection connection;
	
	protected static final Logger logger = LoggerFactory
			.getLogger(ImmPortSQL.class);
	
	public ImmPortSQL( String username, String password ) {
		try {
			this.connection = DriverManager.getConnection("jdbc:mysql://172.20.30.209/immport_shared_data", username, password);
		} catch (SQLException e) {
			logger.error( "Unable to connect ImmPort Database: {}", e.getMessage() );
			System.exit( -1 );
		}
	}
	
	public ArrayList<String> getAllHumanSubjectFromStudy( String studyID ) {
		ArrayList<String> subjects = new ArrayList<String>();
		String sql = "SELECT DISTINCT biosample.subject_accession FROM biosample "
				   + "LEFT JOIN subject ON biosample.subject_accession = subject.subject_accession "
				   + "WHERE study_accession = '" + studyID + "' AND species = 'Homo sapiens';";
		
		try {
			Statement statement = this.connection.createStatement();
			ResultSet result = statement.executeQuery( sql );
			while ( result.next() ) {
				subjects.add( result.getString( "subject_accession" ) );
			}
		} catch (SQLException e) {
			logger.error( "Unable to query ImmPort Database: {}", e.getMessage() );
			System.exit( -1 );
		}
		
		return subjects;
	}
	
	public String getVaccineFromSubject( String subjectID ) {
		ArrayList<String> vaccines = new ArrayList<String>();
		String sql = "SELECT DISTINCT immune_exposure.exposure_material_id FROM immune_exposure "
				   + "LEFT JOIN biosample ON immune_exposure.subject_accession = biosample.subject_accession "
				   + "LEFT JOIN subject ON biosample.subject_accession = subject.subject_accession "
				   + "WHERE biosample.subject_accession = '" + subjectID + "' AND species = 'Homo sapiens';";
		
		try {
			Statement statement = this.connection.createStatement();
			ResultSet result = statement.executeQuery( sql );
			while ( result.next() ) {
				vaccines.add( result.getString( "exposure_material_id" ) );
			}
		} catch (SQLException e) {
			logger.error( "Unable to query ImmPort Database: {}", e.getMessage() );
			System.exit( -1 );
		}
		
		if ( vaccines.size() == 1 ) {
			return vaccines.get(0);
		} else {
			logger.error( "Unable to query unique vaccine: {}", subjectID );
			System.exit( -1 );
			return null;
		}
	}
	
	public String getAgeFromSubject( String subjectID ) {
		String sql = "SELECT min_subject_age FROM immport_shared_data.arm_2_subject WHERE subject_accession = '" + subjectID + "';";
		
		try {
			Statement statement = this.connection.createStatement();
			ResultSet result = statement.executeQuery( sql );
			while ( result.next() ) {
				return result.getString( "min_subject_age" );
			}
		} catch (SQLException e) {
			logger.error( "Unable to query ImmPort Database: {}", e.getMessage() );
			System.exit( -1 );
		}
		return null;
		
	}
	
	public ArrayList<String> getAllBiosampleFromHumanSubjectInStudy( String subjectID, String studyID ) {
		ArrayList<String> biosamples = new ArrayList<String>();
		String sql = "SELECT DISTINCT biosample.biosample_accession FROM biosample "
				   + "LEFT JOIN subject ON biosample.subject_accession = subject.subject_accession "
				   + "WHERE biosample.subject_accession = '" + subjectID + "'AND species = 'Homo sapiens' "
				   + "AND study_accession = '" + studyID + "';";
		
		try {
			Statement statement = this.connection.createStatement();
			ResultSet result = statement.executeQuery( sql );
			while ( result.next() ) {
				biosamples.add( result.getString( "biosample_accession" ) );
			}
		} catch (SQLException e) {
			logger.error( "Unable to query ImmPort Database: {}", e.getMessage() );
			System.exit( -1 );
		}
		
		return biosamples;
	}
	
	public ArrayList<String> getAllExpsampleFromBiosample( String biosampleID ) {
		ArrayList<String> expsamples = new ArrayList<String>();
		String sql = "SELECT DISTINCT expsample_accession FROM expsample_2_biosample WHERE biosample_accession = '" + biosampleID + "';";
		
		try {
			Statement statement = this.connection.createStatement();
			ResultSet result = statement.executeQuery( sql );
			while ( result.next() ) {
				expsamples.add( result.getString( "expsample_accession" ) );
			}
		} catch (SQLException e) {
			logger.error( "Unable to query ImmPort Database: {}", e.getMessage() );
			System.exit( -1 );
		}
		
		return expsamples;
	}
	
	public String getGSMFromExpsample( String expsampleID ) {
		String sql = "SELECT DISTINCT repository_accession FROM expsample_public_repository WHERE expsample_accession = '" + expsampleID + "';";
		
		try {
			Statement statement = this.connection.createStatement();
			ResultSet result = statement.executeQuery( sql );
			while ( result.next() ) {
				String gsm = result.getString( "repository_accession" );
				if ( gsm.startsWith( "GSM" ) ) {
					return gsm;
				}
			}
		} catch (SQLException e) {
			logger.error( "Unable to query ImmPort Database: {}", e.getMessage() );
			System.exit( -1 );
		}
		return null;
	}

}
