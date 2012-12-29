package at.ac.univie.mminf.qskos4j.issues;

import at.ac.univie.mminf.qskos4j.QSkos;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;


public class OmittedOrInvalidLanguageTagTest extends IssueTestCase {

	private QSkos qSkosComponents, qSkosDeprecatedAndIllegal;
	
	@Before
	public void setUp() throws OpenRDFException, IOException {
		qSkosComponents = setUpInstance("components.rdf");
		qSkosDeprecatedAndIllegal = setUpInstance("deprecatedAndIllegalTerms.rdf");
	}
	
	@Test
	public void testMissingLangTagCount_1() throws OpenRDFException {
		Map<Resource, Collection<Literal>> missingLangTags = qSkosComponents.findOmittedOrInvalidLanguageTags().getData();
		
		Assert.assertEquals(2, missingLangTags.size());
	}
	
	@Test
	public void testMissingLangTagCount_2() throws OpenRDFException {
		Map<Resource, Collection<Literal>> missingLangTags = qSkosDeprecatedAndIllegal.findOmittedOrInvalidLanguageTags().getData();
		
		Assert.assertEquals(1, missingLangTags.keySet().size());
		Assert.assertEquals(2, countEntries(missingLangTags.values()));
	}
	
	private int countEntries(Collection<Collection<Literal>> allLiterals) {
		int literalCount = 0;
		for (Collection<Literal> literals : allLiterals) {
			literalCount += literals.size();
		}
		return literalCount;
	}
}
