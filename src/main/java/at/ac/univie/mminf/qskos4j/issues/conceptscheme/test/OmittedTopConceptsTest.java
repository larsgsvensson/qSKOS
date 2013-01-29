package at.ac.univie.mminf.qskos4j.issues.conceptscheme.test;

import at.ac.univie.mminf.qskos4j.issues.conceptscheme.ConceptSchemes;
import at.ac.univie.mminf.qskos4j.issues.conceptscheme.OmittedTopConcepts;
import at.ac.univie.mminf.qskos4j.util.IssueTestCase;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.OpenRDFException;

import java.io.IOException;


public class OmittedTopConceptsTest extends IssueTestCase {

	private OmittedTopConcepts omittedTopConcepts;
	
	@Before
	public void setUp() throws OpenRDFException, IOException {
        omittedTopConcepts = (OmittedTopConcepts) setUpRepository(
                "missingTopConcepts.rdf",
                new OmittedTopConcepts(new ConceptSchemes()));
	}
	
	@Test
	public void testConceptSchemesWithoutTopConceptsCount() throws OpenRDFException {
		Assert.assertEquals(2, omittedTopConcepts.getResult().getData().size());
	}
	
}
